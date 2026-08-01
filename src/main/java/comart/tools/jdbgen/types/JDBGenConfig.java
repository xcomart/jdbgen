/*
 * MIT License
 * 
 * Copyright (c) 2020 Dennis Soungjin Park
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package comart.tools.jdbgen.types;

import comart.tools.jdbgen.types.maven.MavenConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import comart.utils.ObjUtils;
import comart.utils.StrUtils;
import comart.utils.UIUtils;
import java.awt.Container;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author comart
 */
@Slf4j
@Data
public class JDBGenConfig {
    private static final String CONF_PATH = "config.json";
    private static JDBGenConfig INSTANCE = null;
    private boolean isDarkUI = false;
    private List<JDBConnection> connections;
    private List<JDBDriver> drivers;
    private List<JDBPreset> presets;
    private List<JDBAbbr> abbrs = new ArrayList<>();
    private MavenConfig maven;
    private boolean applyAbbr = false;

    public static JDBGenConfig getInstance() {
        return getInstance(false);
    }
    
    public static synchronized JDBGenConfig getInstance(boolean useDefault) {
        if (INSTANCE == null) {
            log.info("config path: {}", CONF_PATH);
            File f = new File(CONF_PATH);
            Gson gson = new Gson();
            if (!useDefault) {
                for (int cnt=0; cnt < 3; cnt++) {
                    boolean isNew = !(f.exists() && f.isFile());
                    String message = isNew ? "Enter new master password": "Enter master password";
                    String master = UIUtils.password(message, isNew);
                    if (master == null)
                        System.exit(1);
                    StrUtils.setMaster(master);
                    if (f.exists() && f.isFile()) {
                        try (FileReader fr = new FileReader(f, StandardCharsets.UTF_8)) {
                            INSTANCE = (JDBGenConfig)gson.fromJson(fr, JDBGenConfig.class);
                            break;
                        } catch (Exception e) {
                            if (cnt < 2) {
                                UIUtils.error(null, "Password Incorrect!");
                            } else {
                                boolean isOk = UIUtils.confirm(null, "Configuration Error",
                                        "Loading configuration failed - Password may not correct : " +
                                        e.getLocalizedMessage() +
                                        "\nDo you want to load default configuration?");
                                if (!isOk) {
                                    System.exit(1);
                                }
                                master = UIUtils.password("Enter new master password", true);
                                if (master == null)
                                    System.exit(1);
                                StrUtils.setMaster(master);
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

            if (INSTANCE == null) {
                log.info("config file not found or not loadable, creating default one.");
                // never overwrite an unreadable configuration without a copy of it
                backupExistingConfig(f);

                try (InputStreamReader ir = new InputStreamReader(
                        JDBGenConfig.class.getResourceAsStream("/defaultConfig.json"), StandardCharsets.UTF_8)) {
                    INSTANCE = (JDBGenConfig)gson.fromJson(ir, JDBGenConfig.class);
                    
                    // create sample connection with H2 Embedded
                    JDBConnection jcon = new JDBConnection();
                    jcon.setAuthor(ObjUtils.getLoginUserId());
                    jcon.setConnectionProps(new HashMap<>());
                    jcon.setConnectionUrl("jdbc:h2:./sample_h2.db");
                    jcon.setDriverType("H2 Embedded");
                    jcon.setIcon("stock:h2.png");
                    jcon.setName("Sample H2 Embedded");
                    jcon.setOutputDir("output");
                    List<JDBTemplate> templates = new ArrayList<>(Arrays.asList(
                        new JDBTemplate("Java Model", "templates/java_model.java", "${name.suffix.pascal}Model.java"),
                        new JDBTemplate("MyBatis mapper", "templates/mybatis_mapper.xml", "${name.suffix.camel}-mapper.xml"),
                        new JDBTemplate("PHP CI Model", "templates/php_ci.php", "${name.suffix.lower}_ci_model.php")
                    ));
                    jcon.setTemplates(templates);
                    INSTANCE.connections = new ArrayList<>(Arrays.asList(jcon));
                    if (!useDefault)
                        saveInstance(null);
                } catch (Exception e) {
                    UIUtils.error(null, "Cannot load default configuration: " + e.getLocalizedMessage());
                    log.error("cannot recover previous error.", e);
                    System.exit(1);
                }
            }
            normalize(INSTANCE);

            // passwords stored by an older release use a weaker scheme; rewrite
            // the whole configuration so that they are upgraded in place.
            if (!useDefault && StrUtils.hasLegacyEncryption()) {
                log.info("configuration contains passwords in the superseded encryption format, re-encrypting.");
                if (saveInstance(null))
                    StrUtils.clearLegacyEncryption();
            }
        }

        return INSTANCE;
    }

    /**
     * move an existing, unloadable configuration file aside so that writing a
     * fresh default configuration cannot destroy the user's data.
     */
    private static void backupExistingConfig(File f) {
        if (!(f.exists() && f.isFile()))
            return;
        Path backup = Paths.get(CONF_PATH + "." +
                StrUtils.dateFormat("yyyyMMdd_HHmmss") + ".bak");
        try {
            Files.move(f.toPath(), backup, StandardCopyOption.REPLACE_EXISTING);
            log.warn("existing configuration could not be loaded, backed up to '{}'", backup);
        } catch (IOException e) {
            log.error("cannot back up existing configuration '" + CONF_PATH + "'", e);
        }
    }

    /**
     * fill in collections omitted from the configuration file, so that callers
     * never have to null-check them.
     */
    private static void normalize(JDBGenConfig conf) {
        if (conf == null)
            return;
        if (conf.connections == null) conf.connections = new ArrayList<>();
        if (conf.drivers == null) conf.drivers = new ArrayList<>();
        if (conf.presets == null) conf.presets = new ArrayList<>();
        if (conf.abbrs == null) conf.abbrs = new ArrayList<>();
        conf.connections.forEach(c -> {
            if (c.getTemplates() == null) c.setTemplates(new ArrayList<>());
            if (c.getCustomVars() == null) c.setCustomVars(new LinkedHashMap<>());
            if (c.getConnectionProps() == null) c.setConnectionProps(new LinkedHashMap<>());
        });
        conf.drivers.forEach(d -> {
            if (d.getProps() == null) d.setProps(new LinkedHashMap<>());
        });
        conf.presets.forEach(p -> {
            if (p.getTemplates() == null) p.setTemplates(new ArrayList<>());
        });
    }

    public static synchronized boolean saveInstance(Container parent) {
        Gson gson = (new GsonBuilder()).setPrettyPrinting().create();

        try {
            String json = gson.toJson(INSTANCE);
            try (FileWriter fw = new FileWriter(CONF_PATH, StandardCharsets.UTF_8)) {
                fw.write(json);
            }
            return true;
        } catch (IOException e) {
            UIUtils.error(null, "Cannot save configuration: " + e.getLocalizedMessage());
        }

        return false;
    }
}

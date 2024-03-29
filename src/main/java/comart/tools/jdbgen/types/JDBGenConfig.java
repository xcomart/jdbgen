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
import comart.utils.StrUtils;
import comart.utils.UIUtils;
import java.awt.Container;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Data;

/**
 *
 * @author comart
 */
@Data
public class JDBGenConfig {
    private static final Logger logger = Logger.getLogger(JDBGenConfig.class.getName());
    private static final String CONF_PATH = "config.json";
    public static byte[] KEY = null;
    public static byte[] IV = null;
    private static JDBGenConfig INSTANCE = null;
    private boolean isDarkUI = false;
    private List<JDBConnection> connections;
    private List<JDBDriver> drivers;
    private List<JDBPreset> presets;
    private MavenConfig maven;

    public static synchronized JDBGenConfig getInstance() {
        if (INSTANCE == null) {
            String master = UIUtils.password("Enter master password");
            if (master == null)
                System.exit(1);
            StrUtils.setMaster(master);
            logger.info("config path: "+CONF_PATH);
            Gson gson = new Gson();
            File f = new File(CONF_PATH);
            if (f.exists() && f.isFile()) {
                try {
                    INSTANCE = (JDBGenConfig)gson.fromJson(new FileReader(f), JDBGenConfig.class);
                } catch (Exception e) {
                    boolean isOk = UIUtils.confirm(null, "Configuration Error",
                            "Loading configuration failed - Password may not correct : " +
                            e.getLocalizedMessage() +
                            "\nDo you want to load default configuration?");
                    if (!isOk) {
                        System.exit(1);
                    }
                }
            }

            if (INSTANCE == null) {
                logger.info("config file not found, creating default one.");

                try (InputStream is = JDBGenConfig.class.getResourceAsStream("/defaultConfig.json")) {
                    INSTANCE = (JDBGenConfig)gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JDBGenConfig.class);
                } catch (Exception e) {
                    UIUtils.error(null, "Cannot load default configuration: " + e.getLocalizedMessage());
                    logger.log(Level.SEVERE, "cannot recover previous error.", e);
                    System.exit(1);
                }
            }
        }

        return INSTANCE;
    }

    public static synchronized boolean saveInstace(Container parent) {
        Gson gson = (new GsonBuilder()).setPrettyPrinting().create();

        try {
            String json = gson.toJson(INSTANCE);
//            System.out.println(json);
            try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(CONF_PATH), StandardCharsets.UTF_8)) {
                fw.write(json);
            }
            return true;
        } catch (IOException e) {
            UIUtils.error(null, "Cannot save configuration: " + e.getLocalizedMessage());
        }

        return false;
    }
}

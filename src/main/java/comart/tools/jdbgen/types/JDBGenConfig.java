/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types;

import comart.tools.jdbgen.types.maven.MavenConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private static JDBGenConfig INSTANCE = null;
    private boolean isDarkUI = false;
    private List<JDBConnection> connections;
    private List<JDBDriver> drivers;
    private MavenConfig maven;

    public static synchronized JDBGenConfig getInstance() {
        if (INSTANCE == null) {
            Gson gson = new Gson();
            File f = new File(CONF_PATH);
            if (f.exists() && f.isFile()) {
                try {
                    INSTANCE = (JDBGenConfig)gson.fromJson(new FileReader(f), JDBGenConfig.class);
                } catch (Exception e) {
                    UIUtils.error(null, "Loading configuration failed: " +
                            e.getLocalizedMessage() +
                            "\nTrying to load default configuration.");
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
            System.out.println(json);
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

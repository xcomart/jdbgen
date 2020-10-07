/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.Component;
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
import javax.swing.JOptionPane;
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

    public static synchronized JDBGenConfig getInstance() {
        if (INSTANCE == null) {
            Gson gson = new Gson();
            File f = new File(CONF_PATH);
            if (f.exists() && f.isFile()) {
                try {
                    INSTANCE = (JDBGenConfig)gson.fromJson(new FileReader(f), JDBGenConfig.class);
                } catch (Exception var15) {
                    logger.log(Level.SEVERE, "cannot load config file.", var15);
                    JOptionPane.showMessageDialog((Component)null, "Loading configuration failed: " + var15.getLocalizedMessage(), "Load failed", 64);
                }
            }

            if (INSTANCE == null) {
                logger.info("config file not found, creating default one.");

                try {
                    InputStream is = JDBGenConfig.class.getResourceAsStream("/defaultConfig.json");
                    Throwable var3 = null;

                    try {
                        INSTANCE = (JDBGenConfig)gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JDBGenConfig.class);
                    } catch (Throwable var14) {
                        var3 = var14;
                        throw var14;
                    } finally {
                        if (is != null) {
                            if (var3 != null) {
                                try {
                                    is.close();
                                } catch (Throwable var13) {
                                    var3.addSuppressed(var13);
                                }
                            } else {
                                is.close();
                            }
                        }

                    }
                } catch (Exception var17) {
                    logger.log(Level.SEVERE, "cannot load config file.", var17);
                    JOptionPane.showMessageDialog((Component)null, "Cannot load default configuration: " + var17.getLocalizedMessage(), "Load failed", 64);
                    logger.log(Level.SEVERE, "cannot recover previous error.", var17);
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
        } catch (IOException var16) {
            logger.log(Level.SEVERE, "cannot save config file.", var16);
        }

        return false;
    }
}

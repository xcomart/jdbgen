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

package comart.utils;

import com.google.gson.Gson;
import java.awt.desktop.AboutHandler;
import java.awt.desktop.PreferencesHandler;
import java.awt.desktop.PrintFilesHandler;
import java.awt.desktop.QuitHandler;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.Request;
import okhttp3.Response;

public class PlatformUtils {
    private static OSType detectedOS = null;
    private static final Logger logger = Logger.getLogger(PlatformUtils.class.getName());

    public PlatformUtils() {
    }

    public synchronized static OSType getOSType() {
        if (detectedOS == null) {
            String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if (!OS.contains("mac") && !OS.contains("darwin")) {
                if (OS.contains("win")) {
                    detectedOS = OSType.Windows;
                } else if (!OS.contains("ux") && !OS.contains("ix")) {
                    detectedOS = OSType.Other;
                } else {
                    detectedOS = OSType.Unix;
                }
            } else {
                detectedOS = OSType.MacOS;
            }
        }

        return detectedOS;
    }

    public static void openURL(String url) {
        Desktop desk = Desktop.getDesktop();
        
        try {
            desk.browse(new URL(url).toURI());
        } catch (Exception e) {
            logger.log(Level.SEVERE, (String)null, e);
        }

    }
    
    private static String getJava() {
        ProcessHandle processHandle = ProcessHandle.current();
        return processHandle.info().command().get();
    }
    
    private static String _version = null;
    
    public synchronized static String getVersion() {
        if (_version == null) {
            Properties prop = new Properties();
            try (InputStreamReader isr = new InputStreamReader(PlatformUtils.class.getResourceAsStream("/version.properties"))) {
                prop.load(isr);
                _version = prop.getProperty("version");
            } catch (Exception e) {
                logger.log(Level.SEVERE, null, e);
            }
        }
        return _version;
    }

    public static void openDoc(String item) {
        String version = getVersion();
        String docUrl = "https://github.com/xcomart/jdbgen/blob/v"+version+"/docs/README.md#"+item;
        openURL(docUrl);
    }

    public static boolean isWindows() {
        return getOSType() == OSType.Windows;
    }

    public static boolean isMac() {
        return getOSType() == OSType.MacOS;
    }

    public static boolean isUnix() {
        return getOSType() == OSType.Unix;
    }

    public static enum OSType {
        Windows,
        MacOS,
        Unix,
        Other;

        private OSType() {
        }
    }
    
    public static void registerHandlers(AboutHandler about,
            PreferencesHandler prefs, PrintFilesHandler print, QuitHandler shut) {
        try {
            Desktop desk = Desktop.getDesktop();

            if (about != null) {
                desk.setAboutHandler(about);
            }

            if (prefs != null) {
                desk.setPreferencesHandler(prefs);
            }

            if (print != null) {
                desk.setPrintFileHandler(print);
            }

            if (shut != null) {
                desk.setQuitHandler(shut);
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "", e);
        }
    }
    
    public static void setDockIcon() {
        try {
            final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
            final URL imageResource = new File("resource/icon.png").toURI().toURL();
            final Image image = defaultToolkit.getImage(imageResource);
            final Taskbar taskbar = Taskbar.getTaskbar();
            taskbar.setIconImage(image);
        } catch(Exception ignored) {
        }
    }
    
    public static void updateCheck() {
        String curVersion = "v"+getVersion();
        String url = "https://api.github.com/repos/xcomart/jdbgen/releases/latest";
        Request req = new Request.Builder().url(url).build();
        try (Response response = HttpUtils.getClient().newCall(req).execute()) {
            Gson gson = new Gson();
            HashMap map = gson.fromJson(response.body().charStream(), HashMap.class);
            String tagName = String.valueOf(map.get("tag_name"));
            if (tagName != null && curVersion.compareTo(tagName) < 0) {
                // updates available
                if (UIUtils.confirm(null, "Update Available", "New version "+tagName+
                        " is available.\nDo you want to update now?")) {
                    // TODO: do update automatically
                    openURL("https://github.com/xcomart/jdbgen/releases/latest");
                    System.exit(0);
                }
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, null, e);
        }
    }
}

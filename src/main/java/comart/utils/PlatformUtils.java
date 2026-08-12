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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import comart.tools.jdbgen.update.UpdateManager;
import java.awt.desktop.AboutHandler;
import java.awt.desktop.PreferencesHandler;
import java.awt.desktop.PrintFilesHandler;
import java.awt.desktop.QuitHandler;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
public class PlatformUtils {
    private static OSType detectedOS = null;

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
            log.error(e.getLocalizedMessage(), e);
        }

    }
    
    public static void openFile(String path) {
        Desktop desk = Desktop.getDesktop();
        
        try {
            desk.open(new File(path));
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }

    }
    
    private static final String UNKNOWN_VERSION = "unknown";
    private static String _version = null;

    public synchronized static String getVersion() {
        if (_version == null) {
            Properties prop = new Properties();
            InputStream is = PlatformUtils.class.getResourceAsStream("/version.properties");
            if (is == null) {
                log.error("'/version.properties' not found in classpath. "
                        + "installation may be corrupted.");
                _version = UNKNOWN_VERSION;
            } else {
                try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    prop.load(isr);
                    _version = prop.getProperty("version", UNKNOWN_VERSION);
                } catch (Exception e) {
                    log.error(e.getLocalizedMessage(), e);
                    _version = UNKNOWN_VERSION;
                }
            }
        }
        return _version;
    }

    /**
     * Opens a page of the documentation shipped with this release.
     *
     * @param page path below {@code docs/}, optionally with an anchor
     *             (for example {@code "icons.md"} or
     *             {@code "custom-queries.md#get-table-list-sql"})
     */
    public static void openDoc(String page) {
        String version = getVersion();
        String docUrl = "https://github.com/xcomart/jdbgen/blob/v"+version+"/docs/"+page;
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
            log.error(e.getLocalizedMessage(), e);
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
    
    /**
     * compare two dotted version strings numerically. A leading 'v' is ignored,
     * and any non numeric segment is treated as 0. Missing trailing segments are
     * treated as 0 too, so "1.2" equals "1.2.0".
     *
     * @return negative if <code>a</code> precedes <code>b</code>, 0 if they are
     *         equal, positive otherwise.
     */
    static int compareVersions(String a, String b) {
        String[] av = stripVersionPrefix(a).split("\\.");
        String[] bv = stripVersionPrefix(b).split("\\.");
        int len = Math.max(av.length, bv.length);
        for (int i=0; i<len; i++) {
            int an = i < av.length ? StrUtils.toInt(av[i]) : 0;
            int bn = i < bv.length ? StrUtils.toInt(bv[i]) : 0;
            if (an != bn)
                return an < bn ? -1 : 1;
        }
        return 0;
    }

    private static String stripVersionPrefix(String v) {
        String res = v == null ? "" : v.trim();
        if (res.length() > 0 && (res.charAt(0) == 'v' || res.charAt(0) == 'V'))
            res = res.substring(1);
        return res;
    }

    private static final String RELEASE_PAGE =
            "https://github.com/xcomart/jdbgen/releases/latest";

    public static void updateCheck() {
        // whatever an earlier update left behind is of no use anymore
        UpdateManager.cleanupStaging();
        String curVersion = getVersion();
        if (UNKNOWN_VERSION.equals(curVersion)) {
            log.warn("current version is unknown, skipping update check.");
            return;
        }
        String url = "https://api.github.com/repos/xcomart/jdbgen/releases/latest";
        Request req = new Request.Builder().url(url).build();
        JsonObject release;
        String tagName;
        // the response is read and closed before anything is downloaded from it
        try (Response response = HttpUtils.getClient().newCall(req).execute()) {
            Gson gson = new Gson();
            release = gson.fromJson(response.body().charStream(), JsonObject.class);
            JsonElement tag = release == null ? null : release.get("tag_name");
            if (tag == null || !tag.isJsonPrimitive()) {
                log.warn("no 'tag_name' in the latest release response, skipping update check.");
                return;
            }
            tagName = tag.getAsString();
        } catch(Exception e) {
            log.error(e.getLocalizedMessage(), e);
            return;
        }
        if (compareVersions(curVersion, tagName) < 0) {
            // updates available
            if (UIUtils.confirm(null, "Update Available", "New version "+tagName+
                    " is available.\nDo you want to update now?")) {
                applyUpdate(release);
            }
        }
    }

    /**
     * download and install <code>release</code>, restarting the application
     * when it worked. A cancelled download just continues the startup, a
     * failed one falls back to the release page in the browser.
     */
    private static void applyUpdate(JsonObject release) {
        UpdateManager.Result res = UpdateManager.performUpdate(release);
        if (res == UpdateManager.Result.LAUNCHED) {
            // the installed files are replaced as soon as this process is gone
            System.exit(0);
        } else if (res == UpdateManager.Result.FAILED) {
            UIUtils.error(null, "The update could not be installed automatically.");
            if (UIUtils.confirm(null, "Update Available",
                    "Do you want to open the release page to download it yourself?")) {
                openURL(RELEASE_PAGE);
                System.exit(0);
            }
        }
    }
}

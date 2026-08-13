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

/**
 * What the operating system and the release around the application are: which
 * platform it runs on, which version it is, how to hand a URL or a file to the
 * desktop, and whether a newer release is available. Everything is
 * <code>static</code> and the detected platform is worked out once.
 */
@Slf4j
public class PlatformUtils {
    /** the platform worked out on the first {@link #getOSType()} call. */
    private static OSType detectedOS = null;

    /**
     * this class only holds <code>static</code> methods.
     */
    public PlatformUtils() {
    }

    /**
     * the platform this application runs on, read from the
     * <code>os.name</code> system property and remembered afterwards.
     *
     * @return the detected platform, {@link OSType#Other} for a platform that
     *         is none of the known ones.
     */
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

    /**
     * open <code>url</code> in the default browser of the desktop. A failure is
     * logged and nothing else happens.
     *
     * @param url
     *            the address to open.
     */
    public static void openURL(String url) {
        Desktop desk = Desktop.getDesktop();
        
        try {
            desk.browse(new URL(url).toURI());
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }

    }
    
    /**
     * hand a file or a directory to the desktop, which opens it with whatever
     * is registered for it. A failure is logged and nothing else happens.
     *
     * @param path
     *            path of the file or directory to open.
     */
    public static void openFile(String path) {
        Desktop desk = Desktop.getDesktop();
        
        try {
            desk.open(new File(path));
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }

    }
    
    /** version reported when <code>/version.properties</code> cannot be read. */
    private static final String UNKNOWN_VERSION = "unknown";
    /** the version read on the first {@link #getVersion()} call. */
    private static String _version = null;

    /**
     * the version of this release, read once from
     * <code>/version.properties</code> on the class path.
     *
     * @return the version string, or <code>"unknown"</code> when the resource
     *         is missing or unreadable.
     */
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

    /**
     * @return this application runs on Windows or not.
     */
    public static boolean isWindows() {
        return getOSType() == OSType.Windows;
    }

    /**
     * @return this application runs on macOS or not.
     */
    public static boolean isMac() {
        return getOSType() == OSType.MacOS;
    }

    /**
     * @return this application runs on a Unix like platform or not.
     */
    public static boolean isUnix() {
        return getOSType() == OSType.Unix;
    }

    /**
     * The platforms told apart by {@link #getOSType()}.
     */
    public static enum OSType {
        /** Microsoft Windows. */
        Windows,
        /** Apple macOS. */
        MacOS,
        /** Linux and the other Unix like platforms. */
        Unix,
        /** anything the <code>os.name</code> property is not recognised for. */
        Other;

        /** the constants carry nothing beyond their identity. */
        private OSType() {
        }
    }
    
    /**
     * hook the application into the platform menus - the macOS application
     * menu above all. A <code>null</code> handler is skipped, and a platform
     * that does not support one of them is logged and ignored.
     *
     * @param about
     *            handler of "About this application".
     * @param prefs
     *            handler of "Preferences".
     * @param print
     *            handler of files dropped on the application to be printed.
     * @param shut
     *            handler of "Quit".
     */
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
    
    /**
     * put the application icon on the platform taskbar or dock. Silently does
     * nothing where the platform has no such thing.
     */
    public static void setDockIcon() {
        try {
            final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
            final URL imageResource = AppDirs.installResourceFile("resource/icon.png").toURI().toURL();
            final Image image = defaultToolkit.getImage(imageResource);
            final Taskbar taskbar = Taskbar.getTaskbar();
            taskbar.setIconImage(image);
        } catch(Exception ignored) {
        }
    }
    
    /**
     * compare two dotted version strings numerically. A leading 'v' is ignored,
     * and missing trailing segments are treated as 0, so "1.2" equals "1.2.0".
     *
     * <p>Whatever follows the dotted number - <code>"-rc1"</code> of
     * <code>"0.3.2-rc1"</code> - is a pre-release mark and not part of the
     * number: the numbers are compared first, and only when they are equal does
     * the marked version count as the older one, so that
     * <code>0.3.2-rc1</code> precedes <code>0.3.2</code> and follows
     * <code>0.3.1</code>. Two pre-releases of the same number are not told
     * apart, the tag itself is never read.</p>
     *
     * @param a
     *            left hand version string, may be <code>null</code>.
     * @param b
     *            right hand version string, may be <code>null</code>.
     * @return negative if <code>a</code> precedes <code>b</code>, 0 if they are
     *         equal, positive otherwise.
     */
    static int compareVersions(String a, String b) {
        String as = stripVersionPrefix(a);
        String bs = stripVersionPrefix(b);
        String[] av = numericPart(as).split("\\.");
        String[] bv = numericPart(bs).split("\\.");
        int len = Math.max(av.length, bv.length);
        for (int i=0; i<len; i++) {
            int an = i < av.length ? StrUtils.toInt(av[i]) : 0;
            int bn = i < bv.length ? StrUtils.toInt(bv[i]) : 0;
            if (an != bn)
                return an < bn ? -1 : 1;
        }
        // same number: a pre-release precedes the release it leads up to
        boolean ap = isPreRelease(as);
        boolean bp = isPreRelease(bs);
        if (ap == bp)
            return 0;
        return ap ? -1 : 1;
    }

    /**
     * the dotted number a version string opens with.
     *
     * @param v
     *            the version, already stripped of its tag prefix.
     * @return the leading run of digits and dots, empty when the version does
     *         not open with one.
     */
    private static String numericPart(String v) {
        int idx = 0;
        while (idx < v.length() &&
                (v.charAt(idx) == '.' || (v.charAt(idx) >= '0' && v.charAt(idx) <= '9')))
            idx++;
        return v.substring(0, idx);
    }

    /**
     * whether a version carries anything beyond its dotted number, which is how
     * <code>0.3.2-rc1</code> is told from <code>0.3.2</code>.
     *
     * @param v
     *            the version, already stripped of its tag prefix.
     * @return there is a pre-release mark or not.
     */
    private static boolean isPreRelease(String v) {
        return numericPart(v).length() < v.length();
    }

    /**
     * drop the <code>v</code> a release tag is usually written with.
     *
     * @param v
     *            the version string, may be <code>null</code>.
     * @return the trimmed version without its leading <code>'v'</code>, empty
     *         for <code>null</code>.
     */
    private static String stripVersionPrefix(String v) {
        String res = v == null ? "" : v.trim();
        if (res.length() > 0 && (res.charAt(0) == 'v' || res.charAt(0) == 'V'))
            res = res.substring(1);
        return res;
    }

    /** page the user is sent to when the update cannot be installed here. */
    private static final String RELEASE_PAGE =
            "https://github.com/xcomart/jdbgen/releases/latest";

    /**
     * Ask GitHub for the latest release and offer the update when it is newer
     * than the running version.
     *
     * <p>Whatever an earlier update left behind is cleaned up first. The check
     * is skipped when the running version is unknown, and every failure - no
     * network, an unexpected response - is logged and the startup simply
     * continues. An installation whose files cannot be replaced is not updated
     * by the application itself; the user is told about the new version and
     * offered the release page instead.</p>
     */
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
            if (!AppDirs.isInstallWritable()) {
                // an installed build - below C:\Program Files, or owned by the
                // package manager it was installed with - cannot replace its
                // own files, so the user is told how to update instead.
                announceManualUpdate(tagName);
                return;
            }
            if (UIUtils.confirm(null, I18n.t("common.update.title"),
                    I18n.t("common.update.available", tagName))) {
                applyUpdate(release);
            }
        }
    }

    /**
     * tell the user about a new version that cannot be installed by the
     * application itself, and offer the release page.
     *
     * @param tagName
     *            tag of the new release, as GitHub reported it.
     */
    private static void announceManualUpdate(String tagName) {
        log.info("'{}' is not writable, the update has to be installed by the user.",
                AppDirs.installResourceBase());
        StringBuilder msg = new StringBuilder(I18n.t("common.update.manual", tagName));
        if (isWindows())
            msg.append("\n\n").append(I18n.t("common.update.manual.winget"));
        msg.append("\n\n").append(I18n.t("common.update.openReleasePage"));
        if (UIUtils.confirm(null, I18n.t("common.update.title"), msg.toString()))
            openURL(RELEASE_PAGE);
    }

    /**
     * download and install <code>release</code>, restarting the application
     * when it worked. A cancelled download just continues the startup, a
     * failed one falls back to the release page in the browser.
     *
     * @param release
     *            the release object GitHub answered the latest release request
     *            with.
     */
    private static void applyUpdate(JsonObject release) {
        UpdateManager.Result res = UpdateManager.performUpdate(release);
        if (res == UpdateManager.Result.LAUNCHED) {
            // the installed files are replaced as soon as this process is gone
            System.exit(0);
        } else if (res == UpdateManager.Result.FAILED) {
            UIUtils.error(null, I18n.t("common.update.failed"));
            if (UIUtils.confirm(null, I18n.t("common.update.title"),
                    I18n.t("common.update.openReleasePage"))) {
                openURL(RELEASE_PAGE);
                System.exit(0);
            }
        }
    }
}

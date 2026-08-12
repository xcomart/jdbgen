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

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

/**
 * Where the application keeps its files.
 *
 * <p>Two directories are told apart:</p>
 * <ul>
 *   <li>the <em>user data directory</em> - everything the application writes:
 *       <code>config.json</code>, its backups, the downloaded driver jars. It
 *       is the per user location of the operating system, so an installation
 *       below <code>C:\Program Files</code> - which is read only for the user
 *       running it - works just as well as an unpacked archive.</li>
 *   <li>the <em>installation resource base</em> - the read only files shipped
 *       with the release: <code>resource/</code>, <code>templates/</code> and
 *       the sample database. That is the directory of the running jar, and the
 *       working directory when the application runs out of class files (a
 *       development run).</li>
 * </ul>
 *
 * <p>Both can be overridden with a system property - {@value #DATA_DIR_PROPERTY}
 * and {@value #RESOURCE_BASE_PROPERTY} - which keeps a portable run (all files
 * next to the application) and the tests possible.</p>
 *
 * @author comart
 */
@Slf4j
public final class AppDirs {
    /** overrides {@link #userDataDir()}. */
    public static final String DATA_DIR_PROPERTY = "jdbgen.dataDir";
    /** overrides {@link #installResourceBase()}. */
    public static final String RESOURCE_BASE_PROPERTY = "jdbgen.resourceBase";

    /** directory name below the per user location of the operating system. */
    static final String APP_NAME = "jdbgen";
    /** the downloaded and hand picked JDBC driver jars. */
    public static final String DRIVERS_DIR = "drivers";
    /** name of the configuration file, below {@link #userDataDir()}. */
    public static final String CONFIG_NAME = "config.json";

    private static final String JAR_SUFFIX = ".jar";

    private AppDirs() {
    }

    /**
     * the directory the application writes to, created when it is not there
     * yet.
     */
    public static File userDataDir() {
        File dir = configuredUserDataDir();
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory())
            log.warn("cannot create the user data directory '{}'", dir);
        return dir;
    }

    /**
     * @return <code>name</code> below {@link #userDataDir()}.
     */
    public static File userDataFile(String name) {
        return new File(userDataDir(), name);
    }

    /**
     * the directory of the JDBC driver jars, created when it is not there yet.
     */
    public static File driversDir() {
        File dir = userDataFile(DRIVERS_DIR);
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory())
            log.warn("cannot create the driver directory '{}'", dir);
        return dir;
    }

    /**
     * the user data directory as it is configured, without creating it.
     */
    static File configuredUserDataDir() {
        String override = System.getProperty(DATA_DIR_PROPERTY);
        if (!StrUtils.isEmpty(override))
            return new File(override.trim()).getAbsoluteFile();
        return defaultUserDataDir(PlatformUtils.getOSType(), System.getenv("APPDATA"),
                System.getenv("XDG_CONFIG_HOME"), System.getProperty("user.home"));
    }

    /**
     * the per user location of an operating system. Pure function of what it
     * is given, so that every platform can be checked from anywhere.
     *
     * @param os the platform to answer for
     * @param appData value of <code>%APPDATA%</code>, Windows only
     * @param xdgConfigHome value of <code>$XDG_CONFIG_HOME</code>, Unix only
     * @param userHome the user's home directory
     */
    static File defaultUserDataDir(PlatformUtils.OSType os, String appData,
            String xdgConfigHome, String userHome) {
        File home = new File(StrUtils.isEmpty(userHome) ? "." : userHome);
        File base;
        if (os == PlatformUtils.OSType.Windows) {
            base = StrUtils.isEmpty(appData)
                    ? new File(new File(home, "AppData"), "Roaming")
                    : new File(appData);
        } else if (os == PlatformUtils.OSType.MacOS) {
            base = new File(new File(home, "Library"), "Application Support");
        } else if (!StrUtils.isEmpty(xdgConfigHome)) {
            base = new File(xdgConfigHome);
        } else {
            base = new File(home, ".config");
        }
        return new File(base, APP_NAME).getAbsoluteFile();
    }

    /**
     * the directory the read only files of the release live in: the directory
     * of the running jar, or the working directory when there is no jar.
     */
    public static File installResourceBase() {
        String override = System.getProperty(RESOURCE_BASE_PROPERTY);
        if (!StrUtils.isEmpty(override))
            return new File(override.trim()).getAbsoluteFile();
        File jar = runningJar();
        File dir = jar == null ? null : jar.getParentFile();
        return dir == null ? workingDir() : dir.getAbsoluteFile();
    }

    /**
     * @return <code>name</code> below {@link #installResourceBase()}.
     */
    public static File installResourceFile(String name) {
        return new File(installResourceBase(), name);
    }

    /**
     * the jar this application runs from, or <code>null</code> when it is
     * started from class files (a development run).
     */
    public static File runningJar() {
        try {
            CodeSource cs = AppDirs.class.getProtectionDomain().getCodeSource();
            if (cs != null && cs.getLocation() != null) {
                File loc = new File(URI.create(cs.getLocation().toString()));
                if (loc.isFile() && loc.getName().toLowerCase(Locale.ENGLISH).endsWith(JAR_SUFFIX))
                    return loc.getAbsoluteFile();
            }
        } catch (Exception e) {
            log.warn("cannot read the code source location: {}", e.getLocalizedMessage());
        }
        return null;
    }

    /** the working directory the application was started from. */
    public static File workingDir() {
        return new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
    }

    /**
     * Turn a path of the configuration into the file it names.
     *
     * <p>An absolute path is taken as it is. A relative one is looked for
     * below the user data directory first - that is where a downloaded driver
     * jar or an edited template ends up - and below the installation
     * afterwards, which is what a configuration carried over from an unpacked
     * archive refers to.</p>
     *
     * @return the user data candidate when the path names nothing that exists,
     *         so that a failure names the location the file is expected in.
     *         <code>null</code> for an empty path.
     */
    public static File resolve(String path) {
        if (StrUtils.isEmpty(path))
            return null;
        File f = new File(path.trim());
        if (f.isAbsolute())
            return f;
        File inData = new File(userDataDir(), f.getPath());
        if (inData.exists())
            return inData;
        File inInstall = new File(installResourceBase(), f.getPath());
        if (inInstall.exists())
            return inInstall;
        return inData;
    }

    /**
     * {@link #resolve(String)} as a path string, for the callers that hand it
     * on to a file API taking a name.
     */
    public static String resolvePath(String path) {
        File f = resolve(path);
        return f == null ? path : f.getPath();
    }

    /**
     * The directory generated files are written into.
     *
     * <p>Resolved like every other configured path, see
     * {@link #resolve(String)}, so that the relative path the directory chooser
     * stored - {@link #relativize(String)} keeps a directory below the user
     * data directory or the installation relative to it - names the directory
     * that was picked, and not the same name below whatever the working
     * directory happens to be.</p>
     *
     * <p>Unlike {@link #resolvePath(String)} the directory is created, so that
     * "open the output directory" after a generation run has something to
     * open even when no file was written.</p>
     *
     * @return the empty path it was given, so that "no output directory" stays
     *         "no output directory".
     */
    public static String resolveOutputDir(String path) {
        File dir = resolve(path);
        if (dir == null)
            return path;
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory())
            log.warn("cannot create the output directory '{}'", dir);
        return dir.getPath();
    }

    /**
     * The counterpart of {@link #resolve(String)}: store a file below one of
     * the two known directories relative to it, so that the configuration
     * keeps working when the application is reinstalled somewhere else.
     * Anything else is stored as the absolute path it is.
     */
    public static String relativize(String path) {
        if (StrUtils.isEmpty(path))
            return path;
        File f = new File(path.trim()).getAbsoluteFile();
        String below = below(configuredUserDataDir(), f);
        if (below == null)
            below = below(installResourceBase(), f);
        return below == null ? f.getAbsolutePath() : below;
    }

    /**
     * @return the path of <code>f</code> relative to <code>dir</code>, or
     *         <code>null</code> when it is not below it.
     */
    private static String below(File dir, File f) {
        String base = dir.getAbsolutePath();
        String path = f.getAbsolutePath();
        if (base.isEmpty() || !path.startsWith(base))
            return null;
        String rest = path.substring(base.length());
        if (rest.isEmpty())
            return null;
        char sep = rest.charAt(0);
        if (sep != File.separatorChar && sep != '/')
            return null;
        // '/' whatever the platform separator is: the configuration is read on
        // the other platforms too, and every file API understands it
        return rest.substring(1).replace(File.separatorChar, '/');
    }

    /**
     * whether files can be created in <code>dir</code>. The permission bits
     * are not asked for, a file is actually created: a directory below
     * <code>C:\Program Files</code> looks writable to
     * {@link File#canWrite()} while every write ends up virtualized or denied.
     */
    public static boolean isWritable(File dir) {
        if (dir == null || !dir.isDirectory())
            return false;
        File probe = null;
        try {
            probe = File.createTempFile(".jdbgen-write-", ".tmp", dir);
            return true;
        } catch (Exception e) {
            log.info("'{}' is not writable: {}", dir, e.getLocalizedMessage());
            return false;
        } finally {
            if (probe != null && probe.exists() && !probe.delete())
                probe.deleteOnExit();
        }
    }

    /**
     * whether the installed files can be replaced, which is what the automatic
     * update needs.
     */
    public static boolean isInstallWritable() {
        return isWritable(installResourceBase());
    }

    /**
     * Copy the files of a release that kept everything next to the
     * application - every release up to 0.3.0 - into the user data directory.
     *
     * <p>Nothing is moved: the old installation keeps working, and a failed
     * copy is not fatal either. The configuration is copied last, so that a
     * half copied state is not taken for a complete one on the next start; a
     * missing configuration simply starts the "create a default one" flow.</p>
     *
     * @return <code>true</code> when a configuration was carried over.
     */
    public static boolean migrateLegacyData() {
        File target = userDataDir();
        if (new File(target, CONFIG_NAME).isFile())
            return false;
        for (File src: legacySources()) {
            if (src.getAbsolutePath().equals(target.getAbsolutePath()))
                continue;
            if (!new File(src, CONFIG_NAME).isFile())
                continue;
            return migrateLegacyData(src, target);
        }
        return false;
    }

    /**
     * the directories a previous release may have kept its files in: the
     * installation and the working directory the application was started from.
     */
    static List<File> legacySources() {
        List<File> res = new ArrayList<>();
        res.add(installResourceBase());
        File cwd = workingDir();
        if (!res.get(0).getAbsolutePath().equals(cwd.getAbsolutePath()))
            res.add(cwd);
        return res;
    }

    /**
     * copy <code>config.json</code>, its backups and the driver jars of
     * <code>from</code> into <code>to</code>.
     *
     * @return <code>true</code> when the configuration itself was copied.
     */
    static boolean migrateLegacyData(File from, File to) {
        File config = new File(from, CONFIG_NAME);
        if (!config.isFile())
            return false;
        log.info("carrying the configuration of '{}' over to '{}'", from, to);
        if (!to.isDirectory() && !to.mkdirs() && !to.isDirectory()) {
            log.error("cannot create the user data directory '{}'", to);
            return false;
        }
        File[] files = from.listFiles();
        if (files != null) {
            for (File f: files) {
                String name = f.getName();
                if (f.isFile() && name.startsWith(CONFIG_NAME + ".") && name.endsWith(".bak"))
                    copyFile(f, new File(to, name));
            }
        }
        copyTree(new File(from, DRIVERS_DIR), new File(to, DRIVERS_DIR));
        // last, see the class comment: an incomplete copy must not look like a
        // finished migration on the next start.
        return copyFile(config, new File(to, CONFIG_NAME));
    }

    private static boolean copyFile(File src, File dst) {
        try {
            Files.createDirectories(dst.toPath().toAbsolutePath().getParent());
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            log.error("cannot copy '" + src + "' to '" + dst + "'", e);
            return false;
        }
    }

    /**
     * copy a directory tree, keeping whatever cannot be copied out of the way
     * of the rest - a driver jar that is missing is reported when it is used.
     */
    private static void copyTree(File src, File dst) {
        if (!src.isDirectory())
            return;
        try {
            Files.createDirectories(dst.toPath());
            File[] children = src.listFiles();
            if (children == null)
                return;
            for (File c: children) {
                if (c.isDirectory())
                    copyTree(c, new File(dst, c.getName()));
                else
                    copyFile(c, new File(dst, c.getName()));
            }
        } catch (Exception e) {
            log.error("cannot copy '" + src + "' to '" + dst + "'", e);
        }
    }
}

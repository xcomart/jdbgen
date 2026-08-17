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
package comart.tools.jdbgen.update;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Replaces the installed files with the release unpacked by
 * {@link UpdateManager} and starts the new version.
 *
 * This runs in a JVM of its own, started with nothing but a copy of the
 * jdbgen jar on its class path - <code>lib/</code> is being replaced while it
 * works. <strong>It must therefore use JDK classes only</strong>: no logging
 * framework, no gson, no okhttp. Its output is redirected into
 * <code>.update/update.log</code> by the process that starts it.
 *
 * @author comart
 */
public class UpdateApplier {
    /** how long to wait for the previous application to release its jar. */
    private static final long WAIT_MILLIS = 60_000L;
    /** pause between two attempts to move a file that is still held open. */
    private static final long RETRY_MILLIS = 500L;

    /** file name prefix of the application jar. */
    private static final String JAR_PREFIX = "jdbgen-";
    /** file name suffix of the application jar. */
    private static final String JAR_SUFFIX = ".jar";
    /** directory holding the dependencies, replaced as a whole. */
    private static final String LIB_DIR = "lib";
    /** directory below the staging directory the previous version is moved to. */
    private static final String BACKUP_DIR = "backup";
    /** sample database, only added when the installation has none. */
    private static final String SAMPLE_DB = "sample_h2.db.mv.db";
    /** launcher scripts, which belong to the release and are always overwritten. */
    private static final String[] SCRIPTS = { "jdbgen.cmd", "jdbgen.sh" };

    /**
     * apply the update and start the new version. The staging directory is
     * cleaned up afterwards either way; a failed update keeps the backup of
     * the previous version around, because it may be the only copy left.
     *
     * @param args
     *            the installation directory and the directory holding the
     *            unpacked release, in that order. Fewer arguments only produce
     *            a usage line in the log.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            log("usage: UpdateApplier <installDir> <extractedDir>");
            return;
        }
        File installDir = new File(args[0]);
        File extractedDir = new File(args[1]);
        log("applying the update of " + installDir + " from " + extractedDir);
        boolean applied = apply(installDir, extractedDir, WAIT_MILLIS);
        if (applied) {
            try {
                restart(installDir);
                log("the new version has been started.");
            } catch (IOException e) {
                log("cannot start the new version: " + e);
            }
        }
        // a failed update keeps the backup around: should putting the previous
        // version back have failed too, it is the only copy left
        cleanup(installDir, !applied);
        log(applied ? "update finished." : "update failed, the installation was left as it was.");
    }

    /**
     * move the installed jar and <code>lib/</code> aside and copy the new
     * release over the installation. Everything the user may have created or
     * edited - <code>config.json</code>, <code>drivers/</code>,
     * <code>output/</code>, the templates and the sample database - is kept.
     *
     * @param installDir the installation directory to update in place.
     * @param extractedDir the directory holding the unpacked release.
     * @param waitMillis how long to keep retrying while the previous
     *        application still holds its jar open.
     * @return <code>false</code> when nothing was applied; the installation is
     *         then left in, or rolled back to, its previous state.
     */
    static boolean apply(File installDir, File extractedDir, long waitMillis) {
        List<File> newJars = listJars(extractedDir);
        if (newJars.isEmpty()) {
            log("no " + JAR_PREFIX + "*" + JAR_SUFFIX + " found in " + extractedDir);
            return false;
        }
        File backup = new File(new File(installDir, UpdateManager.STAGING_NAME), BACKUP_DIR);
        try {
            Files.createDirectories(backup.toPath());
            // the previous application may still hold its files open, so this
            // doubles as the wait for it to exit
            for (File jar: listJars(installDir))
                moveWithRetry(jar, new File(backup, jar.getName()), waitMillis);
            File lib = new File(installDir, LIB_DIR);
            if (lib.isDirectory())
                moveWithRetry(lib, new File(backup, LIB_DIR), waitMillis);
        } catch (IOException e) {
            log("cannot put the installed files aside: " + e);
            // whatever was moved already has to go back, or the installation
            // is left without the jar it was started from
            restoreBackup(installDir, backup);
            return false;
        }
        try {
            for (File jar: newJars)
                copyFile(jar, new File(installDir, jar.getName()));
            File newLib = new File(extractedDir, LIB_DIR);
            if (newLib.isDirectory())
                copyTree(newLib, new File(installDir, LIB_DIR), true);
            // the launcher scripts and the shipped resources belong to the
            // release and are always overwritten
            for (String script: SCRIPTS) {
                File src = new File(extractedDir, script);
                if (src.isFile()) {
                    File dst = new File(installDir, script);
                    copyFile(src, dst);
                    dst.setExecutable(true, false);
                }
            }
            File res = new File(extractedDir, "resource");
            if (res.isDirectory())
                copyTree(res, new File(installDir, "resource"), true);
            // the templates and the sample database may have been edited, so
            // only the ones that are missing are added
            File templates = new File(extractedDir, "templates");
            if (templates.isDirectory())
                copyTree(templates, new File(installDir, "templates"), false);
            File sample = new File(extractedDir, SAMPLE_DB);
            File sampleDst = new File(installDir, SAMPLE_DB);
            if (sample.isFile() && !sampleDst.exists())
                copyFile(sample, sampleDst);
            return true;
        } catch (IOException e) {
            log("cannot copy the new files, rolling back: " + e);
            rollback(installDir, backup);
            return false;
        }
    }

    /**
     * throw the half copied new version away and put the previous one back.
     *
     * @param installDir the installation directory being updated.
     * @param backup the directory the previous version was moved to.
     */
    private static void rollback(File installDir, File backup) {
        for (File jar: listJars(installDir)) {
            try {
                Files.deleteIfExists(jar.toPath());
            } catch (IOException e) {
                log("cannot remove the new " + jar.getName() + ": " + e);
            }
        }
        deleteRecursively(new File(installDir, LIB_DIR));
        restoreBackup(installDir, backup);
    }

    /**
     * put the jar and <code>lib/</code> of the previous version back. What is
     * still in place stays as it is - a file that could not be moved aside is
     * the original one. Anything that cannot be restored is logged and
     * skipped, there is nothing better left to do at that point.
     *
     * @param installDir the installation directory being updated.
     * @param backup the directory the previous version was moved to.
     */
    private static void restoreBackup(File installDir, File backup) {
        try {
            for (File jar: listJars(backup))
                move(jar, new File(installDir, jar.getName()));
            File lib = new File(backup, LIB_DIR);
            if (lib.isDirectory()) {
                copyTree(lib, new File(installDir, LIB_DIR), false);
                deleteRecursively(lib);
            }
            log("the previous version has been restored.");
        } catch (IOException e) {
            log("the previous version could not be restored: " + e);
        }
    }

    /**
     * start the new version from the installation directory. Windows gets
     * <code>javaw</code> so that no console window is left behind.
     *
     * @param installDir the installation directory, which becomes the working
     *        directory of the new process.
     * @return the started process.
     * @throws IOException when the directory holds no
     *         <code>jdbgen-*.jar</code>, or when the process cannot be
     *         started.
     */
    static Process restart(File installDir) throws IOException {
        List<File> jars = listJars(installDir);
        if (jars.isEmpty())
            throw new IOException("no " + JAR_PREFIX + "*" + JAR_SUFFIX + " in " + installDir);
        ProcessBuilder pb = new ProcessBuilder(
                javaExecutable().getAbsolutePath(), "-jar", jars.get(0).getName());
        pb.directory(installDir);
        return pb.start();
    }

    /**
     * the java launcher to start the new version with.
     *
     * @return <code>javaw.exe</code> on Windows so that no console window
     *         appears, falling back to <code>java.exe</code>, and
     *         <code>java</code> everywhere else.
     */
    private static File javaExecutable() {
        File bin = new File(System.getProperty("java.home", "."), "bin");
        boolean windows = System.getProperty("os.name", "")
                .toLowerCase(Locale.ENGLISH).contains("win");
        if (windows) {
            File javaw = new File(bin, "javaw.exe");
            if (javaw.isFile())
                return javaw;
            return new File(bin, "java.exe");
        }
        return new File(bin, "java");
    }

    /**
     * drop everything of the staging directory but this jar and the log. The
     * jar cannot delete itself while it runs on Windows; the application
     * removes what is left on its next start.
     *
     * @param installDir the installation directory holding the staging
     *        directory.
     * @param keepBackup leave the previous version in
     *        <code>.update/backup/</code> as well.
     */
    static void cleanup(File installDir, boolean keepBackup) {
        File staging = new File(installDir, UpdateManager.STAGING_NAME);
        File[] children = staging.listFiles();
        if (children == null)
            return;
        for (File c: children) {
            String name = c.getName();
            if (UpdateManager.UPDATER_NAME.equals(name) || UpdateManager.LOG_NAME.equals(name)
                    || (keepBackup && BACKUP_DIR.equals(name)))
                continue;
            deleteRecursively(c);
        }
        if (keepBackup && new File(staging, BACKUP_DIR).isDirectory())
            log("the previous version is kept in " + new File(staging, BACKUP_DIR));
    }

    /**
     * rename <code>src</code> to <code>dst</code> without waiting for it to be
     * released.
     *
     * @param src the file or directory to move.
     * @param dst the target name.
     * @throws IOException when the move fails.
     */
    private static void move(File src, File dst) throws IOException {
        moveWithRetry(src, dst, 0);
    }

    /**
     * rename <code>src</code>, retrying while the previous application still
     * holds it open. A directory that cannot be renamed at all is copied and
     * removed instead, which is what a move to another file system needs too.
     *
     * @param src the file or directory to move.
     * @param dst the target name, replaced when it already exists.
     * @param waitMillis how long to keep retrying before giving up.
     * @throws IOException the last failure of the move, or an interruption
     *         while waiting.
     */
    private static void moveWithRetry(File src, File dst, long waitMillis) throws IOException {
        long deadline = System.currentTimeMillis() + waitMillis;
        IOException last;
        do {
            try {
                Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException e) {
                last = e;
                if (System.currentTimeMillis() >= deadline)
                    break;
                log("waiting for " + src.getName() + " to be released...");
                try {
                    Thread.sleep(RETRY_MILLIS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted while waiting for " + src, ie);
                }
            }
        } while (true);
        if (src.isDirectory()) {
            copyTree(src, dst, true);
            deleteRecursively(src);
            if (!src.exists())
                return;
        }
        throw last;
    }

    /**
     * copy the whole directory tree below <code>src</code> into
     * <code>dst</code>, creating the directories on the way.
     *
     * @param src the directory to copy; nothing happens when it cannot be
     *        listed.
     * @param dst the target directory, created when missing.
     * @param overwrite when <code>false</code>, files already present below
     *        <code>dst</code> are left untouched.
     * @throws IOException when a directory cannot be created or a file cannot
     *         be copied.
     */
    private static void copyTree(File src, File dst, boolean overwrite) throws IOException {
        File[] children = src.listFiles();
        if (children == null)
            return;
        Files.createDirectories(dst.toPath());
        for (File c: children) {
            File target = new File(dst, c.getName());
            if (c.isDirectory()) {
                copyTree(c, target, overwrite);
            } else if (overwrite || !target.exists()) {
                copyFile(c, target);
            }
        }
    }

    /**
     * copy a single file, creating the parent directory of the target first.
     *
     * @param src the file to copy.
     * @param dst the target file, replaced when it already exists.
     * @throws IOException when the parent cannot be created or the copy fails.
     */
    private static void copyFile(File src, File dst) throws IOException {
        Path parent = dst.toPath().toAbsolutePath().getParent();
        if (parent != null)
            Files.createDirectories(parent);
        Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * delete a file or a whole directory tree, logging what cannot be removed
     * instead of failing.
     *
     * @param f file or directory to remove, may be <code>null</code> or
     *        already gone. A symbolic link is removed without being followed.
     */
    private static void deleteRecursively(File f) {
        if (f == null || !f.exists())
            return;
        if (f.isDirectory() && !Files.isSymbolicLink(f.toPath())) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c: children)
                    deleteRecursively(c);
            }
        }
        if (!f.delete())
            log("cannot delete " + f);
    }

    /**
     * every <code>jdbgen-*.jar</code> of <code>dir</code>, sorted by name.
     *
     * @param dir directory to look into, may be <code>null</code>.
     * @return the matching files, empty when <code>dir</code> is
     *         <code>null</code> or holds none.
     */
    private static List<File> listJars(File dir) {
        List<File> res = new ArrayList<>();
        File[] files = dir == null ? null : dir.listFiles();
        if (files != null) {
            for (File f: files) {
                String name = f.getName().toLowerCase(Locale.ENGLISH);
                if (f.isFile() && name.startsWith(JAR_PREFIX) && name.endsWith(JAR_SUFFIX))
                    res.add(f);
            }
        }
        res.sort(Comparator.comparing(File::getName));
        return res;
    }

    /** time stamp every log line is prefixed with. */
    private static final SimpleDateFormat LOG_TIME =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * write a time stamped line to the standard output, which the starting
     * process redirects into <code>.update/update.log</code>. It is flushed
     * right away so that nothing is lost when the JVM ends abruptly.
     *
     * @param message the line to write.
     */
    private static void log(String message) {
        System.out.println(LOG_TIME.format(new Date()) + " " + message);
        System.out.flush();
    }

    /** this class is never instantiated. */
    private UpdateApplier() {
    }
}

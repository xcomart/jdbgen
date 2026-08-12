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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import comart.utils.HttpUtils;
import comart.utils.I18n;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Downloads a new release and hands it over to {@link UpdateApplier}, which
 * replaces the installed files once this application has exited.
 *
 * Everything happens below <code>&lt;installation&gt;/.update/</code> so a
 * failed or cancelled update leaves nothing behind but that one directory,
 * which is removed on the next start.
 *
 * @author comart
 */
@Slf4j
public class UpdateManager {
    /** staging directory below the installation directory. */
    public static final String STAGING_NAME = ".update";
    /** copy of the running jar, used as the class path of the applier. */
    static final String UPDATER_NAME = "updater.jar";
    /** where the applier writes its log to. */
    static final String LOG_NAME = "update.log";
    /** the unpacked release below the staging directory. */
    static final String EXTRACTED_NAME = "extracted";

    private static final String JAR_PREFIX = "jdbgen-";
    private static final String JAR_SUFFIX = ".jar";
    private static final int BUFFER_SIZE = 64 * 1024;

    /**
     * outcome of {@link #performUpdate(JsonObject)}.
     */
    public static enum Result {
        /** the applier was started, the caller has to exit right away. */
        LAUNCHED,
        /** the user closed the progress dialog, startup simply continues. */
        CANCELLED,
        /** nothing was changed, the caller should fall back to the browser. */
        FAILED
    }

    /**
     * a downloadable file of a GitHub release.
     */
    public static class ReleaseAsset {
        private final String name;
        private final String url;
        private final long size;

        ReleaseAsset(String name, String url, long size) {
            this.name = name;
            this.url = url;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        /** size in bytes, 0 when the release did not tell. */
        public long getSize() {
            return size;
        }
    }

    /**
     * pick the distribution archive out of the <code>assets</code> array of a
     * release. Releases also carry source archives and other attachments, so
     * only a <code>jdbgen-*.zip</code> qualifies.
     *
     * @return empty when the release has no archive to download.
     */
    public static Optional<ReleaseAsset> selectZipAsset(JsonObject release) {
        if (release == null)
            return Optional.empty();
        JsonElement assets = release.get("assets");
        if (assets == null || !assets.isJsonArray())
            return Optional.empty();
        JsonArray arr = assets.getAsJsonArray();
        for (JsonElement el: arr) {
            if (!el.isJsonObject())
                continue;
            JsonObject asset = el.getAsJsonObject();
            String name = asString(asset, "name");
            String url = asString(asset, "browser_download_url");
            if (name == null || url == null)
                continue;
            String lower = name.toLowerCase();
            if (!lower.startsWith(JAR_PREFIX) || !lower.endsWith(".zip"))
                continue;
            JsonElement size = asset.get("size");
            long bytes = 0;
            try {
                if (size != null && size.isJsonPrimitive())
                    bytes = size.getAsLong();
            } catch (NumberFormatException ignored) {
            }
            return Optional.of(new ReleaseAsset(name, url, bytes));
        }
        return Optional.empty();
    }

    private static String asString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el == null || !el.isJsonPrimitive() ? null : el.getAsString();
    }

    /**
     * download the release archive, unpack it and start the applier in a
     * separate JVM. On {@link Result#LAUNCHED} the caller must exit
     * immediately, otherwise the applier cannot replace the installed jar.
     */
    public static Result performUpdate(JsonObject release) {
        File jar = runningJar();
        if (jar == null) {
            log.warn("cannot locate the running jar, skipping the automatic update.");
            return Result.FAILED;
        }
        Optional<ReleaseAsset> selected = selectZipAsset(release);
        if (!selected.isPresent()) {
            log.warn("the latest release carries no jdbgen-*.zip asset.");
            return Result.FAILED;
        }
        ReleaseAsset asset = selected.get();
        File installDir = jar.getParentFile();
        File staging = new File(installDir, STAGING_NAME);
        try {
            deleteRecursively(staging);
            if (!staging.isDirectory() && !staging.mkdirs())
                throw new IOException("cannot create the staging directory " + staging);
            File zip = new File(staging, asset.getName());
            if (!downloadWithProgress(asset, zip)) {
                log.info("update download cancelled by the user.");
                deleteRecursively(staging);
                return Result.CANCELLED;
            }
            File extracted = new File(staging, EXTRACTED_NAME);
            extractZip(zip, extracted);
            File updater = new File(staging, UPDATER_NAME);
            Files.copy(jar.toPath(), updater.toPath(), StandardCopyOption.REPLACE_EXISTING);
            launchApplier(installDir, staging, updater, extracted);
            return Result.LAUNCHED;
        } catch (Exception e) {
            log.error("automatic update failed: " + e.getLocalizedMessage(), e);
            deleteRecursively(staging);
            return Result.FAILED;
        }
    }

    /**
     * remove the staging directory of an earlier run. The applier cannot
     * delete its own jar on Windows, so the leftovers are cleaned up here on
     * the next start. Failures are irrelevant, the next start tries again.
     */
    public static void cleanupStaging() {
        File jar = runningJar();
        if (jar == null)
            return;
        File staging = new File(jar.getParentFile(), STAGING_NAME);
        if (staging.isDirectory()) {
            log.info("removing the update staging directory of an earlier run.");
            deleteRecursively(staging);
        }
    }

    /**
     * the jar this application runs from, or <code>null</code> when it is not
     * started from a jar at all (a development run out of the class files).
     */
    public static File runningJar() {
        try {
            CodeSource cs = UpdateManager.class.getProtectionDomain().getCodeSource();
            if (cs != null && cs.getLocation() != null) {
                File loc = new File(URI.create(cs.getLocation().toString()));
                if (loc.isFile() && loc.getName().toLowerCase().endsWith(JAR_SUFFIX))
                    return loc.getAbsoluteFile();
            }
        } catch (Exception e) {
            log.warn("cannot read the code source location: {}", e.getLocalizedMessage());
        }
        // started with an exploded class path: fall back to the working
        // directory, which is where the launcher scripts start us from.
        List<File> jars = listJars(new File(".").getAbsoluteFile().getParentFile());
        return jars.isEmpty() ? null : jars.get(0);
    }

    /**
     * every <code>jdbgen-*.jar</code> of <code>dir</code>, sorted by name.
     */
    static List<File> listJars(File dir) {
        List<File> res = new ArrayList<>();
        File[] files = dir == null ? null : dir.listFiles();
        if (files != null) {
            for (File f: files) {
                String name = f.getName().toLowerCase();
                if (f.isFile() && name.startsWith(JAR_PREFIX) && name.endsWith(JAR_SUFFIX))
                    res.add(f);
            }
        }
        res.sort(Comparator.comparing(File::getName));
        return res;
    }

    /**
     * unpack <code>zip</code> below <code>destDir</code>. The archive wraps
     * everything in a single <code>jdbgen-&lt;version&gt;/</code> directory,
     * which is stripped so that the content lines up with the installation
     * directory.
     *
     * @throws IOException when an entry would be written outside of
     *         <code>destDir</code> (a "zip slip" archive).
     */
    public static void extractZip(File zip, File destDir) throws IOException {
        Path dest = destDir.toPath().toAbsolutePath().normalize();
        Files.createDirectories(dest);
        byte[] buf = new byte[BUFFER_SIZE];
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zip.toPath()))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                // the raw name is checked as well, so that an entry escaping
                // through the segment that is about to be stripped is caught
                resolveInside(dest, name);
                String stripped = stripFirstSegment(name);
                if (stripped.isEmpty())
                    continue;
                Path target = resolveInside(dest, stripped);
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (OutputStream os = Files.newOutputStream(target)) {
                        int len;
                        while ((len = zin.read(buf)) > 0)
                            os.write(buf, 0, len);
                    }
                }
            }
        }
    }

    private static Path resolveInside(Path dest, String name) throws IOException {
        Path target = dest.resolve(name).toAbsolutePath().normalize();
        if (!target.startsWith(dest))
            throw new IOException("archive entry outside of the target directory: " + name);
        return target;
    }

    /**
     * drop the leading <code>jdbgen-&lt;version&gt;/</code> of an archive
     * entry. Entries without any directory part are dropped altogether.
     */
    static String stripFirstSegment(String name) {
        String res = name;
        while (res.startsWith("/"))
            res = res.substring(1);
        int idx = res.indexOf('/');
        return idx < 0 ? "" : res.substring(idx + 1);
    }

    /**
     * @return <code>false</code> when the user cancelled the download.
     */
    private static boolean downloadWithProgress(ReleaseAsset asset, File dest) throws Exception {
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final AtomicReference<Exception> failure = new AtomicReference<>();
        final ProgressDialog dlg = new ProgressDialog(
                I18n.t("common.update.progress.downloading", asset.getName()),
                asset.getSize(), cancelled);
        Thread worker = new Thread(() -> {
            try {
                download(asset, dest, cancelled, dlg);
            } catch (Exception e) {
                if (!cancelled.get())
                    failure.set(e);
            } finally {
                dlg.finish();
            }
        }, "jdbgen-update-download");
        worker.setDaemon(true);
        worker.start();
        dlg.showAndWait();
        // the stream has to be closed before the archive is unpacked
        worker.join();
        if (failure.get() != null)
            throw failure.get();
        return !cancelled.get();
    }

    private static void download(ReleaseAsset asset, File dest, AtomicBoolean cancelled,
            ProgressDialog dlg) throws IOException {
        Request req = new Request.Builder().url(asset.getUrl()).build();
        try (Response response = HttpUtils.getClient().newCall(req).execute()) {
            if (!response.isSuccessful())
                throw new IOException("download failed with HTTP " + response.code());
            ResponseBody body = response.body();
            if (body == null)
                throw new IOException("the download returned no content.");
            long total = asset.getSize() > 0 ? asset.getSize() : body.contentLength();
            dlg.setTotal(total);
            byte[] buf = new byte[BUFFER_SIZE];
            long done = 0;
            try (InputStream is = body.byteStream();
                    OutputStream os = Files.newOutputStream(dest.toPath())) {
                int len;
                while ((len = is.read(buf)) > 0) {
                    if (cancelled.get())
                        throw new IOException("download cancelled.");
                    os.write(buf, 0, len);
                    done += len;
                    dlg.setDone(done);
                }
            }
            log.info("downloaded {} bytes to {}", done, dest);
        }
    }

    private static void launchApplier(File installDir, File staging, File updater,
            File extracted) throws IOException {
        File logFile = new File(staging, LOG_NAME);
        ProcessBuilder pb = new ProcessBuilder(
                javaExecutable().getAbsolutePath(),
                "-cp", updater.getAbsolutePath(),
                UpdateApplier.class.getName(),
                installDir.getAbsolutePath(),
                extracted.getAbsolutePath());
        pb.directory(installDir);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(logFile));
        log.info("starting the update applier, its log goes to {}", logFile);
        pb.start();
    }

    private static File javaExecutable() {
        File home = new File(System.getProperty("java.home", "."));
        File exe = new File(new File(home, "bin"), "java.exe");
        if (exe.isFile())
            return exe;
        return new File(new File(home, "bin"), "java");
    }

    /**
     * delete a file or a whole directory tree, ignoring what cannot be
     * removed - a leftover is cleaned up on the next start.
     */
    static void deleteRecursively(File f) {
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
            log.debug("cannot delete {}", f);
    }

    /**
     * modal progress dialog of the download. It is built and shown on the
     * event dispatch thread while the download runs on its own thread; the
     * caller blocks in {@link #showAndWait()} until the download ends or the
     * user cancels it.
     */
    private static class ProgressDialog {
        private final AtomicBoolean cancelled;
        private JDialog dialog;
        private JProgressBar bar;
        private boolean finished = false;
        private long total = 0;
        private int lastPercent = -1;

        ProgressDialog(String title, long total, AtomicBoolean cancelled) throws Exception {
            this.cancelled = cancelled;
            this.total = total;
            SwingUtilities.invokeAndWait(() -> build(title));
        }

        private void build(String title) {
            dialog = new JDialog((java.awt.Frame)null,
                    I18n.t("common.update.progress.title"), true);
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    cancel();
                }
            });
            bar = new JProgressBar(0, 100);
            bar.setIndeterminate(total <= 0);
            bar.setStringPainted(total > 0);
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            content.add(new JLabel(title));
            content.add(Box.createVerticalStrut(10));
            content.add(bar);
            content.add(Box.createVerticalStrut(10));
            JButton cancelBtn = new JButton(I18n.t("common.button.cancel"));
            cancelBtn.addActionListener(e -> cancel());
            JPanel buttons = new JPanel(new BorderLayout());
            buttons.add(cancelBtn, BorderLayout.EAST);
            content.add(buttons);
            dialog.setContentPane(content);
            dialog.pack();
            dialog.setSize(Math.max(360, dialog.getWidth()), dialog.getHeight());
            dialog.setLocationRelativeTo(null);
        }

        private void cancel() {
            cancelled.set(true);
            finish();
        }

        void setTotal(long total) {
            SwingUtilities.invokeLater(() -> {
                this.total = total;
                bar.setIndeterminate(total <= 0);
                bar.setStringPainted(total > 0);
            });
        }

        void setDone(long done) {
            if (total <= 0)
                return;
            int percent = (int)Math.min(100, done * 100 / total);
            // repainting on every block would flood the event queue
            if (percent == lastPercent)
                return;
            lastPercent = percent;
            SwingUtilities.invokeLater(() -> bar.setValue(percent));
        }

        /** close the dialog, whatever thread the download ended on. */
        void finish() {
            SwingUtilities.invokeLater(() -> {
                finished = true;
                dialog.setVisible(false);
                dialog.dispose();
            });
        }

        /**
         * show the dialog and return once it is gone. The check for an
         * already finished download happens on the event dispatch thread, so
         * a download that ends before the dialog is up cannot leave it open.
         */
        void showAndWait() throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                if (!finished)
                    dialog.setVisible(true);
            });
        }
    }
}

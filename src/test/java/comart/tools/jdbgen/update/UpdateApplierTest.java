/*
 * The MIT License
 *
 * Copyright 2024 comart.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package comart.tools.jdbgen.update;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The update replaces what belongs to the release and keeps what belongs to
 * the user: the configuration, the downloaded drivers, the generated output,
 * the edited templates and the sample database.
 *
 * The restart is a method of its own so that no test ever starts a JVM.
 */
public class UpdateApplierTest {

    /** the retry loop is only there for a locked jar, no test needs it. */
    private static final long NO_WAIT = 0L;

    private static void write(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(Path file) throws Exception {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    /** an installation as it is after a while of being used. */
    private static Path installation(Path root) throws Exception {
        Path dir = root.resolve("install");
        write(dir.resolve("jdbgen-0.3.0.jar"), "old jar");
        write(dir.resolve("lib/gson-2.11.0.jar"), "old gson");
        write(dir.resolve("lib/okhttp-4.12.0.jar"), "old okhttp");
        write(dir.resolve("jdbgen.cmd"), "old launcher");
        write(dir.resolve("jdbgen.sh"), "old launcher");
        write(dir.resolve("resource/icon.png"), "old icon");
        write(dir.resolve("templates/java_model.hbs"), "edited by the user");
        write(dir.resolve("templates/my_own.hbs"), "written by the user");
        write(dir.resolve("config.json"), "{\"isDarkUI\":true}");
        write(dir.resolve("config.json.20240101_010101.bak"), "the backup");
        write(dir.resolve("drivers/ojdbc11.jar"), "downloaded driver");
        write(dir.resolve("output/Sample.java"), "generated");
        write(dir.resolve("sample_h2.db.mv.db"), "the sample data of the user");
        return dir;
    }

    /** the new release, as it comes out of the archive. */
    private static Path release(Path root) throws Exception {
        Path dir = root.resolve("extracted");
        write(dir.resolve("jdbgen-0.3.1.jar"), "new jar");
        write(dir.resolve("lib/gson-2.11.0.jar"), "new gson");
        write(dir.resolve("lib/flatlaf-3.7.jar"), "added by the new release");
        write(dir.resolve("jdbgen.cmd"), "new launcher");
        write(dir.resolve("jdbgen.sh"), "new launcher");
        write(dir.resolve("resource/icon.png"), "new icon");
        write(dir.resolve("templates/java_model.hbs"), "shipped template");
        write(dir.resolve("templates/php_ci_model.hbs"), "new template");
        write(dir.resolve("sample_h2.db.mv.db"), "the pristine sample");
        return dir;
    }

    @Test
    public void theReleaseFilesAreReplacedAndTheUserFilesAreKept(@TempDir Path root) throws Exception {
        Path install = installation(root);
        Path extracted = release(root);

        assertTrue(UpdateApplier.apply(install.toFile(), extracted.toFile(), NO_WAIT));

        // the jar and the dependencies belong to the release
        assertFalse(Files.exists(install.resolve("jdbgen-0.3.0.jar")),
                "two jars would make the launcher pick the wrong one");
        assertEquals("new jar", read(install.resolve("jdbgen-0.3.1.jar")));
        assertFalse(Files.exists(install.resolve("lib/okhttp-4.12.0.jar")),
                "a dependency dropped by the new release must not stay behind");
        assertEquals("new gson", read(install.resolve("lib/gson-2.11.0.jar")));
        assertEquals("added by the new release", read(install.resolve("lib/flatlaf-3.7.jar")));

        // so do the launcher scripts and the shipped resources
        assertEquals("new launcher", read(install.resolve("jdbgen.cmd")));
        assertEquals("new launcher", read(install.resolve("jdbgen.sh")));
        assertEquals("new icon", read(install.resolve("resource/icon.png")));

        // everything the user owns survives
        assertEquals("{\"isDarkUI\":true}", read(install.resolve("config.json")));
        assertEquals("the backup", read(install.resolve("config.json.20240101_010101.bak")));
        assertEquals("downloaded driver", read(install.resolve("drivers/ojdbc11.jar")));
        assertEquals("generated", read(install.resolve("output/Sample.java")));
        assertEquals("the sample data of the user", read(install.resolve("sample_h2.db.mv.db")));

        // templates are only added, never overwritten
        assertEquals("edited by the user", read(install.resolve("templates/java_model.hbs")));
        assertEquals("written by the user", read(install.resolve("templates/my_own.hbs")));
        assertEquals("new template", read(install.resolve("templates/php_ci_model.hbs")));
    }

    @Test
    public void aMissingTemplateDirectoryIsCreatedFromTheRelease(@TempDir Path root) throws Exception {
        Path install = installation(root);
        Path extracted = release(root);
        UpdateManager.deleteRecursively(install.resolve("templates").toFile());
        Files.delete(install.resolve("sample_h2.db.mv.db"));

        assertTrue(UpdateApplier.apply(install.toFile(), extracted.toFile(), NO_WAIT));

        assertEquals("shipped template", read(install.resolve("templates/java_model.hbs")));
        assertEquals("the pristine sample", read(install.resolve("sample_h2.db.mv.db")));
    }

    @Test
    public void nothingIsTouchedWhenTheReleaseHasNoJar(@TempDir Path root) throws Exception {
        Path install = installation(root);
        Path extracted = root.resolve("empty");
        Files.createDirectories(extracted);

        assertFalse(UpdateApplier.apply(install.toFile(), extracted.toFile(), NO_WAIT));

        assertEquals("old jar", read(install.resolve("jdbgen-0.3.0.jar")));
        assertEquals("old gson", read(install.resolve("lib/gson-2.11.0.jar")));
        assertEquals("old launcher", read(install.resolve("jdbgen.cmd")));
    }

    @Test
    public void aFailureWhileMovingTheOldVersionAsidePutsItBack(@TempDir Path root) throws Exception {
        Path install = installation(root);
        Path extracted = release(root);
        // a directory where a jar has to go makes the copy of lib/ fail, after
        // the jar of the installation has already been moved aside
        write(install.resolve(UpdateManager.STAGING_NAME)
                .resolve("backup/lib/gson-2.11.0.jar/in the way"), "blocked");

        assertFalse(UpdateApplier.apply(install.toFile(), extracted.toFile(), NO_WAIT));

        assertEquals("old jar", read(install.resolve("jdbgen-0.3.0.jar")),
                "without its jar the installation could not be started anymore");
        assertEquals("old gson", read(install.resolve("lib/gson-2.11.0.jar")));
        assertEquals("old okhttp", read(install.resolve("lib/okhttp-4.12.0.jar")));
        assertEquals("{\"isDarkUI\":true}", read(install.resolve("config.json")));
        assertFalse(Files.exists(install.resolve("jdbgen-0.3.1.jar")),
                "nothing of the new version was copied");
    }

    @Test
    public void theStagingDirectoryIsLeftWithNothingButTheUpdaterAndItsLog(@TempDir Path root)
            throws Exception {
        Path install = installation(root);
        Path staging = install.resolve(UpdateManager.STAGING_NAME);
        write(staging.resolve("updater.jar"), "the running updater");
        write(staging.resolve("update.log"), "what happened");
        write(staging.resolve("jdbgen-0.3.1.zip"), "the archive");
        write(staging.resolve("extracted/jdbgen-0.3.1.jar"), "the new jar");
        write(staging.resolve("backup/jdbgen-0.3.0.jar"), "the old jar");

        UpdateApplier.cleanup(install.toFile(), false);

        assertTrue(Files.exists(staging.resolve("updater.jar")),
                "a jar cannot delete itself while it runs on Windows");
        assertTrue(Files.exists(staging.resolve("update.log")),
                "the log is what tells the user what went wrong");
        assertFalse(Files.exists(staging.resolve("jdbgen-0.3.1.zip")));
        assertFalse(Files.exists(staging.resolve("extracted")));
        assertFalse(Files.exists(staging.resolve("backup")));
    }

    @Test
    public void afterAFailedUpdateThePreviousVersionIsKept(@TempDir Path root) throws Exception {
        Path install = installation(root);
        Path staging = install.resolve(UpdateManager.STAGING_NAME);
        write(staging.resolve("jdbgen-0.3.1.zip"), "the archive");
        write(staging.resolve("backup/jdbgen-0.3.0.jar"), "the old jar");

        UpdateApplier.cleanup(install.toFile(), true);

        assertEquals("the old jar", read(staging.resolve("backup/jdbgen-0.3.0.jar")),
                "a rollback that failed leaves this as the only copy");
        assertFalse(Files.exists(staging.resolve("jdbgen-0.3.1.zip")));
    }
}

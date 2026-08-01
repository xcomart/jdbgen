package comart.tools.jdbgen.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An unreadable configuration is never thrown away: it is moved aside, the user
 * is told where it went, and it is moved back when the replacement cannot be
 * written.
 *
 * Every test works inside a temporary directory - the real config.json of the
 * working directory must never be touched.
 */
public class JDBGenConfigBackupTest {

    private static Path writeConfig(Path dir, String content) throws Exception {
        Path f = dir.resolve("config.json");
        Files.writeString(f, content, StandardCharsets.UTF_8);
        return f;
    }

    @Test
    public void backupMovesTheFileAsideAndReportsWhereItWent(@TempDir Path dir) throws Exception {
        Path conf = writeConfig(dir, "{\"isDarkUI\":true}");

        Path backup = JDBGenConfig.backupExistingConfig(conf.toFile());

        assertNotNull(backup, "the caller has to learn where the configuration went");
        assertTrue(backup.isAbsolute(), "the path is shown to the user, so it must be absolute");
        assertEquals(dir.toAbsolutePath(), backup.getParent(),
                "the backup stays next to the configuration it was made from");
        assertTrue(backup.getFileName().toString().startsWith("config.json."));
        assertTrue(backup.getFileName().toString().endsWith(".bak"));

        assertFalse(Files.exists(conf), "the original name is free for the new configuration");
        assertEquals("{\"isDarkUI\":true}", Files.readString(backup, StandardCharsets.UTF_8),
                "the backup is the untouched original content");
    }

    @Test
    public void nothingIsBackedUpWhenThereIsNoConfiguration(@TempDir Path dir) {
        Path missing = dir.resolve("config.json");

        assertNull(JDBGenConfig.backupExistingConfig(missing.toFile()));
        assertNull(JDBGenConfig.backupExistingConfig(dir.toFile()),
                "a directory is not a configuration file");
    }

    @Test
    public void aFailedBackupIsReportedAsNull(@TempDir Path dir) throws Exception {
        // a file whose parent directory has been replaced by a file cannot be
        // moved anywhere; a null return keeps the caller from overwriting it.
        Path conf = writeConfig(dir, "{}");
        File unmovable = new File(conf.toFile(), "config.json") {
            @Override
            public boolean exists() {
                return true;
            }

            @Override
            public boolean isFile() {
                return true;
            }
        };

        assertNull(JDBGenConfig.backupExistingConfig(unmovable));
        assertTrue(Files.exists(conf), "the real file is left alone");
    }

    @Test
    public void restoringPutsTheConfigurationBackUnderItsOriginalName(@TempDir Path dir) throws Exception {
        Path conf = writeConfig(dir, "{\"isDarkUI\":true}");

        Path backup = JDBGenConfig.backupExistingConfig(conf.toFile());
        assertNotNull(backup);

        assertTrue(JDBGenConfig.restoreBackup(backup, conf.toFile()));

        assertFalse(Files.exists(backup), "the backup was moved back, not copied");
        assertTrue(Files.exists(conf));
        assertEquals("{\"isDarkUI\":true}", Files.readString(conf, StandardCharsets.UTF_8),
                "the user gets exactly the configuration back that was moved aside");
    }

    @Test
    public void restoringOverwritesAHalfWrittenReplacement(@TempDir Path dir) throws Exception {
        Path conf = writeConfig(dir, "original");
        Path backup = JDBGenConfig.backupExistingConfig(conf.toFile());
        assertNotNull(backup);
        // a failed save may well have left a truncated file behind
        Files.writeString(conf, "", StandardCharsets.UTF_8);

        assertTrue(JDBGenConfig.restoreBackup(backup, conf.toFile()));
        assertEquals("original", Files.readString(conf, StandardCharsets.UTF_8));
    }

    @Test
    public void restoringWithoutABackupFails(@TempDir Path dir) {
        Path conf = dir.resolve("config.json");

        assertFalse(JDBGenConfig.restoreBackup(null, conf.toFile()));
        assertFalse(JDBGenConfig.restoreBackup(dir.resolve("nothing.bak"), conf.toFile()));
        assertFalse(Files.exists(conf), "a missing backup never creates a configuration");
    }
}

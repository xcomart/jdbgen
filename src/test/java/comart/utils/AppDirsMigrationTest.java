package comart.utils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Carrying the files of a release that kept everything next to the application
 * over into the user data directory. Nothing is moved: the previous
 * installation keeps working exactly as it did.
 */
public class AppDirsMigrationTest {

    private String userDir;

    @AfterEach
    public void clearOverrides() {
        System.clearProperty(AppDirs.DATA_DIR_PROPERTY);
        System.clearProperty(AppDirs.RESOURCE_BASE_PROPERTY);
        if (userDir != null)
            System.setProperty("user.dir", userDir);
        userDir = null;
    }

    /**
     * an unpacked archive below <code>install/</code>, an empty user data
     * directory below <code>data/</code>. The working directory is moved out
     * of the way as well - the checkout the tests run in carries a
     * config.json of its own, and it is a legacy location too.
     */
    private Path prepare(Path dir, boolean withLegacyConfig) throws Exception {
        Path install = dir.resolve("install");
        Path data = dir.resolve("data");
        Files.createDirectories(install);
        Files.createDirectories(data);
        Files.createDirectories(dir.resolve("cwd"));
        userDir = System.getProperty("user.dir");
        System.setProperty("user.dir", dir.resolve("cwd").toString());
        System.setProperty(AppDirs.RESOURCE_BASE_PROPERTY, install.toString());
        System.setProperty(AppDirs.DATA_DIR_PROPERTY, data.toString());
        if (withLegacyConfig) {
            write(install.resolve("config.json"), "{\"language\":\"ko\"}");
            write(install.resolve("config.json.20240101_101010.bak"), "{}");
            write(install.resolve("drivers/h2.jar"), "jar");
            write(install.resolve("drivers/nested/pg.jar"), "jar");
        }
        return data;
    }

    private static void write(Path f, String content) throws Exception {
        Files.createDirectories(f.getParent());
        Files.write(f, content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void theConfigurationAndTheDriversAreCopiedOver(@TempDir Path dir) throws Exception {
        Path data = prepare(dir, true);

        assertTrue(AppDirs.migrateLegacyData());

        assertEquals("{\"language\":\"ko\"}",
                Files.readString(data.resolve("config.json"), StandardCharsets.UTF_8));
        assertTrue(Files.isRegularFile(data.resolve("config.json.20240101_101010.bak")),
                "the backups of the configuration are carried over as well");
        assertTrue(Files.isRegularFile(data.resolve("drivers/h2.jar")));
        assertTrue(Files.isRegularFile(data.resolve("drivers/nested/pg.jar")),
                "the driver directory is copied as a whole");
        // the previous installation has to keep working
        assertTrue(Files.isRegularFile(dir.resolve("install/config.json")));
        assertTrue(Files.isRegularFile(dir.resolve("install/drivers/h2.jar")));
    }

    @Test
    public void anExistingConfigurationIsNeverOverwritten(@TempDir Path dir) throws Exception {
        Path data = prepare(dir, true);
        write(data.resolve("config.json"), "{\"language\":\"ja\"}");

        assertFalse(AppDirs.migrateLegacyData());

        assertEquals("{\"language\":\"ja\"}",
                Files.readString(data.resolve("config.json"), StandardCharsets.UTF_8));
        assertFalse(Files.exists(data.resolve("drivers")),
                "nothing at all is touched once the user data directory is in use");
    }

    @Test
    public void nothingHappensWithoutAPreviousInstallation(@TempDir Path dir) throws Exception {
        Path data = prepare(dir, false);

        assertFalse(AppDirs.migrateLegacyData());

        assertFalse(Files.exists(data.resolve("config.json")));
        assertEquals(0, data.toFile().list().length);
    }

    @Test
    public void theWorkingDirectoryIsALegacyLocationToo(@TempDir Path dir) throws Exception {
        Path data = prepare(dir, false);
        // started from the unpacked archive with a different working directory:
        // the installation has nothing, the working directory has everything
        write(dir.resolve("cwd/config.json"), "{\"language\":\"es\"}");
        write(dir.resolve("cwd/drivers/h2.jar"), "jar");

        assertTrue(AppDirs.migrateLegacyData());

        assertEquals("{\"language\":\"es\"}",
                Files.readString(data.resolve("config.json"), StandardCharsets.UTF_8));
        assertTrue(Files.isRegularFile(data.resolve("drivers/h2.jar")));
    }

    @Test
    public void aLegacyDirectoryWithoutDriversIsCopiedAllTheSame(@TempDir Path dir) throws Exception {
        Path data = prepare(dir, false);
        write(dir.resolve("install/config.json"), "{}");

        assertTrue(AppDirs.migrateLegacyData(dir.resolve("install").toFile(), data.toFile()));

        assertTrue(Files.isRegularFile(data.resolve("config.json")));
        assertFalse(Files.exists(data.resolve("drivers")));
    }

    @Test
    public void aLegacyDirectoryWithoutAConfigurationIsNotMigrated(@TempDir Path dir) throws Exception {
        Path data = prepare(dir, false);

        assertFalse(AppDirs.migrateLegacyData(dir.resolve("install").toFile(), data.toFile()));
        assertEquals(0, data.toFile().list().length);
    }
}

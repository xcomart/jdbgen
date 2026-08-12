package comart.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two directories the application uses: the per user location it writes to
 * and the installation it reads its resources from. An installation below
 * <code>C:\Program Files</code> is read only for the user running it, so
 * nothing may be written next to the application anymore.
 */
public class AppDirsTest {

    @AfterEach
    public void clearOverrides() {
        System.clearProperty(AppDirs.DATA_DIR_PROPERTY);
        System.clearProperty(AppDirs.RESOURCE_BASE_PROPERTY);
    }

    private static File subDir(Path dir, String name) {
        return new File(dir.toFile(), name).getAbsoluteFile();
    }

    @Test
    public void windowsKeepsItsFilesBelowAppData(@TempDir Path dir) {
        File appData = subDir(dir, "Roaming");
        File home = subDir(dir, "home");

        assertEquals(new File(appData, "jdbgen"),
                AppDirs.defaultUserDataDir(PlatformUtils.OSType.Windows,
                        appData.getPath(), null, home.getPath()));
    }

    @Test
    public void windowsFallsBackToTheHomeDirectoryWithoutAppData(@TempDir Path dir) {
        File home = subDir(dir, "home");

        File expected = new File(new File(new File(home, "AppData"), "Roaming"), "jdbgen");
        assertEquals(expected, AppDirs.defaultUserDataDir(
                PlatformUtils.OSType.Windows, null, null, home.getPath()));
        assertEquals(expected, AppDirs.defaultUserDataDir(
                PlatformUtils.OSType.Windows, "  ", null, home.getPath()));
    }

    @Test
    public void macOsUsesTheApplicationSupportDirectory(@TempDir Path dir) {
        File home = subDir(dir, "home");

        assertEquals(new File(new File(new File(home, "Library"), "Application Support"), "jdbgen"),
                AppDirs.defaultUserDataDir(PlatformUtils.OSType.MacOS,
                        null, null, home.getPath()));
    }

    @Test
    public void unixPrefersTheXdgConfigDirectory(@TempDir Path dir) {
        File home = subDir(dir, "home");
        File xdg = subDir(dir, "xdg");

        assertEquals(new File(xdg, "jdbgen"), AppDirs.defaultUserDataDir(
                PlatformUtils.OSType.Unix, null, xdg.getPath(), home.getPath()));
        assertEquals(new File(new File(home, ".config"), "jdbgen"),
                AppDirs.defaultUserDataDir(PlatformUtils.OSType.Unix,
                        null, null, home.getPath()));
        // an unknown platform is treated like any other Unix
        assertEquals(new File(new File(home, ".config"), "jdbgen"),
                AppDirs.defaultUserDataDir(PlatformUtils.OSType.Other,
                        null, "   ", home.getPath()));
    }

    @Test
    public void everyPlatformAnswersWithAnAbsolutePath() {
        for (PlatformUtils.OSType os: PlatformUtils.OSType.values()) {
            File res = AppDirs.defaultUserDataDir(os, null, null, null);
            assertTrue(res.isAbsolute(), os + " has to answer with an absolute path");
            assertEquals("jdbgen", res.getName());
        }
    }

    @Test
    public void theSystemPropertyOverridesThePlatformLocation(@TempDir Path dir) {
        File data = subDir(dir, "portable-data");
        System.setProperty(AppDirs.DATA_DIR_PROPERTY, data.getPath());

        assertEquals(data, AppDirs.configuredUserDataDir());
        assertEquals(data, AppDirs.userDataDir());
        assertTrue(data.isDirectory(), "the directory is created on the first access");
        assertEquals(new File(data, "config.json"), AppDirs.userDataFile("config.json"));
        assertEquals(new File(data, "drivers"), AppDirs.driversDir());
        assertTrue(new File(data, "drivers").isDirectory());
    }

    @Test
    public void theSystemPropertyOverridesTheInstallation(@TempDir Path dir) {
        File install = subDir(dir, "install");
        System.setProperty(AppDirs.RESOURCE_BASE_PROPERTY, install.getPath());

        assertEquals(install, AppDirs.installResourceBase());
        assertEquals(new File(install, "resource/icon.png").getAbsoluteFile(),
                AppDirs.installResourceFile("resource/icon.png").getAbsoluteFile());
    }

    /**
     * both directories below one temporary directory, as a test runs with.
     */
    private static void useDirectories(Path dir) throws Exception {
        Path data = dir.resolve("data");
        Path install = dir.resolve("install");
        Files.createDirectories(data);
        Files.createDirectories(install);
        System.setProperty(AppDirs.DATA_DIR_PROPERTY, data.toString());
        System.setProperty(AppDirs.RESOURCE_BASE_PROPERTY, install.toString());
    }

    private static void write(Path dir, String path) throws Exception {
        Path f = dir.resolve(path);
        Files.createDirectories(f.getParent());
        Files.write(f, "x".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void aRelativePathIsLookedForInTheUserDataDirectoryFirst(@TempDir Path dir) throws Exception {
        useDirectories(dir);
        write(dir, "data/templates/java_model.java");
        write(dir, "install/templates/java_model.java");

        assertEquals(dir.resolve("data/templates/java_model.java").toFile(),
                AppDirs.resolve("templates/java_model.java"));
    }

    @Test
    public void aRelativePathFallsBackToTheInstallation(@TempDir Path dir) throws Exception {
        useDirectories(dir);
        write(dir, "install/templates/java_model.java");

        assertEquals(dir.resolve("install/templates/java_model.java").toFile(),
                AppDirs.resolve("templates/java_model.java"));
        assertEquals(dir.resolve("install/templates/java_model.java").toFile().getPath(),
                AppDirs.resolvePath("templates/java_model.java"));
    }

    @Test
    public void aPathThatNamesNothingPointsAtTheUserDataDirectory(@TempDir Path dir) throws Exception {
        useDirectories(dir);

        // the failure has to name the location the file is expected in
        assertEquals(dir.resolve("data/drivers/missing.jar").toFile(),
                AppDirs.resolve("drivers/missing.jar"));
    }

    @Test
    public void anAbsolutePathIsTakenAsItIs(@TempDir Path dir) throws Exception {
        useDirectories(dir);
        Path outside = dir.resolve("elsewhere/h2.jar");
        write(dir, "elsewhere/h2.jar");

        assertEquals(outside.toFile(), AppDirs.resolve(outside.toString()));
        assertEquals(outside.toString(), AppDirs.resolvePath(outside.toString()));
    }

    @Test
    public void anEmptyPathNamesNoFile(@TempDir Path dir) throws Exception {
        useDirectories(dir);

        assertNull(AppDirs.resolve(null));
        assertNull(AppDirs.resolve(""));
        assertNull(AppDirs.resolve("   "));
        assertEquals("", AppDirs.resolvePath(""));
    }

    @Test
    public void aStoredPathIsKeptRelativeToTheDirectoryItIsBelow(@TempDir Path dir) throws Exception {
        useDirectories(dir);

        assertEquals("drivers/h2.jar",
                AppDirs.relativize(dir.resolve("data/drivers/h2.jar").toString()));
        assertEquals("templates/java_model.java",
                AppDirs.relativize(dir.resolve("install/templates/java_model.java").toString()));
        // anything else stays the absolute path it is
        String outside = dir.resolve("elsewhere/h2.jar").toFile().getAbsolutePath();
        assertEquals(outside, AppDirs.relativize(outside));
        assertNull(AppDirs.relativize(null));
    }

    @Test
    public void relativizingRoundTripsThroughResolve(@TempDir Path dir) throws Exception {
        useDirectories(dir);
        write(dir, "data/drivers/h2.jar");

        String stored = AppDirs.relativize(dir.resolve("data/drivers/h2.jar").toString());

        assertEquals(dir.resolve("data/drivers/h2.jar").toFile(), AppDirs.resolve(stored));
    }

    /**
     * the output directory is read back the same way the directory chooser
     * stored it. It used to be handed to the file API as it is, which wrote the
     * generated files below the working directory - a different directory than
     * the one that was picked.
     */
    @Test
    public void theOutputDirectoryIsResolvedLikeEveryOtherConfiguredPath(@TempDir Path dir) throws Exception {
        useDirectories(dir);
        String stored = AppDirs.relativize(dir.resolve("data/output").toString());
        assertEquals("output", stored, "the chooser stores it relative to the user data directory");

        String resolved = AppDirs.resolveOutputDir(stored);

        assertEquals(dir.resolve("data/output").toFile().getPath(), resolved);
        assertTrue(dir.resolve("data/output").toFile().isDirectory(),
                "the directory is created, so that it can be opened afterwards");
    }

    @Test
    public void anAbsoluteOrEmptyOutputDirectoryIsLeftAlone(@TempDir Path dir) throws Exception {
        useDirectories(dir);
        File outside = dir.resolve("elsewhere/generated").toFile();

        assertEquals(outside.getPath(), AppDirs.resolveOutputDir(outside.getPath()));
        assertTrue(outside.isDirectory());
        // no output directory stays no output directory
        assertEquals("", AppDirs.resolveOutputDir(""));
        assertNull(AppDirs.resolveOutputDir(null));
    }

    @Test
    public void writabilityIsDecidedByActuallyWritingAFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("not-a-directory");
        Files.write(file, "x".getBytes(StandardCharsets.UTF_8));

        assertTrue(AppDirs.isWritable(dir.toFile()));
        assertFalse(AppDirs.isWritable(file.toFile()), "a file is not a directory");
        assertFalse(AppDirs.isWritable(dir.resolve("missing").toFile()));
        assertFalse(AppDirs.isWritable(null));
        assertEquals(1, dir.toFile().list().length, "the probe file is removed again");
    }

    @Test
    public void theInstallationIsCheckedForWritability(@TempDir Path dir) throws Exception {
        useDirectories(dir);

        assertTrue(AppDirs.isInstallWritable());

        System.setProperty(AppDirs.RESOURCE_BASE_PROPERTY,
                dir.resolve("does-not-exist").toString());
        assertFalse(AppDirs.isInstallWritable());
    }
}

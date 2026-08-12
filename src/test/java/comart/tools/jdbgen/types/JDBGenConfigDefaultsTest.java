package comart.tools.jdbgen.types;

import comart.utils.AppDirs;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sample connection of a fresh configuration. It has to work when the
 * application is installed in a directory the user cannot write to, so the
 * sample database is copied next to the configuration and every path it
 * carries is absolute - a relative one would be read from wherever the
 * application happens to be started.
 */
public class JDBGenConfigDefaultsTest {

    @AfterEach
    public void clearOverrides() {
        System.clearProperty(AppDirs.DATA_DIR_PROPERTY);
        System.clearProperty(AppDirs.RESOURCE_BASE_PROPERTY);
    }

    /**
     * a release as it is installed: templates and the sample database below
     * the installation, an empty user data directory.
     */
    private Path prepare(Path dir, boolean withSampleDb) throws Exception {
        Path install = dir.resolve("install");
        Path data = dir.resolve("data");
        Files.createDirectories(install.resolve("templates"));
        Files.createDirectories(data);
        for (String t: new String[]{"java_model.java", "mybatis_mapper.xml", "php_ci.php"})
            Files.write(install.resolve("templates").resolve(t), "x".getBytes(StandardCharsets.UTF_8));
        if (withSampleDb)
            Files.write(install.resolve(JDBGenConfig.SAMPLE_DB_FILE),
                    "sample".getBytes(StandardCharsets.UTF_8));
        System.setProperty(AppDirs.RESOURCE_BASE_PROPERTY, install.toString());
        System.setProperty(AppDirs.DATA_DIR_PROPERTY, data.toString());
        return data;
    }

    @Test
    public void theSampleDatabaseIsCopiedNextToTheConfiguration(@TempDir Path dir) throws Exception {
        Path data = prepare(dir, true);

        JDBConnection conn = JDBGenConfig.createSampleConnection();

        Path copy = data.resolve(JDBGenConfig.SAMPLE_DB_FILE);
        assertTrue(Files.isRegularFile(copy), "the installation may be read only");
        assertEquals("sample", Files.readString(copy, StandardCharsets.UTF_8));
        // H2 appends '.mv.db' itself, and understands '/' on every platform
        String expected = "jdbc:h2:" + data.resolve(JDBGenConfig.SAMPLE_DB_NAME)
                .toFile().getAbsolutePath().replace('\\', '/');
        assertEquals(expected, conn.getConnectionUrl());
    }

    @Test
    public void anExistingSampleDatabaseIsKept(@TempDir Path dir) throws Exception {
        Path data = prepare(dir, true);
        Path copy = data.resolve(JDBGenConfig.SAMPLE_DB_FILE);
        Files.write(copy, "mine".getBytes(StandardCharsets.UTF_8));

        JDBGenConfig.createSampleConnection();

        assertEquals("mine", Files.readString(copy, StandardCharsets.UTF_8),
                "a database the user already worked with is never overwritten");
    }

    @Test
    public void aReleaseWithoutTheSampleDatabaseStillGetsAConnection(@TempDir Path dir) throws Exception {
        Path data = prepare(dir, false);

        JDBConnection conn = JDBGenConfig.createSampleConnection();

        assertFalse(Files.exists(data.resolve(JDBGenConfig.SAMPLE_DB_FILE)));
        assertTrue(conn.getConnectionUrl().startsWith("jdbc:h2:"));
    }

    @Test
    public void theTemplatesAreReadOutOfTheInstallation(@TempDir Path dir) throws Exception {
        prepare(dir, true);

        List<JDBTemplate> templates = JDBGenConfig.createSampleConnection().getTemplates();

        assertEquals(3, templates.size());
        for (JDBTemplate t: templates) {
            Path expected = dir.resolve("install/templates");
            assertTrue(new java.io.File(t.getTemplateFile()).isAbsolute(),
                    t.getTemplateFile() + " has to be absolute");
            assertEquals(expected.toFile().getAbsolutePath(),
                    new java.io.File(t.getTemplateFile()).getParentFile().getAbsolutePath());
            assertTrue(new java.io.File(t.getTemplateFile()).isFile(),
                    t.getTemplateFile() + " has to name a shipped template");
        }
    }

    @Test
    public void theGeneratedSourcesGoBelowTheUserDataDirectory(@TempDir Path dir) throws Exception {
        Path data = prepare(dir, true);

        JDBConnection conn = JDBGenConfig.createSampleConnection();

        assertEquals(data.resolve("output").toFile().getAbsolutePath(), conn.getOutputDir());
    }
}

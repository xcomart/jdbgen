package comart.tools.jdbgen.types;

import comart.utils.AppDirs;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The language has to be known before the master password is asked for, so it
 * is read straight out of the configuration file. Whatever is wrong with that
 * file is reported later, by the load that needs the rest of it - the peek only
 * ever falls back to the system language.
 */
public class JDBGenConfigLanguageTest {

    private static Path write(Path dir, String content) throws Exception {
        Path f = dir.resolve("config.json");
        Files.write(f, content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    @Test
    public void theStoredLanguageIsRead(@TempDir Path dir) throws Exception {
        Path f = write(dir, "{\"isDarkUI\":true,\"language\":\"ko\"}");

        assertEquals("ko", JDBGenConfig.peekLanguage(f.toFile()));
    }

    @Test
    public void surroundingWhitespaceIsRemoved(@TempDir Path dir) throws Exception {
        Path f = write(dir, "{\"language\":\"  en  \"}");

        assertEquals("en", JDBGenConfig.peekLanguage(f.toFile()));
    }

    @Test
    public void theSystemValueIsPassedOnAsItIs(@TempDir Path dir) throws Exception {
        // I18n.toLocale() is what turns it into "keep the system locale"
        Path f = write(dir, "{\"language\":\"system\"}");

        assertEquals("system", JDBGenConfig.peekLanguage(f.toFile()));
    }

    @Test
    public void aConfigurationWithoutTheEntryHasNoLanguage(@TempDir Path dir) throws Exception {
        assertNull(JDBGenConfig.peekLanguage(write(dir, "{\"isDarkUI\":false}").toFile()));
        assertNull(JDBGenConfig.peekLanguage(write(dir, "{}").toFile()));
    }

    @Test
    public void anEmptyOrNullEntryHasNoLanguage(@TempDir Path dir) throws Exception {
        assertNull(JDBGenConfig.peekLanguage(write(dir, "{\"language\":\"\"}").toFile()));
        assertNull(JDBGenConfig.peekLanguage(write(dir, "{\"language\":\"   \"}").toFile()));
        assertNull(JDBGenConfig.peekLanguage(write(dir, "{\"language\":null}").toFile()));
    }

    @Test
    public void aBrokenConfigurationHasNoLanguage(@TempDir Path dir) throws Exception {
        assertNull(JDBGenConfig.peekLanguage(write(dir, "{not json at all").toFile()));
        assertNull(JDBGenConfig.peekLanguage(write(dir, "[1,2,3]").toFile()));
        assertNull(JDBGenConfig.peekLanguage(write(dir, "").toFile()));
    }

    @Test
    public void aMissingConfigurationHasNoLanguage(@TempDir Path dir) {
        assertNull(JDBGenConfig.peekLanguage(dir.resolve("config.json").toFile()));
        assertNull(JDBGenConfig.peekLanguage(dir.toFile()), "a directory is not a configuration");
        assertNull(JDBGenConfig.peekLanguage((java.io.File)null));
    }

    /**
     * The configuration lives in the user data directory of the operating
     * system, not next to the application - which may well be installed
     * somewhere the user cannot write to.
     */
    @Test
    public void theConfigurationIsReadFromTheUserDataDirectory(@TempDir Path dir) throws Exception {
        System.setProperty(AppDirs.DATA_DIR_PROPERTY, dir.toString());
        try {
            assertEquals(dir.resolve("config.json").toFile().getAbsoluteFile(),
                    JDBGenConfig.configFile().getAbsoluteFile());
            assertNull(JDBGenConfig.peekLanguage(), "there is no configuration yet");

            write(dir, "{\"language\":\"ja\"}");

            assertEquals("ja", JDBGenConfig.peekLanguage());
        } finally {
            System.clearProperty(AppDirs.DATA_DIR_PROPERTY);
        }
    }

    @Test
    public void theLanguageSurvivesASaveAndLoadCycle() {
        JDBGenConfig conf = new JDBGenConfig();
        assertNull(conf.getLanguage(), "the system language is the default");

        conf.setLanguage("ko");
        assertEquals("ko", conf.getLanguage());
    }
}

package comart.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped bundles themselves: a translation that quietly lost an entry
 * would only show up as an English string in a Korean dialog, which nobody
 * notices until a user reports it.
 *
 * Every bundle below <code>src/main/resources/i18n/</code> is checked against
 * every supported language, so a newly added bundle is covered without
 * touching this test. A new language is added to {@link #LANGUAGES} - and to
 * the language combo of the main window.
 */
public class I18nBundleTest {
    private static final File BUNDLE_DIR = new File("src/main/resources/i18n");

    /**
     * the locale suffixes every bundle has to ship a translation for. Keep in
     * step with <code>JDBGeneratorMain.LANGUAGES</code>.
     */
    private static final String[] LANGUAGES = { "ko", "es", "ja", "zh_CN" };

    private static Properties load(File file) throws Exception {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(file)) {
            props.loadFromXML(is);
        }
        return props;
    }

    /**
     * the bundle files without a locale suffix - the English originals.
     */
    private static List<File> baseBundles() {
        List<File> res = new ArrayList<>();
        File[] files = BUNDLE_DIR.listFiles();
        assertTrue(files != null && files.length > 0,
                BUNDLE_DIR + " is expected to hold the translation bundles");
        for (File f: files) {
            String name = f.getName();
            if (name.endsWith(".xml") && !name.contains("_"))
                res.add(f);
        }
        assertTrue(!res.isEmpty(), "no base bundle found in " + BUNDLE_DIR);
        return res;
    }

    private static File translationOf(File base, String language) {
        String name = base.getName();
        return new File(base.getParentFile(),
                name.substring(0, name.length() - 4) + "_" + language + ".xml");
    }

    @Test
    public void everyBundleCarriesEverySupportedLanguage() {
        for (File base: baseBundles()) {
            for (String language: LANGUAGES) {
                assertTrue(translationOf(base, language).isFile(),
                        base.getName() + " has no " + language + " counterpart "
                        + translationOf(base, language).getName());
            }
        }
    }

    @Test
    public void everyTranslationCarriesExactlyTheKeysOfTheOriginal() throws Exception {
        for (File base: baseBundles()) {
            Set<String> english = new TreeSet<>(load(base).stringPropertyNames());
            assertTrue(english.size() > 0, base.getName() + " is empty");
            for (String language: LANGUAGES) {
                File file = translationOf(base, language);
                if (!file.isFile())
                    continue; // the missing file has its own test
                Set<String> translated = new TreeSet<>(load(file).stringPropertyNames());
                assertEquals(english, translated, "every entry of " + base.getName()
                        + " needs a counterpart in " + file.getName() + " and vice versa");
            }
        }
    }

    @Test
    public void everyKeyIsPrefixedWithItsBundleName() throws Exception {
        for (File base: baseBundles()) {
            String name = base.getName();
            String prefix = name.substring(0, name.length() - 4) + ".";
            for (String key: load(base).stringPropertyNames()) {
                assertTrue(key.startsWith(prefix),
                        "'" + key + "' is looked up in i18n/" + name +
                        ", so it has to start with '" + prefix + "'");
            }
        }
    }

    @Test
    public void everyTranslationUsesThePlaceholdersOfTheOriginal() throws Exception {
        for (File base: baseBundles()) {
            Properties english = load(base);
            for (String language: LANGUAGES) {
                File file = translationOf(base, language);
                if (!file.isFile())
                    continue;
                Properties translated = load(file);
                for (String key: new TreeSet<>(english.stringPropertyNames())) {
                    String value = translated.getProperty(key);
                    if (value == null)
                        continue; // the key set mismatch has its own test
                    assertEquals(placeholders(english.getProperty(key)), placeholders(value),
                            "'" + key + "' uses different arguments in " + file.getName());
                }
            }
        }
    }

    @Test
    public void everyEntryIsAValidMessagePattern() throws Exception {
        for (File base: baseBundles()) {
            List<File> all = new ArrayList<>();
            all.add(base);
            for (String language: LANGUAGES)
                all.add(translationOf(base, language));
            for (File file: all) {
                if (!file.isFile())
                    continue;
                Properties props = load(file);
                for (String key: new TreeSet<>(props.stringPropertyNames())) {
                    String pattern = props.getProperty(key);
                    try {
                        new MessageFormat(pattern);
                    } catch (IllegalArgumentException e) {
                        throw new AssertionError(file.getName() + " '" + key +
                                "' is not a message pattern: " + pattern, e);
                    }
                }
            }
        }
    }

    /**
     * the argument indexes a pattern refers to, in order.
     */
    private static List<Integer> placeholders(String pattern) {
        MessageFormat fmt = new MessageFormat(pattern);
        List<Integer> res = new ArrayList<>();
        // formatsByArgumentIndex() has one slot per argument the pattern uses
        for (int i=0; i<fmt.getFormatsByArgumentIndex().length; i++)
            res.add(i);
        return res;
    }
}

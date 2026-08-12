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
 * Every bundle below <code>src/main/resources/i18n/</code> is checked, so a
 * newly added one is covered without touching this test.
 */
public class I18nBundleTest {
    private static final File BUNDLE_DIR = new File("src/main/resources/i18n");

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

    private static File koreanOf(File base) {
        String name = base.getName();
        return new File(base.getParentFile(),
                name.substring(0, name.length() - 4) + "_ko.xml");
    }

    @Test
    public void everyBundleCarriesAKoreanTranslation() {
        for (File base: baseBundles()) {
            assertTrue(koreanOf(base).isFile(),
                    base.getName() + " has no Korean counterpart " + koreanOf(base).getName());
        }
    }

    @Test
    public void theKoreanTranslationCarriesExactlyTheKeysOfTheOriginal() throws Exception {
        for (File base: baseBundles()) {
            Set<String> english = new TreeSet<>(load(base).stringPropertyNames());
            Set<String> korean = new TreeSet<>(load(koreanOf(base)).stringPropertyNames());

            assertTrue(english.size() > 0, base.getName() + " is empty");
            assertEquals(english, korean,
                    "every entry of " + base.getName() + " needs a Korean counterpart and vice versa");
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
    public void bothBundlesUseTheSamePlaceholders() throws Exception {
        for (File base: baseBundles()) {
            Properties english = load(base);
            Properties korean = load(koreanOf(base));

            for (String key: new TreeSet<>(english.stringPropertyNames())) {
                String kor = korean.getProperty(key);
                if (kor == null)
                    continue; // the key set mismatch has its own test
                assertEquals(placeholders(english.getProperty(key)), placeholders(kor),
                        "'" + key + "' uses different arguments in the two languages");
            }
        }
    }

    @Test
    public void everyEntryIsAValidMessagePattern() throws Exception {
        for (File base: baseBundles()) {
            for (File file: new File[] { base, koreanOf(base) }) {
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

package comart.utils;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped bundles themselves: a translation that quietly lost an entry
 * would only show up as an English string in a Korean dialog, which nobody
 * notices until a user reports it.
 */
public class I18nBundleTest {

    private static Properties load(String resource) throws Exception {
        Properties props = new Properties();
        try (InputStream is = I18nBundleTest.class.getResourceAsStream(resource)) {
            assertNotNull(is, resource + " is not on the class path");
            props.loadFromXML(is);
        }
        return props;
    }

    @Test
    public void theKoreanTranslationCarriesExactlyTheKeysOfTheOriginal() throws Exception {
        Set<String> english = new TreeSet<>(load("/i18n/common.xml").stringPropertyNames());
        Set<String> korean = new TreeSet<>(load("/i18n/common_ko.xml").stringPropertyNames());

        assertTrue(english.size() > 0, "the original bundle is empty");
        assertEquals(english, korean,
                "every entry of i18n/common.xml needs a Korean counterpart and vice versa");
    }

    @Test
    public void everyKeyIsPrefixedWithItsBundleName() throws Exception {
        for (String key: load("/i18n/common.xml").stringPropertyNames()) {
            assertTrue(key.startsWith("common."),
                    "'" + key + "' is looked up in i18n/common.xml, so it has to start with 'common.'");
        }
    }

    @Test
    public void bothBundlesUseTheSamePlaceholders() throws Exception {
        Properties english = load("/i18n/common.xml");
        Properties korean = load("/i18n/common_ko.xml");

        for (String key: new TreeSet<>(english.stringPropertyNames())) {
            assertEquals(placeholders(english.getProperty(key)),
                    placeholders(korean.getProperty(key)),
                    "'" + key + "' uses different arguments in the two languages");
        }
    }

    @Test
    public void everyEntryIsAValidMessagePattern() throws Exception {
        for (String resource: new String[] { "/i18n/common.xml", "/i18n/common_ko.xml" }) {
            Properties props = load(resource);
            for (String key: new TreeSet<>(props.stringPropertyNames())) {
                String pattern = props.getProperty(key);
                try {
                    new MessageFormat(pattern);
                } catch (IllegalArgumentException e) {
                    throw new AssertionError(
                            resource + " '" + key + "' is not a message pattern: " + pattern, e);
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

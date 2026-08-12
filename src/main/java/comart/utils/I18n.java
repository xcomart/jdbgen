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

package comart.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * The translation lookup of every user visible string.
 *
 * <p>Bundles are plain {@link Properties} XML documents below the class path
 * directory <code>i18n/</code>, one file per bundle plus one per translated
 * locale: <code>i18n/common.xml</code> holds the English originals,
 * <code>i18n/common_ko.xml</code> the Korean translation. English is the
 * default: a bundle without a locale suffix is what every locale falls back
 * to.</p>
 *
 * <p>A key carries the bundle it lives in as its first segment, and is looked
 * up in that bundle <em>as a whole</em>: <code>common.update.available</code>
 * is the entry <code>common.update.available</code> of
 * <code>i18n/common.xml</code>. That keeps a key unique across the whole
 * application while still telling the reader which file to edit.</p>
 *
 * <p>Nothing here ever throws. A missing bundle, a missing key or a broken
 * message pattern is logged and the key is returned instead, so a half
 * translated build still starts and stays usable.</p>
 *
 * @author comart
 */
@Slf4j
public final class I18n {
    /** class path directory the bundle files live in. */
    private static final String BUNDLE_PACKAGE = "i18n";
    /** language tag standing for "whatever the operating system says". */
    public static final String SYSTEM_LANGUAGE = "system";

    private static final ResourceBundle.Control CONTROL = new XmlControl();

    private static volatile Locale locale = Locale.getDefault();

    private I18n() {
    }

    /**
     * Use <code>loc</code> for every following lookup and drop what has been
     * cached for the previous one.
     *
     * @param loc <code>null</code> selects the JVM default locale.
     */
    public static synchronized void init(Locale loc) {
        locale = loc == null ? Locale.getDefault() : loc;
        ResourceBundle.clearCache(I18n.class.getClassLoader());
        log.info("user interface language: {}", locale.toLanguageTag());
    }

    /**
     * @return the locale the lookups currently use.
     */
    public static Locale getLocale() {
        return locale;
    }

    /**
     * Translate a stored language setting into a locale.
     *
     * @param language <code>"en"</code>, <code>"ko"</code>, ... or
     *                 <code>null</code>/empty/<code>"system"</code>
     * @return <code>null</code> when the operating system locale is to be kept.
     */
    public static Locale toLocale(String language) {
        if (StrUtils.isEmpty(language))
            return null;
        String tag = language.trim().replace('_', '-');
        if (SYSTEM_LANGUAGE.equalsIgnoreCase(tag))
            return null;
        Locale res = Locale.forLanguageTag(tag);
        if (res.getLanguage().isEmpty()) {
            log.warn("'{}' is not a language tag, keeping the system locale.", language);
            return null;
        }
        return res;
    }

    /**
     * Apply a stored language setting to the whole JVM: an explicit language
     * becomes the default locale as well, so that the Swing components and the
     * platform dialogs speak it too. An unset setting keeps the operating
     * system locale.
     *
     * @param language the value of the <code>language</code> configuration
     *                 entry, see {@link #toLocale(String)}
     */
    public static void applyLanguage(String language) {
        Locale loc = toLocale(language);
        if (loc != null)
            Locale.setDefault(loc);
        init(loc);
    }

    /**
     * @return the translation of <code>key</code>, or <code>key</code> itself
     *         when there is none.
     */
    public static String t(String key) {
        if (key == null)
            return "";
        int idx = key.indexOf('.');
        if (idx <= 0) {
            log.warn("'{}' does not name a bundle, a key reads '<bundle>.<rest>'.", key);
            return key;
        }
        String baseName = BUNDLE_PACKAGE + "." + key.substring(0, idx);
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(
                    baseName, locale, I18n.class.getClassLoader(), CONTROL);
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            log.warn("no translation of '{}' for {}", key, locale.toLanguageTag());
            return key;
        }
    }

    /**
     * @return the translation of <code>key</code> with <code>args</code>
     *         substituted into its {@link MessageFormat} placeholders.
     */
    public static String t(String key, Object... args) {
        String pattern = t(key);
        if (args == null || args.length == 0)
            return pattern;
        try {
            return new MessageFormat(pattern, locale).format(args);
        } catch (Exception e) {
            log.warn("'" + key + "' is not a valid message pattern", e);
            return pattern;
        }
    }

    /**
     * Loads the <code>xml</code> format, which {@link ResourceBundle} does not
     * know about on its own.
     */
    private static final class XmlControl extends ResourceBundle.Control {
        private static final List<String> FORMATS =
                Collections.unmodifiableList(Arrays.asList("xml"));

        @Override
        public List<String> getFormats(String baseName) {
            if (baseName == null)
                throw new NullPointerException("baseName");
            return FORMATS;
        }

        /**
         * The inherited implementation falls back to the JVM default locale,
         * which would make an explicitly requested locale unreliable. Only the
         * bundle without a locale suffix - the English original - is used as a
         * fallback here.
         */
        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null || locale == null)
                throw new NullPointerException();
            return null;
        }

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                ClassLoader loader, boolean reload) throws IOException {
            if (!"xml".equals(format))
                return null;
            String resourceName = toResourceName(toBundleName(baseName, locale), format);
            URL url = loader.getResource(resourceName);
            if (url == null)
                return null;
            URLConnection conn = url.openConnection();
            if (reload)
                conn.setUseCaches(false);
            Properties props = new Properties();
            try (InputStream is = conn.getInputStream()) {
                props.loadFromXML(is);
            }
            return new XmlResourceBundle(props);
        }
    }

    /**
     * A {@link ResourceBundle} over the entries of one bundle file.
     */
    private static final class XmlResourceBundle extends ResourceBundle {
        private final Map<String, Object> entries = new HashMap<>();

        XmlResourceBundle(Properties props) {
            for (String name: props.stringPropertyNames())
                entries.put(name, props.getProperty(name));
        }

        @Override
        protected Object handleGetObject(String key) {
            if (key == null)
                throw new NullPointerException("key");
            return entries.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            Set<String> keys = new HashSet<>(entries.keySet());
            if (parent != null) {
                Enumeration<String> pkeys = parent.getKeys();
                while (pkeys.hasMoreElements())
                    keys.add(pkeys.nextElement());
            }
            return Collections.enumeration(keys);
        }

        @Override
        protected Set<String> handleKeySet() {
            return entries.keySet();
        }
    }
}

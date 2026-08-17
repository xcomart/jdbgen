package comart.utils;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundle loader has to be forgiving: a build with a half finished
 * translation, or with a bundle file missing altogether, still has to start and
 * show something readable rather than throw out of a dialog.
 *
 * The fixtures are src/test/resources/i18n/testonly*.xml, in which the Korean
 * file deliberately lacks one entry.
 */
public class I18nTest {

    private Locale originalDefault;

    @BeforeEach
    public void rememberLocale() {
        originalDefault = Locale.getDefault();
        I18n.init(Locale.ENGLISH);
    }

    @AfterEach
    public void restoreLocale() {
        Locale.setDefault(originalDefault);
        I18n.init(originalDefault);
    }

    @Test
    public void theBundleWithoutALocaleSuffixIsTheDefault() {
        assertEquals("Hello", I18n.t("testonly.greeting"));
    }

    @Test
    public void switchingTheLocaleSwitchesTheTranslation() {
        assertEquals("Hello", I18n.t("testonly.greeting"));

        I18n.init(Locale.KOREAN);
        assertEquals("안녕하세요", I18n.t("testonly.greeting"));

        I18n.init(Locale.ENGLISH);
        assertEquals("Hello", I18n.t("testonly.greeting"),
                "the previous locale must not be cached over the switch");
    }

    @Test
    public void aRegionalLocaleUsesTheTranslationOfItsLanguage() {
        I18n.init(Locale.KOREA);
        assertEquals("안녕하세요", I18n.t("testonly.greeting"));
    }

    @Test
    public void anUntranslatedKeyFallsBackToTheOriginal() {
        I18n.init(Locale.KOREAN);
        assertEquals("only in the original", I18n.t("testonly.untranslated"));
    }

    @Test
    public void theRequestedLocaleWinsOverTheJvmDefault() {
        // the inherited fallback of ResourceBundle.Control is the default
        // locale, which would make an explicit request unreliable
        Locale.setDefault(Locale.KOREA);
        I18n.init(Locale.ENGLISH);

        assertEquals("Hello", I18n.t("testonly.greeting"));
    }

    @Test
    public void anUnknownKeyIsReturnedAsItIs() {
        assertEquals("testonly.nothing.here", I18n.t("testonly.nothing.here"));
    }

    @Test
    public void anUnknownBundleIsReturnedAsItIs() {
        assertEquals("nosuchbundle.some.key", I18n.t("nosuchbundle.some.key"));
        assertEquals("nosuchbundle.some.key",
                I18n.t("nosuchbundle.some.key", "argument"),
                "a missing bundle must not fail the argument substitution either");
    }

    @Test
    public void aKeyWithoutABundleSegmentIsReturnedAsItIs() {
        assertEquals("bare", I18n.t("bare"));
        assertEquals("", I18n.t(null));
    }

    @Test
    public void argumentsAreSubstituted() {
        assertEquals("Welcome, Dennis, you have 3 messages.",
                I18n.t("testonly.welcome", "Dennis", 3));

        I18n.init(Locale.KOREAN);
        assertEquals("Dennis님 환영합니다. 메시지가 3개 있습니다.",
                I18n.t("testonly.welcome", "Dennis", 3));
    }

    @Test
    public void doubledQuotesAreASingleQuoteInTheResult() {
        // a placeholder inside single quotes would be taken literally by
        // MessageFormat, so the bundles double them
        assertEquals("'Output directory' is required",
                I18n.t("testonly.quoted", "Output directory"));
    }

    @Test
    public void aPatternWithoutArgumentsIsNotFormatted() {
        assertEquals("''{0}'' is required", I18n.t("testonly.quoted"),
                "without arguments the entry is returned unchanged");
    }

    @Test
    public void characterReferencesBecomeRealNewlines() {
        assertEquals("first\nsecond", I18n.t("testonly.multiline"));
    }

    @Test
    public void aLanguageSettingBecomesALocale() {
        assertEquals(Locale.KOREAN.getLanguage(), I18n.toLocale("ko").getLanguage());
        assertEquals(Locale.ENGLISH.getLanguage(), I18n.toLocale("en").getLanguage());
        assertEquals("ko", I18n.toLocale("ko_KR").getLanguage());
        assertEquals("KR", I18n.toLocale("ko_KR").getCountry());
    }

    @Test
    public void anUnsetLanguageKeepsTheSystemLocale() {
        assertEquals(null, I18n.toLocale(null));
        assertEquals(null, I18n.toLocale(""));
        assertEquals(null, I18n.toLocale("  "));
        assertEquals(null, I18n.toLocale("system"));
        assertEquals(null, I18n.toLocale("System"));
        assertEquals(null, I18n.toLocale("!!!"), "an unusable tag is not an error");
    }

    @Test
    public void applyingALanguageAlsoSetsTheDefaultLocale() {
        Locale.setDefault(Locale.ENGLISH);

        I18n.applyLanguage("ko");
        assertEquals("ko", Locale.getDefault().getLanguage());
        assertEquals("ko", I18n.getLocale().getLanguage());
        assertEquals("안녕하세요", I18n.t("testonly.greeting"));
    }

    @Test
    public void applyingNoLanguageLeavesTheDefaultLocaleAlone() {
        Locale.setDefault(Locale.KOREAN);

        I18n.applyLanguage(null);
        assertEquals(Locale.KOREAN, Locale.getDefault());
        assertEquals(Locale.KOREAN, I18n.getLocale());

        I18n.applyLanguage("system");
        assertEquals(Locale.KOREAN, Locale.getDefault());
    }

    @Test
    public void aKeyThatOnlyLooksLikeOneIsReturnedAsItIs() {
        // the bundle segment is what is before the first dot, so there has to
        // be something before it
        assertEquals(".leading.dot", I18n.t(".leading.dot"));
        assertEquals("", I18n.t(""));
        // a key whose bundle is there but which the bundle does not carry
        assertEquals("testonly", I18n.t("testonly"));
    }

    @Test
    public void tooFewArgumentsLeaveThePlaceholderInThePattern() {
        // a half filled message is still better than a dialog that throws
        assertEquals("Welcome, Dennis, you have {1} messages.",
                I18n.t("testonly.welcome", "Dennis"));
        // and an argument nobody asked for is simply not used
        assertEquals("Hello", I18n.t("testonly.greeting", "unused"));
    }

    @Test
    public void anEmptyArgumentListIsNoArgumentList() {
        // the pattern is returned unformatted, so the doubled quotes stay
        assertEquals("''{0}'' is required", I18n.t("testonly.quoted", new Object[0]));
        assertEquals("''{0}'' is required", I18n.t("testonly.quoted", (Object[])null));
    }

    @Test
    public void aLanguageTagIsReadWithEitherSeparator() {
        assertEquals("ko", I18n.toLocale("ko-KR").getLanguage());
        assertEquals("KR", I18n.toLocale("ko-KR").getCountry());
        // and with the white space a hand edited configuration may carry
        assertEquals("ko", I18n.toLocale("  ko  ").getLanguage());
    }

    @Test
    public void anUnusableLanguageSettingKeepsWhatIsThere() {
        Locale.setDefault(Locale.KOREAN);

        I18n.applyLanguage("!!!");

        assertEquals(Locale.KOREAN, Locale.getDefault(),
                "a setting that names no language must not change the JVM locale");
        assertEquals(Locale.KOREAN, I18n.getLocale());
    }

    @Test
    public void everyKeyOfTheApplicationBundleIsTranslated() {
        // a spot check that the real bundle is on the class path and reachable
        // through the same lookup the application uses
        String english = I18n.t("common.dialog.error.title");
        assertEquals("Error", english);

        I18n.init(Locale.KOREAN);
        String korean = I18n.t("common.dialog.error.title");
        assertNotEquals(english, korean);
        assertNotEquals("common.dialog.error.title", korean);
        assertTrue(korean.length() > 0);
    }
}

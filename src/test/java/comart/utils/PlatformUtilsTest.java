/*
 * The MIT License
 *
 * Copyright 2024 comart.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package comart.utils;

import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The platform detection and the version comparison of {@link PlatformUtils}.
 * The detected platform is worked out from <code>os.name</code> and cached in a
 * static, so it is checked against the property this JVM actually runs with
 * rather than against a hard coded platform. Everything that hands something to
 * the desktop - the browser, the file manager, the dock - needs a desktop and is
 * left to the integration tests.
 */
public class PlatformUtilsTest {

    @Test
    public void theDetectedPlatformMatchesTheOsNameProperty() {
        String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        PlatformUtils.OSType expected;
        if (os.contains("mac") || os.contains("darwin"))
            expected = PlatformUtils.OSType.MacOS;
        else if (os.contains("win"))
            expected = PlatformUtils.OSType.Windows;
        else if (os.contains("ux") || os.contains("ix"))
            expected = PlatformUtils.OSType.Unix;
        else
            expected = PlatformUtils.OSType.Other;

        assertEquals(expected, PlatformUtils.getOSType());
    }

    @Test
    public void theDetectedPlatformIsWorkedOutOnlyOnce() {
        PlatformUtils.OSType first = PlatformUtils.getOSType();
        String original = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Some Other System");
            assertSame(first, PlatformUtils.getOSType(),
                    "the platform is detected once and kept, so that a property "
                    + "changed at runtime cannot move the user data directory");
        } finally {
            if (original == null)
                System.clearProperty("os.name");
            else
                System.setProperty("os.name", original);
        }
    }

    @Test
    public void exactlyOnePlatformFlagAnswersTrue() {
        int flags = 0;
        if (PlatformUtils.isWindows()) flags++;
        if (PlatformUtils.isMac()) flags++;
        if (PlatformUtils.isUnix()) flags++;

        if (PlatformUtils.getOSType() == PlatformUtils.OSType.Other)
            assertEquals(0, flags, "an unrecognised platform is none of the three");
        else
            assertEquals(1, flags, "the flags must not overlap");

        assertEquals(PlatformUtils.getOSType() == PlatformUtils.OSType.Windows,
                PlatformUtils.isWindows());
        assertEquals(PlatformUtils.getOSType() == PlatformUtils.OSType.MacOS,
                PlatformUtils.isMac());
        assertEquals(PlatformUtils.getOSType() == PlatformUtils.OSType.Unix,
                PlatformUtils.isUnix());
    }

    @Test
    public void theVersionIsReadFromTheFilteredResource() {
        String version = PlatformUtils.getVersion();

        assertNotEquals("unknown", version,
                "version.properties is expanded by processResources and has to be "
                + "on the class path - the update check is skipped without it");
        assertTrue(version.matches("\\d+(\\.\\d+)*"),
                "'" + version + "' is expected to be a dotted release number, and "
                + "the '${version}' placeholder is expected to be expanded");
        assertSame(version, PlatformUtils.getVersion(), "the version is read once");
    }

    @Test
    public void aNewerVersionSortsAfterAnOlderOne() {
        assertTrue(PlatformUtils.compareVersions("0.3.1", "0.3.2") < 0);
        assertTrue(PlatformUtils.compareVersions("0.3.2", "0.3.1") > 0);
        assertEquals(0, PlatformUtils.compareVersions("0.3.1", "0.3.1"));
        // segments are numbers and not text: 10 comes after 9
        assertTrue(PlatformUtils.compareVersions("1.9.0", "1.10.0") < 0);
        assertTrue(PlatformUtils.compareVersions("0.9.0", "1.0.0") < 0);
    }

    @Test
    public void theTagPrefixOfAReleaseIsIgnored() {
        // GitHub reports the tag, which is written 'v0.3.2'
        assertEquals(0, PlatformUtils.compareVersions("0.3.2", "v0.3.2"));
        assertEquals(0, PlatformUtils.compareVersions("V0.3.2", "0.3.2"));
        assertTrue(PlatformUtils.compareVersions("0.3.1", "v0.3.2") < 0);
        // and the tag may come with white space around it
        assertEquals(0, PlatformUtils.compareVersions("0.3.2", "  v0.3.2  "));
    }

    @Test
    public void aMissingTrailingSegmentIsZero() {
        assertEquals(0, PlatformUtils.compareVersions("1.2", "1.2.0"));
        assertEquals(0, PlatformUtils.compareVersions("1.2.0.0", "1.2"));
        assertTrue(PlatformUtils.compareVersions("1.2", "1.2.1") < 0);
        // no version at all is the oldest there is
        assertEquals(0, PlatformUtils.compareVersions(null, "0.0.0"));
        assertTrue(PlatformUtils.compareVersions(null, "0.0.1") < 0);
        assertEquals(0, PlatformUtils.compareVersions("", null));
    }

    @Test
    public void aPreReleaseIsOlderThanTheReleaseItLeadsUpTo() {
        // the mark is not part of the number: '0.3.2-rc1' is a 0.3.2, and it
        // used to be read as '0.3.0' because '2-rc1' counted as zero
        assertTrue(PlatformUtils.compareVersions("0.3.2-rc1", "0.3.2") < 0);
        assertTrue(PlatformUtils.compareVersions("0.3.2", "0.3.2-rc1") > 0);
        assertTrue(PlatformUtils.compareVersions("0.3.1", "v0.3.2-rc1") < 0);
        assertTrue(PlatformUtils.compareVersions("0.3.2-rc1", "0.3.3") < 0);
        assertTrue(PlatformUtils.compareVersions("0.3.0", "0.3.2-rc1") < 0);
        // the mark is never read, so two pre-releases of one number are not
        // told apart
        assertEquals(0, PlatformUtils.compareVersions("0.3.2-rc1", "0.3.2-rc2"));
        assertEquals(0, PlatformUtils.compareVersions("1.0.0-beta", "1.0-beta"));
    }

    @Test
    public void aVersionThatIsNoNumberAtAllIsTheOldestThereIs() {
        // nothing numeric to read is a 0 carrying a mark, and a marked version
        // precedes the plain one
        assertTrue(PlatformUtils.compareVersions("nightly", "0") < 0);
        assertTrue(PlatformUtils.compareVersions("nightly", "0.0.1") < 0);
        assertEquals(0, PlatformUtils.compareVersions("nightly", "snapshot"));
    }
}

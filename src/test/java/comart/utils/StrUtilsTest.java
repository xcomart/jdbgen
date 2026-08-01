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

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the pure string helpers of {@link StrUtils}.
 *
 * @author comart
 */
public class StrUtilsTest {

    @Test
    public void testTrimStripsEnclosingQuotes() {
        assertEquals("abc", StrUtils.trim("  abc  "));
        assertEquals("abc", StrUtils.trim("  'abc'  "));
        assertEquals("abc", StrUtils.trim("\"abc\""));
        // an enclosing pair around nothing collapses to an empty string
        assertEquals("", StrUtils.trim("''"));
        assertEquals("", StrUtils.trim("   "));
    }

    @Test
    public void testTrimSingleQuoteCharacterIsNotAQuotePair() {
        // a one character string cannot be a quoted string - it used to raise
        // StringIndexOutOfBoundsException through substring(1, 0)
        assertEquals("'", StrUtils.trim("'"));
        assertEquals("\"", StrUtils.trim("  \"  "));
        assertEquals("a", StrUtils.trim(" a "));
    }

    @Test
    public void testToSnakeCaseHasNoLeadingSeparator() {
        assertEquals("user_name", StrUtils.toSnakeCase("UserName"));
        assertEquals("user_name", StrUtils.toSnakeCase("userName"));
        assertEquals("user_name", StrUtils.toSnakeCase("user_name"));
        assertEquals("user_name", StrUtils.toSnakeCase("user-name"));
        assertEquals("abc", StrUtils.toSnakeCase("ABC"));
        assertEquals("", StrUtils.toSnakeCase(""));
    }

    @Test
    public void testCaseConversionsDerivedFromSnakeCase() {
        assertEquals("USER_NAME", StrUtils.toScreamingSnakeCase("UserName"));
        assertEquals("user-name", StrUtils.toSkewerCase("UserName"));
        assertEquals("user-name", StrUtils.toKebabCase("UserName"));
    }

    @Test
    public void testSplitReturnsNullForNullInput() {
        // used to append the delimiter first and return ["null"]
        assertNull(StrUtils.split(null, ","));
        assertNull(StrUtils.split(null, ",", true));
        assertNull(StrUtils.split("a,b", null));
    }

    @Test
    public void testSplitNormalCases() {
        assertArrayEquals(new String[]{"a", "b", "c"}, StrUtils.split("a,b,c", ","));
        assertArrayEquals(new String[]{"a", "b"}, StrUtils.split("a , b ", ",", true));
        assertArrayEquals(new String[]{"a"}, StrUtils.split("a", ","));
        assertArrayEquals(new String[]{""}, StrUtils.split("", ","));
    }

    @Test
    public void testReplaceWithKeepsPlaceholderWhenValueIsMissing() throws ParseException {
        Map<String, String> map = new HashMap<>();
        map.put("known", "VALUE");

        assertEquals("x VALUE y", StrUtils.replaceWith("x ${known} y", map, "${", "}"));
        // a missing value must leave the placeholder untouched instead of
        // exploding with a NullPointerException wrapped in a RuntimeException
        assertEquals("x ${unknown} y", StrUtils.replaceWith("x ${unknown} y", map, "${", "}"));
        assertEquals("${a}VALUE", StrUtils.replaceWith("${a}${known}", map, "${", "}"));
    }

    @Test
    public void testReplaceWithReportsMissingEndDelimiter() {
        Map<String, String> map = new HashMap<>();
        // the intended ParseException must not be swallowed and rethrown as a
        // RuntimeException by the surrounding catch block
        assertThrows(ParseException.class,
                () -> StrUtils.replaceWith("x ${unterminated", map, "${", "}"));
    }

    @Test
    public void testReplace() {
        assertEquals("a-b-c", StrUtils.replace("a_b_c", "_", "-"));
        assertEquals("abc", StrUtils.replace("abc", "_", "-"));
        assertEquals("", StrUtils.replace(null, "_", "-"));
    }

    @Test
    public void testToInt() {
        assertEquals(1234, StrUtils.toInt("1,234"));
        assertEquals(0, StrUtils.toInt("abc"));
        assertEquals(0, StrUtils.toInt(null));
        assertEquals(-7, StrUtils.toInt("-7"));
    }
}

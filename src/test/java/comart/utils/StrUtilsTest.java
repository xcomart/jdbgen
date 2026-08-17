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
import java.util.Calendar;
import java.util.GregorianCalendar;
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
        // a delimiter the source does not hold yields the source as one item,
        // and a multi character delimiter is one delimiter
        assertArrayEquals(new String[]{"abc"}, StrUtils.split("abc", "||"));
        assertArrayEquals(new String[]{"a", "b"}, StrUtils.split("a||b", "||"));
        // an empty item between two delimiters is an item
        assertArrayEquals(new String[]{"a", "", "b"}, StrUtils.split("a,,b", ","));
        assertArrayEquals(new String[]{"", "a"}, StrUtils.split(",a", ","));
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

    @Test
    public void testToIntBoundaries() {
        assertEquals(1234567, StrUtils.toInt("1,234,567"));
        assertEquals(5, StrUtils.toInt("+5"));
        // nothing but separators reads as zero rather than failing
        assertEquals(0, StrUtils.toInt(""));
        assertEquals(0, StrUtils.toInt(","));
        // a value beyond int range is a failure, not an overflow
        assertEquals(0, StrUtils.toInt("99999999999"));
    }

    @Test
    public void testToIntCutsAFractionOff() {
        // a fraction used to yield the 0 fallback, because the decimal point
        // was kept and Integer.parseInt refused the whole value
        assertEquals(1, StrUtils.toInt("1.5"));
        assertEquals(1, StrUtils.toInt("1.0"));
        // the fraction is dropped, not rounded, and towards zero at that
        assertEquals(1, StrUtils.toInt("1.9"));
        assertEquals(-1, StrUtils.toInt("-1.9"));
        assertEquals(1234, StrUtils.toInt("1,234.9"));
        assertEquals(0, StrUtils.toInt("0.5"));
        // nothing in front of the point is no number
        assertEquals(0, StrUtils.toInt(".5"));
        assertEquals(0, StrUtils.toInt("-.5"));
    }

    @Test
    public void testTrimLeftAndTrimRightKeepTheOtherEnd() {
        assertEquals("abc  ", StrUtils.trimLeft("  abc  "));
        assertEquals("  abc", StrUtils.trimRight("  abc  "));
        assertEquals("", StrUtils.trimLeft(" \t\r\n "));
        assertEquals("", StrUtils.trimRight(" \t\r\n "));
        // only trim() unquotes, the one sided ones leave the quotes in place
        assertEquals("'abc'", StrUtils.trimLeft("  'abc'"));
        assertEquals("'abc'", StrUtils.trimRight("'abc'  "));
    }

    @Test
    public void testTrimWithAnExplicitSpaceCharacterSet() {
        assertEquals("abc", StrUtils.trim("xxabcxx", "x"));
        assertEquals("abcxx", StrUtils.trimLeft("xxabcxx", "x"));
        assertEquals("xxabc", StrUtils.trimRight("xxabcxx", "x"));
        // a generic space is not a space character here anymore
        assertEquals(" abc ", StrUtils.trim("x abc x", "x"));
    }

    @Test
    public void testTrimOnlyStripsAMatchingQuotePair() {
        // the two ends have to carry the same quote character
        assertEquals("\"abc'", StrUtils.trim("\"abc'"));
        assertEquals("'abc\"", StrUtils.trim("'abc\""));
        // only one pair is stripped, the inner quotes are part of the value
        assertEquals("'abc'", StrUtils.trim("''abc''"));
        assertEquals("a'b", StrUtils.trim("a'b"));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(StrUtils.isEmpty(null));
        assertTrue(StrUtils.isEmpty(""));
        assertTrue(StrUtils.isEmpty("  \t\r\n "));
        assertFalse(StrUtils.isEmpty("a"));
        assertFalse(StrUtils.isEmpty("  a  "));
        assertFalse(StrUtils.isEmpty(new StringBuilder("x")));
    }

    @Test
    public void testIsSpace() {
        assertTrue(StrUtils.isSpace(' '));
        assertTrue(StrUtils.isSpace('\t'));
        assertTrue(StrUtils.isSpace('\r'));
        assertTrue(StrUtils.isSpace('\n'));
        assertFalse(StrUtils.isSpace('a'));
        // a vertical tab is no generic space here
        assertFalse(StrUtils.isSpace(0x0b));

        assertTrue(StrUtils.isSpace('b', "abc"));
        assertFalse(StrUtils.isSpace('d', "abc"));
        assertTrue(StrUtils.isSpace("cab", "abc"));
        assertFalse(StrUtils.isSpace("cad", "abc"));
        // nothing to look at is nothing that is not a space
        assertTrue(StrUtils.isSpace("", "abc"));
    }

    @Test
    public void testIsSpaceTakesAMissingArgument() {
        // a string that is not there is no string of spaces, and it is no
        // NullPointerException either
        assertFalse(StrUtils.isSpace((String) null, "abc"));
        assertFalse(StrUtils.isSpace("abc", (String) null));
        assertFalse(StrUtils.isSpace('a', (String) null));
        assertFalse(StrUtils.isSpace(' ', (byte[]) null));
    }

    @Test
    public void testSpaceAndCharArrayContains() {
        assertEquals("xxx", StrUtils.space(3, 'x'));
        assertEquals("", StrUtils.space(0, 'x'));
        assertEquals("   ", StrUtils.space(3, ' '));

        assertTrue(StrUtils.contains(new char[]{'a', 'b'}, 'b'));
        assertFalse(StrUtils.contains(new char[]{'a', 'b'}, 'c'));
        assertFalse(StrUtils.contains(new char[0], 'a'));
    }

    @Test
    public void testPadStringPadsOnTheRequestedSide() {
        // 'right' names the side the padding goes on
        assertEquals("ab   ", StrUtils.padString("ab", 5, ' ', true));
        assertEquals("   ab", StrUtils.padString("ab", 5, ' ', false));
        assertEquals("ab000", StrUtils.padString("ab", 5, '0', true));
        assertEquals("00012", StrUtils.padString("12", 5, '0', false));
        // an exact fit is left alone
        assertEquals("abcde", StrUtils.padString("abcde", 5, ' ', true));
        assertEquals("", StrUtils.padString("abc", 0, ' ', true));
    }

    @Test
    public void testPadStringKeepsTheEndItPadsAwayFrom() {
        // too long for the field: right padding keeps the tail, left padding
        // keeps the head - the padded side is the one that is cut
        assertEquals("def", StrUtils.padString("abcdef", 3, ' ', true));
        assertEquals("abc", StrUtils.padString("abcdef", 3, ' ', false));
    }

    @Test
    public void testPadStringOfAMissingStringIsMissingToo() {
        // like split() and replace(), a value that is not there is passed on
        // rather than turned into a NullPointerException
        assertNull(StrUtils.padString(null, 5, ' ', true));
        assertNull(StrUtils.padString(null, 5, '0', false));
    }

    @Test
    public void testToCamelCase() {
        assertEquals("userName", StrUtils.toCamelCase("user_name"));
        assertEquals("userName", StrUtils.toCamelCase("USER_NAME"));
        assertEquals("userName", StrUtils.toCamelCase("user-name"));
        assertEquals("userName", StrUtils.toCamelCase("UserName"));
        assertEquals("userName", StrUtils.toCamelCase("userName"));
        // a name written in one case is lower cased as a whole
        assertEquals("abc", StrUtils.toCamelCase("ABC"));
        assertEquals("", StrUtils.toCamelCase(""));
        // a trailing separator names no following character
        assertEquals("user", StrUtils.toCamelCase("user_"));
    }

    @Test
    public void testToPascalCase() {
        assertEquals("UserName", StrUtils.toPascalCase("user_name"));
        assertEquals("UserName", StrUtils.toPascalCase("USER_NAME"));
        assertEquals("UserName", StrUtils.toPascalCase("userName"));
        assertEquals("Abc", StrUtils.toPascalCase("ABC"));
        assertEquals("", StrUtils.toPascalCase(""));
    }

    @Test
    public void testIsUpper() {
        assertTrue(StrUtils.isUpper("USER_ID"));
        assertTrue(StrUtils.isUpper("ID2"));
        // no lower case letter at all, so nothing tells the two cases apart
        assertTrue(StrUtils.isUpper("123"));
        assertTrue(StrUtils.isUpper(""));
        assertFalse(StrUtils.isUpper("UserId"));
        assertFalse(StrUtils.isUpper("userid"));
    }

    @Test
    public void testStrInAndObjectArrayContains() {
        String[] haystack = {"a", "b", "c"};
        assertTrue(StrUtils.strIn("b", haystack));
        assertFalse(StrUtils.strIn("d", haystack));
        assertFalse(StrUtils.strIn("a", new String[0]));

        assertTrue(StrUtils.contains(haystack, "c"));
        assertFalse(StrUtils.contains(haystack, "d"));
        // neither a missing array nor a missing value is an error
        assertFalse(StrUtils.contains(null, "a"));
        assertFalse(StrUtils.contains(haystack, null));
    }

    @Test
    public void testAnElementThatIsNotThereIsSkipped() {
        // a hole in the array used to be dereferenced and blow up before the
        // element that was actually looked for was reached
        Object[] holed = {null, "b", null};
        assertTrue(StrUtils.contains(holed, "b"));
        assertFalse(StrUtils.contains(holed, "a"));
        assertFalse(StrUtils.contains(new Object[]{null}, "a"));

        String[] holedStrings = {null, "b"};
        assertTrue(StrUtils.strIn("b", holedStrings));
        assertFalse(StrUtils.strIn("a", holedStrings));
    }

    @Test
    public void testStrInTakesAMissingArgument() {
        assertFalse(StrUtils.strIn(null, new String[]{"a", null}));
        assertFalse(StrUtils.strIn("a", null));
        assertFalse(StrUtils.strIn(null, null));
    }

    @Test
    public void testReplaceWithASingleDelimiter() throws ParseException {
        Map<String, String> map = new HashMap<>();
        map.put("name", "VALUE");

        assertEquals("x VALUE y", StrUtils.replaceWith("x #name# y", map, "#"));
        assertEquals("x #other# y", StrUtils.replaceWith("x #other# y", map, "#"));
        assertThrows(ParseException.class,
                () -> StrUtils.replaceWith("x #name", map, "#"));
    }

    /** a bean the placeholders are read from, as a template is run against. */
    public static class Bean {
        public String getName() { return "outer"; }
        public Bean getChild() { return null; }
        public String getBoom() { throw new IllegalArgumentException("accessor failed"); }
    }

    @Test
    public void testReplaceWithReadsPropertiesOfAnObject() throws ParseException {
        assertEquals("hello outer!", StrUtils.replaceWith("hello ${name}!", new Bean(), "${", "}"));
        // a property that resolves to null is a value that is not there
        assertEquals("${child}", StrUtils.replaceWith("${child}", new Bean(), "${", "}"));
    }

    @Test
    public void testReplaceWithLeavesAnEmptyPlaceholderAloneInEveryContext() throws ParseException {
        // '${}' names no property, so it is a placeholder that resolves to
        // nothing and stays as it is - against a bean exactly as against a map,
        // where the empty name used to raise a StringIndexOutOfBoundsException
        // wrapped in a RuntimeException for the bean only
        Map<String, String> map = new HashMap<>();
        map.put("known", "VALUE");

        assertEquals("a ${} b", StrUtils.replaceWith("a ${} b", map, "${", "}"));
        assertEquals("a ${} b", StrUtils.replaceWith("a ${} b", new Bean(), "${", "}"));
        assertEquals("${}VALUE", StrUtils.replaceWith("${}${known}", map, "${", "}"));
        assertEquals("${}outer", StrUtils.replaceWith("${}${name}", new Bean(), "${", "}"));
    }

    @Test
    public void testReplaceWithWrapsAFailingAccessor() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> StrUtils.replaceWith("${boom}", new Bean(), "${", "}"));
        assertEquals("cannot get value from object", ex.getMessage());
        assertNotNull(ex.getCause(), "the accessor failure has to stay readable");
    }

    @Test
    public void testDateFormat() {
        // a fixed local date, so the pattern and not the time zone is checked
        GregorianCalendar cal = new GregorianCalendar(2024, Calendar.MARCH, 5, 14, 30, 45);
        assertEquals("2024-03-05", StrUtils.dateFormat("yyyy-MM-dd", cal.getTime()));
        assertEquals("20240305 143045", StrUtils.dateFormat("yyyyMMdd HHmmss", cal.getTime()));

        // the no-date overload formats now
        assertEquals(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)),
                StrUtils.dateFormat("yyyy"));
    }
}

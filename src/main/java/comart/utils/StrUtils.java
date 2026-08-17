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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;

/**
 * string utilities. This class contains various methods related with
 * <code>String</code>. All methods are <code>static</code> and independent
 * with every character-set.
 *
 * @author Soungjin Park
 * @version 1.0
 */
@Slf4j
public class StrUtils
{
    /** the characters counted as generic space: blank, tab, CR and LF. */
    private final static byte[] SPACE_CHARS     = " \t\r\n".getBytes(StandardCharsets.UTF_8);

    /**
     * this class only holds <code>static</code> methods.
     */
    private StrUtils() {}

    /**
     * split string with delimiter <code>delim</code>.
     *
     * @param src
     *            source string.
     * @param delim
     *            delimiter.
     * @return split string array, or <code>null</code> if any argument is
     *         <code>null</code>.
     */
    public static String[] split(String src, String delim)
    {
        return split(src, delim, false);
    }

    /**
     * split string with delimiter <code>delim</code> with trim option.
     *
     * @param src
     *            source string.
     * @param delim
     *            delimiter.
     * @param trim
     *            trim or not.
     * @return split string array, or <code>null</code> if any argument is
     *         <code>null</code>.
     */
    public static String[] split(String src, String delim, boolean trim)
    {
        if (src == null || delim == null)
            return null;

        ArrayList<String> res = new ArrayList<>();
        // add last delimiter for convenience(no need to process last item)
        src = src + delim;
        int idx, prevIdx = 0;
        int delimlen = delim.length();

        while ((idx = src.indexOf(delim, prevIdx)) > -1) {
            String item = src.substring(prevIdx, idx);
            if (trim)
                item = item.trim();
            res.add(item);
            prevIdx = idx + delimlen;
        }
        return res.toArray(new String[0]);
    }

    /**
     * replaces each substring of <code>src</code> that matches the given
     * <code>find</code> with given <code>rep</code>.
     *
     * @param src
     *            source string.
     * @param find
     *            string to be replaced.
     * @param rep
     *            replacement string.
     * @return replaced string.
     */
    public static String replace(String src, String find, String rep)
    {
        StringBuilder res = new StringBuilder();
        int idx, prevIdx = 0;
        if (src != null && find != null) {
            int delimlen = find.length();

            if (src.contains(find)) {
                while ((idx = src.indexOf(find, prevIdx)) > -1) {
                    res.append(src.substring(prevIdx, idx));
                    res.append(rep);
                    prevIdx = idx + delimlen;
                }
            }
            res.append(src.substring(prevIdx, src.length()));
        }
        return res.toString();
    }

    /**
     * create character array with <code>length</code> containing <code>bt</code>.
     *
     * @param bt
     *            padding character.
     * @param length
     *            length of result character array.
     * @return padded character array.
     */
    private static char[] pad(char bt, int length)
    {
        char[] res = new char[length];
        Arrays.fill(res, bt);
        return res;
    }

    /**
     * pad string <code>src</code> with <code>length</code> and <code>padchar</code>.
     *
     * @param src
     *            source string to be padded.
     * @param length
     *            length of result string.
     * @param padchar
     *            padding character.
     * @param right
     *            pad right or not.
     * @return padded string, or <code>null</code> if <code>src</code> is
     *         <code>null</code>.
     */
    public static String padString(String src, int length, char padchar,
            boolean right)
    {
        if (src == null)
            return null;
        char[] data = src.toCharArray();
        char[] res = pad(padchar, length);
        int stpos = 0, len = data.length, destPos = 0;
        if (len > length) {
            len = length;
            if (right)
                stpos = data.length - len;
        } else {
            if (!right)
                destPos = length - len;
        }
        System.arraycopy(data, stpos, res, destPos, len);
        return new String(res);
    }

    /**
     * given byte <code>src</code> is generic space or not.
     *
     * @param src
     *            byte to be tested.
     * @return is space or not.
     */
    public static boolean isSpace(int src)
    {
        return isSpace(src, SPACE_CHARS);
    }

    /**
     * given byte <code>src</code> is in <code>spc</code> or not.
     *
     * @param src
     *            byte to be tested.
     * @param spc
     *            array of bytes, may be <code>null</code>.
     * @return <code>src</code> is in <code>spc</code> or not;
     *         <code>false</code> if <code>spc</code> is <code>null</code>.
     */
    public static boolean isSpace(int src, byte[] spc)
    {
        if (spc == null)
            return false;
        for (int i = 0; i < spc.length; i++) {
            if (src == spc[i])
                return true;
        }
        return false;
    }

    /**
     * given character <code>src</code> is in <code>spc</code> or not.
     *
     * @param src
     *            character to be tested.
     * @param spc
     *            space string, may be <code>null</code>.
     * @return <code>src</code> is in <code>spc</code> or not;
     *         <code>false</code> if <code>spc</code> is <code>null</code>.
     */
    public static boolean isSpace(char src, String spc)
    {
        if (spc == null)
            return false;
        for (int i = 0; i < spc.length(); i++) {
            if (src == spc.charAt(i))
                return true;
        }
        return false;
    }

    /**
     * every character in <code>src</code> are in <code>spc</code> return true, any
     * one of them is not in <code>spc</code> return false. A string with nothing
     * in it holds no character that is not a space, so it answers
     * <code>true</code>; a string that is not there at all is nothing to look
     * at and answers <code>false</code>.
     *
     * @param src
     *            string to be tested, may be <code>null</code>.
     * @param spc
     *            space string, may be <code>null</code>.
     * @return all chracters of <code>src</code> is space or not;
     *         <code>false</code> whenever either argument is
     *         <code>null</code>.
     */
    public static boolean isSpace(String src, String spc)
    {
        if (src == null || spc == null)
            return false;
        for (int i = 0; i < src.length(); i++) {
            if (!isSpace(src.charAt(i), spc))
                return false;
        }
        return true;
    }

    /**
     * given sequence is <code>null</code>, empty or nothing but white space.
     *
     * @param seq
     *            sequence to be tested.
     * @return there is no visible character in <code>seq</code> or not.
     */
    public static boolean isEmpty(CharSequence seq) {
        if (seq != null) {
            return seq.toString().trim().isEmpty();
        }
        return true;
    }

    /**
     * create a string which length is <code>size</code> filled with <code>c</code>.
     * call of this method is exactly same as
     *
     * <pre>
     * new String(StrUtils.pad(c, size))
     * </pre>
     *
     * @param size
     *            length of result string.
     * @param c
     *            result string will be filled with <code>c</code>
     * @return result string.
     */
    public static String space(int size, char c)
    {
        return new String(pad(c, size));
    }

    /**
     * strip the generic space characters off both ends of <code>input</code>,
     * and one enclosing pair of quotes with them. See
     * {@link #trim(String, char[])}.
     *
     * @param input
     *            string to be trimmed.
     * @return trimmed and unquoted string.
     */
    public static String trim(String input) {
        return trim(input, new String(SPACE_CHARS, StandardCharsets.UTF_8));
    }

    /**
     * strip the generic space characters off the beginning of
     * <code>input</code>.
     *
     * @param input
     *            string to be trimmed.
     * @return trimmed string.
     */
    public static String trimLeft(String input) {
        return trimLeft(input, new String(SPACE_CHARS, StandardCharsets.UTF_8));
    }

    /**
     * strip the generic space characters off the end of <code>input</code>.
     *
     * @param input
     *            string to be trimmed.
     * @return trimmed string.
     */
    public static String trimRight(String input) {
        return trimRight(input, new String(SPACE_CHARS, StandardCharsets.UTF_8));
    }

    /**
     * strip the characters of <code>spaceChars</code> off both ends of
     * <code>input</code>, and one enclosing pair of quotes with them. See
     * {@link #trim(String, char[])}.
     *
     * @param input
     *            string to be trimmed.
     * @param spaceChars
     *            the characters counted as space.
     * @return trimmed and unquoted string.
     */
    public static String trim(String input, String spaceChars) {
        return trim(input, spaceChars.toCharArray());
    }

    /**
     * strip the characters of <code>spaceChars</code> off the beginning of
     * <code>input</code>.
     *
     * @param input
     *            string to be trimmed.
     * @param spaceChars
     *            the characters counted as space.
     * @return trimmed string.
     */
    public static String trimLeft(String input, String spaceChars) {
        return trimLeft(input, spaceChars.toCharArray());
    }

    /**
     * strip the characters of <code>spaceChars</code> off the end of
     * <code>input</code>.
     *
     * @param input
     *            string to be trimmed.
     * @param spaceChars
     *            the characters counted as space.
     * @return trimmed string.
     */
    public static String trimRight(String input, String spaceChars) {
        return trimRight(input, spaceChars.toCharArray());
    }

    /**
     * given character array holds <code>c</code> or not.
     *
     * @param charArr
     *            array to be searched.
     * @param c
     *            character to look for.
     * @return <code>c</code> is in <code>charArr</code> or not.
     */
    public static boolean contains(char[] charArr, char c) {
        for (char ca:charArr)
            if (ca == c) return true;
        return false;
    }

    /**
     * strip the characters of <code>spaceChars</code> off the beginning of
     * <code>input</code>.
     *
     * @param input
     *            string to be trimmed.
     * @param spaceChars
     *            the characters counted as space.
     * @return trimmed string, empty when nothing else is left.
     */
    public static String trimLeft(String input, char[] spaceChars) {
        int st = 0, ed = input.length();
        while (st < ed && contains(spaceChars, input.charAt(st)))
            st++;
        if (st < ed)
            return input.substring(st);
        return "";
    }

    /**
     * strip the characters of <code>spaceChars</code> off the end of
     * <code>input</code>.
     *
     * @param input
     *            string to be trimmed.
     * @param spaceChars
     *            the characters counted as space.
     * @return trimmed string, empty when nothing else is left.
     */
    public static String trimRight(String input, char[] spaceChars) {
        int st = 0, ed = input.length();
        while (st < ed && contains(spaceChars, input.charAt(ed-1)))
            ed--;
        if (st < ed)
            return input.substring(0, ed);
        return "";
    }

    /**
     * strip the characters of <code>spaceChars</code> off both ends of
     * <code>input</code>. What is left is unquoted as well: a leading and a
     * trailing <code>"</code> or <code>'</code> are dropped when they are the
     * same character and the value is longer than one character, so that a
     * quoted configuration value reads as what it stands for.
     *
     * @param input
     *            string to be trimmed.
     * @param spaceChars
     *            the characters counted as space.
     * @return trimmed and unquoted string, empty when nothing else is left.
     */
    public static String trim(String input, char[] spaceChars) {
        int st = 0, ed = input.length();
        while (st < ed && contains(spaceChars, input.charAt(st)))
            st++;
        if (st < ed) {
            while (st < ed && contains(spaceChars, input.charAt(ed-1)))
                ed--;
            String res = input.substring(st, ed);
            // a single quote character is not an enclosing quote pair
            if (res.length() > 1 &&
                    contains(new char[]{'"','\''}, res.charAt(0)) &&
                    res.charAt(0) == res.charAt(res.length()-1)) {
                res = res.substring(1, res.length()-1);
            }
            return res;
        }
        return "";
    }

    /**
     * replace every <code>delimSt</code>...<code>delimEd</code> placeholder of
     * <code>source</code> with the property of <code>obj</code> it names, read
     * with {@link ObjUtils#getValue(Object, String)}. A placeholder that names
     * nothing is left in place, so that a template can be run through this more
     * than once.
     *
     * @param source
     *            the text holding the placeholders.
     * @param obj
     *            the object or map the values are read from.
     * @param delimSt
     *            string opening a placeholder, <code>"${"</code> for instance.
     * @param delimEd
     *            string closing a placeholder, <code>"}"</code> for instance.
     * @return the text with its placeholders filled in.
     * @throws ParseException
     *             if a placeholder is opened but never closed.
     * @throws RuntimeException
     *             wrapping whatever reading a property threw.
     */
    public static String replaceWith(
            String source, Object obj,
            String delimSt, String delimEd
            ) throws ParseException
    {
        int stIdx = 0, currIdx;
        int stLen = delimSt.length();
        int edLen = delimEd.length();
        StringBuilder sb = new StringBuilder();

        while ((currIdx = source.indexOf(delimSt, stIdx)) > -1) {
            sb.append(source, stIdx, currIdx);
            currIdx += stLen;
            int lstIdx = source.indexOf(delimEd, currIdx);
            if (lstIdx < 0)
                throw new ParseException("End delimiter not presented.", currIdx);

            String key = source.substring(currIdx, lstIdx);
            Object val;
            try {
                val = ObjUtils.getValue(obj, key);
            } catch (Exception e) {
                throw new RuntimeException("cannot get value from object", e);
            }
            if (val != null)
                sb.append(val);
            else
                // no such value, keep the placeholder as-is
                sb.append(delimSt).append(key).append(delimEd);
            stIdx = lstIdx + edLen;
        }
        sb.append(source.substring(stIdx));
        return sb.toString();
    }

    /**
     * {@link #replaceWith(String, Object, String, String)} for a placeholder
     * that is opened and closed by the same string, as in
     * <code>#name#</code>.
     *
     * @param source
     *            the text holding the placeholders.
     * @param map
     *            the values, by placeholder name.
     * @param delim
     *            string both opening and closing a placeholder.
     * @return the text with its placeholders filled in.
     * @throws ParseException
     *             if a placeholder is opened but never closed.
     */
    public static String replaceWith(
            String source, Map<String,String> map, String delim
            ) throws ParseException
    {
        return replaceWith(source, map, delim, delim);
    }

    /** the characters an integer literal may be written with. */
    private final static String NUM_DIGITS = "+-0123456789";

    /**
     * strip the thousands separators off a number written for a human.
     *
     * @param intval
     *            the number as it was typed, may be <code>null</code>.
     * @return the digits, the sign and the decimal points of
     *         <code>intval</code>, <code>"0"</code> when there are none.
     * @throws ParseException
     *             if <code>intval</code> holds a character that is none of
     *             these.
     */
    private static String parseInt(String intval) throws ParseException
    {
        StringBuilder sb = new StringBuilder();
        if (intval != null) {
            for (int i=0; i < intval.length(); i++) {
                char c = intval.charAt(i);
                switch (c) {
                case ',':
                    break;
                case '.':
                    sb.append(c);
                    break;
                default:
                    if (NUM_DIGITS.indexOf(c) > -1) {
                        sb.append(c);
                    } else {
                        throw new ParseException(
                                "parameter containing non-numeric digit.",0);
                    }
                    break;
                }
            }
        }
        if ( "".equals(sb.toString()) )
            sb.append("0");
        return sb.toString();
    }

    /**
     * read an integer, tolerating the thousands separators and never throwing.
     * A fraction is cut off at the decimal point rather than rejected, so it is
     * truncated towards zero: <code>"1.5"</code> reads as <code>1</code> and
     * <code>"-1.9"</code> as <code>-1</code>.
     *
     * @param intval
     *            the number as it was typed, may be <code>null</code>.
     * @return the value, or <code>0</code> when it cannot be read as an
     *         <code>int</code>.
     */
    @SuppressWarnings("UseSpecificCatch")
    public static int toInt(String intval)
    {
        try {
            String num = parseInt(intval);
            int dot = num.indexOf('.');
            if (dot > -1)
                num = num.substring(0, dot);
            return Integer.parseInt(num);
        } catch(Exception t) {
            return 0;
        }
    }

    /**
     * format a date with a {@link SimpleDateFormat} pattern.
     *
     * @param format
     *            the pattern.
     * @param dt
     *            the date to format.
     * @return the formatted date.
     */
    public static String dateFormat(String format, Date dt)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(dt);
    }

    /**
     * format the current time with a {@link SimpleDateFormat} pattern, which is
     * what a template asking for the generation date uses.
     *
     * @param format
     *            the pattern.
     * @return the formatted current date and time.
     */
    public static String dateFormat(String format)
    {
        return dateFormat(format, new Date());
    }

    /**
     * given array holds an element equal to <code>val</code> or not.
     *
     * @param arr
     *            array to be searched, may be <code>null</code>.
     * @param val
     *            value to look for, may be <code>null</code>.
     * @return an equal element is in <code>arr</code> or not;
     *         <code>false</code> whenever either argument is
     *         <code>null</code>. An element that is <code>null</code> itself is
     *         skipped instead of breaking the comparison.
     */
    public static boolean contains(Object[] arr, Object val) {
        if (arr == null || val == null)
            return false;
        for (Object c:arr)
            if (Objects.equals(c, val)) return true;
        return false;
    }

    /**
     * given sequence holds no lower case ASCII letter, which is how a name
     * written in one case - <code>USER_ID</code> - is told from a mixed case
     * one.
     *
     * @param str
     *            sequence to be tested.
     * @return there is no character between <code>'a'</code> and
     *         <code>'z'</code> in <code>str</code> or not.
     */
    public static boolean isUpper(CharSequence str) {
        return str.chars()
                .filter(c -> c >= 'a' && c <= 'z')
                .findFirst()
                .orElse(-1) == -1;
    }

    /**
     * convert a name to <code>camelCase</code>. A name holding <code>_</code>
     * or <code>-</code> is split at those and every following character is
     * upper cased; a name written in one case is lower cased as a whole;
     * anything else only gets its first character lower cased.
     *
     * @param s
     *            the name to convert.
     * @return the name in <code>camelCase</code>.
     */
    public static String toCamelCase(String s) {
        if (s.contains("_") || s.contains("-")) {
            StringBuilder sb = new StringBuilder();
            boolean upper = false;
            for (int i=0; i<s.length(); i++) {
                char c = s.charAt(i);
                if (c == '_' || c == '-')
                    upper = true;
                else if (upper) {
                    sb.append(s.substring(i, i+1).toUpperCase());
                    upper = false;
                } else
                    sb.append(s.substring(i, i+1).toLowerCase());
            }
            return sb.toString();
        } else if (isUpper(s)) {
            return s.toLowerCase();
        } else {
            return s.substring(0, 1).toLowerCase() + s.substring(1);
        }
    }

    /**
     * convert a name to <code>PascalCase</code>: {@link #toCamelCase(String)}
     * with its first character upper cased.
     *
     * @param s
     *            the name to convert.
     * @return the name in <code>PascalCase</code>.
     */
    public static String toPascalCase(String s) {
        String res = toCamelCase(s);
        if (res.length() > 0) {
            return res.substring(0, 1).toUpperCase() + res.substring(1);
        } else {
            return res;
        }
    }

    /**
     * convert a name to <code>snake_case</code>. A name already holding
     * <code>_</code> or <code>-</code> only gets its dashes turned into
     * underscores and is lower cased; anything else has an underscore put
     * before every upper case character but the first one.
     *
     * @param s
     *            the name to convert.
     * @return the name in <code>snake_case</code>.
     */
    public static String toSnakeCase(String s) {
        if (s.contains("_") || s.contains("-")) {
            return replace(s, "-", "_").toLowerCase();
        } else {
            if (isUpper(s))
                s = s.toLowerCase();
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<s.length(); i++) {
                char c = s.charAt(i);
                // do not prepend a separator to a leading upper-case character
                if (c >= 'A' && c <= 'Z' && sb.length() > 0) {
                    sb.append('_');
                }
                sb.append((""+c).toLowerCase());
            }
            return sb.toString();
        }
    }

    /**
     * convert a name to <code>SCREAMING_SNAKE_CASE</code>:
     * {@link #toSnakeCase(String)} upper cased.
     *
     * @param s
     *            the name to convert.
     * @return the name in <code>SCREAMING_SNAKE_CASE</code>.
     */
    public static String toScreamingSnakeCase(String s) {
        return toSnakeCase(s).toUpperCase();
    }

    /**
     * convert a name to <code>skewer-case</code>:
     * {@link #toSnakeCase(String)} with dashes instead of underscores.
     *
     * @param s
     *            the name to convert.
     * @return the name in <code>skewer-case</code>.
     */
    public static String toSkewerCase(String s) {
        return replace(toSnakeCase(s), "_", "-");
    }

    /**
     * another name for {@link #toSkewerCase(String)}.
     *
     * @param s
     *            the name to convert.
     * @return the name in <code>kebab-case</code>.
     */
    public static String toKebabCase(String s) {
        return toSkewerCase(s);
    }

    /*
     * ------------------------------------------------------------------
     * password based encryption
     *
     * Current format (v2):
     *
     *   "ENC2:" + Base64( salt(16) || iv(12) || AES-256-GCM(ciphertext||tag) )
     *
     * The key is derived from the master password with
     * PBKDF2WithHmacSHA256 (210,000 iterations, 256 bit key) using the salt
     * embedded in the value itself, so a value stays readable even after the
     * session salt has been rotated.
     *
     * Legacy format (no prefix) is plain Base64 of an AES-128/CBC ciphertext
     * whose key and IV are the two halves of SHA-256(master). It is still
     * readable so that existing configurations keep working; every legacy
     * value seen raises {@link #hasLegacyEncryption()} so the caller can
     * rewrite the file in the current format.
     * ------------------------------------------------------------------
     */
    /** marks a value stored in the current format. */
    private static final String ENC_V2_PREFIX = "ENC2:";
    /** key derivation function the master password is stretched with. */
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    /** iteration count of the key derivation. */
    private static final int PBKDF2_ITERATIONS = 210_000;
    /** length of the derived key, in bits. */
    private static final int KEY_BITS = 256;
    /** length of the salt, in bytes. */
    private static final int SALT_LEN = 16;
    /** length of the GCM initialisation vector, in bytes. */
    private static final int GCM_IV_LEN = 12;
    /** length of the GCM authentication tag, in bits. */
    private static final int GCM_TAG_BITS = 128;
    /** length of the GCM authentication tag, in bytes. */
    private static final int GCM_TAG_LEN = GCM_TAG_BITS / 8;

    /** source of the salts and the initialisation vectors. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * derived keys by salt. Loading a configuration decrypts many values that
     * share a single salt, and PBKDF2 is deliberately expensive, so each salt
     * is stretched only once per master password.
     */
    private static final Map<String, SecretKeySpec> KEY_CACHE = new ConcurrentHashMap<>();

    /** the master password in effect, <code>null</code> until it is entered. */
    private static volatile char[] masterPassword = null;
    /** salt used for every value encrypted during this session. */
    private static volatile byte[] sessionSalt = null;
    /** key of the superseded scheme, derived on first use. */
    private static volatile SecretKeySpec legacyKey = null;
    /** initialisation vector of the superseded scheme, derived with the key. */
    private static volatile IvParameterSpec legacyIv = null;
    /** a value in the superseded format has been read. */
    private static volatile boolean legacySeen = false;

    /** number of PBKDF2 runs, for tests to assert the key cache is effective. */
    private static final AtomicInteger DERIVATION_COUNT = new AtomicInteger();

    /**
     * set the master password every following {@link #encrypt(String)} and
     * {@link #decrypt(String)} works with, and draw a fresh session salt. The
     * derived keys of the previous password and the legacy flag are dropped.
     *
     * @param master
     *            the master password.
     * @throws IllegalArgumentException
     *             if <code>master</code> is <code>null</code>.
     */
    public static void setMaster(String master) {
        if (master == null)
            throw new IllegalArgumentException("master password cannot be null");
        KEY_CACHE.clear();
        legacyKey = null;
        legacyIv = null;
        legacySeen = false;
        masterPassword = master.toCharArray();
        byte[] salt = new byte[SALT_LEN];
        RANDOM.nextBytes(salt);
        sessionSalt = salt;
    }

    /**
     * whether any value decrypted since the last {@link #setMaster(String)}
     * was stored in the superseded format.
     *
     * @return a legacy value has been read or not.
     */
    public static boolean hasLegacyEncryption() {
        return legacySeen;
    }

    /**
     * forget that legacy values have been read, to be called once they have
     * been rewritten in the current format.
     */
    public static void clearLegacyEncryption() {
        legacySeen = false;
    }

    /**
     * test hook: how many times PBKDF2 has actually been executed.
     *
     * @return the number of key derivations since the class was loaded.
     */
    static int keyDerivationCount() {
        return DERIVATION_COUNT.get();
    }

    /** test hook: return to the state before a master password was entered. */
    static void clearMaster() {
        KEY_CACHE.clear();
        masterPassword = null;
        sessionSalt = null;
        legacyKey = null;
        legacyIv = null;
        legacySeen = false;
    }

    /**
     * @return the master password in effect.
     * @throws IllegalStateException
     *             if no master password has been set.
     */
    private static char[] requireMaster() {
        char[] master = masterPassword;
        if (master == null)
            throw new IllegalStateException("master password is not set");
        return master;
    }

    /**
     * stretch the master password into an AES key with <code>salt</code>,
     * remembering the result so that the many values sharing one salt are
     * derived only once.
     *
     * @param salt
     *            the salt the value was, or is to be, encrypted with.
     * @return the derived key.
     * @throws IllegalStateException
     *             if no master password has been set.
     * @throws RuntimeException
     *             if the key derivation itself fails.
     */
    private static SecretKeySpec deriveKey(byte[] salt) {
        char[] master = requireMaster();
        String cacheKey = Base64.getEncoder().encodeToString(salt);
        return KEY_CACHE.computeIfAbsent(cacheKey, ignore -> {
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
                PBEKeySpec spec = new PBEKeySpec(master, salt, PBKDF2_ITERATIONS, KEY_BITS);
                try {
                    DERIVATION_COUNT.incrementAndGet();
                    return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
                } finally {
                    spec.clearPassword();
                }
            } catch (GeneralSecurityException ex) {
                throw new RuntimeException("Key derivation failed", ex);
            }
        });
    }

    /**
     * key and IV of the superseded scheme: the two halves of SHA-256(master).
     * Both are worked out on the first call and kept afterwards.
     *
     * @return the legacy AES key; the matching IV is left in
     *         <code>legacyIv</code>.
     * @throws IllegalStateException
     *             if no master password has been set.
     */
    private static SecretKeySpec legacyKey() {
        SecretKeySpec key = legacyKey;
        if (key == null) {
            char[] master = requireMaster();
            byte[] digest;
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                digest = md.digest(new String(master).getBytes(StandardCharsets.UTF_8));
            } catch (GeneralSecurityException ex) {
                throw new RuntimeException("Decryption failed", ex);
            }
            key = new SecretKeySpec(Arrays.copyOfRange(digest, 0, 16), "AES");
            legacyKey = key;
            legacyIv = new IvParameterSpec(Arrays.copyOfRange(digest, 16, 32));
        }
        return key;
    }

    /**
     * encrypt a value in the current format: AES-256-GCM under a key derived
     * from the master password and the session salt, written as
     * <code>ENC2:</code> and the Base64 of salt, IV and ciphertext.
     *
     * @param value
     *            the plain text.
     * @return the encrypted value; <code>null</code> for <code>null</code> and
     *         an empty string for a blank value.
     * @throws IllegalStateException
     *             if no master password has been set.
     * @throws RuntimeException
     *             if the encryption itself fails.
     */
    public static String encrypt(String value) {
        if (value == null)
            return null;
        if (isEmpty(value))
            return "";
        try {
            byte[] salt = sessionSalt;
            if (salt == null)
                throw new IllegalStateException("master password is not set");
            SecretKeySpec key = deriveKey(salt);
            byte[] iv = new byte[GCM_IV_LEN];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[salt.length + iv.length + encrypted.length];
            System.arraycopy(salt, 0, out, 0, salt.length);
            System.arraycopy(iv, 0, out, salt.length, iv.length);
            System.arraycopy(encrypted, 0, out, salt.length + iv.length, encrypted.length);
            return ENC_V2_PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("encryption failed", ex);
            throw new RuntimeException("Encryption failed", ex);
        }
    }

    /**
     * decrypt a value written in either format. A value in the superseded
     * format is read as well and raises {@link #hasLegacyEncryption()}, so that
     * the caller can rewrite the file in the current one.
     *
     * @param encrypted
     *            the stored value.
     * @return the plain text; <code>null</code> for <code>null</code> and an
     *         empty string for a blank value.
     * @throws IllegalStateException
     *             if no master password has been set.
     * @throws RuntimeException
     *             if the value cannot be decrypted, a wrong master password
     *             included.
     */
    public static String decrypt(String encrypted) {
        if (encrypted == null)
            return null;
        if (isEmpty(encrypted))
            return "";
        if (encrypted.startsWith(ENC_V2_PREFIX))
            return decryptV2(encrypted.substring(ENC_V2_PREFIX.length()));
        String res = decryptLegacy(encrypted);
        legacySeen = true;
        return res;
    }

    /**
     * decrypt a value of the current format, whose salt and initialisation
     * vector are carried in the value itself.
     *
     * @param payload
     *            the Base64 payload, without the <code>ENC2:</code> prefix.
     * @return the plain text.
     * @throws IllegalStateException
     *             if no master password has been set.
     * @throws RuntimeException
     *             if the value is truncated, tampered with or encrypted under
     *             another password.
     */
    @SuppressWarnings("UseSpecificCatch")
    private static String decryptV2(String payload) {
        try {
            byte[] enc = Base64.getDecoder().decode(payload);
            if (enc.length < SALT_LEN + GCM_IV_LEN + GCM_TAG_LEN)
                throw new IllegalArgumentException("encrypted value is truncated");

            byte[] salt = Arrays.copyOfRange(enc, 0, SALT_LEN);
            byte[] iv = Arrays.copyOfRange(enc, SALT_LEN, SALT_LEN + GCM_IV_LEN);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(salt), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] orig = cipher.doFinal(enc, SALT_LEN + GCM_IV_LEN,
                    enc.length - SALT_LEN - GCM_IV_LEN);
            return new String(orig, StandardCharsets.UTF_8);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("decryption failed", ex);
            throw new RuntimeException("Decryption failed", ex);
        }
    }

    /**
     * decrypt a value of the superseded format: AES-128/CBC under the key and
     * IV of {@link #legacyKey()}.
     *
     * @param encrypted
     *            the stored Base64 value.
     * @return the plain text.
     * @throws IllegalStateException
     *             if no master password has been set.
     * @throws RuntimeException
     *             if the value cannot be decrypted.
     */
    @SuppressWarnings("UseSpecificCatch")
    private static String decryptLegacy(String encrypted) {
        try {
            byte[] enc = Base64.getDecoder().decode(encrypted);
            SecretKeySpec key = legacyKey();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, key, legacyIv);
            return new String(cipher.doFinal(enc), StandardCharsets.UTF_8);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("decryption failed", ex);
            throw new RuntimeException("Decryption failed", ex);
        }
    }

    /**
     * given array holds a string equal to <code>key</code> or not.
     *
     * @param key
     *            string to look for, may be <code>null</code>.
     * @param haystack
     *            array to be searched, may be <code>null</code>.
     * @return <code>key</code> is in <code>haystack</code> or not;
     *         <code>false</code> whenever either argument is
     *         <code>null</code>. An element that is <code>null</code> itself is
     *         skipped instead of breaking the comparison.
     */
    public static boolean strIn(String key, String[] haystack) {
        if (key == null || haystack == null)
            return false;
        for (String h: haystack) {
            if (key.equals(h))
                return true;
        }
        return false;
    }
}

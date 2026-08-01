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
    private final static byte[] SPACE_CHARS     = " \t\r\n".getBytes(StandardCharsets.UTF_8);

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

        if (src.contains(delim)) {
            while ((idx = src.indexOf(delim, prevIdx)) > -1) {
                String item = src.substring(prevIdx, idx);
                if (trim)
                    item = item.trim();
                res.add(item);
                prevIdx = idx + delimlen;
            }
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
     * @return padded string.
     */
    public static String padString(String src, int length, char padchar,
            boolean right)
    {
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
     *            array of bytes.
     * @return <code>src</code> is in <code>spc</code> or not.
     */
    public static boolean isSpace(int src, byte[] spc)
    {
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
     *            space string.
     * @return <code>src</code> is in <code>spc</code> or not.
     */
    public static boolean isSpace(char src, String spc)
    {
        for (int i = 0; i < spc.length(); i++) {
            if (src == spc.charAt(i))
                return true;
        }
        return false;
    }

    /**
     * every character in <code>src</code> are in <code>spc</code> return true, any
     * one of them is not in <code>spc</code> return false.
     *
     * @param src
     *            string to be tested.
     * @param spc
     *            space string.
     * @return all chracters of <code>src</code> is space or not.
     */
    public static boolean isSpace(String src, String spc)
    {
        for (int i = 0; i < src.length(); i++) {
            if (!isSpace(src.charAt(i), spc))
                return false;
        }
        return true;
    }

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

    public static String trim(String input) {
        return trim(input, new String(SPACE_CHARS, StandardCharsets.UTF_8));
    }

    public static String trimLeft(String input) {
        return trimLeft(input, new String(SPACE_CHARS, StandardCharsets.UTF_8));
    }

    public static String trimRight(String input) {
        return trimRight(input, new String(SPACE_CHARS, StandardCharsets.UTF_8));
    }

    public static String trim(String input, String spaceChars) {
        return trim(input, spaceChars.toCharArray());
    }

    public static String trimLeft(String input, String spaceChars) {
        return trimLeft(input, spaceChars.toCharArray());
    }

    public static String trimRight(String input, String spaceChars) {
        return trimRight(input, spaceChars.toCharArray());
    }

    public static boolean contains(char[] charArr, char c) {
        for (char ca:charArr)
            if (ca == c) return true;
        return false;
    }

    public static String trimLeft(String input, char[] spaceChars) {
        int st = 0, ed = input.length();
        while (st < ed && contains(spaceChars, input.charAt(st)))
            st++;
        if (st < ed)
            return input.substring(st);
        return "";
    }

    public static String trimRight(String input, char[] spaceChars) {
        int st = 0, ed = input.length();
        while (st < ed && contains(spaceChars, input.charAt(ed-1)))
            ed--;
        if (st < ed)
            return input.substring(0, ed);
        return "";
    }

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

    public static String replaceWith(
            String source, Map<String,String> map, String delim
            ) throws ParseException
    {
        return replaceWith(source, map, delim, delim);
    }

    private final static String NUM_DIGITS = "+-0123456789";
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

    @SuppressWarnings("UseSpecificCatch")
    public static int toInt(String intval)
    {
        try {
            return Integer.parseInt(parseInt(intval));
        } catch(Exception t) {
            return 0;
        }
    }

    public static String dateFormat(String format, Date dt)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(dt);
    }

    public static String dateFormat(String format)
    {
        return dateFormat(format, new Date());
    }

    public static boolean contains(Object[] arr, Object val) {
        if (arr == null || val == null)
            return false;
        for (Object c:arr)
            if (c.equals(val)) return true;
        return false;
    }

    public static boolean isUpper(CharSequence str) {
        return str.chars()
                .filter(c -> c >= 'a' && c <= 'z')
                .findFirst()
                .orElse(-1) == -1;
    }

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

    public static String toPascalCase(String s) {
        String res = toCamelCase(s);
        if (res.length() > 0) {
            return res.substring(0, 1).toUpperCase() + res.substring(1);
        } else {
            return res;
        }
    }

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

    public static String toScreamingSnakeCase(String s) {
        return toSnakeCase(s).toUpperCase();
    }

    public static String toSkewerCase(String s) {
        return replace(toSnakeCase(s), "_", "-");
    }

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
    private static final String ENC_V2_PREFIX = "ENC2:";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_LEN = 16;
    private static final int GCM_IV_LEN = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_TAG_LEN = GCM_TAG_BITS / 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * derived keys by salt. Loading a configuration decrypts many values that
     * share a single salt, and PBKDF2 is deliberately expensive, so each salt
     * is stretched only once per master password.
     */
    private static final Map<String, SecretKeySpec> KEY_CACHE = new ConcurrentHashMap<>();

    private static volatile char[] masterPassword = null;
    /** salt used for every value encrypted during this session. */
    private static volatile byte[] sessionSalt = null;
    private static volatile SecretKeySpec legacyKey = null;
    private static volatile IvParameterSpec legacyIv = null;
    private static volatile boolean legacySeen = false;

    /** number of PBKDF2 runs, for tests to assert the key cache is effective. */
    private static final AtomicInteger DERIVATION_COUNT = new AtomicInteger();

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

    /** test hook: how many times PBKDF2 has actually been executed. */
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

    private static char[] requireMaster() {
        char[] master = masterPassword;
        if (master == null)
            throw new IllegalStateException("master password is not set");
        return master;
    }

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

    public static boolean strIn(String key, String[] haystack) {
        for (String h: haystack) {
            if (key.equals(h))
                return true;
        }
        return false;
    }
}

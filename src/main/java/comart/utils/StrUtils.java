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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * string and byte utilities. This class contains various methods related with
 * <code>String</code> and <code>byte</code>. Some methods are <code>static</code>,
 * and the others are normal. Static methods are independent with every
 * character-set. If you want process strings in certain character-set, you must
 * instantiate this class by calling of <code>getInstance</code> method with
 * character-set you want.
 * 
 * @author Soungjin Park
 * @version 1.0
 */
public class StrUtils
{
    private static final Logger logger = Logger.getLogger(StrUtils.class.getName());
    private final static byte[] SPACE_CHARS     = " \t\r\n".getBytes();
    private static Charset      _defaultCharset = StandardCharsets.UTF_8;
    private Charset             _charset        = _defaultCharset;
    private static final HashMap<Charset,StrUtils>    _instances = new HashMap<>();

    private StrUtils(Charset charset)
    {
        _charset = charset;
    }
    
    
    /**
     * get instance of <code>StrUtils</code> which operates bytes with
     * <code>charset</code>. this class caches instance of each character-set.
     * 
     * @param charset
     *            character-set.
     * @return instance of <code>StrUtils</code>.
     * @throws UnsupportedEncodingException
     *             if the named <code>charset</code> is not supported
     */
    public synchronized static StrUtils getInstance(String charset)
            throws UnsupportedEncodingException
    {
        return getInstance(Charset.forName(charset));
    }

    /**
     * get instance of <code>StrUtils</code> which operates bytes with
     * <code>charset</code>. this class caches instance of each character-set.
     * 
     * @param charset
     *            character-set.
     * @return instance of <code>StrUtils</code>.
     */
    public synchronized static StrUtils getInstance(Charset charset)
    {
        StrUtils instance = (StrUtils) _instances.get(charset);
        if (instance == null) {
            instance = new StrUtils(charset);
            _instances.put(charset, new StrUtils(charset));
        }
        return instance;
    }
    
    /**
     * set default character-set. After call of this method,
     * <code>getInstance()</code> method will return <code>StrUtils</code> object
     * which corresponding with given <code>charset</code>.
     * 
     * @param charset
     *            default character-set.
     */
    public static void setDefault(Charset charset)
    {
        _defaultCharset = charset;
    }

    /**
     * get <code>StrUtils</code> object with default character-set.
     * 
     * @return <code>StrUtils</code> object.
     */
    public static StrUtils getInstance()
    {
        return getInstance(_defaultCharset);
    }

    /**
     * split string with delimiter <code>delim</code>.
     * 
     * @param src
     *            source string.
     * @param delim
     *            delimiter.
     * @return split string array.
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
     * @return split string array.
     */
    public static String[] split(String src, String delim, boolean trim)
    {
        String retStrs[] = null;
        ArrayList<String> res = new ArrayList<>();
        src = src + delim;  // add last delimiter for convenience(no need to process last item)
        int idx, prevIdx = 0;
        if (src != null && delim != null) {
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
            retStrs = new String[res.size()];
            retStrs = (String[]) res.toArray(retStrs);
        }
        return retStrs;
    }

    /**
     * get index of subsequence bytes from source byte sequence. returns the
     * index within <code>src</code> of the first occurrence of the specified
     * sub-byte array, starting at the specified index. the integer returned is
     * the smallest value <i>k</i> for which:
     * 
     * <pre>
     * k &gt;= Math.min(offset, delim.length) &amp;&amp;
     *         this.startsWith(src, delim, &lt;i&gt;k&lt;/i&gt;)
     * </pre>
     * 
     * if no such value of <i>k</i> exists, then -1 is returned.
     * 
     * @param src
     *            source byte array.
     * @param delim
     *            the sub-byte array for which to search.
     * @param offset
     *            the index from which to start the search.
     * @return the index within this byte array of the first occurrence of the
     *         specified sub-byte array, starting at the specified index.
     */
    @SuppressWarnings("empty-statement")
    public static int indexOf(byte[] src, byte[] delim, int offset)
    {
        int i, j;
        int delimlen = delim.length;
        int srclen = src.length - delimlen;
        for (i = offset; i < srclen; i++) {
            for (j = 0; j < delimlen && src[i + j] == delim[j]; j++)
                ;
            if (j == delimlen)
                return i;
        }
        return -1;
    }

    /**
     * returns a new byte array that is a sub-byte array of <code>src</code>. the
     * sub-byte array begins at the specified <code>beginindex</code> and extends
     * to the byte at index <code>endindex - 1</code>. thus the length of the
     * sub-byte array is <code>endindex-beginindex</code>. <br>
     * <br>
     * examples:
     * 
     * <pre>
     * StrUtils.subbytes(&quot;hamburger&quot;.getBytes(), 4, 8)
     *  - returns byte array representation of &quot;urge&quot;
     * StrUtils.subbytes(&quot;smiles&quot;.getBytes(), 1, 5)
     *  - returns byte array representation of &quot;mile&quot;
     * </pre>
     * 
     * @param src
     *            source byte array.
     * @param beginindex
     *            the beginning index, inclusive.
     * @param endindex
     *            the ending index, exclusive.
     * @return the specified sub-byte array.
     */
    public static byte[] subbytes(byte[] src, int beginindex, int endindex)
    {
        int length = endindex - beginindex;
        byte[] res = new byte[length];
        System.arraycopy(src, beginindex, res, 0, length);
        return res;
    }

    /**
     * split byte array into multiple byte array using <code>delim</code>.
     * sub-byte array of <code>src</code>(<code>length</code> bytes from index
     * <code>offset</code>) would be split.
     * 
     * @param src
     *            source byte array.
     * @param offset
     *            offset of sub-byte array of <code>src</code> to be split.
     * @param length
     *            length of sub-byte array of <code>src</code> ot be split.
     * @param delim
     *            delimiter.
     * @return array of byte arrays.
     */
    public static byte[][] split(byte[] src, int offset, int length,
            byte[] delim)
    {
        byte retbytes[][] = null;
        ArrayList<byte[]> res = new ArrayList<>();
        int idx, prevIdx = offset;
        if (src != null && delim != null) {
            int delimlen = delim.length;

            if (indexOf(src, delim, 0) > -1) {
                while ((idx = indexOf(src, delim, prevIdx)) > -1
                        && idx < length) {
                    res.add(subbytes(src, prevIdx, idx));
                    prevIdx = idx + delimlen;
                }
            }
            res.add(subbytes(src, prevIdx, offset + length));
            retbytes = new byte[res.size()][];
            retbytes = (byte[][]) res.toArray(retbytes);
        }
        return retbytes;
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
     * create byte array with <code>length</code> containing <code>bt</code>.
     * 
     * @param bt
     *            padding byte.
     * @param length
     *            length of result byte array.
     * @return padded byte array.
     */
    private static byte[] pad(byte bt, int length)
    {
        byte[] res = new byte[length];
        Arrays.fill(res, bt);
        return res;
    }

    /**
     * pad <code>data</code> with <code>length</code> and <code>padbyte</code>.
     * 
     * @param data
     *            source byte array.
     * @param length
     *            length of result byte array.
     * @param padbyte
     *            padding byte.
     * @param right
     *            right pad or not.
     * @return padded byte array.
     */
    public static byte[] padBytes(byte[] data, int length, byte padbyte,
            boolean right)
    {
        byte[] res = pad(padbyte, length);
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
        return res;
    }

    /**
     * create new byte array representing <code>data</code> padded with
     * <code>length</code> and <code>padbyte</code>.
     * 
     * @param data
     *            source data to be padded.
     * @param length
     *            length of result byte array.
     * @param padbyte
     *            padding byte.
     * @param right
     *            right pad or not.
     * @return padded byte array.
     */
    public static byte[] padBytes(int data, int length, byte padbyte,
            boolean right)
    {
        String sdata = data + "";
        return padBytes(sdata.getBytes(), length, padbyte, right);
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
     * convert byte array to string with predefined character-set.
     * 
     * @param src
     *            source byte array.
     * @return converted string.
     */
    public String toString(byte[] src)
    {
        return toString(src, 0, src.length);
    }

    /**
     * convert sub-byte array to string with predefined character-set.
     * 
     * @param src
     *            source byte array.
     * @param offset
     *            start index of sub-byte array to be converted.
     * @param length
     *            length of sub-byte array to be converted.
     * @return converted string.
     */
    public String toString(byte[] src, int offset, int length)
    {
        return new String(src, offset, length, _charset);
    }

    /**
     * convert string to byte array with predefined character-set.
     * 
     * @param src
     *            source string.
     * @return converted byte array.
     */
    public byte[] getBytes(String src)
    {
        if (src == null)
            return null;
        return src.getBytes(_charset);
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
     * every bytes in <code>src</code> are in <code>spc</code> return true, any one
     * of them is not in <code>spc</code> return false.
     * 
     * @param src
     *            bytes to be tested.
     * @param spc
     *            array of bytes.
     * @return all bytes of <code>src</code> is space or not.
     */
    public static boolean isSpace(byte[] src, byte[] spc)
    {
        for (int i = 0; i < src.length; i++) {
            if (!isSpace(src[i], spc))
                return false;
        }
        return true;
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

    /**
     * tests if <code>src</code> starts with the specified <code>arg</code>. this
     * is the same method with <code>String.startsWith</code> except applied to
     * byte arrays.
     * 
     * @param src
     *            source bytes to be tested.
     * @param arg
     *            the prefix.
     * @return <code>true</code> if the byte sequence represented by the
     *         argument is a prefix of the byte sequence represented by
     *         <code>src</code>; false otherwise. Note also that <code>true</code>
     *         will be returned if the argument is an empty array. or is equal
     *         to <code>src</code> byte array object as determined by the
     *         StrUtils.equals method.
     */
    public static boolean startsWith(byte[] src, byte[] arg)
    {
        for (int i = 0; i < arg.length; i++)
            if (src[i] != arg[i])
                return false;
        return true;
    }

    /**
     * compares <code>a</code> to the specified <code>b</code>. the result is
     * <code>true</code> if and only if the <code>b</code> is not <code>null</code>
     * and is a byte array object that represents the same sequence of bytes as
     * <code>a</code>.
     * 
     * @param a
     *            byte array.
     * @param b
     *            byte array.
     * @return <code>true</code> if byte arrays are equal; <code>false</code>
     *         otherwise.
     */
    public static boolean equals(byte[] a, byte[] b)
    {
        int alen = a.length;
        int blen = b.length;
        if (alen == blen) {
            for (int i = 0; i < alen; i++)
                if (a[i] != b[i])
                    return false;
            return true;
        } else {
            return false;
        }
    }
    
    public static boolean isEmpty(CharSequence seq) {
        if (seq != null) {
            return seq.toString().trim().isEmpty();
        }
        return true;
    }

    /**
     * retreive substring of <code>src</code> from subsequence <code>from</code> to
     * subsequence <code>to</code>.
     * 
     * <pre>
     * StrUtils.pilloff(&quot;abcd{efg}hi&quot;, &quot;{&quot;, &quot;}&quot;)
     *     - results &quot;efg&quot;
     * 
     * StrUtils.pilloff(&quot;he said \&quot;I'm hungry\&quot;&quot;,
     *                  &quot;\&quot;&quot;, &quot;\&quot;&quot;)
     *     - results &quot;I'm hungry&quot;
     * </pre>
     * 
     * @param src
     *            source string to be pill off.
     * @param from
     *            left subsequence.
     * @param to
     *            right subsequence.
     * @return result string.
     */
    public static String pilloff(String src, String from, String to)
    {
        int start, end;
        start = src.indexOf(from);
        if (start >= 0) {
            start++;
            end = src.indexOf(to, start);
            if (end >= 0) {
                return src.substring(start, end);
            }
        }
        return null;
    }
    
    public static String[] pillAllOff(String src, String from, String to)
    {
        ArrayList<String> vec = new ArrayList<>();
        int start, end = 0;
        while (true) {
            start = src.indexOf(from, end);
            if (start >= 0) {
                start++;
                end = src.indexOf(to, start);
                if (end >= 0) {
                    vec.add(src.substring(start, end));
                    if (src.length() > end)
                        end++;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        if (!vec.isEmpty()) {
            String arr[] = new String[vec.size()];
            arr = (String[])vec.toArray(arr);
            return arr;
        } else {
            return null;
        }
    }

    private static final String HEXCHARS = "0123456789ABCDEF";

    /**
     * convert hexadecimal string to byte array. every 2 characters of
     * hexadecimal string converted into one byte.
     * 
     * @param hex
     *            hexadecimal string to be converted.
     * @return converted byte array.
     */
    public static byte[] hexToBytes(String hex)
    {
        return hexToBytes(hex, hex.length() / 2);
    }

    public static byte[] hexToBytes(String hex, int size)
    {
        byte[] res = new byte[size];
        String uhex = hex.toUpperCase().trim();
        for (int i = 0; i < hex.length() / 2; i++) {
            int b = HEXCHARS.indexOf(uhex.charAt(i * 2)) << 4;
            b |= HEXCHARS.indexOf(uhex.charAt(i * 2 + 1));
            res[i] = (byte) (b & 0xFF);
        }
        for (int j = hex.length() / 2; j < size; j++) {
            res[j] = (byte) 0x00;
        }
        return res;
    }

    /**
     * convert bytes to hexadecimal string.each bytes are converted two
 hexadecimal digits, conversion of upper 4-bits are located former, and
 lower 4-bits are latter.
     * 
     * @param bytes
     *            bytes to be converted to hexadecimal.
     * @param offset
     * @param length
     * @return hexadecimal string.
     */
    public static String bytesToHex(byte[] bytes, int offset, int length)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int upper = (((int) bytes[offset+i]) >> 4) & 0xF;
            int lower = ((int) bytes[offset+i]) & 0xF;
            sb.append(HEXCHARS.charAt(upper));
            sb.append(HEXCHARS.charAt(lower));
        }
        return sb.toString();
    }
    public static String bytesToHex(byte[] bytes)
    {
        return bytesToHex(bytes, 0, bytes.length);
    }

    /**
     * alias of <code>pad(b, size)</code>.
     * 
     * @param size
     *            length of result byte array.
     * @param b
     *            result byte array will be filled with <code>b</code>.
     * @return byte array.
     */
    public static byte[] space(int size, byte b)
    {
        return pad(b, size);
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
        return trim(input, new String(SPACE_CHARS));
    }
    
    public static String trimLeft(String input) {
        return trimLeft(input, new String(SPACE_CHARS));
    }
    
    public static String trimRight(String input) {
        return trimRight(input, new String(SPACE_CHARS));
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
            if (contains(new char[]{'"','\''}, res.charAt(0)) &&
                    res.charAt(0) == res.charAt(res.length()-1)) {
                res = res.substring(1, res.length()-1);
            }
            return res;
        }
        return "";
    }
    
    public static String[] mapSubs(
            String mapper, String source, String sub, String delim
            ) throws ParseException
    {
        ArrayList<String> arrs = new ArrayList<>();
        if (mapper != null && source != null && sub != null) {
            int baseidx = 0, srcbase = 0;
            while (true) {
                int stidx = mapper.indexOf(sub, baseidx);
                int fridx, toidx;
                if (stidx > -1) {
                    fridx = mapper.lastIndexOf(delim, stidx-1);
                    baseidx = stidx+sub.length();
                    toidx = mapper.indexOf(delim, baseidx);
                    
                    if (fridx < 0)
                        fridx = 0;
                    else
                        fridx++;
                    if (toidx < 0)
                        toidx = mapper.length();
    
                    String prefix, postfix;
                    prefix = mapper.substring(fridx, stidx);
                    postfix = mapper.substring(stidx+sub.length(), toidx);
                    
                    int stpos, edpos;
                    if ("".equals(prefix)) {
                        stpos = 0;
                    } else {
                        stpos = source.indexOf(prefix, srcbase);
                        if (stpos < 0)
                            throw new ParseException(
                                    "source does not match with template :["+
                                            source+"],["+mapper+"] finding["+
                                            sub+"]", 0);
                        else
                            stpos += prefix.length(); 
                    }
                    if ("".equals(postfix)) {
                        edpos = source.length();
                    } else {
                        edpos = source.indexOf(postfix, stpos);
                        if (edpos < 0)
                            throw new ParseException(
                                    "source does not match with template :["+
                                            source+"],["+mapper+"]", 0);
                    }
                    srcbase = edpos;
                    
                    arrs.add(source.substring(stpos, edpos));
                } else {
                    break;
                }
            }
        }
        String[] res = new String[arrs.size()];
        res = arrs.toArray(res);
        return res;
    }
    
    public static String mapSub(
            String mapper, String source, String sub, String delim
            ) throws ParseException
    {
        String [] res = mapSubs(mapper, source, sub, delim);
        if (res != null && res.length > 0)
            return res[0];
        return null;
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

        try {
            while ((currIdx = source.indexOf(delimSt, stIdx)) > -1) {
                sb.append(source.substring(stIdx, currIdx));
                currIdx += stLen;
                int lstIdx = source.indexOf(delimEd, currIdx);
                if (lstIdx > -1) {
                    String key = source.substring(currIdx, lstIdx);
                    String val = ObjUtils.getValue(obj, key).toString();
                    if (val != null)
                        sb.append(val);
                    else
                        sb.append(delimSt).append(key).append(delimEd);
                    stIdx = lstIdx + edLen;
                } else {
                    throw new ParseException("End delimiter not presented.", 0);
                }
            }
            sb.append(source.substring(stIdx));
        } catch(Exception e) {
            throw new RuntimeException("cannot get value from object", e);
        }
        return sb.toString();
    }

    public static String replaceWith(
            String source, Map<String,String> map, String delim
            ) throws ParseException
    {
        return replaceWith(source, map, delim, delim);
    }

    private final static String NUM_DIGITS = "+-0123456789";
    public static String parseInt(String intval) throws ParseException
    {
        StringBuilder sb = new StringBuilder();
        if (intval != null) {
//            OUTBLOCK: {
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
//            }
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
    
    private final static String DATE_DELIMS = "-/.";
    private final static String[] DATE_MONSTRS={
            "jan", "feb", "mar", "apr", "may", "jun",
            "jul", "aug", "sep", "oct", "nov", "dec"
    };
    public static String formatDate(String date)
    {
        if (date == null) return null;
        String dtvals[] = {};
        for (int i=0; i < date.length() && dtvals.length < 3; i++) {
            dtvals = split(date, ""+DATE_DELIMS.charAt(i));
        }
        if (dtvals.length != 3) {
            //System.out.println(
            //        "WARN!! StrUtils.formatDate() unknown format :"+date);
            String[] s2 = split(date, ",");
            if (s2.length == 2) {
                for (int i=0; i<DATE_MONSTRS.length; i++) {
                    if (s2[0].toLowerCase().startsWith(DATE_MONSTRS[i])) {
                        String[] s3 = split(s2[0], " ");
                        dtvals[2] = s3[1].trim();
                        dtvals[1] = "" + (i+1);
                    }
                }
                dtvals[0] = s2[1].trim();
            } else {
                return date;
            }
        }
        
        // trim all elements
        for (int i=0; i<3; i++)
            dtvals[i] = dtvals[i].trim();
        
        // format year part
        if (dtvals[0].length() == 2) {
            // calculate century dynamically
            int cent = Calendar.getInstance().get(Calendar.YEAR);
            int cur = cent % 100;
            cent = (int)(cent / 100);
            int ival = Integer.parseInt(dtvals[0]);
            if (ival > cur)
                cent--;
            dtvals[0] = "" + cent + dtvals[0];
        } else if (dtvals[0].length() != 4) {
            //System.out.println(
            //        "WARN!! StrUtils.formatDate() unknown format :"+date);
            return date;
        }
        
        // format month and day part
        for (int i=1; i<3; i++) {
            if (dtvals[i].length() < 2)
                dtvals[i] = "0"+dtvals[i];
        }
        
        return dtvals[0]+dtvals[1]+dtvals[2];
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
                if (c >= 'A' && c <= 'Z') {
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
    
    public static String unCamelCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.append('_').append(c);
            } else {
                sb.append((""+c).toUpperCase());
            }
        }
        return sb.toString();
    }

    static byte[] raw = new byte[]{
        (byte)0xE1, (byte)0xAB, (byte)0x86, (byte)0xA1, (byte)0x45,
        (byte)0x0D, (byte)0x1C, (byte)0x5B, (byte)0xA2, (byte)0xA0,
        (byte)0xCA, (byte)0x79, (byte)0xA0, (byte)0x1A, (byte)0x92,
        (byte)0x8A
    };

    static final byte[] biv = new byte[] {
        (byte)0x16, (byte)0x44, (byte)0xCC, (byte)0x82, (byte)0x76,
        (byte)0x90, (byte)0x4B, (byte)0x65, (byte)0x2D, (byte)0x0D,
        (byte)0x2E, (byte)0x26, (byte)0x39, (byte)0x62, (byte)0xE8,
        (byte)0x93
    };
    
    static IvParameterSpec iv = new IvParameterSpec(biv);
    
    public static void setMaster(String master) {
        byte[] enc;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            enc = md.digest(master.getBytes(StandardCharsets.UTF_8));
        } catch(Throwable ignore) {
            enc = encrypt(raw, master.getBytes(StandardCharsets.UTF_8));
        }
        if (enc.length == 16) {
            raw = enc;
            iv = new IvParameterSpec(enc);
        } else {
            raw = Arrays.copyOfRange(enc, 0, 16);
            iv = new IvParameterSpec(Arrays.copyOfRange(enc, 16, 32));
        }
    }

    public static String encrypt(String value) {
        if (value == null)
            return null;
        if (isEmpty(value))
            return "";
        byte[] encrypted = encrypt(raw, value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    @SuppressWarnings("UseSpecificCatch")
    public static byte[] encrypt(byte[] key, byte[] data) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec,iv);
            return cipher.doFinal(data);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "encryption failed", ex);
            throw new RuntimeException("Encryption failed", ex);
        }
    }

    public static String decrypt(String encrypted) {
        if (encrypted == null)
            return null;
        if (isEmpty(encrypted))
            return "";
        byte[] enc = Base64.getDecoder().decode(encrypted);
        byte[] orig = decrypt(raw, enc);
        if (orig != null)
            return new String(orig, StandardCharsets.UTF_8);
        return null;
    }
    
    @SuppressWarnings("UseSpecificCatch")
    public static byte[] decrypt(byte[] key, byte[] encrypted) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec,iv);
            
            return cipher.doFinal(encrypted);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "decryption failed", ex);
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
    
    public static void main(String []args) throws ParseException
    {
//        byte[] enc = encrypt(raw, "12345678".getBytes());
//        System.out.println(bytesToHex(enc));
//        byte[] dec = decrypt(raw, enc);
//        System.out.println(new String(dec, StandardCharsets.UTF_8));

        String sample = "   \"\n\"   ";
        System.out.println("Sample String: '"+sample+"'");
        System.out.println("Trimmed Sample String: '"+trim(sample)+"'");


//        String encrypt = encrypt("1234567890123456");
//        System.out.println("encrypted value:" + encrypt);
//        System.out.println("decrypted value:" + (decrypt(encrypt)));
//        
//        
////        String mapper = "%itemTotal% 주%orgTotal%";
////        String source = "14,493,404 주";
//        String mapper = "%P3%,qwwwefunbv=%P1%,guella=%P1%,qwpir9834hn=%P2%";
//        String source = "what the hell,qwwwefunbv=this is,guella=shit!,qwpir9834hn=fuck man!";
//        System.out.println("mapper:" + mapper);
//        System.out.println("source:" + source);
//        String[] p1 = mapSubs(mapper, source, "%P1%", "%");
//        String p2 = mapSub(mapper, source, "%P2%", "%");
//        String p3 = mapSub(mapper, source, "%P3%", "%");
//        System.out.println(p1[0]+" "+p1[1]+" "+p2+". "+p3);
//        String itemTotal = mapSub(mapper, source, "%itemTotal%", "%");
//        String orgTotal = mapSub(mapper, source, "%orgTotal%", "%");
//        System.out.println("itemTotal: "+itemTotal+"  ,orgTotal: "+orgTotal);
//        System.out.println(new String(Base64.getDecoder().decode("lco39FTz98NvhT==")));
//        HashMap<String,String> map = new HashMap<>();
//        map.put("test1", "testVal1");
//        map.put("test2", "testVal2");
//        System.out.println(replaceWithMap("This is test test1 value is $test1$ and test2 value is $test2$", map, "$"));
    }
}

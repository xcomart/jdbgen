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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the password based encryption of {@link StrUtils}: the current
 * PBKDF2 + AES/GCM format and the fallback that keeps values written by
 * earlier releases readable.
 *
 * @author comart
 */
public class EncryptionTest {

    private static final String MASTER = "correct horse battery staple";

    /**
     * encrypt the way releases before the AES/GCM change did: plain Base64 of
     * an AES-128/CBC ciphertext keyed with the two halves of SHA-256(master).
     */
    private static String legacyEncrypt(String master, String value) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(master.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec key = new SecretKeySpec(Arrays.copyOfRange(digest, 0, 16), "AES");
        IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(digest, 16, 32));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return Base64.getEncoder().encodeToString(
                cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testCurrentFormatRoundTrip() {
        StrUtils.setMaster(MASTER);
        String secret = "p@ssw0rd - 한글 - ";
        String enc = StrUtils.encrypt(secret);

        assertTrue(enc.startsWith("ENC2:"), "current format carries its version prefix");
        assertNotEquals(secret, enc);
        assertEquals(secret, StrUtils.decrypt(enc));
        assertFalse(StrUtils.hasLegacyEncryption(),
                "nothing in the current format may be reported as legacy");
    }

    @Test
    public void testSamePlainTextEncryptsDifferently() {
        StrUtils.setMaster(MASTER);
        String secret = "same value twice";
        String first = StrUtils.encrypt(secret);
        String second = StrUtils.encrypt(secret);

        assertNotEquals(first, second, "a fresh IV must be drawn for every value");
        assertEquals(secret, StrUtils.decrypt(first));
        assertEquals(secret, StrUtils.decrypt(second));
    }

    @Test
    public void testWrongMasterIsRejected() {
        StrUtils.setMaster(MASTER);
        String enc = StrUtils.encrypt("secret");

        StrUtils.setMaster("not the master password");
        // the GCM tag does not verify, which is how a wrong master password is
        // detected while loading a configuration
        assertThrows(RuntimeException.class, () -> StrUtils.decrypt(enc));
    }

    @Test
    public void testLegacyValueIsStillReadable() throws Exception {
        StrUtils.setMaster(MASTER);
        String secret = "stored by an older release";
        String legacy = legacyEncrypt(MASTER, secret);

        assertFalse(legacy.startsWith("ENC2:"));
        assertEquals(secret, StrUtils.decrypt(legacy));
        assertTrue(StrUtils.hasLegacyEncryption(),
                "reading a legacy value must be flagged so it can be rewritten");

        // re-encrypting yields the current format, and the flag can be cleared
        // once the configuration has been rewritten
        String migrated = StrUtils.encrypt(StrUtils.decrypt(legacy));
        assertTrue(migrated.startsWith("ENC2:"));
        assertEquals(secret, StrUtils.decrypt(migrated));
        StrUtils.clearLegacyEncryption();
        assertFalse(StrUtils.hasLegacyEncryption());
    }

    @Test
    public void testLegacyValueWithWrongMasterIsRejected() throws Exception {
        String legacy = legacyEncrypt(MASTER, "stored by an older release");
        StrUtils.setMaster("not the master password");
        assertThrows(RuntimeException.class, () -> StrUtils.decrypt(legacy));
    }

    @Test
    public void testNullAndEmptyPassThrough() {
        StrUtils.setMaster(MASTER);
        assertNull(StrUtils.encrypt(null));
        assertNull(StrUtils.decrypt(null));
        assertEquals("", StrUtils.encrypt(""));
        assertEquals("", StrUtils.decrypt(""));
        assertEquals("", StrUtils.encrypt("   "));
        assertEquals("", StrUtils.decrypt("   "));
    }

    @Test
    public void testMalformedValueFails() {
        StrUtils.setMaster(MASTER);
        // too short to hold a salt, an IV and a tag
        assertThrows(RuntimeException.class,
                () -> StrUtils.decrypt("ENC2:" + Base64.getEncoder().encodeToString(new byte[8])));
        // not Base64 at all
        assertThrows(RuntimeException.class, () -> StrUtils.decrypt("ENC2:!!!not base64!!!"));
    }

    @Test
    public void testKeyIsDerivedOncePerSalt() {
        StrUtils.setMaster(MASTER);
        // every value of a session shares one salt, so the first encryption
        // stretches the password and nothing after it should
        String a = StrUtils.encrypt("first");
        int afterFirst = StrUtils.keyDerivationCount();

        String b = StrUtils.encrypt("second");
        String c = StrUtils.encrypt("third");
        assertEquals(afterFirst, StrUtils.keyDerivationCount(),
                "encrypting reuses the session key");

        assertEquals("first", StrUtils.decrypt(a));
        assertEquals("second", StrUtils.decrypt(b));
        assertEquals("third", StrUtils.decrypt(c));
        assertEquals(afterFirst, StrUtils.keyDerivationCount(),
                "decrypting values of a shared salt reuses the cached key");

        // a new master password invalidates the cache
        StrUtils.setMaster(MASTER);
        StrUtils.encrypt("first");
        assertEquals(afterFirst + 1, StrUtils.keyDerivationCount());
    }

    @Test
    public void testEncryptionRequiresMaster() throws Exception {
        StrUtils.clearMaster();
        try {
            assertThrows(IllegalStateException.class, () -> StrUtils.encrypt("secret"));
            assertThrows(IllegalStateException.class,
                    () -> StrUtils.decrypt("ENC2:" + Base64.getEncoder()
                            .encodeToString(new byte[64])));
        } finally {
            StrUtils.setMaster(MASTER);
        }
    }
}

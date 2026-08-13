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

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Gson adapter that keeps the secrets of the configuration encrypted. It is
 * what puts {@link StrUtils#encrypt(String)} between the field and the file, so
 * what is checked here is that a plain text value never reaches the document and
 * comes back unchanged.
 */
public class EncryptionAdapterTest {

    private static final String MASTER = "correct horse battery staple";

    /** the configuration bean, cut down to the fields that carry a secret. */
    private static class Connection {
        private String name;
        @JsonAdapter(EncryptionAdapter.class)
        private String userPassword;
    }

    @BeforeEach
    public void setMaster() {
        StrUtils.setMaster(MASTER);
    }

    private static String write(String value) throws Exception {
        StringWriter out = new StringWriter();
        try (JsonWriter writer = new JsonWriter(out)) {
            new EncryptionAdapter().write(writer, value);
        }
        return out.toString();
    }

    private static String read(String json) throws Exception {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            return new EncryptionAdapter().read(reader);
        }
    }

    @Test
    public void aValueIsWrittenEncryptedAndReadBackAsItWas() throws Exception {
        String secret = "p@ssw0rd - 한글";

        String json = write(secret);

        assertFalse(json.contains(secret), "the plain text must not reach the document");
        assertTrue(json.startsWith("\"ENC2:"), json);
        assertEquals(secret, read(json));
    }

    @Test
    public void everyWriteOfTheSameValueLooksDifferent() throws Exception {
        String first = write("same value twice");
        String second = write("same value twice");

        assertEquals("same value twice", read(first));
        assertEquals("same value twice", read(second));
        // a fresh initialisation vector per value, so that two connections
        // sharing a password are not recognisable as such in the file
        assertNotEquals(first, second);
    }

    @Test
    public void aBlankValueStaysBlankAndAMissingOneStaysMissing() throws Exception {
        assertEquals("\"\"", write(""));
        assertEquals("\"\"", write("   "), "a blank secret is stored as no secret");
        assertEquals("", read("\"\""));
        // nothing to encrypt is written as a JSON null
        assertEquals("null", write(null));
    }

    @Test
    public void aValueOfAnotherMasterPasswordIsRejected() throws Exception {
        String json = write("secret");

        StrUtils.setMaster("not the master password");

        assertThrows(RuntimeException.class, () -> read(json));
    }

    @Test
    public void theAnnotatedFieldOfABeanIsTheOnlyEncryptedOne() {
        Gson gson = new Gson();
        Connection conn = new Connection();
        conn.name = "local h2";
        conn.userPassword = "s3cret";

        String json = gson.toJson(conn);

        assertTrue(json.contains("\"local h2\""), "an unannotated field stays plain: " + json);
        assertFalse(json.contains("s3cret"), "the annotated field is encrypted: " + json);
        assertTrue(json.contains("ENC2:"), json);

        Connection back = gson.fromJson(json, Connection.class);
        assertEquals("local h2", back.name);
        assertEquals("s3cret", back.userPassword);
    }

    @Test
    public void aFieldThatWasNeverSetSurvivesTheRoundTrip() {
        Gson gson = new Gson();
        Connection conn = new Connection();
        conn.name = "no password";

        String json = gson.toJson(conn);
        Connection back = gson.fromJson(json, Connection.class);

        assertNull(back.userPassword,
                "a connection without a password has to stay one");
    }
}

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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

/**
 * A Gson type adapter that keeps a <code>String</code> encrypted in the
 * configuration file. Registered on the fields holding a secret - a database
 * password, for instance - so that they are written with
 * {@link StrUtils#encrypt(String)} and read back with
 * {@link StrUtils#decrypt(String)}, which requires the master password to have
 * been set beforehand.
 *
 * @author comart
 */
public class EncryptionAdapter extends TypeAdapter<String> {

    /**
     * write <code>t</code> in its encrypted form.
     *
     * @param writer
     *            the writer of the document being produced.
     * @param t
     *            the plain text value, may be <code>null</code>.
     * @throws IOException
     *             if the value cannot be written.
     */
    @Override
    public void write(JsonWriter writer, String t) throws IOException {
        writer.value(StrUtils.encrypt(t));
    }

    /**
     * read an encrypted string and return its plain text form.
     *
     * @param reader
     *            the reader of the document being parsed.
     * @return the decrypted value.
     * @throws IOException
     *             if the value cannot be read.
     */
    @Override
    public String read(JsonReader reader) throws IOException {
        return StrUtils.decrypt(reader.nextString());
    }
    
}

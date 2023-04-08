/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package comart.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

/**
 *
 * @author comart
 */
public class EncryptionAdapter extends TypeAdapter<String> {

    @Override
    public void write(JsonWriter writer, String t) throws IOException {
        writer.value(StrUtils.encrypt(t));
    }

    @Override
    public String read(JsonReader reader) throws IOException {
        return StrUtils.decrypt(reader.nextString());
    }
    
}

package comart.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class StringInputStream extends InputStream {
    private ByteArrayInputStream _bais = null;

    public StringInputStream(String str, String charset) throws UnsupportedEncodingException
    {
        this(str, Charset.forName(str));
    }
    
    public StringInputStream(String str, Charset charset)
    {
        _bais = new ByteArrayInputStream(str.getBytes(charset));
    }
    
    @Override
    public int read() throws IOException {
        return _bais.read();
    }
}

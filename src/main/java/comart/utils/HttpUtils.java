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

import java.time.Duration;
import okhttp3.OkHttpClient;

/**
 * The single <code>OkHttpClient</code> every HTTP call of the application goes
 * through. Sharing one client keeps its connection pool and thread pool shared
 * as well, and gives every call the same one minute connect, read and write
 * timeout.
 *
 * @author comart
 */
public class HttpUtils {
    /** connect, read and write timeout of every call. */
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    /** the one client of the application, built once. */
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT)
            .readTimeout(TIMEOUT)
            .writeTimeout(TIMEOUT)
            .build();
    
    /**
     * the shared HTTP client.
     *
     * @return the client every caller is expected to use, never
     *         <code>null</code>.
     */
    public static OkHttpClient getClient() {
        return client;
    }
}

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

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The shared HTTP client. Nothing is requested here - what matters is that
 * every caller gets the same client, so that the connection and thread pools
 * are shared, and that a call cannot hang forever.
 */
public class HttpUtilsTest {

    @Test
    public void everyCallerGetsTheSameClient() {
        OkHttpClient client = HttpUtils.getClient();

        assertSame(client, HttpUtils.getClient());
        assertSame(client.dispatcher(), HttpUtils.getClient().dispatcher(),
                "sharing the client is what shares its thread pool");
        assertSame(client.connectionPool(), HttpUtils.getClient().connectionPool());
    }

    @Test
    public void everyCallIsGivenTheSameOneMinuteTimeout() {
        OkHttpClient client = HttpUtils.getClient();
        long oneMinute = TimeUnit.MINUTES.toMillis(1);

        // the default of OkHttp is 10 seconds to connect and no read timeout at
        // all, which would let a stalled download block the update check
        assertEquals(oneMinute, client.connectTimeoutMillis());
        assertEquals(oneMinute, client.readTimeoutMillis());
        assertEquals(oneMinute, client.writeTimeoutMillis());
    }
}

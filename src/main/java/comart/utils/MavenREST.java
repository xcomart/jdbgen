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

import com.google.gson.Gson;
import comart.tools.jdbgen.types.JDBGenConfig;
import comart.tools.jdbgen.types.maven.MavenConfig;
import comart.tools.jdbgen.types.maven.SearchParams;
import comart.tools.jdbgen.types.maven.SearchResponseItem;
import comart.tools.jdbgen.types.maven.SearchResult;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The Maven Central search REST API, used to find and download a JDBC driver
 * jar without leaving the application. The endpoints are not hard coded: the
 * URL templates come from the <code>maven</code> section of the configuration
 * and are filled in with {@link StrUtils#replaceWith(String, Object, String,
 * String)}, so a mirror can be used instead.
 *
 * @author comart
 */
@Slf4j
public class MavenREST {
    /** number of results asked for per request. */
    private static final int PAGE_SIZE=20;
    
    /**
     * fill the placeholders of <code>urlTemplate</code> from <code>param</code>,
     * request the result and parse the response body as <code>clazz</code>.
     *
     * @param urlTemplate
     *            URL with <code>${...}</code> placeholders.
     * @param param
     *            the object or map the placeholders are read from.
     * @param clazz
     *            the type to parse the response into.
     * @param <T>
     *            the response type.
     * @return the parsed response.
     * @throws ParseException
     *             if the template is malformed.
     * @throws IOException
     *             if the request fails, the status is not a success or the body
     *             is empty.
     */
    private static <T> T restCall(String urlTemplate, Object param, Class<T> clazz) throws ParseException, IOException {
        String url = StrUtils.replaceWith(urlTemplate, param, "${", "}");
        log.info("requesting to {}", url);
        Request req = new Request.Builder().url(url).build();
        try (Response response = HttpUtils.getClient().newCall(req).execute()) {
            if (!response.isSuccessful())
                throw new IOException("request to " + url + " failed: HTTP " +
                        response.code() + " " + response.message());
            ResponseBody body = response.body();
            if (body == null)
                throw new IOException("request to " + url + " returned an empty body.");
            Gson gson = new Gson();
            return gson.fromJson(body.charStream(), clazz);
        }
    }
    
    /**
     * @return the <code>maven</code> section of the configuration, holding the
     *         URL templates of the endpoints.
     */
    private static MavenConfig mavenConfig() {
        return JDBGenConfig.getInstance().getMaven();
    }
    
    /**
     * search Maven Central for artifacts matching <code>queryStr</code>.
     *
     * @param queryStr
     *            the search expression, handed to the API as its
     *            <code>q</code> parameter.
     * @param pageNo
     *            zero based page number; a page holds 20 results.
     * @return the parsed response.
     * @throws ParseException
     *             if the configured URL template is malformed.
     * @throws IOException
     *             if the request fails or the response is not successful.
     */
    public static SearchResult search(String queryStr, int pageNo) throws ParseException, IOException {
        SearchParams query = new SearchParams();
        query.setQ(queryStr);
        query.setRows(""+PAGE_SIZE);
        query.setStart(""+(PAGE_SIZE*pageNo));
        MavenConfig mconf = mavenConfig();
        String searchUrl = mconf.getUrlBase() + mconf.getSearch();
        return restCall(searchUrl, query, SearchResult.class);
    }

    /**
     * every published version of the artifact <code>qitem</code> belongs to,
     * looked up by its group and artifact id.
     *
     * @param qitem
     *            an item of a {@link #search(String, int)} result.
     * @param pageNo
     *            zero based page number; a page holds 20 results.
     * @return the parsed response.
     * @throws ParseException
     *             if the configured URL template is malformed.
     * @throws IOException
     *             if the request fails or the response is not successful.
     */
    public static SearchResult version(SearchResponseItem qitem, int pageNo) throws ParseException, IOException {
        HashMap<String,Object> query = new HashMap<>();
        query.put("g", qitem.getG());
        query.put("a", qitem.getA());
        query.put("rows", PAGE_SIZE);
        query.put("start", (PAGE_SIZE*pageNo));
        MavenConfig mconf = mavenConfig();
        String versionUrl = mconf.getUrlBase() + mconf.getVersion();
        return restCall(versionUrl, query, SearchResult.class);
    }

    /**
     * the URL the jar of <code>qitem</code> can be downloaded from. Nothing is
     * requested here, the configured download template is only filled in with
     * the file path of the item.
     *
     * @param qitem
     *            the artifact version to download.
     * @return the download URL.
     * @throws ParseException
     *             if the configured URL template is malformed.
     * @throws IOException
     *             never thrown, part of the signature for the callers that
     *             handle it together with the requesting methods.
     */
    public static String downloadLink(SearchResponseItem qitem) throws ParseException, IOException {
        HashMap<String,Object> query = new HashMap<>();
        query.put("fpath", qitem.getFilePath());
        MavenConfig mconf = mavenConfig();
        String url = mconf.getUrlBase() + mconf.getDownload();
        return StrUtils.replaceWith(url, query, "${", "}");
    }
}

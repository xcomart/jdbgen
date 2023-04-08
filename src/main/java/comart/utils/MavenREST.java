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
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 *
 * @author comart
 */
public class MavenREST {
    private static final int PAGE_SIZE=20;
    private static final Logger logger = Logger.getLogger(MavenREST.class.getSimpleName());
    private static final OkHttpClient client = new OkHttpClient();
    
    public static OkHttpClient getHttpClient() {
        return client;
    }
    
    private static <T> T restCall(String urlTemplate, Object param, Class<T> clazz) throws ParseException, IOException {
        String url = StrUtils.replaceWith(urlTemplate, param, "${", "}");
        Request req = new Request.Builder().url(url).build();
        try (Response response = client.newCall(req).execute()) {
            Gson gson = new Gson();
            return gson.fromJson(response.body().charStream(), clazz);
        }
    }
    
    private static MavenConfig mavenConfig() {
        return JDBGenConfig.getInstance().getMaven();
    }
    
    public static SearchResult search(String queryStr, int pageNo) throws ParseException, IOException {
        SearchParams query = new SearchParams();
        query.setQ(queryStr);
        query.setRows(""+PAGE_SIZE);
        query.setStart(""+(PAGE_SIZE*pageNo));
        MavenConfig mconf = mavenConfig();
        String searchUrl = mconf.getUrlBase() + mconf.getSearch();
        return restCall(searchUrl, query, SearchResult.class);
    }

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

    public static String downloadLink(SearchResponseItem qitem) throws ParseException, IOException {
        HashMap<String,Object> query = new HashMap<>();
        query.put("fpath", qitem.getFilePath());
        MavenConfig mconf = mavenConfig();
        String url = mconf.getUrlBase() + mconf.getDownload();
        return StrUtils.replaceWith(url, query, "${", "}");
    }
}

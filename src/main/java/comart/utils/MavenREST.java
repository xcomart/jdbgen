/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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

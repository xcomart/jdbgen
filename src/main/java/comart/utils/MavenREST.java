/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package comart.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

/**
 *
 * @author comart
 */
public class MavenREST {
    private static final Logger logger = Logger.getLogger(MavenREST.class.getSimpleName());
    private final static String SEARCH_URL="https://search.maven.org/solrsearch/select?q=${search}&rows=20&wt=json&start=${start}&fl=id,g,a,latestVersion";
    private final static String VERSION_URL="https://search.maven.org/solrsearch/select?q=g:${group}+AND+a:${artifact}&rows=20&wt=json&fl=id,g,a,v";
    private final static String DOWNLOAD_URL="https://search.maven.org/remotecontent?filepath=${filePath}";
    private static final OkHttpClient client = new OkHttpClient();
    
    public static class ResponseItem {
        public String id;
        public String group;
        public String artifact;
        public String version;
        public String latestVersion;
        public String repository;
        public int versionCount;
        public String getFilePath() {
            StringBuilder sb = new StringBuilder();
            sb.append(group.replace('.', '/'));
            sb.append('/').append(artifact);
            sb.append('/').append(version);
            sb.append('/').append(artifact);
            sb.append('-').append(version);
            sb.append(".jar");
            return sb.toString();
        }
    }
    public static class SearchResult {
        public int status;
        public int totalCount;
        public List<ResponseItem> items = new ArrayList<>();
    }
    
    private static SearchResult restCall(String urlTemplate, Object param) {
        try {
            String url = StrUtils.replaceWith(urlTemplate, param, "${", "}");
            Request req = new Request.Builder().url(url).build();
            try (Response response = client.newCall(req).execute()) {
                JSONObject jobj = new JSONObject(response.body().string());
            } catch(Exception e) {
                logger.log(Level.SEVERE, "cannot get url contents: " + e.getLocalizedMessage(), e);
                throw new RuntimeException(e.getLocalizedMessage(), e);
            }
        } catch(Exception e) {
            e.printStackTrace();
            
        }
        return null;
    }
    
    public static SearchResult search(String query) {
        Map<String, String> param = new HashMap<String, String>() {{
            put("query", query);
        }};
        return restCall(SEARCH_URL, param);
    }
}

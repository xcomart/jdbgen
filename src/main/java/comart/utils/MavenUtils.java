
package comart.utils;

import comart.tools.jdbgen.types.JDBGenConfig;
import comart.tools.jdbgen.types.maven.MavenConfig;
import comart.tools.jdbgen.types.maven.MavenConfigItem;
import comart.tools.jdbgen.types.maven.MavenSearchItem;
import comart.tools.jdbgen.types.maven.MavenTemplate;
import comart.tools.jdbgen.types.maven.MavenTemplateItem;
import comart.utils.tuple.Pair;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class MavenUtils {
    private static final Logger logger = Logger.getLogger(MavenUtils.class.getName());
    private static final OkHttpClient client = new OkHttpClient();
    
    public static OkHttpClient getHttpClient() {
        return client;
    }
    
    private static MavenConfig getMavenConfig() {
        return JDBGenConfig.getInstance().getMaven();
    }
    
    @SuppressWarnings("null")
    private static XNode getContents(String url) {
        Request req = new Request.Builder().url(url).build();
        try (Response response = client.newCall(req).execute()) {
            return XManager.htmlToXml(response.body().string());
        } catch(Exception e) {
            logger.log(Level.SEVERE, "cannot get url contents: " + e.getLocalizedMessage(), e);
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }
    
    @SuppressWarnings("null")
    private static XNode getNode(MavenTemplate template, XNode root) {
        boolean doRepeat = false;
        if (template.isMatch(root))
            return root;
        XNode res = null;
        if (!ObjectUtils.isEmpty(root.getChildren())) {
            for (XNode node:root.getChildren()) {
                if (node.isText()) continue;
                if (template.isMatch(node)) {
                    if (doRepeat) {
                        res.addChild(node);
                    } else {
                        doRepeat = template.isRepeat();
                        if (doRepeat) {
                            res = new XNode("root");
                            res.addChild(node);
                        } else {
                            return node;
                        }
                    }
                } else if (!doRepeat) {
                    res = getNode(template, node);
                    if (res != null) {
                        return res;
                    }
                }
            }
        }
        return res;
    }
    
    private static final Map<String, XNode> TLOC_CACHE = new HashMap<>();
    
    private static class LocParser {
        Stack<XNode> stack;
        String location;
        int pos;
        int len;
        static final String DELIMS = ",[]";
        
        LocParser(String location) {
            this.stack = new Stack<>();
            this.location = location;
            this.pos = 0;
            this.len = location.length();
        }
        
        String next() {
            if (pos < (len - 1)) {
                if (DELIMS.indexOf(location.charAt(pos)) > -1) {
                    return "" + location.charAt(pos++);
                } else {
                    StringBuilder sb = new StringBuilder();
                    for(; pos < (len - 1) && DELIMS.indexOf(location.charAt(pos)) == -1; pos++)
                        sb.append(location.charAt(pos));
                    return sb.toString();
                }
            } else {
                return null;
            }
        }
        
        XNode parse() {
            XNode root = new XNode("");
            XNode curr = root;
            String token;
            while ((token = next()) != null) {
                switch (token) {
                    case "[":
                        // does nothing
                        stack.push(curr);
                        break;
                    case ",":
                        // does nothing
                        break;
                    case "]":
                        curr = stack.pop();
                        break;
                    default:
                        curr = new XNode(token);
                        stack.top().addChild(curr);
                        break;
                }
            }
            return root;
        }
    }
    
    private static XNode parseLocation(String location) {
        return new LocParser(location).parse();
    }
    
    private synchronized static XNode getLocation(String location) {
        XNode res = TLOC_CACHE.get(location);
        if (res == null) {
            res = parseLocation(location);
            TLOC_CACHE.put(location, res);
        }
        return res;
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private static String getNodeValue(MavenTemplateItem titem, XNode node) {
        if (node != null) {
            String value;
            if ("attribute".equalsIgnoreCase(titem.getType())) {
                value = node.getAttribute(titem.getAttr());
            } else if ("text".equalsIgnoreCase(titem.getType())) {
                value = node.getChildren().stream().filter(c -> c.isText()).findFirst().get().getName();
            } else {
                throw new RuntimeException("Unknown template item type: "+titem.getType());
            }
            // replace multiple white spaces to one and trim.
            String v = value.replaceAll("\\s\\s+", " ").trim();
            String format = titem.getFormat();
            if (StringUtils.isNotBlank(format)) {
                String formatData = titem.getFormatData();
                String delim = titem.getDelimiter();
                try {
                    v = StrUtils.mapSub(format, v, formatData, delim);
                } catch(Exception e) {
                    throw new RuntimeException(
                            "StrUtils.mapSub() failed(format:" +
                                    format + ", value:" + v + ").", e);
                }
            }
            return v;
        } else {
            throw new RuntimeException("Html structure does not match with template.");
        }
    }
    
    @SuppressWarnings({"null", "UnusedAssignment"})
    private static List<String> getValueLocation(MavenTemplateItem titem, XNode node) {
        XNode loc = getLocation(titem.getLocation());
        List<XNode> srcs = node.getChildren();
        int srcIdx = 0;
        List<String> res = new ArrayList<>();
        do {
            XNode tloc = loc;
            XNode tnode = node;
            for (;srcIdx < srcs.size() && srcs.get(srcIdx).isText(); ) srcIdx++;
            if (srcIdx >= srcs.size()) break;
            List<XNode> tsrc = srcs.subList(srcIdx++, srcs.size());
            boolean doNext = false;
            while (ObjectUtils.isNotEmpty(tloc.getChildren())) {
                Iterator<XNode> locIter = tloc.getChildren().iterator();
                Iterator<XNode> nodeIter = tsrc.iterator();
                XNode curr = null;
                XNode locCurr = null;
                while (locIter.hasNext()) {
                    locCurr = locIter.next();
                    curr = null;
                    while (nodeIter.hasNext() && (curr = nodeIter.next()).isText())
                        curr = null;
                    if (curr == null) {
                        doNext = true;
                        break;
                    }
                    if (!locCurr.getName().equalsIgnoreCase(curr.getName())) {
                        doNext = true;
                        break;
                    }
                }
                if (doNext) break;
                tloc = locCurr;
                tnode = curr;
                tsrc = curr.getChildren();
            }
            if (!doNext)
                res.add(getNodeValue(titem, tnode));
        } while (titem.isRepeat());
        return res;
    }
    
    private static List<String> getvalueAttribute(MavenTemplateItem titem, XNode node) {
        Pair<String,String> pair = titem.getAttrPair();
        List<String> res = new ArrayList<>();
        if (pair == null) {
            throw new RuntimeException("location or attrPair must be defined in template item.");
        } else {
            node.traverse(n -> {
                String v = n.getAttribute(pair.getFirst());
                if (pair.getSecond().equals(v)) {
                    res.add(getNodeValue(titem, n));
                    if (!titem.isRepeat())
                        return false;
                }
                return true;
            });
        }
        return res;
    }
    
    private static List<String> getValue(MavenTemplateItem titem, XNode node) {
        if (StringUtils.isBlank(titem.getLocation())) {
            return getvalueAttribute(titem, node);
        } else {
            return getValueLocation(titem, node);
        }
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private static void setMapField(MavenTemplateItem titem, XNode node, MavenSearchItem target) {
        try {
            String value = getValue(titem, node).get(0);
            switch (titem.getName()) {
                case "image": target.setIcon(value); break;
                case "title": target.setName(value); break;
                case "groupId": target.setGroupId(value); break;
                case "artifactId": target.setArtifactId(value); break;
                case "description": target.setDescription(value); break;
                default:
                    throw new RuntimeException("Unknown search item name: "+titem.getName());
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "cannot set field " + titem, e);
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }
    
    public static List<MavenSearchItem> searchMaven(String query, int page) throws ParseException {
        MavenConfig mvnCfg = getMavenConfig();
        MavenConfigItem search = mvnCfg.getSearch();
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("query", query);
            put("page", page);
        }};
        XNode html = getContents(mvnCfg.getUrlBase() +
                StrUtils.replaceWith(search.getUrl(), map, "$J{", "}"));
        MavenTemplate template = search.getTemplates().stream()
                .filter(t -> "item".equals(t.getName())).findFirst().get();
        XNode found = getNode(template, html);
        ArrayList<MavenSearchItem> res = new ArrayList<>();
        found.getChildren().forEach(c -> {
            MavenSearchItem item = new MavenSearchItem();
            template.getItems().forEach(ti -> setMapField(ti, c, item));
            item.setIcon(mvnCfg.getUrlBase() + item.getIcon());
            res.add(item);
        });
        return res;
    }
    
    public static List<String> getRepositories(MavenSearchItem sitem) throws ParseException {
        MavenConfig mvnCfg = getMavenConfig();
        MavenConfigItem repo = mvnCfg.getRepository();
        XNode html = getContents(mvnCfg.getUrlBase() +
                StrUtils.replaceWith(repo.getUrl(), sitem, "$J{", "}"));
        MavenTemplate template = repo.getTemplates().get(0);
        XNode found = getNode(template, html);
        Set<String> set = new LinkedHashSet<>(getValue(template.getItems().get(0), found));
        return new ArrayList<>(set);
    }
    
    public static List<String> getVersions(MavenSearchItem sitem, String repo) throws ParseException {
        Map<String, Object> map = ObjUtils.objToMap(sitem);
        map.put("repository", repo.toLowerCase());
        MavenConfig mvnCfg = getMavenConfig();
        MavenConfigItem version = mvnCfg.getVersion();
        XNode html = getContents(mvnCfg.getUrlBase() +
                StrUtils.replaceWith(version.getUrl(), map, "$J{", "}"));
        MavenTemplate template = version.getTemplates().get(0);
        XNode found = getNode(template, html);
        Set<String> set = new LinkedHashSet<>(getValue(template.getItems().get(0), found));
        return new ArrayList<>(set);
    }
    
    public static String getDownLink(MavenSearchItem sitem, String version) throws ParseException {
        Map<String, Object> map = ObjUtils.objToMap(sitem);
        map.put("version", version);
        MavenConfig mvnCfg = getMavenConfig();
        MavenConfigItem down = mvnCfg.getDownload();
        XNode html = getContents(mvnCfg.getUrlBase() +
                StrUtils.replaceWith(down.getUrl(), map, "$J{", "}"));
        logger.info(html.toString());
        MavenTemplate template = down.getTemplates().get(0);
        XNode found = getNode(template, html);
        return getValue(template.getItems().get(0), found).get(0);
    }
//    private static final String MAVEN_URL_FORMAT = "https://search.maven.org/solrsearch/select?q=g:%%22%s%%22+AND+a:%%22%s%%22&core=gav&rows=20&wt=json";
//
//    private MavenUtils() {
//    }
//
//    public static Pair<String, String> downloadMaven(String groupId, String artifactId, String versionInclude) {
//        try {
//            URLConnection c = (new URL(String.format(MAVEN_URL_FORMAT, groupId, artifactId))).openConnection();
//            
//            JsonElement jobj;
//            try (InputStream is = c.getInputStream()) {
//                jobj = (JsonElement)(new Gson()).fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonElement.class);
//            }
//
//            JsonObject resp = jobj.getAsJsonObject().getAsJsonObject("response");
//            int numFound = resp.get("numFound").getAsInt();
//            if (numFound > 0) {
//                boolean found = false;
//
//                for(int i = 0; i < numFound && !found; ++i) {
//                    JsonObject item = resp.getAsJsonArray("docs").get(0).getAsJsonObject();
//                    String version = item.get("v").getAsString();
//                    if (ObjectUtils.isEmpty(versionInclude) || version.contains(versionInclude)) {
//                        ArtifactResolver ar = new ArtifactResolver();
//                        Artifact a = ar.artifactFor(groupId + ":" + artifactId + ":" + version);
//                        ResolvedArtifact ra = ar.resolveArtifact(a);
//                        if (ra != null) {
//                            FetchStatus r = ar.downloadArtifact(ra);
//                            if (r instanceof SUCCESSFUL) {
//                                return new Pair(version, ra.getMain().getLocalFile().toString());
//                            }
//                        }
//                        logger.severe("cannot download maven artifact");
//                    }
//                }
//            } else {
//                logger.severe("no matched result");
//            }
//        } catch (Exception e) {
//            logger.log(Level.SEVERE, (String)null, e);
//        }
//
//        return null;
//    }
}

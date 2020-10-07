
package comart.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.tools.maven.resolution.Artifact;
import com.squareup.tools.maven.resolution.ArtifactResolver;
import com.squareup.tools.maven.resolution.FetchStatus;
import com.squareup.tools.maven.resolution.ResolvedArtifact;
import com.squareup.tools.maven.resolution.FetchStatus.RepositoryFetchStatus.SUCCESSFUL;
import comart.utils.tuple.Pair;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ObjectUtils;

public class MavenUtils {
    private static final Logger logger = Logger.getLogger(MavenUtils.class.getName());
    private static final String MAVEN_URL_FORMAT = "https://search.maven.org/solrsearch/select?q=g:%%22%s%%22+AND+a:%%22%s%%22&core=gav&rows=20&wt=json";

    private MavenUtils() {
    }

    public static Pair<String, String> downloadMaven(String groupId, String artifactId, String versionInclude) {
        try {
            URLConnection c = (new URL(String.format(MAVEN_URL_FORMAT, groupId, artifactId))).openConnection();
            
            JsonElement jobj;
            try (InputStream is = c.getInputStream()) {
                jobj = (JsonElement)(new Gson()).fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonElement.class);
            }

            JsonObject resp = jobj.getAsJsonObject().getAsJsonObject("response");
            int numFound = resp.get("numFound").getAsInt();
            if (numFound > 0) {
                boolean found = false;

                for(int i = 0; i < numFound && !found; ++i) {
                    JsonObject item = resp.getAsJsonArray("docs").get(0).getAsJsonObject();
                    String version = item.get("v").getAsString();
                    if (ObjectUtils.isEmpty(versionInclude) || version.contains(versionInclude)) {
                        ArtifactResolver ar = new ArtifactResolver();
                        Artifact a = ar.artifactFor(groupId + ":" + artifactId + ":" + version);
                        ResolvedArtifact ra = ar.resolveArtifact(a);
                        if (ra != null) {
                            FetchStatus r = ar.downloadArtifact(ra);
                            if (r instanceof SUCCESSFUL) {
                                return new Pair(version, ra.getMain().getLocalFile().toString());
                            }
                        }
                        logger.severe("cannot download maven artifact");
                    }
                }
            } else {
                logger.severe("no matched result");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, (String)null, e);
        }

        return null;
    }
}

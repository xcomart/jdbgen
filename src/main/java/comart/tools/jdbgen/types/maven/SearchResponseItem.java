/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package comart.tools.jdbgen.types.maven;

import comart.tools.jdbgen.types.HasTitle;
import comart.utils.StrUtils;
import lombok.Data;

/**
 *
 * @author comart
 */
@Data
public class SearchResponseItem implements HasTitle {
    String id;
    String g;
    String a;
    String v;
    String latestVersion;
    String repositoryId;
    String p;
    String timestamp;
    int versionCount;
    String[] text;
    String[] ec;
    String[] tags;
    
    @Override
    public String getTitle() {
        if (StrUtils.isEmpty(latestVersion)) {
            return v;
        } else {
            return id;
        }
    }
    
    public String getToolTip() {
        StringBuilder sb = new StringBuilder();
        sb.append("Group ID: ").append(g).append("\n");
        sb.append("Artifact ID: ").append(a).append("\n");
        if (StrUtils.isEmpty(latestVersion)) {
            sb.append("Version: ").append(v);
        } else {
            sb.append("Latest Version: ").append(latestVersion);
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getTitle();
    }
    
    public String getFilePath() {
        StringBuilder sb = new StringBuilder();
        sb.append(g.replace('.', '/')).append('/');
        sb.append(a).append('/').append(v).append('/');
        sb.append(a).append('-').append(v).append(".jar");
        return sb.toString();
    }
}

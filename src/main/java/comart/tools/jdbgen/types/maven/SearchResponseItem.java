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
package comart.tools.jdbgen.types.maven;

import comart.tools.jdbgen.types.HasTitle;
import comart.utils.StrUtils;
import lombok.Data;

/**
 * One match of a Maven Central search: either an artifact, which carries its
 * latest version, or a single version of an artifact, which carries the version
 * itself. Which of the two it is decides how the item is labelled and described
 * in the driver download dialog.
 *
 * @author comart
 */
@Data
public class SearchResponseItem implements HasTitle {
    /** identifier of the match, group and artifact id and, for a version match, the version. */
    String id;
    /** group id of the artifact. */
    String g;
    /** artifact id. */
    String a;
    /** version of this match; only filled in when the match is a single version. */
    String v;
    /** latest version of the artifact; only filled in when the match is an artifact. */
    String latestVersion;
    /** identifier of the repository the artifact was found in. */
    String repositoryId;
    /** packaging of the artifact, for example <code>"jar"</code>. */
    String p;
    /** publication time, in milliseconds since the epoch. */
    String timestamp;
    /** number of versions the artifact has. */
    int versionCount;
    /** the indexed text fields the match was found in. */
    String[] text;
    /** the extensions and classifiers published for this artifact. */
    String[] ec;
    /** the tags of the artifact. */
    String[] tags;
    
    /**
     * label of this match in the search result list.
     *
     * @return the version when this is a version match, the identifier of the
     *         artifact otherwise.
     */
    @Override
    public String getTitle() {
        if (StrUtils.isEmpty(latestVersion)) {
            return v;
        } else {
            return id;
        }
    }
    
    /**
     * a multi line description of this match for the tooltip of the result
     * list.
     *
     * @return group id and artifact id, followed by the version of a version
     *         match or by the latest version of an artifact match.
     */
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
    
    /**
     * the title of this match, so that a plain list model renders it correctly.
     *
     * @return the value of {@link #getTitle()}.
     */
    @Override
    public String toString() {
        return getTitle();
    }
    
    /**
     * repository path of the jar of this match, built the way a Maven
     * repository lays artifacts out. Only meaningful on a version match, since
     * it is built from the version field.
     *
     * @return the group id with every <code>'.'</code> turned into a
     *         <code>'/'</code>, followed by artifact id, version and the jar
     *         file named <code>artifact-version.jar</code>.
     */
    public String getFilePath() {
        return g.replace('.', '/') + '/' +
                a + '/' + v + '/' +
                a + '-' + v + ".jar";
    }
}

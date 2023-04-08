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

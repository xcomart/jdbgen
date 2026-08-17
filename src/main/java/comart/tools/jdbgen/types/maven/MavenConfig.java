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

import lombok.Data;

/**
 * The endpoints of the Maven repository the JDBC driver jars are searched in
 * and downloaded from. Every path is appended to {@link #urlBase} to form the
 * request URL.
 *
 * @author comart
 */
@Data
public class MavenConfig {
    /** base URL of the repository, the other entries are appended to it. */
    private String urlBase;
    /** path of the artifact search endpoint. */
    private String search;
    /** path of the endpoint listing the versions of one artifact. */
    private String version;
    /** path of the jar download, with the artifact path as the <code>${fpath}</code> variable. */
    private String download;
}

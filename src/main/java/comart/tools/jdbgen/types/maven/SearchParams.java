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
 * The query parameters of a Maven Central search call. The fields are the Solr
 * parameters of the search service; the application only fills in the query, the
 * page bounds and the response format, and receives the rest back in the header
 * of the reply. On a request the fields are substituted into the URL template
 * of the configured endpoint, where they appear as <code>${...}</code>
 * variables named after the field.
 *
 * @author comart
 */
@Data
public class SearchParams {
    /** the search term. */
    String q;
    /** the search core, which selects what is searched, for example the version list of an artifact. */
    String core;
    /** the Solr query parser to use. */
    String defType;
    /** the fields the query terms are matched against. */
    String qf;
    /** whether the reply is indented. */
    String indent;
    /** whether spell checking is run for the query. */
    String spellcheck;
    /** the fields to return per match. */
    String fl;
    /** index of the first match to return. */
    String start;
    /** how many spell checking suggestions to return. */
    String spellcheck_count;
    /** the sort order of the matches. */
    String sort;
    /** how many matches to return, that is the page size. */
    String rows;
    /** the response format, always <code>"json"</code>. */
    String wt = "json";
    /** the response format version. */
    String version;
}

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
package comart.tools.jdbgen.template;

import comart.tools.jdbgen.types.JDBAbbr;
import comart.tools.jdbgen.types.JDBGenConfig;
import comart.utils.ObjUtils;
import comart.utils.StrUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Template engine of JDBGen. A template is parsed once by the constructor into
 * a tree of items, and {@link #applyMapper(java.lang.Object)} renders that tree
 * against a model object - typically a table or a column read from the database
 * metadata - as often as needed.
 * <p>
 * Everything outside of a <code>${...}</code> placeholder is copied verbatim.
 * The line separator of the parsed template (<code>\r\n</code> or
 * <code>\n</code>) is remembered and reused wherever the engine itself inserts
 * a line break; a template without any line break falls back to the line
 * separator of the platform.
 * <p>
 * A placeholder is written as <code>${type:name=value, ...}</code>. The
 * attribute list is a comma separated list of name/value pairs; a value may be
 * wrapped in <code>'</code>, <code>"</code> or <code>(...)</code> so that it
 * may contain commas, and <code>\n</code>, <code>\r</code>, <code>\t</code> and
 * <code>\x</code> escapes are recognized. Attribute names are case
 * insensitive. Three shorthands exist:
 * <ul>
 * <li><code>${'text'}</code> and <code>${"text"}</code> emit the quoted text
 * verbatim, which is how a literal <code>${</code> is written.</li>
 * <li><code>${user}</code>, <code>${date}</code> and <code>${author}</code> are
 * read as the matching type without any attribute.</li>
 * <li>anything else without a <code>:</code> is read as
 * <code>${item:key=...}</code>.</li>
 * </ul>
 * The supported types are:
 * <ul>
 * <li><code>item</code> - a value of the current model. The <code>key</code>
 * (or <code>item</code>) attribute names the member; when the model has no such
 * member the custom variables are consulted, and an unknown name renders as an
 * empty string with a warning in the log.</li>
 * <li><code>super</code> - the same, but resolved against the model of the
 * enclosing <code>for</code> loop.</li>
 * <li><code>for</code> - repeats its body, up to <code>${endfor}</code>, over
 * the collection named by <code>key</code>. <code>instr</code> is inserted
 * between the elements and re-indented by <code>indent</code> additional
 * spaces on every line break it contains, <code>skiplist</code> is a comma
 * separated list of element names to leave out, and every element gets its
 * one based position assigned to its <code>no</code> member.</li>
 * <li><code>if</code> - renders its body, up to <code>${endif}</code>, when
 * every condition holds; <code>${elif:...}</code> and <code>${else}</code>
 * are supported. The conditions are <code>equals</code>/<code>value</code>,
 * <code>notequals</code>, <code>contains</code>, <code>notcontains</code>,
 * <code>startswith</code>, <code>notstartswith</code>, <code>endswith</code>,
 * <code>notendswith</code>, <code>matches</code> and
 * <code>notmatches</code>. All of them compare case insensitively except the
 * regular expression of <code>matches</code>; <code>contains</code> accepts
 * either a collection member, whose elements are compared by their
 * <code>name</code>, or a comma separated list of alternatives.</li>
 * <li><code>date</code> - the current date, formatted by
 * <code>SimpleDateFormat</code>. The <code>format</code> attribute may be
 * given directly as in <code>${date:yyyy-MM}</code> and defaults to
 * <code>yyyy-MM-dd</code>.</li>
 * <li><code>user</code> - the login id of the user running the application.</li>
 * <li><code>author</code> - the <code>author</code> custom variable.</li>
 * </ul>
 * The key of an <code>item</code>, <code>super</code> or <code>if</code> may be
 * followed by a chain of dot separated processors, as in
 * <code>${name.suffix.camel}</code>, each of which rewrites the value handed to
 * it: <code>prefix</code>, <code>suffix</code>, <code>camel</code>,
 * <code>pascal</code>, <code>snake</code>, <code>screaming</code>,
 * <code>skewer</code>/<code>kebab</code>, <code>lower</code>,
 * <code>upper</code>, <code>replace(find, replacement)</code> and
 * <code>abbr</code>. When the abbreviation option of the configuration is
 * turned on, <code>abbr</code> is inserted automatically behind a
 * <code>name</code> key.
 * <p>
 * Every rendered value finally passes the same set of optional decorations:
 * <code>prepend</code> and <code>postpend</code> (or <code>quote</code> for
 * both at once) surround it, and <code>padsize</code> together with
 * <code>paddir</code> pads it to a fixed width - counted in EUC-KR bytes, so
 * that double byte characters line up in a fixed width font.
 *
 * @author comart
 */
@Slf4j
public class TemplateManager {
    /** login id of the user running the application, rendered by <code>${user}</code>. */
    private static final String USER_ID = ObjUtils.getLoginUserId();
    /** format used by <code>${date}</code> when the template gives none. */
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    
    /**
     * parser of one placeholder type. The implementations are registered in
     * {@link #handlers} under the name that introduces the placeholder.
     */
    private interface TemplateHandler {
        /**
         * turn the attribute list of a placeholder into a template item.
         *
         * @param extra
         *            everything behind the <code>:</code> of the placeholder.
         * @param ctx
         *            parse position, which a block type keeps reading from
         *            until it meets its closing keyword.
         * @return the parsed item.
         * @throws ParseException on invalid syntax.
         */
        TemplateItem process(String extra, ParseContext ctx) throws ParseException;
    }
    
    /**
     * renderer of one template item type. The implementations are registered
     * in {@link #appenders} per {@link TemplateType}.
     */
    private interface TemplateAppender {
        /**
         * render one item into the output.
         *
         * @param sb
         *            output built so far, appended to in place.
         * @param template
         *            the item to render.
         * @param mapper
         *            model the values are read from.
         * @param supr
         *            model of the enclosing <code>for</code> loop, or
         *            <code>null</code> at the top level.
         * @throws Exception whatever reading the model or rendering the item
         *         fails with.
         */
        void append(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception;
    }
    
    /**
     * one string processor of a key chain such as
     * <code>${name.suffix.camel}</code>. The implementations are registered in
     * {@link #procs} under the name used in the template.
     */
    private interface ItemProcHandler {
        /**
         * rewrite the value handed down the chain.
         *
         * @param item
         *            value produced by the preceding step.
         * @param params
         *            arguments written in parentheses behind the processor
         *            name, empty when there were none.
         * @return the rewritten value.
         */
        String process(String item, List<Object> params);
    }
    
    /**
     * one condition of an <code>if</code> placeholder. The implementations are
     * registered in {@link #ifconds} under the attribute name used in the
     * template.
     */
    private interface IfCondHandler {
        /**
         * evaluate the condition.
         *
         * @param key
         *            key chain of the <code>if</code>, resolved against the
         *            model.
         * @param condVal
         *            value the attribute was given in the template.
         * @param mapper
         *            model the value is read from.
         * @param customs
         *            custom variables, consulted when the model has no such
         *            member.
         * @return <code>true</code> when the condition holds.
         * @throws Exception whatever resolving the key fails with.
         */
        boolean check(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception;
    }
    
    /**
     * kind of a parsed template item. <code>TEXT</code> is the literal text
     * between the placeholders, the others match the placeholder types.
     */
    private enum TemplateType {
        TEXT, ITEM, SUPER, FOR, IF, USER, DATE, AUTHOR
    }
    
    /**
     * one node of the parsed template. Depending on {@link #type} the content
     * is the literal text (<code>TEXT</code>) or the attribute map of the
     * placeholder, which for a block type also carries the parsed body.
     */
    private static class TemplateItem {
        /** kind of this item. */
        TemplateType type;
        /** literal text or attribute map, depending on {@link #type}. */
        Object cont;
        
        /**
         * @param type
         *            kind of this item.
         * @param cont
         *            literal text or attribute map, depending on the type.
         */
        public TemplateItem(TemplateType type, Object cont) {
            this.type = type;
            this.cont = cont;
        }
    }
    
    /**
     * cursor over the template text. It carries the current offset and the
     * line number, which is reported with a {@link ParseException} so that a
     * broken template can be located.
     */
    private static class ParseContext {
        /** cursor offset, length of the template and the current line number. */
        int curr, len, line;
        /** the template text being parsed. */
        String template;
        /**
         * @param template
         *            the template text to walk over.
         */
        public ParseContext(String template) {
            this.template = template;
            line = curr = 0;
            len = template.length();
        }
        
        /**
         * jump to <code>end</code>, counting the line breaks that are skipped
         * over on the way.
         *
         * @param end
         *            offset to move the cursor to.
         */
        public void updateLineCount(int end) {
            String text = template.substring(curr, end);
            line += text.split("\n").length - 1;
            curr = end;
        }

        /**
         * read the character under the cursor and step over it.
         *
         * @return the character, or -1 at the end of the template.
         */
        public int nextChar() {
            if (curr < len) {
                int res = template.charAt(curr++);
                if (res == '\n')
                    line++;
                return res;
            } else {
                return -1;
            }
        }

        /**
         * step over up to <code>count</code> characters, stopping at the end
         * of the template.
         *
         * @param count
         *            number of characters to skip.
         */
        public void moveTo(int count) {
            for (int i=0; i< count; i++) {
                if (nextChar() < 0)
                    break;
            }
        }

        /** step over the white space under the cursor, if any. */
        public void skipSpace() {
            int c;
            while ((c = nextChar()) > -1) {
                if (!StrUtils.isSpace(c)) {
                    curr--;
                    break;
                }
            }
        }

        /**
         * look at the character under the cursor without stepping over it.
         *
         * @return the character, or -1 at the end of the template.
         */
        public int peek() {
            if (curr < len) {
                return template.charAt(curr);
            } else {
                return -1;
            }
        }
        
        /**
         * the text right behind the cursor, used to point the user at the
         * place a parse error was found.
         *
         * @return up to 100 characters of the template, followed by
         *         <code>...</code> when it was cut off.
         */
        public String near() {
            int length = 100;
            if (curr + length < len)
                return template.substring(curr, curr+length) + "...";
            else
                return template.substring(curr);
        }
    }
    
    
    /**
     * advance to the next placeholder, appending the literal text passed on
     * the way to <code>items</code>. A quoted placeholder -
     * <code>${'text'}</code> or <code>${"text"}</code> - is a literal itself:
     * its unescaped content is appended as text and the search continues.
     *
     * @param ctx
     *            parse position.
     * @param items
     *            list the literal text is appended to.
     * @return the body of the next placeholder, without the surrounding
     *         <code>${</code> and <code>}</code>, or <code>null</code> when
     *         the end of the template was reached.
     * @throws ParseException when a placeholder is not closed by
     *         <code>}</code>.
     */
    private static String next(ParseContext ctx, ArrayList<TemplateItem> items) throws ParseException {
        if (ctx.curr == ctx.len)
            return null;
        
        int sp = ctx.template.indexOf("${", ctx.curr);
        
        if (sp < 0)
            sp = ctx.len;
        
        // add previous text
        if (sp > ctx.curr) {
            String pretext = ctx.template.substring(ctx.curr, sp);
            items.add(new TemplateItem(TemplateType.TEXT, pretext));
        }

        if (sp+1 < ctx.len)
            sp += 2;    // skip "${"
        
        ctx.updateLineCount(sp);
        if (sp == ctx.len) {
            return null;
        } else {
            ctx.skipSpace();
            int c = ctx.peek();
            StringBuilder sb = new StringBuilder();
            // check escaping string
            if (c == '"' || c == '\'') {
                int openChar = ctx.nextChar();
                boolean isEscape = false;
                while ((c = ctx.nextChar()) > -1) {
                    if (!isEscape) {
                        if (c == '\\') {
                            // the escape character itself is not part of the literal
                            isEscape = true;
                            continue;
                        } else if (c == openChar) {
                            break;
                        }
                    } else {
                        isEscape = false;
                    }
                    sb.append((char)c);
                }
                sp = ctx.curr;
            }
            int lst = ctx.template.indexOf("}", sp);
            if (lst < 0) {
                throw new ParseException("'}' not found, before: "+ctx.near(), ctx.line);
            }
            String res = sb.length() == 0 ? StrUtils.trim(ctx.template.substring(sp, lst)):sb.toString();
            ctx.updateLineCount(lst);
            ctx.nextChar(); // skip '}'
            if (sb.length() > 0) {
                items.add(new TemplateItem(TemplateType.TEXT, res));
                // current one is text escape template, so find next one.
                return next(ctx, items);
            } else {
                return res;
            }
        }
    }

    /**
     * split the attribute list of a placeholder into name/value pairs. Values
     * may be wrapped in <code>'</code>, <code>"</code> or <code>(...)</code>,
     * in which case the delimiters stay part of the value and commas inside do
     * not separate; <code>\n</code>, <code>\r</code> and <code>\t</code> are
     * translated and any other escaped character stands for itself. Names are
     * lower cased.
     *
     * @param ctx
     *            parse position, only used to describe where an error was
     *            found.
     * @param data
     *            the attribute list.
     * @return the pairs, empty when <code>data</code> holds none.
     * @throws ParseException on a dangling escape character or a pair without
     *         a name.
     */
    private static Map<String,Object> parseNVPairs(ParseContext ctx, String data) throws ParseException {
        int idx = 0;
        int openChar = -1;
        Map<String,Object> map = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        String name = "";
        String value = "";
        while (idx < data.length()) {
            char c = data.charAt(idx);
            if (c == '\\') {
                idx++;
                if (idx >= data.length())
                    throw new ParseException("Dangling escape character at end of: "+
                            data+". invalid syntax before: "+ctx.near(), idx);
                c = data.charAt(idx);
                switch(c) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default : sb.append(c);
                }
            } else {
                if (openChar < 0 && (c == '\"' || c == '\'' || c =='(')) {
                    openChar = c == '('? ')':c;
                    sb.append(c);
                } else if (c == openChar) {
                    sb.append(c);
                    openChar = -1;
                } else if (openChar < 0 && c == '=') {
                    if (!StrUtils.isEmpty(name))
                        throw new ParseException("Name value pair not matched: "+
                                data+". invalid syntax before: "+ctx.near(), idx);
                    name = StrUtils.trim(sb.toString());
                    sb = new StringBuilder();
                } else if (openChar < 0 && c == ',') {
                    if (!StrUtils.isEmpty(value))
                        throw new ParseException("Name value pair not matched: "+
                                data+". invalid syntax before: "+ctx.near(), idx);
                    value = StrUtils.trim(sb.toString());
                    if (StrUtils.isEmpty(name))
                        throw new ParseException("Name value pair not matched: "+
                                data+". invalid syntax before: "+ctx.near(), idx);
                    map.put(name.toLowerCase(), value);
                    name = "";
                    value = "";
                    sb = new StringBuilder();
                } else {
                    sb.append(c);
                }
            }
            idx++;
        }
        if (!StrUtils.isEmpty(value))
            throw new ParseException("Name value pair not matched: "+
                    data+". invalid syntax before: "+ctx.near(), idx);
        value = StrUtils.trim(sb.toString());
        if (!value.isEmpty()) {
            if (StrUtils.isEmpty(name)) {
                throw new ParseException("Name value pair not matched: "+
                        data+". invalid syntax before: "+ctx.near(), idx);
            }
            map.put(name.toLowerCase(), value);
        }
        return map;
    }
    
    /**
     * parse one placeholder body and hand it to the handler of its type. The
     * shorthands are expanded first: <code>user</code>, <code>date</code> and
     * <code>author</code> gain their empty attribute list, and a body without
     * a <code>:</code> becomes <code>item:key=...</code>.
     *
     * @param itemString
     *            body of the placeholder as returned by
     *            {@link #next(ParseContext, ArrayList)}.
     * @param ctx
     *            parse position, from which a block type reads its body.
     * @return the parsed item.
     * @throws ParseException when the type is unknown, or from the handler.
     */
    private static TemplateItem parseOne(String itemString, ParseContext ctx) throws ParseException {
        if (StrUtils.contains(new String[]{"user", "date", "author"},
                itemString.toLowerCase())) {
            itemString += ":";
        } else if (itemString.indexOf(':') < 0) {
            itemString = "item:key=" + itemString;
        }
        int idx = itemString.indexOf(':');
        String type = StrUtils.trim(itemString.substring(0, idx)).toLowerCase();
        String typeOptions = itemString.substring(idx+1);
        TemplateHandler handler = handlers.get(type);
        if (handler == null) {
            throw new ParseException("Unknown template: "+itemString+", before: "+ctx.near(), ctx.line);
        }
        return handler.process(typeOptions, ctx);
    }
    
    /**
     * parse a whole template into its items.
     *
     * @param ctx
     *            parse position, positioned at the start of the template.
     * @return the parsed items in template order.
     * @throws ParseException on invalid syntax anywhere in the template.
     */
    private static List<TemplateItem> parseTemplate(ParseContext ctx) throws ParseException {
        ArrayList<TemplateItem> res = new ArrayList<>();
        while (true) {
            String itemString = next(ctx, res);
            if (itemString == null)
                break;
            res.add(parseOne(itemString, ctx));
        }
        return res;
    }

    /**
     * handler of <code>${item:...}</code>, a value of the current model.
     *
     * @param extra
     *            attribute list of the placeholder.
     * @param ctx
     *            parse position.
     * @return the parsed item.
     * @throws ParseException on an invalid attribute list.
     */
    @SuppressWarnings("unused")
    private static TemplateItem parseItem(String extra, ParseContext ctx) throws ParseException {
        return new TemplateItem(TemplateType.ITEM, parseNVPairs(ctx, extra));
    }

    /**
     * handler of <code>${super:...}</code>, a value of the model enclosing the
     * current <code>for</code> loop.
     *
     * @param extra
     *            attribute list of the placeholder.
     * @param ctx
     *            parse position.
     * @return the parsed item.
     * @throws ParseException on an invalid attribute list.
     */
    @SuppressWarnings("unused")
    private static TemplateItem parseSuper(String extra, ParseContext ctx) throws ParseException {
        return new TemplateItem(TemplateType.SUPER, parseNVPairs(ctx, extra));
    }
    
    /**
     * make sure an <code>if</code> carries nothing but <code>key</code>,
     * <code>item</code> and known conditions, so that a misspelled condition
     * is reported instead of silently holding.
     *
     * @param pairs
     *            attributes of the <code>if</code>.
     * @param extra
     *            the attribute list as it was written, for the message.
     * @param ctx
     *            parse position, for the message.
     * @throws ParseException when an attribute is neither a key nor a known
     *         condition.
     */
    private static void checkIfConditions(Map<String, Object> pairs, String extra, ParseContext ctx) throws ParseException {
        Set<String> available = new HashSet<String>() {{
            add("key");
            add("item");
        }};
        available.addAll(ifconds.keySet());
        for (String key: pairs.keySet()) {
            if (!available.contains(key))
                throw new ParseException("Unknown if condition: "+extra+", before: "+ctx.near(), ctx.line);
        }
    }
    
    /**
     * handler of <code>${if:...}</code>. It reads the body up to the matching
     * <code>${endif}</code>, storing the items of the true branch under
     * <code>true</code> and those of the false branch under <code>false</code>
     * in the attribute map. Every <code>${elif:...}</code> becomes a nested
     * <code>if</code> in the false branch of the preceding one.
     *
     * @param extra
     *            attribute list of the placeholder.
     * @param ctx
     *            parse position, read on until the body is closed.
     * @return the parsed item, carrying the whole statement.
     * @throws ParseException on an invalid attribute list, an unknown
     *         condition, or a statement that is never closed.
     */
    private static TemplateItem parseIf(String extra, ParseContext ctx) throws ParseException {
        // this codes makes like below
        //
        // if
        //   true statement
        // elif
        //   true statement
        // ...
        // else
        //   false statement
        //
        // will be processed to below
        //
        // if
        //   true statements
        // else
        //   if
        //     true statements
        //   ...
        //   else
        //     false statements
        Map<String, Object> pairs = parseNVPairs(ctx, extra);
        checkIfConditions(pairs, extra, ctx);
        TemplateItem res = new TemplateItem(TemplateType.IF, pairs);
        ArrayList<TemplateItem> items = new ArrayList<>();
        pairs.put("true", items); // set to true for future statements
        while (true) {
            String itemString = next(ctx, items);
            if (itemString == null)
                throw new ParseException("if statements not closed, before: "+ctx.near(), ctx.line);
            if (itemString.startsWith("elif:")) {
                extra = StrUtils.trim(itemString.substring(5));
                Map<String, Object> npairs = parseNVPairs(ctx, extra);
                checkIfConditions(npairs, extra, ctx);
                TemplateItem curr = new TemplateItem(TemplateType.IF, npairs);
                items = new ArrayList<>();
                npairs.put("true", items); // set to true for future statements
                pairs.put("false", curr); // new if statement for false
                pairs = npairs; // replace pairs to npairs
            } else if ("else".equals(itemString)) {
                items = new ArrayList<>();
                pairs.put("false", items); // set to false for future statements
            } else if ("endif".equals(itemString)) {
                // reached end of if statement.
                break;
            } else {
                items.add(parseOne(itemString, ctx));
            }
        }
        return res;
    }
    
    /**
     * handler of <code>${for:...}</code>. It reads the body up to the matching
     * <code>${endfor}</code> and stores it under <code>items</code> in the
     * attribute map.
     *
     * @param extra
     *            attribute list of the placeholder.
     * @param ctx
     *            parse position, read on until the body is closed.
     * @return the parsed item, carrying the whole loop.
     * @throws ParseException on an invalid attribute list, or a loop that is
     *         never closed.
     */
    private static TemplateItem parseFor(String extra, ParseContext ctx) throws ParseException {
        Map<String, Object> pairs = parseNVPairs(ctx, extra);
        TemplateItem res = new TemplateItem(TemplateType.FOR, pairs);
        ArrayList<TemplateItem> items = new ArrayList<>();
        pairs.put("items", items); // set to true for future statements
        while (true) {
            String itemString = next(ctx, items);
            if (itemString == null)
                throw new ParseException("for statements not closed. before: " + ctx.near(), ctx.line);
            if ("endfor".equals(itemString)) {
                // reached end of for statement.
                break;
            } else {
                items.add(parseOne(itemString, ctx));
            }
        }
        return res;
    }
    
    /**
     * handler of <code>${date:...}</code>. An attribute list without any
     * <code>=</code> is the format itself, so that <code>${date:yyyy-MM}</code>
     * works as well as <code>${date:format=yyyy-MM}</code>.
     *
     * @param extra
     *            attribute list of the placeholder, or a bare date format.
     * @param ctx
     *            parse position.
     * @return the parsed item.
     * @throws ParseException on an invalid attribute list.
     */
    @SuppressWarnings("unused")
    private static TemplateItem parseDate(String extra, ParseContext ctx) throws ParseException {
        if (!extra.contains("="))
            extra = "format="+extra;
        return new TemplateItem(TemplateType.DATE, parseNVPairs(ctx, extra));
    }
    
    /**
     * handler of <code>${user:...}</code>, the login id of the current user.
     *
     * @param extra
     *            attribute list of the placeholder.
     * @param ctx
     *            parse position.
     * @return the parsed item.
     * @throws ParseException on an invalid attribute list.
     */
    @SuppressWarnings("unused")
    private static TemplateItem parseUser(String extra, ParseContext ctx) throws ParseException {
        return new TemplateItem(TemplateType.USER, parseNVPairs(ctx, extra));
    }
    
    /**
     * handler of <code>${author:...}</code>, the <code>author</code> custom
     * variable.
     *
     * @param extra
     *            attribute list of the placeholder.
     * @param ctx
     *            parse position.
     * @return the parsed item.
     * @throws ParseException on an invalid attribute list.
     */
    @SuppressWarnings("unused")
    private static TemplateItem parseAuthor(String extra, ParseContext ctx) throws ParseException {
        return new TemplateItem(TemplateType.AUTHOR, parseNVPairs(ctx, extra));
    }
    
    /**
     * <code>prefix</code> processor: everything before the last
     * <code>_</code>, or the value itself when it holds none.
     *
     * @param item
     *            value to rewrite.
     * @param params
     *            unused.
     * @return the prefix of the value.
     */
    @SuppressWarnings("unused")
    private static String procPrefix(String item, List<Object> params) {
        int idx = item.lastIndexOf("_");
        if (idx > -1) {
            return item.substring(0, idx);
        } else{
            return item;
        }
    }
    
    /**
     * <code>suffix</code> processor: everything behind the first
     * <code>_</code>, or the value itself when it holds none.
     *
     * @param item
     *            value to rewrite.
     * @param params
     *            unused.
     * @return the suffix of the value.
     */
    @SuppressWarnings("unused")
    private static String procSuffix(String item, List<Object> params) {
        int idx = item.indexOf("_");
        if (idx > -1) {
            return item.substring(idx+1);
        } else{
            return item;
        }
    }
    
    /**
     * <code>camel</code> processor: the value in <code>camelCase</code>.
     *
     * @param item
     *            value to rewrite.
     * @param params
     *            unused.
     * @return the converted value.
     */
    @SuppressWarnings("unused")
    private static String procCamel(String item, List<Object> params) {
        return StrUtils.toCamelCase(item);
    }
    
    /**
     * <code>pascal</code> processor: the value in <code>PascalCase</code>.
     *
     * @param item
     *            value to rewrite.
     * @param params
     *            unused.
     * @return the converted value.
     */
    @SuppressWarnings("unused")
    private static String procPascal(String item, List<Object> params) {
        return StrUtils.toPascalCase(item);
    }
    
    /**
     * <code>snake</code> processor: the value in <code>snake_case</code>.
     *
     * @param item
     *            value to rewrite.
     * @param params
     *            unused.
     * @return the converted value.
     */
    @SuppressWarnings("unused")
    private static String procSnake(String item, List<Object> params) {
        return StrUtils.toSnakeCase(item);
    }
    
    /**
     * <code>screaming</code> processor: the value in
     * <code>SCREAMING_SNAKE_CASE</code>.
     *
     * @param item
     *            value to rewrite.
     * @param params
     *            unused.
     * @return the converted value.
     */
    @SuppressWarnings("unused")
    private static String procScreaming(String item, List<Object> params) {
        return StrUtils.toScreamingSnakeCase(item);
    }
    
    /**
     * <code>skewer</code>/<code>kebab</code> processor: the value in
     * <code>skewer-case</code>.
     *
     * @param item
     *            value to rewrite.
     * @param params
     *            unused.
     * @return the converted value.
     */
    @SuppressWarnings("unused")
    private static String procSkewer(String item, List<Object> params) {
        return StrUtils.toSkewerCase(item);
    }
    
    /**
     * <code>lower</code> processor: the value in lower case.
     *
     * @param item
     *            value to rewrite.
     * @param params
     *            unused.
     * @return the converted value.
     */
    @SuppressWarnings("unused")
    private static String procLower(String item, List<Object> params) {
        return item.toLowerCase();
    }
    
    /**
     * <code>upper</code> processor: the value in upper case.
     *
     * @param item
     *            value to rewrite.
     * @param params
     *            unused.
     * @return the converted value.
     */
    @SuppressWarnings("unused")
    private static String procUpper(String item, List<Object> params) {
        return item.toUpperCase();
    }
    
    /**
     * <code>replace</code> processor: replace every occurrence of the first
     * argument with the second one.
     *
     * @param item
     *            value to rewrite.
     * @param params
     *            the text to find and its replacement.
     * @return the rewritten value.
     * @throws RuntimeException when fewer than two arguments were given.
     */
    private static String procReplace(String item, List<Object> params) {
        if (params.size() < 2)
            throw new RuntimeException(
                    "'replace' processor requires 2 arguments - replace(find, replacement), but got "+
                    params.size()+": "+params);
        String find = params.get(0).toString();
        String repl = params.get(1).toString();
        return StrUtils.replace(item, find, repl);
    }
    
    /**
     * <code>abbr</code> processor: apply the abbreviations configured by the
     * user. A whole name that is registered as an abbreviation is replaced at
     * once; otherwise the value is split on <code>_</code> and <code>-</code>
     * and every word is looked up on its own, keeping the separators.
     *
     * @param item
     *            value to rewrite.
     * @param params
     *            unused.
     * @return the abbreviated value.
     */
    @SuppressWarnings("unused")
    private static String procAbbr(String item, List<Object> params) {
        if (JDBAbbr.abbrNameMap.containsKey(item.toLowerCase())) {
            return JDBAbbr.abbrNameMap.get(item.toLowerCase());
        } else {
            // add last underbar for convenience
            item = item + "_";
            StringBuilder res = new StringBuilder();
            StringBuilder buf = new StringBuilder();
            for (char c: item.toCharArray()) {
                if (c == '_' || c == '-') {
                    String word = buf.toString();
                    if (JDBAbbr.abbrMap.containsKey(word))
                        word = JDBAbbr.abbrMap.get(word);
                    res.append(word);
                    res.append(c);
                    buf = new StringBuilder();
                } else {
                    buf.append(c);
                }
            }
            // remove last underbar
            res.deleteCharAt(res.length()-1);
            return res.toString();
        }
    }
    
    /** placeholder parsers by the type name used in the template. */
    private static final Map<String, TemplateHandler> handlers = new HashMap<>();
    /** string processors by the name used in a key chain. */
    private static final Map<String, ItemProcHandler> procs = new HashMap<>();
    /** conditions of an <code>if</code> by their attribute name. */
    private static final Map<String, IfCondHandler> ifconds = new HashMap<>();
    // registry of everything the template syntax offers: the placeholder
    // types, the string processors of a key chain and the conditions of an
    // 'if' statement. A name is only valid in a template once it appears here.
    static {
        handlers.put("item"  , TemplateManager::parseItem  );
        handlers.put("super" , TemplateManager::parseSuper );
        handlers.put("if"    , TemplateManager::parseIf    );
        handlers.put("for"   , TemplateManager::parseFor   );
        handlers.put("date"  , TemplateManager::parseDate  );
        handlers.put("user"  , TemplateManager::parseUser  );
        handlers.put("author", TemplateManager::parseAuthor);
        
        procs.put("prefix"   , TemplateManager::procPrefix   );
        procs.put("suffix"   , TemplateManager::procSuffix   );
        procs.put("camel"    , TemplateManager::procCamel    );
        procs.put("pascal"   , TemplateManager::procPascal   );
        procs.put("snake"    , TemplateManager::procSnake    );
        procs.put("screaming", TemplateManager::procScreaming);
        procs.put("skewer"   , TemplateManager::procSkewer   );
        procs.put("kebab"    , TemplateManager::procSkewer   );
        procs.put("lower"    , TemplateManager::procLower    );
        procs.put("upper"    , TemplateManager::procUpper    );
        procs.put("replace"  , TemplateManager::procReplace  );
        procs.put("abbr"     , TemplateManager::procAbbr     );
        
        ifconds.put("equals", TemplateManager::condEquals);
        ifconds.put("value", TemplateManager::condEquals);
        ifconds.put("notequals", TemplateManager::condNotEquals);
        ifconds.put("contains", TemplateManager::condContains);
        ifconds.put("notcontains", TemplateManager::condNotContains);
        ifconds.put("startswith", TemplateManager::condStartsWith);
        ifconds.put("notstartswith", TemplateManager::condNotStartsWith);
        ifconds.put("endswith", TemplateManager::condEndsWith);
        ifconds.put("notendswith", TemplateManager::condNotEndsWith);
        ifconds.put("matches", TemplateManager::condMatches);
        ifconds.put("notmatches", TemplateManager::condNotMatches);
    }
    
    /** the parsed template. */
    private final List<TemplateItem> items;
    /** line separator of the parsed template, reused on every line break the engine inserts. */
    private String lineEnd = System.lineSeparator();
    /** renderers by item type. */
    private final Map<TemplateType, TemplateAppender> appenders = new HashMap<>();
    
    /** custom variables, consulted whenever the model has no matching member. */
    private Map<String, String> customs = null;
    
    /**
     * parse a template. The template is parsed once here and may then be
     * applied to any number of models.
     *
     * @param template
     *            the template text.
     * @param customs
     *            custom variables, which back every key the model does not
     *            answer and hold the <code>author</code> of
     *            <code>${author}</code>.
     * @throws ParseException on invalid template syntax, with the line number
     *         and the surrounding text of the offending place.
     */
    public TemplateManager(String template, Map<String, String> customs) throws ParseException {
        appenders.put(TemplateType.TEXT  , this::appendText  );
        appenders.put(TemplateType.ITEM  , this::appendItem  );
        appenders.put(TemplateType.SUPER , this::appendSuper );
        appenders.put(TemplateType.IF    , this::appendIf    );
        appenders.put(TemplateType.FOR   , this::appendFor   );
        appenders.put(TemplateType.DATE  , this::appendDate  );
        appenders.put(TemplateType.USER  , this::appendUser  );
        appenders.put(TemplateType.AUTHOR, this::appendAuthor);

        // preserve line end with source
        int idx = template.indexOf("\n");
        if (idx >= 0) {
            if (idx > 0 && template.charAt(idx - 1) == '\r')
                lineEnd = "\r\n";
            else
                lineEnd = "\n";
        }
        this.customs = customs;
        items = parseTemplate(new ParseContext(template));
    }
    
    /**
     * append a rendered value with the decorations of its placeholder:
     * <code>prepend</code> and <code>postpend</code> - both defaulting to
     * <code>quote</code> - surround the value, and <code>padsize</code>
     * together with <code>paddir</code> pad it to a fixed width. The width is
     * counted in EUC-KR bytes so that double byte characters take two columns.
     * A <code>null</code> value appends nothing at all.
     *
     * @param sb
     *            output built so far.
     * @param map
     *            attributes of the placeholder.
     * @param val
     *            the value to append, may be <code>null</code>.
     * @throws Exception when <code>padsize</code> is not a number, or the
     *         EUC-KR encoding is unavailable.
     */
    private void appendBase(StringBuilder sb, Map<String,Object> map, Object val) throws Exception {
        String spadsz = (String)map.get("padsize");
        int padsz = spadsz == null? 0:Integer.parseInt(spadsz);
        String spaddr = (String)map.get("paddir");
        boolean padLeft = "left".equalsIgnoreCase(spaddr);
        String quote = (String)map.get("quote");
        String qpre = (String)map.get("prepend");
        String qpos = (String)map.get("postpend");
        if (qpre == null) qpre = quote;
        if (qpos == null) qpos = quote;
        if (val != null) {
            String valstr = String.valueOf(val);
            if (qpre != null)
                valstr = qpre + valstr;
            if (qpos != null)
                valstr = valstr + qpos;
            if (!padLeft)
                sb.append(valstr);
            if (padsz > 0) {
                int vsize = padsz - valstr.getBytes("EUC-KR").length;
                if (vsize < 0) vsize = 0;
                sb.append(StrUtils.space(vsize, ' '));
            }
            if (padLeft)
                sb.append(valstr);
        }
    }
    
    /**
     * the key chain of a placeholder, written either as <code>key</code> or as
     * <code>item</code>.
     *
     * @param props
     *            attributes of the placeholder.
     * @return the key chain.
     * @throws ParseException when neither attribute is present.
     */
    private String getKey(Map<String,Object> props) throws ParseException {
        String mkey = (String)props.get("key");
        if (mkey == null) mkey = (String)props.get("item");
        if (mkey == null)
            throw new ParseException(
                    "'key' or 'item' is required, but none given in: "+props.keySet(), 0);
        return mkey;
    }
    
    /**
     * renderer of literal text, which is copied as it is.
     *
     * @param sb
     *            output built so far.
     * @param template
     *            the text item.
     * @param mapper
     *            unused.
     * @param supr
     *            unused.
     * @throws Exception never thrown, the signature comes from
     *         {@link TemplateAppender}.
     */
    @SuppressWarnings("unused")
    private void appendText(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        sb.append(template.cont.toString());
    }
    
    /**
     * one step of a key chain: the member name of the first step, and the name
     * of a string processor together with its arguments in every following
     * step.
     */
    private static class ItemKey {
        /** member name in the first step, processor name in the later ones. */
        String key = null;
        /** arguments written in parentheses behind a processor name. */
        List<Object> params = new ArrayList<>();
        /** a step whose name is filled in while it is being parsed. */
        public ItemKey() {}
        /**
         * @param key
         *            member or processor name of this step.
         */
        public ItemKey(String key) {
            this.key = key;
        }
    }
    
    /**
     * split a key chain such as <code>name.replace('_','-').camel</code> into
     * its steps. Arguments may be quoted or bare, white space outside of a
     * quoted argument is dropped. When the abbreviation option of the
     * configuration is turned on, an <code>abbr</code> step is inserted behind
     * a leading <code>name</code>, so that <code>${name}</code> abbreviates by
     * itself.
     *
     * @param mkey
     *            the key chain as it was written in the template.
     * @return the steps, the first of which names the member of the model.
     */
    private static List<ItemKey> parseKeys(String mkey) {
        if (!mkey.endsWith("."))
            mkey = mkey + ".";

        int i=0, len = mkey.length();
        List<ItemKey> res = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        ItemKey curr = new ItemKey();
        boolean isParam = false;
        boolean isOpen = false;
        int openchar = -1;
        while (i < len) {
            char c = mkey.charAt(i);
            if (isOpen) {
                if (c == openchar) {
                    curr.params.add(sb.toString());
                    sb = new StringBuilder();
                    openchar = -1;
                    isOpen = false;
                } else {
                    sb.append(c);
                }
            } else if (StrUtils.contains(new char[]{'\'','"'}, c)) {
                openchar = c;
                isOpen = true;
                sb = new StringBuilder();
            } else if (c == '.') {
                if (curr.key == null) {
                    curr.key = sb.toString();
                }
                res.add(curr);
                curr = new ItemKey();
                sb = new StringBuilder();
            } else if (c == '(') {
                curr.key = sb.toString();
                sb = new StringBuilder();
                isParam = true;
            } else if (isParam) {
                if (c == ')' || c == ',') {
                    // collect the accumulated unquoted argument, if any.
                    // quoted arguments are already collected by the isOpen branch,
                    // which leaves sb empty here.
                    String param = sb.toString();
                    if (!param.isEmpty())
                        curr.params.add(param);
                    sb = new StringBuilder();
                    if (c == ')')
                        isParam = false;
                } else if (!StrUtils.isSpace(c)) {
                    sb.append(c);
                }
            } else if (!StrUtils.isSpace(c)) {
                sb.append(c);
            }
            i++;
        }
        
        if (JDBGenConfig.getInstance().isApplyAbbr() &&
                !res.isEmpty() &&
                "name".equalsIgnoreCase(res.get(0).key))
            res.add(1, new ItemKey("abbr"));
        
        return res;
    }
    
    /**
     * resolve a key chain against a model and run the value through the
     * processors of the chain. The first step is looked up on
     * <code>mapper</code> and, when that has no such member, on
     * <code>customs</code>; a name neither of them answers is logged as a
     * warning and renders as an empty string.
     *
     * @param mkey
     *            the key chain as it was written in the template.
     * @param mapper
     *            model the value is read from.
     * @param customs
     *            custom variables used as the fallback.
     * @return the processed value; a chain without processors may return a
     *         value of any type, including a collection.
     * @throws Exception when a processor is unknown or fails, or when reading
     *         the model fails.
     */
    private static Object getItemProcessed(String mkey, Object mapper, Map<String, String> customs) throws Exception {
        List<ItemKey> keys = parseKeys(mkey);
        String key = StrUtils.trim(keys.get(0).key);
        Object val = ObjUtils.getValue(mapper, key);
        if (val == null)
            val = ObjUtils.getValue(customs, key);
        if (val == null) {
            log.warn("cannot find '{}' information from database/custom variables", key);
            val = "";
        }
        for (int i=1; i<keys.size(); i++) {
            ItemKey ikey = keys.get(i);
            String proc = StrUtils.trim(ikey.key).toLowerCase();
            if (!procs.containsKey(proc))
                throw new RuntimeException("cannot find '"+proc+"' in string processors, valid values are: "+procs.keySet());
            val = procs.get(proc).process(val.toString(), ikey.params);
        }
        return val;
    }
    
    /**
     * resolve the key of an <code>item</code> or <code>super</code> against
     * the given model and append the decorated value. Shared by
     * {@link #appendItem} and {@link #appendSuper}, which differ only in the
     * model they hand in.
     *
     * @param sb
     *            output built so far.
     * @param template
     *            the item to render.
     * @param mapper
     *            model the value is read from.
     * @throws Exception when the key is missing, unknown or fails to resolve.
     */
    private void appendItemBase(StringBuilder sb, TemplateItem template, Object mapper) throws Exception {
        Map<String,Object> map = (Map<String,Object>)template.cont;
        String mkey = getKey(map);
        Object val = getItemProcessed(mkey, mapper, customs);
        appendBase(sb, map, val);
    }
    
    /**
     * renderer of <code>${item:...}</code>, resolved against the current
     * model.
     *
     * @param sb
     *            output built so far.
     * @param template
     *            the item to render.
     * @param mapper
     *            model the value is read from.
     * @param supr
     *            unused.
     * @throws Exception when the key is missing, unknown or fails to resolve.
     */
    @SuppressWarnings("unused")
    private void appendItem(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        appendItemBase(sb, template, mapper);
    }
    
    /**
     * renderer of <code>${super:...}</code>, resolved against the model of the
     * enclosing <code>for</code> loop.
     *
     * @param sb
     *            output built so far.
     * @param template
     *            the item to render.
     * @param mapper
     *            unused.
     * @param supr
     *            model of the enclosing loop.
     * @throws Exception when the key is missing, unknown or fails to resolve.
     */
    @SuppressWarnings("unused")
    private void appendSuper(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        appendItemBase(sb, template, supr);
    }
    
    /**
     * <code>equals</code>/<code>value</code> condition: the value equals
     * <code>condVal</code>, ignoring case.
     *
     * @param key
     *            key chain resolved against the model.
     * @param condVal
     *            value to compare against.
     * @param mapper
     *            model the value is read from.
     * @param customs
     *            custom variables used as the fallback.
     * @return <code>true</code> when both are equal.
     * @throws Exception when the key fails to resolve.
     */
    private static boolean condEquals(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        String oval = String.valueOf(getItemProcessed(key, mapper, customs));
        return condVal.equalsIgnoreCase(oval);
    }
    
    /**
     * <code>notequals</code> condition, the negation of
     * {@link #condEquals(String, String, Object, Map)}.
     *
     * @param key
     *            key chain resolved against the model.
     * @param condVal
     *            value to compare against.
     * @param mapper
     *            model the value is read from.
     * @param customs
     *            custom variables used as the fallback.
     * @return <code>true</code> when both differ.
     * @throws Exception when the key fails to resolve.
     */
    private static boolean condNotEquals(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        return !condEquals(key, condVal, mapper, customs);
    }
    
    /**
     * <code>contains</code> condition. A collection holds
     * <code>condVal</code> when one of its elements has it as its
     * <code>name</code>; a string matches when it equals one of the comma
     * separated alternatives of <code>condVal</code>. Both compare ignoring
     * case.
     *
     * @param key
     *            key chain resolved against the model.
     * @param condVal
     *            element name, or a comma separated list of alternatives.
     * @param mapper
     *            model the value is read from.
     * @param customs
     *            custom variables used as the fallback.
     * @return <code>true</code> when the value holds <code>condVal</code>.
     * @throws Exception when the key fails to resolve.
     * @throws RuntimeException when the value is neither a collection nor a
     *         string.
     */
    private static boolean condContains(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        Object objValue = getItemProcessed(key, mapper, customs);
        boolean contains = false;
        if (objValue instanceof List) {
            List<Object> collection = (List<Object>)objValue;
            for(Object o: collection) {
                if (String.valueOf(ObjUtils.getValue(o, "name")).equalsIgnoreCase(condVal)) {
                    contains = true;
                    break;
                }
            }
        } else if (objValue instanceof CharSequence) {
            String strValue = String.valueOf(objValue);
            String []names = StrUtils.split(condVal, ",", true);
            for (String item:names) {
                if (strValue.equalsIgnoreCase(item)) {
                    contains = true;
                    break;
                }
            }
        } else {
            throw new RuntimeException("contains/notcontains in if statement item must be a collection object or a ',' separated string.");
        }
        return contains;
    }
    
    /**
     * <code>notcontains</code> condition, the negation of
     * {@link #condContains(String, String, Object, Map)}.
     *
     * @param key
     *            key chain resolved against the model.
     * @param condVal
     *            element name, or a comma separated list of alternatives.
     * @param mapper
     *            model the value is read from.
     * @param customs
     *            custom variables used as the fallback.
     * @return <code>true</code> when the value does not hold
     *         <code>condVal</code>.
     * @throws Exception when the key fails to resolve.
     */
    private static boolean condNotContains(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        return !condContains(key, condVal, mapper, customs);
    }
    
    /**
     * <code>startswith</code> condition: the value starts with
     * <code>condVal</code>, ignoring case.
     *
     * @param key
     *            key chain resolved against the model.
     * @param condVal
     *            prefix to look for.
     * @param mapper
     *            model the value is read from.
     * @param customs
     *            custom variables used as the fallback.
     * @return <code>true</code> when the value starts with the prefix.
     * @throws Exception when the key fails to resolve.
     */
    private static boolean condStartsWith(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        String oval = String.valueOf(getItemProcessed(key, mapper, customs));
        return oval.toLowerCase().startsWith(condVal.toLowerCase());
    }
    
    /**
     * <code>notstartswith</code> condition, the negation of
     * {@link #condStartsWith(String, String, Object, Map)}.
     *
     * @param key
     *            key chain resolved against the model.
     * @param condVal
     *            prefix to look for.
     * @param mapper
     *            model the value is read from.
     * @param customs
     *            custom variables used as the fallback.
     * @return <code>true</code> when the value does not start with the prefix.
     * @throws Exception when the key fails to resolve.
     */
    private static boolean condNotStartsWith(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        return !condStartsWith(key, condVal, mapper, customs);
    }
    
    /**
     * <code>endswith</code> condition: the value ends with
     * <code>condVal</code>, ignoring case.
     *
     * @param key
     *            key chain resolved against the model.
     * @param condVal
     *            suffix to look for.
     * @param mapper
     *            model the value is read from.
     * @param customs
     *            custom variables used as the fallback.
     * @return <code>true</code> when the value ends with the suffix.
     * @throws Exception when the key fails to resolve.
     */
    private static boolean condEndsWith(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        String oval = String.valueOf(getItemProcessed(key, mapper, customs));
        return oval.toLowerCase().endsWith(condVal.toLowerCase());
    }
    
    /**
     * <code>notendswith</code> condition, the negation of
     * {@link #condEndsWith(String, String, Object, Map)}.
     *
     * @param key
     *            key chain resolved against the model.
     * @param condVal
     *            suffix to look for.
     * @param mapper
     *            model the value is read from.
     * @param customs
     *            custom variables used as the fallback.
     * @return <code>true</code> when the value does not end with the suffix.
     * @throws Exception when the key fails to resolve.
     */
    private static boolean condNotEndsWith(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        return !condEndsWith(key, condVal, mapper, customs);
    }
    
    /**
     * <code>matches</code> condition: the whole value matches the regular
     * expression <code>condVal</code>. This is the only condition that is case
     * sensitive.
     *
     * @param key
     *            key chain resolved against the model.
     * @param condVal
     *            regular expression the whole value has to match.
     * @param mapper
     *            model the value is read from.
     * @param customs
     *            custom variables used as the fallback.
     * @return <code>true</code> when the value matches.
     * @throws Exception when the key fails to resolve.
     */
    private static boolean condMatches(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        String oval = String.valueOf(getItemProcessed(key, mapper, customs));
        return Pattern.matches(condVal, oval);
    }
    
    /**
     * <code>notmatches</code> condition, the negation of
     * {@link #condMatches(String, String, Object, Map)}.
     *
     * @param key
     *            key chain resolved against the model.
     * @param condVal
     *            regular expression the whole value has to match.
     * @param mapper
     *            model the value is read from.
     * @param customs
     *            custom variables used as the fallback.
     * @return <code>true</code> when the value does not match.
     * @throws Exception when the key fails to resolve.
     */
    private static boolean condNotMatches(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        return !condMatches(key, condVal, mapper, customs);
    }
    
    /**
     * renderer of <code>${if:...}</code>. Every condition attribute has to
     * hold for the true branch to be rendered; the false branch is either the
     * items of an <code>${else}</code> or the nested <code>if</code> of an
     * <code>${elif:...}</code>, which is rendered recursively.
     *
     * @param sb
     *            output built so far.
     * @param template
     *            the statement to render.
     * @param mapper
     *            model the conditions are evaluated against.
     * @param supr
     *            model of the enclosing <code>for</code> loop, handed down to
     *            the branch that is rendered.
     * @throws Exception when the key is missing, or a condition fails to
     *         evaluate.
     */
    private void appendIf(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        Map<String,Object> map = (Map<String,Object>)template.cont;
        List<TemplateItem> ttpls = (List<TemplateItem>)map.get("true");
        Object ftpls = map.get("false");
        String mkey = getKey(map);
        boolean condMet = true;
        
        for (String key:map.keySet()) {
            if (ifconds.containsKey(key)) {
                String condVal = String.valueOf(map.get(key));
                if (!ifconds.get(key).check(mkey, condVal, mapper, customs)) {
                    condMet = false;
                    break;
                }
            }
        }

        if (condMet) {
            appendMapper(sb, ttpls, mapper, supr);
        } else if (ftpls != null) {
            if (ftpls instanceof TemplateItem) {
                appendIf(sb, (TemplateItem)ftpls, mapper, supr);
            } else if (ftpls instanceof List) {
                appendMapper(sb, (List<TemplateItem>)ftpls, mapper, supr);
            }
        }
    }
    
    /**
     * renderer of <code>${for:...}</code>. The body is rendered once per
     * element of the collection named by the key, with the element as the
     * model and the current model as its super. The <code>instr</code>
     * separator is written between the elements; every line break it contains
     * is normalized to the line separator of the template and the following
     * fragment is indented to the column the loop started in, plus the
     * <code>indent</code> attribute. Elements whose <code>name</code> appears
     * in the comma separated <code>skiplist</code> are left out, and every
     * rendered element gets its one based position assigned to its
     * <code>no</code> member.
     *
     * @param sb
     *            output built so far.
     * @param template
     *            the loop to render.
     * @param mapper
     *            model holding the collection.
     * @param supr
     *            unused; the loop hands its own model down as the super.
     * @throws Exception when the key is missing, or <code>indent</code> is not
     *         a number.
     * @throws RuntimeException when the model has no such member.
     */
    @SuppressWarnings("unused")
    private void appendFor(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        Map<String,Object> map = (Map<String,Object>)template.cont;
        String mkey = getKey(map);
        String instr = (String)map.get("instr");
        String indent = (String)map.get("indent");
        String[] skips = StrUtils.split((String)map.get("skiplist"), ",", true);
        int idnt = indent == null? 0:Integer.parseInt(indent);
        List<TemplateItem> tpls = (List<TemplateItem>)map.get("items");
        List<Object> litems = (List<Object>)ObjUtils.getValue(mapper, mkey);
        if (litems == null) {
            throw new RuntimeException("Model has no '"+mkey+"' member: "+mapper);
        }
        int stidx = sb.lastIndexOf("\n")+1;
        int splen = sb.substring(stidx).getBytes("EUC-KR").length; // space length(no utf-8)
        splen += idnt;
        String prepend = StrUtils.space(splen, ' ');
        boolean isFirst = true;
        for (int i=0; i<litems.size(); i++) {
            Object o = litems.get(i);
            if (skips != null) {
                String n = (String)ObjUtils.getValue(o, "name");
                if (StrUtils.contains(skips, n))
                    continue;
            }
            if (!isFirst) {
                if (instr != null) {
                    // normalize every line break in 'instr' to the template line end
                    // and re-indent each following fragment
                    String[] parts = instr.split("\r?\n", -1);
                    sb.append(parts[0]);
                    for (int p=1; p<parts.length; p++)
                        sb.append(lineEnd).append(prepend).append(parts[p]);
                }
            }
            ObjUtils.setValue(o, "no", (i+1));
            appendMapper(sb, tpls, o, mapper);
            isFirst = false;
        }
    }
    
    /**
     * renderer of <code>${date:...}</code>: the current date, formatted with
     * the <code>format</code> attribute or with <code>yyyy-MM-dd</code> when
     * none was given.
     *
     * @param sb
     *            output built so far.
     * @param template
     *            the item to render.
     * @param mapper
     *            unused.
     * @param supr
     *            unused.
     * @throws Exception when the format is not a valid
     *         <code>SimpleDateFormat</code> pattern.
     */
    @SuppressWarnings("unused")
    private void appendDate(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        Map<String,Object> map = (Map<String,Object>)template.cont;
        String format = (String)map.getOrDefault("format", DEFAULT_DATE_FORMAT);
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        appendBase(sb, map, sdf.format(new Date()));
    }
    
    /**
     * renderer of <code>${user:...}</code>: the login id of the user running
     * the application.
     *
     * @param sb
     *            output built so far.
     * @param template
     *            the item to render.
     * @param mapper
     *            unused.
     * @param supr
     *            unused.
     * @throws Exception when a decoration attribute is invalid.
     */
    @SuppressWarnings("unused")
    private void appendUser(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        Map<String,Object> map = (Map<String,Object>)template.cont;
        appendBase(sb, map, USER_ID);
    }
    
    /**
     * renderer of <code>${author:...}</code>: the <code>author</code> custom
     * variable. Nothing is appended when it was not set.
     *
     * @param sb
     *            output built so far.
     * @param template
     *            the item to render.
     * @param mapper
     *            unused.
     * @param supr
     *            unused.
     * @throws Exception when a decoration attribute is invalid.
     */
    @SuppressWarnings("unused")
    private void appendAuthor(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        Map<String,Object> map = (Map<String,Object>)template.cont;
        appendBase(sb, map, ObjUtils.getValue(customs, "author"));
    }
    
    /**
     * render a list of items in order, dispatching every one of them to the
     * renderer of its type.
     *
     * @param sb
     *            output built so far.
     * @param templates
     *            the items to render.
     * @param mapper
     *            model the values are read from.
     * @param supr
     *            model of the enclosing <code>for</code> loop, or
     *            <code>null</code> at the top level.
     * @throws Exception whatever one of the renderers fails with.
     */
    private void appendMapper(StringBuilder sb, List<TemplateItem> templates, Object mapper, Object supr) throws Exception {
        for (TemplateItem tpl:templates) {
            appenders.get(tpl.type).append(sb, tpl, mapper, supr);
        }
    }
    
    /**
     * render the parsed template against a model. The same instance may be
     * applied to any number of models.
     *
     * @param mapper
     *            the model, typically a table or a column of the database
     *            metadata; a <code>Map</code> works as well.
     * @return the rendered text.
     * @throws Exception when the template refers to something the model and
     *         the custom variables cannot answer, or when an attribute of a
     *         placeholder turns out to be invalid at render time.
     */
    public String applyMapper(Object mapper) throws Exception {
        StringBuilder sb = new StringBuilder();
        appendMapper(sb, items, mapper, null);
        return sb.toString();
    }
}

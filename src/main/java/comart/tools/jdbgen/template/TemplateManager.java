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
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author comart
 */
public class TemplateManager {
    private final static Logger logger = Logger.getLogger(TemplateManager.class.getName());
    private static final String USER_ID = ObjUtils.getLoginUserId();
    
    private interface TemplateHandler {
        TemplateItem process(String extra, ParseContext ctx) throws ParseException;
    }
    
    private interface TemplateAppender {
        void append(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception;
    }
    
    private interface ItemProcHandler {
        String process(String item, List<Object> params);
    }
    
    private interface IfCondHandler {
        boolean check(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception;
    }
    
    private static enum TemplateType {
        TEXT, ITEM, SUPER, FOR, IF, USER, DATE, AUTHOR;
    }
    
    private static class TemplateItem {
        TemplateType type;
        Object cont;
        
        public TemplateItem(TemplateType type, Object cont) {
            this.type = type;
            this.cont = cont;
        }
    }
    
    private static class ParseContext {
        int curr, len, line;
        String template;
        public ParseContext(String template) {
            this.template = template;
            line = curr = 0;
            len = template.length();
        }
        
        public void updateLineCount(int end) {
            String text = template.substring(curr, end);
            line += text.split("\n").length - 1;
            curr = end;
        }

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

        public void moveTo(int count) {
            for (int i=0; i< count; i++) {
                if (nextChar() < 0)
                    break;
            }
        }

        public void skipSpace() {
            int c;
            while ((c = nextChar()) > -1) {
                if (!StrUtils.isSpace(c)) {
                    curr--;
                    break;
                }
            }
        }

        public int peek() {
            if (curr < len) {
                return template.charAt(curr);
            } else {
                return -1;
            }
        }
        
        public String near() {
            int length = 100;
            if (curr + length < len)
                return template.substring(curr, curr+length) + "...";
            else
                return template.substring(curr);
        }
    }
    
    
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
                        if (c == '\\')
                            isEscape = true;
                        else if (c == openChar)
                            break;
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
        if (value.length() > 0) {
            if (StrUtils.isEmpty(name)) {
                throw new ParseException("Name value pair not matched: "+
                        data+". invalid syntax before: "+ctx.near(), idx);
            }
            map.put(name.toLowerCase(), value);
        }
        return map;
    }
    
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

    @SuppressWarnings("unused")
    private static TemplateItem parseItem(String extra, ParseContext ctx) throws ParseException {
        return new TemplateItem(TemplateType.ITEM, parseNVPairs(ctx, extra));
    }

    @SuppressWarnings("unused")
    private static TemplateItem parseSuper(String extra, ParseContext ctx) throws ParseException {
        return new TemplateItem(TemplateType.SUPER, parseNVPairs(ctx, extra));
    }
    
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
    
    @SuppressWarnings("unused")
    private static TemplateItem parseDate(String extra, ParseContext ctx) throws ParseException {
        if (!extra.contains("="))
            extra = "format="+extra;
        return new TemplateItem(TemplateType.DATE, parseNVPairs(ctx, extra));
    }
    
    @SuppressWarnings("unused")
    private static TemplateItem parseUser(String extra, ParseContext ctx) throws ParseException {
        return new TemplateItem(TemplateType.USER, parseNVPairs(ctx, extra));
    }
    
    @SuppressWarnings("unused")
    private static TemplateItem parseAuthor(String extra, ParseContext ctx) throws ParseException {
        return new TemplateItem(TemplateType.AUTHOR, parseNVPairs(ctx, extra));
    }
    
    @SuppressWarnings("unused")
    private static String procPrefix(String item, List<Object> params) {
        int idx = item.lastIndexOf("_");
        if (idx > -1) {
            return item.substring(0, idx);
        } else{
            return item;
        }
    }
    
    @SuppressWarnings("unused")
    private static String procSuffix(String item, List<Object> params) {
        int idx = item.indexOf("_");
        if (idx > -1) {
            return item.substring(idx+1);
        } else{
            return item;
        }
    }
    
    @SuppressWarnings("unused")
    private static String procCamel(String item, List<Object> params) {
        return StrUtils.toCamelCase(item);
    }
    
    @SuppressWarnings("unused")
    private static String procPascal(String item, List<Object> params) {
        return StrUtils.toPascalCase(item);
    }
    
    @SuppressWarnings("unused")
    private static String procSnake(String item, List<Object> params) {
        return StrUtils.toSnakeCase(item);
    }
    
    @SuppressWarnings("unused")
    private static String procScreaming(String item, List<Object> params) {
        return StrUtils.toScreamingSnakeCase(item);
    }
    
    @SuppressWarnings("unused")
    private static String procSkewer(String item, List<Object> params) {
        return StrUtils.toSkewerCase(item);
    }
    
    @SuppressWarnings("unused")
    private static String procLower(String item, List<Object> params) {
        return item.toLowerCase();
    }
    
    @SuppressWarnings("unused")
    private static String procUpper(String item, List<Object> params) {
        return item.toUpperCase();
    }
    
    private static String procReplace(String item, List<Object> params) {
        String find = params.get(0).toString();
        String repl = params.get(1).toString();
        return StrUtils.replace(item, find, repl);
    }
    
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
    
    private static final Map<String, TemplateHandler> handlers = new HashMap<>();
    private static final Map<String, ItemProcHandler> procs = new HashMap<>();
    private static final Map<String, IfCondHandler> ifconds = new HashMap<>();
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
    
    private final List<TemplateItem> items;
    private String lineEnd = System.lineSeparator();
    private final Map<TemplateType, TemplateAppender> appenders = new HashMap<>();
    
    private Map<String, String> customs = null;
    
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
        if (idx > 0) {
            if (template.charAt(idx - 1) == '\r')
                lineEnd = "\r\n";
            else
                lineEnd = "\n";
        }
        this.customs = customs;
        items = parseTemplate(new ParseContext(template));
    }
    
    private void appendBase(StringBuilder sb, Map<String,Object> map, Object val) throws Exception {
        String spadsz = (String)map.get("padsize");
        int padsz = spadsz == null? 0:Integer.parseInt(spadsz);
        String spaddr = (String)map.get("paddir");
        boolean padLeft = spaddr == null? false:"left".equalsIgnoreCase(spaddr);
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
    
    private String getKey(Map<String,Object> props) {
        String mkey = (String)props.get("key");
        if (mkey == null) mkey = (String)props.get("item");
        return mkey;
    }
    
    @SuppressWarnings("unused")
    private void appendText(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        sb.append(template.cont.toString());
    }
    
    private static class ItemKey {
        String key = null;
        List<Object> params = new ArrayList<>();
        public ItemKey() {}
        public ItemKey(String key) {
            this.key = key;
        }
    }
    
    private static List<ItemKey> parseKeys(String mkey) {
        if (!mkey.endsWith("."))
            mkey = mkey + ".";

        int i=0, len = mkey.length();
        List<ItemKey> res = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        ItemKey curr = new ItemKey();
        boolean isParam = false;
        int openchar = -1;
        while (i < len) {
            char c = mkey.charAt(i);
            if (openchar > -1 && c == openchar) {
                curr.params.add(sb.toString());
                sb = new StringBuilder();
                openchar = -1;
            } else if (openchar > -1) {
                sb.append(c);
            } else if (openchar < 0 && StrUtils.contains(new char[]{'\'','"'}, c)) {
                openchar = c;
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
                    isParam = c == ')';
                    sb = new StringBuilder();
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
                "name".equals(res.get(0).key.toLowerCase()))
            res.add(1, new ItemKey("abbr"));
        
        return res;
    }
    
    private static Object getItemProcessed(String mkey, Object mapper, Map<String, String> customs) throws Exception {
        List<ItemKey> keys = parseKeys(mkey);
        String key = StrUtils.trim(keys.get(0).key);
        Object val = ObjUtils.getValue(mapper, key);
        if (val == null)
            val = ObjUtils.getValue(customs, key);
        if (val == null) {
            logger.log(Level.WARNING, "cannot find '{0}' information from database/custom variables", key);
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
    
    private void appendItemBase(StringBuilder sb, TemplateItem template, Object mapper) throws Exception {
        Map<String,Object> map = (Map<String,Object>)template.cont;
        String mkey = getKey(map);
        Object val = getItemProcessed(mkey, mapper, customs);
        appendBase(sb, map, val);
    }
    
    @SuppressWarnings("unused")
    private void appendItem(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        appendItemBase(sb, template, mapper);
    }
    
    @SuppressWarnings("unused")
    private void appendSuper(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        appendItemBase(sb, template, supr);
    }
    
    private static boolean condEquals(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        String oval = String.valueOf(getItemProcessed(key, mapper, customs));
        return condVal.equalsIgnoreCase(oval);
    }
    
    private static boolean condNotEquals(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        return !condEquals(key, condVal, mapper, customs);
    }
    
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
    
    private static boolean condNotContains(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        return !condContains(key, condVal, mapper, customs);
    }
    
    private static boolean condStartsWith(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        String oval = String.valueOf(getItemProcessed(key, mapper, customs));
        return oval.toLowerCase().startsWith(condVal.toLowerCase());
    }
    
    private static boolean condNotStartsWith(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        return !condStartsWith(key, condVal, mapper, customs);
    }
    
    private static boolean condEndsWith(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        String oval = String.valueOf(getItemProcessed(key, mapper, customs));
        return oval.toLowerCase().endsWith(condVal.toLowerCase());
    }
    
    private static boolean condNotEndsWith(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        return !condEndsWith(key, condVal, mapper, customs);
    }
    
    private static boolean condMatches(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        String oval = String.valueOf(getItemProcessed(key, mapper, customs));
        return Pattern.matches(condVal, oval);
    }
    
    private static boolean condNotMatches(String key, String condVal, Object mapper, Map<String, String> customs) throws Exception {
        return !condMatches(key, condVal, mapper, customs);
    }
    
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
                    int idx = instr.indexOf("\n");
                    if (idx > -1) {
                        if (idx > 0)
                            sb.append(instr.substring(0, idx));
                        sb.append(lineEnd).append(prepend);
                        if (instr.length()-1 > idx)
                            sb.append(instr.substring(idx+1));
                    } else {
                        sb.append(instr);
                    }
                }
            }
            ObjUtils.setValue(o, "no", (i+1));
            appendMapper(sb, tpls, o, mapper);
            isFirst = false;
        }
    }
    
    @SuppressWarnings("unused")
    private void appendDate(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        Map<String,Object> map = (Map<String,Object>)template.cont;
        String format = (String)map.get("format");
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        appendBase(sb, map, sdf.format(new Date()));
    }
    
    @SuppressWarnings("unused")
    private void appendUser(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        Map<String,Object> map = (Map<String,Object>)template.cont;
        appendBase(sb, map, USER_ID);
    }
    
    @SuppressWarnings("unused")
    private void appendAuthor(StringBuilder sb, TemplateItem template, Object mapper, Object supr) throws Exception {
        Map<String,Object> map = (Map<String,Object>)template.cont;
        appendBase(sb, map, ObjUtils.getValue(customs, "author"));
    }
    
    private void appendMapper(StringBuilder sb, List<TemplateItem> templates, Object mapper, Object supr) throws Exception {
        for (TemplateItem tpl:templates) {
            appenders.get(tpl.type).append(sb, tpl, mapper, supr);
        }
    }
    
    public String applyMapper(Object mapper) throws Exception {
        StringBuilder sb = new StringBuilder();
        appendMapper(sb, items, mapper, null);
        return sb.toString();
    }
}

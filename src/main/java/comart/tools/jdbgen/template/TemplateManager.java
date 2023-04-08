/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package comart.tools.jdbgen.template;

import comart.utils.ObjUtils;
import comart.utils.StrUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author comart
 */
public class TemplateManager {
    private static final String USER_ID = ObjUtils.getLoginUserId();
    
    private interface TemplateHandler {
        TemplateItem process(String extra, ParseContext ctx) throws ParseException;
    }
    
    private static enum TemplateType {
        TEXT, ITEM, FOR, IF, USER, DATE;
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
    }
    
    
    private static String next(ParseContext ctx, ArrayList<TemplateItem> items) throws ParseException {
        if (ctx.curr == ctx.len)
            return null;
        
        int sp = ctx.template.indexOf("${", ctx.curr);
        
        if (sp < 0)
            sp = ctx.len;
        
        // add previous text
        if (sp > ctx.curr+1) {
            String pretext = ctx.template.substring(ctx.curr, sp);
            items.add(new TemplateItem(TemplateType.TEXT, pretext));
        }

        if (sp+1 < ctx.len)
            sp += 2;    // skip "${"
        
        if (sp == ctx.len) {
            ctx.updateLineCount(sp);
            return null;
        } else {
            ctx.skipSpace();
            int c = ctx.peek();
            StringBuilder sb = new StringBuilder();
            // check escaping string
            if (c == '"' || c == '\'') {
                int openChar = c;
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
                sp = ctx.curr+1;
            }
            int lst = ctx.template.indexOf("}", sp);
            if (lst < 0) {
                throw new ParseException("'}' not found ", ctx.line);
            }
            String res = sb.length() == 0 ? ctx.template.substring(sp, lst).trim():sb.toString();
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

    private static Map<String,Object> parseNVPairs(String data) throws ParseException {
        int idx = 0;
        int openChar = -1;
        Map<String,Object> map = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        while (idx < data.length()) {
            char c = data.charAt(idx);
            if (c == '\\') {
                idx++;
                c = data.charAt(idx);
                switch(c) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default:
                        sb.append(c);
                }
            }else {
                if (openChar < 0 && (c == '\"' || c == '\'')) {
                    openChar = c;
                } else if (c == openChar) {
                    openChar = -1;
                } else if (openChar < 0 && c == ',') {
                    String[] nv = StrUtils.split(sb.toString(), "=", true);
                    if (nv.length < 2)
                        throw new ParseException("Name value pair not matched: "+data, idx);
                    map.put(nv[0], nv[1]);
                    sb.delete(0, sb.length());
                } else {
                    sb.append(c);
                }
            }
            idx++;
        }
        String remain = sb.toString().trim();
        if (remain.length() > 0) {
            String[] nv = StrUtils.split(remain, "=", true);
            if (nv.length < 2)
                throw new ParseException("Name value pair not matched: "+data, idx);
            map.put(nv[0], nv[1]);
        }
        return map;
    }
    
    private static TemplateItem parseOne(String itemString, ParseContext ctx) throws ParseException {
        if (itemString.indexOf(':') < 0) {
            itemString = "item:key=" + itemString;
        }
        int idx = itemString.indexOf(':');
        String type = itemString.substring(0, idx).trim().toLowerCase();
        String typeOptions = itemString.substring(idx+1);
        TemplateHandler handler = handlers.get(type);
        if (handler == null) {
            throw new ParseException("Unknown template: "+itemString, ctx.line);
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

    private static TemplateItem parseItem(String extra, ParseContext ctx) throws ParseException {
        return new TemplateItem(TemplateType.ITEM, parseNVPairs(extra));
    }
    
    private static TemplateItem parseIf(String extra, ParseContext ctx) throws ParseException {
        // this codes makes like below
        // if
        //   true statements
        // else
        //   if
        //     true statements
        //   ...
        //   else
        //     false statements
        Map<String, Object> pairs = parseNVPairs(extra);
        TemplateItem res = new TemplateItem(TemplateType.IF, pairs);
        ArrayList<TemplateItem> items = new ArrayList<>();
        pairs.put("true", items); // set to true for future statements
        while (true) {
            String itemString = next(ctx, items);
            if (itemString == null)
                throw new ParseException("if statements not closed.", ctx.line);
            if (itemString.startsWith("elif:")) {
                extra = itemString.substring(5).trim();
                Map<String, Object> npairs = parseNVPairs(extra);
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
        Map<String, Object> pairs = parseNVPairs(extra);
        TemplateItem res = new TemplateItem(TemplateType.FOR, pairs);
        ArrayList<TemplateItem> items = new ArrayList<>();
        pairs.put("items", items); // set to true for future statements
        while (true) {
            String itemString = next(ctx, items);
            if (itemString == null)
                throw new ParseException("for statements not closed.", ctx.line);
            if ("endfor".equals(itemString)) {
                // reached end of for statement.
                break;
            } else {
                items.add(parseOne(itemString, ctx));
            }
        }
        return res;
    }
    
    private static TemplateItem parseDate(String extra, ParseContext ctx) throws ParseException {
        if (!extra.contains("="))
            extra = "format="+extra;
        return new TemplateItem(TemplateType.DATE, parseNVPairs(extra));
    }
    
    private static TemplateItem parseUser(String extra, ParseContext ctx) throws ParseException {
        return new TemplateItem(TemplateType.USER, parseNVPairs(extra));
    }
    
    private static final Map<String, TemplateHandler> handlers = new HashMap<>();
    static {
        handlers.put("item", TemplateManager::parseItem);
        handlers.put("if", TemplateManager::parseIf);
        handlers.put("for", TemplateManager::parseFor);
        handlers.put("date", TemplateManager::parseDate);
        handlers.put("user", TemplateManager::parseUser);
    }
    
    private final List<TemplateItem> items;
    private String lineEnd = System.lineSeparator();
    
    public TemplateManager(String template) throws ParseException {
        // preserve line end with source
        int idx = template.indexOf("\n");
        if (idx > 0) {
            if (template.charAt(idx - 1) == '\r')
                lineEnd = "\r\n";
            else
                lineEnd = "\n";
        }
        items = parseTemplate(new ParseContext(template));
    }
    

}

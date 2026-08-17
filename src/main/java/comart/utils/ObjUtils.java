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
package comart.utils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;

/**
 * Reflective property access and a few small file helpers. The template engine
 * resolves a placeholder such as <code>${table.name}</code> against whatever
 * object it was given - a bean, a <code>Map</code> or a mixture of both - and
 * that lookup is what this class provides.
 *
 * @author comart
 */
public class ObjUtils {
    /**
     * read a single, non nested property of <code>obj</code>. A
     * <code>Map</code> is looked up by key; anything else is asked for a public
     * field of that name first and then, walking up the class hierarchy, for a
     * no argument method named <code>get&lt;Property&gt;</code>,
     * <code>&lt;property&gt;</code> or <code>is&lt;Property&gt;</code>, in that
     * order.
     *
     * @param obj
     *            the object to read from, may be <code>null</code>.
     * @param property
     *            name of the property, may be <code>null</code> or empty.
     * @return the value, or <code>null</code> when <code>obj</code> is
     *         <code>null</code>, when there is no property name to read or when
     *         <code>obj</code> has no such property.
     * @throws Exception
     *             if the accessor itself fails.
     */
    @SuppressWarnings("UseSpecificCatch")
    private static Object getValuePrivate(Object obj, String property) throws Exception {
        // nothing names nothing, whatever kind of context this is asked of
        if (property == null || property.isEmpty()) {
            return null;
        } else if (obj instanceof Map) {
            return ((Map)obj).get(property);
        } else if (obj != null) {
            Class<?> c = obj.getClass();
            try {
                Field f = c.getField(property);
                return f.get(obj);
            } catch(Throwable fieldNotVisible) {
                String capitalized = property.substring(0, 1).toUpperCase()+property.substring(1);
                // candidate accessor names, tried in order on each class of the hierarchy
                String[] candidates = new String[]{
                    "get"+capitalized, property, "is"+capitalized
                };
                Method m = null;
                while (c != null && m == null) {
                    for (String candidate: candidates) {
                        try {
                            m = c.getMethod(candidate, new Class[]{});
                            break;
                        } catch (Exception ignored) {}
                    }
                    if (m == null)
                        c = c.getSuperclass();
                }

                if (m == null)
                    return null;
                return m.invoke(obj, new Object[]{});
            }
        } else {
            return null;
        }
    }
    
    /**
     * read a possibly nested property of <code>obj</code>. The name is split at
     * every <code>'.'</code> and each segment is read from the value the
     * previous one yielded, so <code>"a.b.c"</code> reads <code>c</code> of
     * <code>b</code> of <code>a</code>.
     *
     * @param obj
     *            the object to read from.
     * @param property
     *            property name, dot separated for a nested one, may be
     *            <code>null</code> or empty.
     * @return the value, or <code>null</code> when there is no property name to
     *         read or when any segment of the path resolves to nothing. An
     *         empty name reads as nothing whatever <code>obj</code> is, so that
     *         a <code>${}</code> of a template is left alone against a bean and
     *         against a map alike.
     * @throws Exception
     *             if an accessor along the path fails.
     */
    public static Object getValue(Object obj, String property) throws Exception {
        if (property == null)
            return null;
        int idx = property.indexOf('.');
        if (idx < 0) {
            return getValuePrivate(obj, property);
        } else {
            return getValue(
                    getValuePrivate(obj, property.substring(0, idx)),
                    property.substring(idx+1));
        }
    }
    
    /**
     * {@link #getValue(Object, String)} with a fallback.
     *
     * @param obj
     *            the object to read from.
     * @param property
     *            property name, dot separated for a nested one.
     * @param defVal
     *            returned when the property resolves to <code>null</code>.
     * @return the value, or <code>defVal</code>.
     * @throws Exception
     *             if an accessor along the path fails.
     */
    public static Object getValue(Object obj, String property, Object defVal) throws Exception {
        Object res = getValue(obj, property);
        return res == null ? defVal: res;
    }
    
    /**
     * boxed type to its primitive counterpart. Reflection lookups by
     * <code>val.getClass()</code> yield the boxed type, while generated setters
     * (Lombok included) usually declare the primitive one.
     */
    private static final Map<Class<?>, Class<?>> PRIMITIVES = new HashMap<Class<?>, Class<?>>() {{
        put(Integer.class  , int.class    );
        put(Long.class     , long.class   );
        put(Short.class    , short.class  );
        put(Byte.class     , byte.class   );
        put(Character.class, char.class   );
        put(Boolean.class  , boolean.class);
        put(Float.class    , float.class  );
        put(Double.class   , double.class );
    }};

    /**
     * write a property of <code>obj</code>. Walking up the class hierarchy, a
     * one argument method named <code>set&lt;Property&gt;</code> or
     * <code>&lt;property&gt;</code> is looked for, taking the class of
     * <code>val</code> or - for a boxed value - its primitive counterpart. The
     * call is a no-op when no such method exists.
     *
     * @param obj
     *            the object to write to.
     * @param property
     *            name of the property.
     * @param val
     *            the value to write. A <code>null</code> value matches no
     *            setter and therefore writes nothing.
     * @throws Exception
     *             if the setter itself fails.
     */
    @SuppressWarnings("UseSpecificCatch")
    public static void setValue(Object obj, String property, Object val) throws Exception {
        String setter = "set"+property.substring(0, 1).toUpperCase()+property.substring(1);
        Class<?> c = obj.getClass();
        // try the declared type first, then its primitive counterpart
        Class<?>[] argTypes = val == null ? new Class<?>[]{}
                : PRIMITIVES.containsKey(val.getClass())
                    ? new Class<?>[]{val.getClass(), PRIMITIVES.get(val.getClass())}
                    : new Class<?>[]{val.getClass()};
        String[] candidates = new String[]{ setter, property };
        Method m = null;
        while (c != null && m == null) {
            OUTER:
            for (Class<?> argType: argTypes) {
                for (String candidate: candidates) {
                    try {
                        m = c.getMethod(candidate, new Class<?>[]{argType});
                        break OUTER;
                    } catch (Exception ignored) {}
                }
            }
            if (m == null)
                c = c.getSuperclass();
        }
        if (m != null)
            m.invoke(obj, new Object[]{val});
    }
    
    /**
     * read a whole file as UTF-8 text.
     *
     * @param file
     *            path of the file.
     * @return the contents of the file.
     * @throws Exception
     *             if the file cannot be read.
     */
    public static String getFileContents(String file) throws Exception {
        return FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8);
    }

    /**
     * write <code>content</code> as UTF-8 text, creating the parent
     * directories when they are missing and replacing an existing file.
     *
     * @param fname
     *            path of the file to write.
     * @param content
     *            the text to write.
     * @throws Exception
     *             if the file cannot be written.
     */
    public static void writeFile(String fname, String content) throws Exception {
        FileUtils.writeStringToFile(new File(fname), content, StandardCharsets.UTF_8);
    }
    
    /**
     * the account name the JVM runs under.
     *
     * @return the <code>user.name</code> system property.
     */
    public static String getLoginUserId() {
        return System.getProperty("user.name");
    }
    
    /**
     * every field of <code>obj</code> and of its super classes as a map of name
     * to value, so that a bean can be handed to something expecting a map -
     * the template variables, for instance.
     *
     * @param obj
     *            the object to flatten.
     * @return a new map holding one entry per declared field.
     */
    public static Map<String, Object> objToMap(Object obj) {
        HashMap<String, Object> res = new HashMap<>();
        setFieldsTo(obj, res);
        return res;
    }
    
    /**
     * put every declared field of <code>obj</code> and of its super classes
     * into <code>vars</code>, keyed by the field name and read through the
     * accessor rules of {@link #getValue(Object, String)}. A field that cannot
     * be read is skipped, and an entry already present is overwritten.
     *
     * @param obj
     *            the object to read the fields of.
     * @param vars
     *            the map the entries are added to.
     */
    public static void setFieldsTo(Object obj, HashMap<String, Object> vars) {
        Class cls = obj.getClass();
        while (cls != null) {
            for (Field f:cls.getDeclaredFields()) {
                try {
                    vars.put(f.getName(), getValuePrivate(obj, f.getName()));
                } catch(Exception ignored) {}
            }
            try {
                cls = cls.getSuperclass();
            } catch(Exception ignored) {
                cls = null;
            }
        }
    }
}

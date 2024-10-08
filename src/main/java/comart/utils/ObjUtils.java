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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author comart
 */
public class ObjUtils {
    @SuppressWarnings("UseSpecificCatch")
    private static Object getValuePrivate(Object obj, String property) throws Exception {
        if (obj instanceof Map) {
            return ((Map)obj).get(property);
        } else if (obj != null) {
            Class c = obj.getClass();
            try {
                Field f = c.getField(property);
                return f.get(obj);
            } catch(Throwable fieldNotVisible) {
                String getter = "get"+property.substring(0, 1).toUpperCase()+property.substring(1);
                @SuppressWarnings("null")
                Method m = null;
                while (c != null && m == null) {
                    try {
                        m = c.getMethod(getter, new Class[]{});
                    } catch (Exception ignored) {}
                    try {
                        if (m == null)
                            m = c.getMethod(property, new Class[]{});
                    } catch (Exception ignored) {}
                    try {
                        getter = "is"+getter.substring(3);
                        if (m == null)
                            m = c.getMethod(getter, new Class[]{});
                    } catch (Exception ignored) {}
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
    
    public static Object getValue(Object obj, String property) throws Exception {
        int idx = property.indexOf('.');
        if (idx < 0) {
            return getValuePrivate(obj, property);
        } else {
            return getValue(
                    getValuePrivate(obj, property.substring(0, idx)),
                    property.substring(idx+1));
        }
    }
    
    public static Object getValue(Object obj, String property, Object defVal) throws Exception {
        Object res = getValue(obj, property);
        return res == null ? defVal: res;
    }
    
    @SuppressWarnings("UseSpecificCatch")
    public static void setValue(Object obj, String property, Object val) throws Exception {
        String setter = "set"+property.substring(0, 1).toUpperCase()+property.substring(1);
        Class c = obj.getClass();
        Method m = null;
        while (c != null && m == null) {
            try {
                m = c.getMethod(setter, new Class[]{val.getClass()});
            } catch (Exception ignored) {}
            try {
                if (m == null)
                    m = c.getMethod(property, new Class[]{val.getClass()});
            } catch (Exception ignored) {}
            if (m == null)
                c = c.getSuperclass();
        }
        if (m != null)
            m.invoke(obj, new Object[]{val});
    }
    
    public static String getFileContents(String file) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        BufferedInputStream bis = null;
        try {
            byte[] buffer = new byte[1024];
            int rsize;
            bis = new BufferedInputStream(new FileInputStream(file));
            while ((rsize = bis.read(buffer)) >= 0) {
                if (rsize > 0)
                    baos.write(buffer, 0, rsize);
            }
        } finally {
            if (bis != null) bis.close();
        }
        return new String(baos.toByteArray(), "utf-8");
    }
    
    public static void writeFile(String fname, String content) throws Exception {
        BufferedOutputStream bos = null;
        try {
            File f = new File(fname);
            if (f.getParentFile() != null)
                f.getParentFile().mkdirs();
            bos = new BufferedOutputStream(new FileOutputStream(f));
            bos.write(content.getBytes("utf-8"));
        } finally {
            if (bos != null) bos.close();
        }
    }
    
    public static String getLoginUserId() {
        return System.getProperty("user.name");
    }
    
    public static Map<String, Object> objToMap(Object obj) {
        HashMap<String, Object> res = new HashMap<>();
        setFieldsTo(obj, res);
        return res;
    }
    
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

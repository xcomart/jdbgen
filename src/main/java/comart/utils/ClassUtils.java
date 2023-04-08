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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassUtils {
    private static final Logger logger = Logger.getLogger(ClassUtils.class.getName());

    public ClassUtils() {
    }
    
    @SuppressWarnings("UseSpecificCatch")
    public static <T> List<String> getClasses(File f, Class<T> type) throws FileNotFoundException, IOException {
        final List<String> classesTobeReturned = new ArrayList<>();
        if (f.exists()) {
            final ClassLoader classLoader = ClassUtils.class.getClassLoader();
            final URL url = f.toURI().toURL();
            URLClassLoader ucl = new URLClassLoader(new URL[] { url }, classLoader);
            try (JarInputStream jarFile = new JarInputStream(new FileInputStream(f))) {
                JarEntry jarEntry;
                while (true) {
                    jarEntry = jarFile.getNextJarEntry();
                    if (jarEntry == null)
                        break;
                    if (jarEntry.getName().endsWith(".class")) {
                        String classname = jarEntry.getName();
                        classname = classname.substring(0, classname.length() - 6);
                        classname = classname.replaceAll("/", "\\.");
                        logger.severe(classname);
                        if (!classname.contains("$")) {
                            try {
                                final Class<?> myLoadedClass = Class.forName(classname, false, ucl);
                                if (type.isAssignableFrom(myLoadedClass)) {
                                    classesTobeReturned.add(classname);
                                }
                            } catch (Throwable ignored) {
                                logger.severe(classname + " -> " + ignored.getLocalizedMessage());
                            }
                        }
                    }
                }
            }
        }
        return classesTobeReturned;
    }

    @SuppressWarnings("UseSpecificCatch")
    public static List<String> getDrivers(String jarFile) {
        try {
            return getClasses(new File(jarFile), Driver.class);
        } catch (Exception e) {
            logger.log(Level.SEVERE, (String)null, e);
            return null;
        }
    }
    
    public static void main(String[] args) {
        System.out.println(getDrivers("drivers/ojdbc8-19.7.0.0.jar"));
    }
}

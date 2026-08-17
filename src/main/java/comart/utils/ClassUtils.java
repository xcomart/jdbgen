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
import lombok.extern.slf4j.Slf4j;

/**
 * Looks into a jar file for the classes it contains. Used to find the JDBC
 * driver classes of a driver jar the user picked, so that the driver class name
 * does not have to be typed in by hand.
 */
@Slf4j
public class ClassUtils {

    /**
     * this class only holds <code>static</code> methods.
     */
    public ClassUtils() {
    }
    
    /**
     * every class of the jar <code>f</code> that <code>type</code> can be
     * assigned from. The jar is read entry by entry and each class is loaded
     * through a throw away class loader over that jar alone; a class that
     * cannot be loaded - a missing dependency, for instance - is skipped
     * silently. Inner classes (those whose name contains <code>$</code>) are
     * not looked at.
     *
     * @param f
     *            the jar file to look into. A file that does not exist yields
     *            an empty list.
     * @param type
     *            the interface or class the results have to implement or
     *            extend.
     * @param <T>
     *            the type looked for.
     * @return the fully qualified names of the matching classes, never
     *         <code>null</code>.
     * @throws FileNotFoundException
     *             if the jar disappears between the existence check and the
     *             read.
     * @throws IOException
     *             if the jar cannot be read.
     */
    @SuppressWarnings("UseSpecificCatch")
    public static <T> List<String> getClasses(File f, Class<T> type) throws FileNotFoundException, IOException {
        final List<String> classesTobeReturned = new ArrayList<>();
        if (f.exists()) {
            final ClassLoader classLoader = ClassUtils.class.getClassLoader();
            final URL url = f.toURI().toURL();
            try (URLClassLoader ucl = new URLClassLoader(new URL[] { url }, classLoader);
                    JarInputStream jarFile = new JarInputStream(new FileInputStream(f))) {
                JarEntry jarEntry;
                while (true) {
                    jarEntry = jarFile.getNextJarEntry();
                    if (jarEntry == null)
                        break;
                    if (jarEntry.getName().endsWith(".class")) {
                        String classname = jarEntry.getName();
                        classname = classname.substring(0, classname.length() - 6);
                        classname = classname.replace('/', '.');
                        log.trace(classname);
                        if (!classname.contains("$")) {
                            try {
                                final Class<?> myLoadedClass = Class.forName(classname, false, ucl);
                                if (type.isAssignableFrom(myLoadedClass)) {
                                    classesTobeReturned.add(classname);
                                }
                            } catch (Throwable ignored) {
                                log.trace("{} -> {}", classname, ignored.getLocalizedMessage());
                            }
                        }
                    }
                }
            }
        }
        return classesTobeReturned;
    }

    /**
     * the <code>java.sql.Driver</code> implementations of a driver jar.
     *
     * @param jarFile
     *            path of the jar, resolved with
     *            {@link AppDirs#resolve(String)} so that a path stored relative
     *            to the user data directory or the installation works.
     * @return the driver class names, or <code>null</code> when the jar cannot
     *         be read.
     */
    @SuppressWarnings("UseSpecificCatch")
    public static List<String> getDrivers(String jarFile) {
        try {
            return getClasses(AppDirs.resolve(jarFile), Driver.class);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            return null;
        }
    }
}

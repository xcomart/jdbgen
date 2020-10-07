
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
                        String classname = jarEntry.getName().replaceAll("/", "\\.");
                        classname = classname.substring(0, classname.length() - 6);
                        if (!classname.contains("$")) {
                            try {
                                final Class<?> myLoadedClass = Class.forName(classname, false, ucl);
                                if (type.isAssignableFrom(myLoadedClass)) {
                                    classesTobeReturned.add(classname);
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }
        }
        return classesTobeReturned;
    }

    public static List<String> getDrivers(String jarFile) {
        try {
            return getClasses(new File(jarFile), Driver.class);
        } catch (Exception e) {
            logger.log(Level.SEVERE, (String)null, e);
            return null;
        }
    }
    
    public static void main(String[] args) {
        System.out.println(getDrivers("/home/comart/.m2/repository/com/oracle/database/jdbc/ojdbc8/19.7.0.0/ojdbc8-19.7.0.0.jar"));
    }
}

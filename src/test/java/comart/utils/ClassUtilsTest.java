/*
 * The MIT License
 *
 * Copyright 2024 comart.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package comart.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Looking into a driver jar for the classes it holds. The jars are built here
 * out of class files that are already on the class path, so that
 * {@link ClassUtils#getClasses(File, Class)} really loads what it finds and the
 * type filter is decided by the class and not by its name.
 */
public class ClassUtilsTest {

    @AfterEach
    public void clearOverrides() {
        System.clearProperty(AppDirs.DATA_DIR_PROPERTY);
        System.clearProperty(AppDirs.RESOURCE_BASE_PROPERTY);
    }

    /** the compiled class file of <code>cls</code>, read from the class path. */
    private static byte[] bytecodeOf(Class<?> cls) throws IOException {
        String resource = "/" + cls.getName().replace('.', '/') + ".class";
        try (InputStream is = ClassUtilsTest.class.getResourceAsStream(resource)) {
            assertTrue(is != null, resource + " is expected on the class path");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int read;
            while ((read = is.read(buf)) > 0)
                out.write(buf, 0, read);
            return out.toByteArray();
        }
    }

    /** a jar holding the class files of <code>classes</code>, and nothing else. */
    private static File jarOf(Path dir, String name, Class<?>... classes) throws IOException {
        File jar = new File(dir.toFile(), name);
        try (OutputStream fos = Files.newOutputStream(jar.toPath());
                ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (Class<?> cls: classes) {
                zos.putNextEntry(new ZipEntry(cls.getName().replace('.', '/') + ".class"));
                zos.write(bytecodeOf(cls));
                zos.closeEntry();
            }
        }
        return jar;
    }

    private static void addEntry(ZipOutputStream zos, String name, byte[] content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content);
        zos.closeEntry();
    }

    @Test
    public void theClassesOfAJarAreFilteredByTheirType(@TempDir Path dir) throws Exception {
        File jar = jarOf(dir, "mixed.jar", FakeDriver.class, StrUtils.class);

        List<String> drivers = ClassUtils.getClasses(jar, Driver.class);
        assertEquals(1, drivers.size(), "only the Driver implementation matches");
        assertEquals(FakeDriver.class.getName(), drivers.get(0));

        // the same jar, asked for a type both of them are
        List<String> all = ClassUtils.getClasses(jar, Object.class);
        assertEquals(2, all.size());
        assertTrue(all.contains(StrUtils.class.getName()));
        assertTrue(all.contains(FakeDriver.class.getName()));

        // ... and one neither of them is
        assertTrue(ClassUtils.getClasses(jar, CharSequence.class).isEmpty());
    }

    @Test
    public void anythingThatIsNoLoadableTopLevelClassIsSkipped(@TempDir Path dir) throws Exception {
        File jar = new File(dir.toFile(), "noisy.jar");
        try (OutputStream fos = Files.newOutputStream(jar.toPath());
                ZipOutputStream zos = new ZipOutputStream(fos)) {
            addEntry(zos, "comart/utils/FakeDriver.class", bytecodeOf(FakeDriver.class));
            // not a class file at all
            addEntry(zos, "META-INF/services/java.sql.Driver",
                    "comart.utils.FakeDriver".getBytes(StandardCharsets.UTF_8));
            addEntry(zos, "readme.txt", "hello".getBytes(StandardCharsets.UTF_8));
            // an inner class: skipped by its name, before anything is loaded -
            // Map.Entry would pass the Object filter if it were looked at
            addEntry(zos, "java/util/Map$Entry.class", new byte[]{1, 2, 3});
            // a class file that cannot be loaded, a missing dependency for
            // instance, must not abort the scan of the rest
            addEntry(zos, "com/nowhere/Broken.class", new byte[]{1, 2, 3});
        }

        List<String> found = ClassUtils.getClasses(jar, Object.class);

        assertEquals(1, found.size(), "found " + found);
        assertEquals(FakeDriver.class.getName(), found.get(0));
        assertFalse(found.contains("java.util.Map$Entry"));
    }

    @Test
    public void aJarThatIsNotThereHoldsNoClasses(@TempDir Path dir) throws Exception {
        File missing = new File(dir.toFile(), "not-here.jar");

        assertTrue(ClassUtils.getClasses(missing, Driver.class).isEmpty());
    }

    @Test
    public void theDriversOfAConfiguredJarAreFoundThroughAppDirs(@TempDir Path dir) throws Exception {
        Path data = dir.resolve("data");
        Files.createDirectories(data.resolve(AppDirs.DRIVERS_DIR));
        System.setProperty(AppDirs.DATA_DIR_PROPERTY, data.toString());
        System.setProperty(AppDirs.RESOURCE_BASE_PROPERTY, dir.resolve("install").toString());

        File jar = jarOf(data.resolve(AppDirs.DRIVERS_DIR), "fake-driver.jar",
                FakeDriver.class, StrUtils.class);
        assertTrue(jar.isFile());

        // the configuration stores the path relative to the user data directory
        List<String> byRelativePath = ClassUtils.getDrivers("drivers/fake-driver.jar");
        assertEquals(1, byRelativePath.size());
        assertEquals(FakeDriver.class.getName(), byRelativePath.get(0));

        // an absolute path is taken as it is
        assertEquals(byRelativePath, ClassUtils.getDrivers(jar.getAbsolutePath()));
    }

    @Test
    public void aDriverJarThatCannotBeReadIsReported(@TempDir Path dir) throws Exception {
        System.setProperty(AppDirs.DATA_DIR_PROPERTY, dir.resolve("data").toString());
        System.setProperty(AppDirs.RESOURCE_BASE_PROPERTY, dir.resolve("install").toString());

        // a configured jar that is gone is simply no driver, not a failure
        assertTrue(ClassUtils.getDrivers("drivers/gone.jar").isEmpty());
        // no jar configured at all cannot be looked into, which the caller has
        // to be able to tell apart from "there are no drivers in it"
        assertNull(ClassUtils.getDrivers(null));
        assertNull(ClassUtils.getDrivers("   "));
    }
}

/**
 * A JDBC driver that drives nothing. Only its type matters: it is packed into
 * the jars above so that the driver lookup has something to find.
 */
class FakeDriver implements Driver {
    @Override
    public Connection connect(String url, Properties info) {
        return null;
    }

    @Override
    public boolean acceptsURL(String url) {
        return false;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
}

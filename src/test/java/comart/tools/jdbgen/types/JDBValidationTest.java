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
package comart.tools.jdbgen.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A connection or a driver that is only half filled in fails somewhere inside
 * the JDBC driver, with a message nobody can act on. Both are therefore checked
 * before they are used.
 */
public class JDBValidationTest {

    /** a connection carrying everything it needs. */
    private static JDBConnection connection() {
        JDBConnection conn = new JDBConnection();
        conn.setName("Sample");
        conn.setDriverType("H2 Embedded");
        conn.setConnectionUrl("jdbc:h2:mem:test");
        conn.setOutputDir("out");
        return conn;
    }

    /** a driver carrying everything it needs. */
    private static JDBDriver driver() {
        JDBDriver driver = new JDBDriver();
        driver.setName("H2 Embedded");
        driver.setJdbcJar("drivers/h2.jar");
        driver.setDriverClass("org.h2.Driver");
        return driver;
    }

    @Test
    public void aCompleteConnectionIsAccepted() {
        assertTrue(connection().validate());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    public void aConnectionWithoutADriverIsRefused(String driverType) {
        JDBConnection conn = connection();
        conn.setDriverType(driverType);

        assertFalse(conn.validate());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    public void aConnectionWithoutAUrlIsRefused(String url) {
        JDBConnection conn = connection();
        conn.setConnectionUrl(url);

        assertFalse(conn.validate());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    public void aConnectionWithoutAnOutputDirectoryIsRefused(String dir) {
        JDBConnection conn = connection();
        conn.setOutputDir(dir);

        assertFalse(conn.validate());
    }

    @Test
    public void aConnectionNeedsAQueryOnlyWhenTheKeepAliveIsTurnedOn() {
        JDBConnection conn = connection();
        assertTrue(conn.validate(), "an unused keep-alive query is not missing");

        conn.setUseKeepAlive(true);
        assertFalse(conn.validate(), "a keep-alive without a statement would do nothing");

        conn.setKeepAliveQuery("select 1");
        assertTrue(conn.validate());
    }

    @Test
    public void aConnectionWithoutCredentialsIsStillAccepted() {
        // a driver marked as 'no auth' - an embedded database, say - is used
        // without a user name and a password
        JDBConnection conn = connection();
        conn.setUserName(null);
        conn.setUserPassword(null);

        assertTrue(conn.validate());
    }

    @Test
    public void aCompleteDriverIsAccepted() {
        assertTrue(driver().validate());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    public void aDriverWithoutAJarIsRefused(String jar) {
        JDBDriver driver = driver();
        driver.setJdbcJar(jar);

        assertFalse(driver.validate());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    public void aDriverWithoutADriverClassIsRefused(String driverClass) {
        JDBDriver driver = driver();
        driver.setDriverClass(driverClass);

        assertFalse(driver.validate());
    }

    @Test
    public void aDriverNeedsACommentQueryOnlyWhenItIsTurnedOn() {
        JDBDriver driver = driver();

        driver.setUseTableComments(true);
        assertFalse(driver.validate());
        driver.setTableCommentsSql("select 1, 2 from dual");
        assertTrue(driver.validate());

        driver.setUseColumnComments(true);
        assertFalse(driver.validate());
        driver.setColumnCommentsSql("select 1, 2 from dual");
        assertTrue(driver.validate());
    }

    @Test
    public void aDriverNeedsATableOrColumnQueryOnlyWhenItIsTurnedOn() {
        // a turned on query that was never written would be handed to the
        // database as an empty statement
        JDBDriver driver = driver();

        driver.setUseTables(true);
        assertFalse(driver.validate());
        driver.setTablesSql("select * from information_schema.tables");
        assertTrue(driver.validate());

        driver.setUseColumns(true);
        assertFalse(driver.validate());
        driver.setColumnsSql("select * from information_schema.columns");
        assertTrue(driver.validate());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    public void aTurnedOnTableQueryThatIsBlankIsRefused(String sql) {
        JDBDriver driver = driver();
        driver.setUseTables(true);
        driver.setTablesSql(sql);

        assertFalse(driver.validate());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    public void aTurnedOnColumnQueryThatIsBlankIsRefused(String sql) {
        JDBDriver driver = driver();
        driver.setUseColumns(true);
        driver.setColumnsSql(sql);

        assertFalse(driver.validate());
    }

    @Test
    public void aTableOrColumnQueryThatIsNotTurnedOnIsNotChecked() {
        JDBDriver driver = driver();
        driver.setTablesSql(null);
        driver.setColumnsSql(null);

        assertTrue(driver.validate(), "an unused query is not missing");
    }
}

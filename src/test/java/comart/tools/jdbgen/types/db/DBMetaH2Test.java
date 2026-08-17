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
package comart.tools.jdbgen.types.db;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import comart.tools.jdbgen.types.JDBConnection;
import comart.tools.jdbgen.types.JDBDriver;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The metadata reader against a real database: the H2 driver jar that ships
 * with the repository is loaded exactly the way the application loads a driver,
 * and an in-memory database is read through it - once through the metadata of
 * the driver and once through the custom queries the shipped 'H2 Embedded'
 * driver definition declares.
 */
public class DBMetaH2Test {

    /** the driver jar of the repository, loaded like the application loads one. */
    private static File jdbcJar;
    /** catalog name h2 gives the database. */
    private static String catalog;
    /** connection url of the fixture database. */
    private static String url;
    /** the directory the fixture databases are created in. */
    private static Path fixtureDir;

    private static final String USER = "sa";
    private static final String PASSWORD = "";

    /**
     * create the fixture database.
     *
     * <p>It is a file database rather than an in-memory one on purpose: every
     * {@link DBMeta} loads the driver in a class loader of its own, and the
     * in-memory databases of h2 live in a static of the driver classes - so
     * the database seeded here would simply not be the one the application
     * connects to.</p>
     */
    @BeforeAll
    public static void createTheDatabase(@TempDir Path dir) throws Exception {
        jdbcJar = findDriverJar();
        assumeTrue(jdbcJar != null, "the h2 driver jar of the repository is missing");
        fixtureDir = dir;
        url = "jdbc:h2:" + dir.resolve("jdbgen_dbmeta").toAbsolutePath()
                .toString().replace('\\', '/');

        try (URLClassLoader loader = new URLClassLoader(new URL[]{ jdbcJar.toURI().toURL() },
                DBMetaH2Test.class.getClassLoader())) {
            Driver driver = (Driver)Class.forName("org.h2.Driver", true, loader)
                    .getDeclaredConstructor().newInstance();
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            try (Connection conn = driver.connect(url, props)) {
                assertNotNull(conn, "the h2 driver has to accept its own url");
                catalog = conn.getCatalog();
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("create schema APP");
                    stmt.execute("create table APP.TB_USER ("
                            + "ORG_ID int not null,"
                            + "USER_ID int not null,"
                            + "USER_NAME varchar(40) not null,"
                            + "EMAIL varchar(120),"
                            + "CREATED timestamp,"
                            + "primary key (ORG_ID, USER_ID))");
                    stmt.execute("comment on table APP.TB_USER is 'the users'");
                    stmt.execute("comment on column APP.TB_USER.USER_NAME is 'the display name'");
                    stmt.execute("create table APP.TB_LOG (MESSAGE varchar(200))");
                    stmt.execute("comment on table APP.TB_LOG is 'the log'");
                    stmt.execute("create view APP.VW_USER as "
                            + "select USER_ID, USER_NAME from APP.TB_USER");
                    // a schema of its own, so that the table lists asserted on
                    // 'APP' stay what they are
                    stmt.execute("create schema ORD");
                    // the key is declared against the alphabetical order of its
                    // columns, which is the order getPrimaryKeys() reports them in
                    stmt.execute("create table ORD.TB_ORDER_ITEM ("
                            + "ZONE_ID int not null,"
                            + "ACCOUNT_ID int not null,"
                            + "NOTE varchar(50),"
                            + "primary key (ZONE_ID, ACCOUNT_ID))");
                }
            }
        }
    }

    /** the h2 jar below the 'drivers' directory of the repository. */
    private static File findDriverJar() {
        File dir = new File("drivers");
        if (!dir.isDirectory())
            dir = new File(System.getProperty("user.dir"), "drivers");
        File[] jars = dir.listFiles((d, name) ->
                name.startsWith("h2-") && name.endsWith(".jar"));
        return jars == null || jars.length == 0 ? null : jars[0].getAbsoluteFile();
    }

    /** the 'H2 Embedded' driver as it is shipped in the default configuration. */
    private static JDBDriver stockDriver() throws Exception {
        try (InputStreamReader ir = new InputStreamReader(
                DBMetaH2Test.class.getResourceAsStream("/defaultConfig.json"),
                StandardCharsets.UTF_8)) {
            JsonObject conf = JsonParser.parseReader(ir).getAsJsonObject();
            for (JsonElement el: conf.getAsJsonArray("drivers")) {
                JsonObject obj = el.getAsJsonObject();
                if ("H2 Embedded".equals(obj.get("name").getAsString())) {
                    JDBDriver driver = new Gson().fromJson(obj, JDBDriver.class);
                    driver.setJdbcJar(jdbcJar.getAbsolutePath());
                    return driver;
                }
            }
        }
        throw new IllegalStateException("the default configuration has no 'H2 Embedded' driver");
    }

    /** a driver definition reading everything through the driver metadata. */
    private static JDBDriver plainDriver() {
        JDBDriver driver = new JDBDriver();
        driver.setName("H2");
        driver.setJdbcJar(jdbcJar.getAbsolutePath());
        driver.setDriverClass("org.h2.Driver");
        driver.setNoAuth(true);
        return driver;
    }

    private static JDBConnection connection() {
        JDBConnection conn = new JDBConnection();
        conn.setName("h2 test");
        conn.setDriverType("H2");
        conn.setConnectionUrl(url);
        conn.setUserName(USER);
        conn.setUserPassword(PASSWORD);
        conn.setOutputDir("out");
        return conn;
    }

    /** the schema the fixture lives in. */
    private static DBSchema appSchema(DBMeta meta) throws Exception {
        return schemaNamed(meta, "APP");
    }

    /** the named schema of the fixture database. */
    private static DBSchema schemaNamed(DBMeta meta, String name) throws Exception {
        for (DBSchema schema: meta.getSchemas())
            if (name.equals(schema.getSchema()))
                return schema;
        throw new IllegalStateException("the '" + name + "' schema is missing");
    }

    private static List<String> namesOf(List<DBTable> tables) {
        return tables.stream().map(DBTable::getTable).sorted().collect(Collectors.toList());
    }

    private static DBTable tableNamed(List<DBTable> tables, String name) {
        for (DBTable t: tables)
            if (name.equals(t.getTable()))
                return t;
        throw new IllegalStateException("no table '" + name + "' in " + namesOf(tables));
    }

    private static DBColumn columnNamed(List<DBColumn> columns, String name) {
        for (DBColumn c: columns)
            if (name.equals(c.getColumn()))
                return c;
        throw new IllegalStateException("no column '" + name + "'");
    }

    // ------------------------------------------------------------------ schemas

    @Test
    public void everySchemaOfTheDatabaseIsRead() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            List<String> schemas = new ArrayList<>();
            for (DBSchema s: meta.getSchemas())
                schemas.add(s.getSchema());

            assertTrue(schemas.contains("APP"), schemas.toString());
            assertTrue(schemas.contains("PUBLIC"), schemas.toString());
            assertTrue(schemas.contains("INFORMATION_SCHEMA"), schemas.toString());
            for (DBSchema s: meta.getSchemas()) {
                assertEquals(s.getSchema(), s.getName(), "the display name starts as the schema");
                assertEquals(catalog, s.getCatalog());
            }
        }
    }

    @Test
    public void theSchemasAreReadOnceAndCached() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            List<DBSchema> first = meta.getSchemas();

            assertSame(first, meta.getSchemas(), "every call would be a round trip otherwise");
            assertSame(first.get(0), meta.getSchemaTree().get(catalog).get(0));
        }
    }

    @Test
    public void theSchemasAreGroupedByTheirCatalogForTheTree() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            Map<String, List<DBSchema>> tree = meta.getSchemaTree();

            assertEquals(1, tree.size(), "h2 reports a single catalog: " + tree.keySet());
            assertTrue(tree.containsKey(catalog), tree.keySet().toString());
            assertEquals(meta.getSchemas().size(), tree.get(catalog).size());
        }
    }

    // ------------------------------------------------------------------- tables

    @Test
    public void theTablesOfASchemaAreReadThroughTheDriverMetadata() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            List<DBTable> tables = meta.getTables(appSchema(meta), false);

            assertEquals(java.util.Arrays.asList("TB_LOG", "TB_USER"), namesOf(tables),
                    "a view is not a table");
            DBTable user = tableNamed(tables, "TB_USER");
            assertEquals("TABLE", user.getType(), "h2 reports 'BASE TABLE'");
            assertEquals("APP", user.getSchema());
            assertEquals(catalog, user.getCatalog());
            assertEquals("the users", user.getRemarks());
            assertEquals("TB_USER", user.getTitle());
        }
    }

    @Test
    public void theViewsAreReturnedWhenTheyAreAskedFor() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            List<DBTable> tables = meta.getTables(appSchema(meta), true);

            assertEquals(java.util.Arrays.asList("TB_LOG", "TB_USER", "VW_USER"), namesOf(tables));
            assertEquals("VIEW", tableNamed(tables, "VW_USER").getType());
        }
    }

    @Test
    public void theTablesAreReadOnceAndCachedOnTheSchema() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            DBSchema schema = appSchema(meta);
            assertNull(schema.getTables(), "nothing is read before it is asked for");

            List<DBTable> tables = meta.getTables(schema, false);

            assertEquals(3, schema.getTables().size(),
                    "the view is cached as well, only the answer is filtered");
            assertEquals(2, tables.size());
            assertSame(schema.getTables().get(0), meta.getTables(schema, true).get(0));
        }
    }

    @Test
    public void aSchemaWithoutAnyTableIsAnsweredWithAnEmptyList() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            DBSchema empty = new DBSchema("PUBLIC", "PUBLIC", catalog, null);

            assertTrue(meta.getTables(empty, true).isEmpty());
        }
    }

    // ------------------------------------------------------------------ columns

    @Test
    public void theColumnsOfATableAreReadInOrderAndNumbered() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            DBTable user = tableNamed(meta.getTables(appSchema(meta), false), "TB_USER");

            List<DBColumn> columns = meta.getTableColumns(user);

            assertEquals(java.util.Arrays.asList(
                    "ORG_ID", "USER_ID", "USER_NAME", "EMAIL", "CREATED"),
                    columns.stream().map(DBColumn::getColumn).collect(Collectors.toList()));
            for (int i = 0; i < columns.size(); i++)
                assertEquals(i + 1, columns.get(i).getNo(), "the position is one based");
        }
    }

    @Test
    public void theTypeOfEveryColumnIsDerivedForTheTemplates() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            DBTable user = tableNamed(meta.getTables(appSchema(meta), false), "TB_USER");
            List<DBColumn> columns = meta.getTableColumns(user);

            DBColumn name = columnNamed(columns, "USER_NAME");
            assertEquals("VARCHAR", name.getJdbcType());
            assertEquals("String", name.getJavaType());
            assertTrue(name.isCharType(), name.getTypeName());
            assertEquals(40, name.getLength());
            assertTrue(name.getTypeString().endsWith("(40)"), name.getTypeString());
            assertEquals("the display name", name.getRemarks());

            DBColumn id = columnNamed(columns, "USER_ID");
            assertEquals("INTEGER", id.getJdbcType());
            assertEquals("Integer", id.getJavaType());
            assertFalse(id.isCharType());

            assertEquals("TIMESTAMP", columnNamed(columns, "CREATED").getJdbcType());
        }
    }

    @Test
    public void theKeyColumnsAreMarkedAndKeptApart() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            DBTable user = tableNamed(meta.getTables(appSchema(meta), false), "TB_USER");

            meta.getTableColumns(user);

            assertEquals(java.util.Arrays.asList("ORG_ID", "USER_ID"),
                    user.getKeys().stream().map(DBColumn::getColumn).sorted()
                            .collect(Collectors.toList()));
            assertEquals(java.util.Arrays.asList("CREATED", "EMAIL", "USER_NAME"),
                    user.getNotKeys().stream().map(DBColumn::getColumn).sorted()
                            .collect(Collectors.toList()));
            assertTrue(columnNamed(user.getColumns(), "ORG_ID").isKey());
            assertFalse(columnNamed(user.getColumns(), "EMAIL").isKey());
            assertEquals(user.getColumns().size(),
                    user.getKeys().size() + user.getNotKeys().size(),
                    "every column is either a key or not");
        }
    }

    @Test
    public void theKeyColumnsAreInTheOrderTheKeyWasDeclaredIn() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            DBSchema ord = schemaNamed(meta, "ORD");
            DBTable item = tableNamed(meta.getTables(ord, false), "TB_ORDER_ITEM");

            meta.getTableColumns(item);

            // getPrimaryKeys() answers in column name order, so without the
            // KEY_SEQ sort this would be ACCOUNT_ID, ZONE_ID - and a template
            // building a 'where' clause out of it would swap the two
            assertEquals(java.util.Arrays.asList("ZONE_ID", "ACCOUNT_ID"),
                    item.getKeys().stream().map(DBColumn::getColumn)
                            .collect(Collectors.toList()));
            assertEquals(java.util.Arrays.asList("NOTE"),
                    item.getNotKeys().stream().map(DBColumn::getColumn)
                            .collect(Collectors.toList()));
        }
    }

    @Test
    public void aTableWithoutAPrimaryKeyHasNoKeyColumns() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            DBTable log = tableNamed(meta.getTables(appSchema(meta), false), "TB_LOG");

            List<DBColumn> columns = meta.getTableColumns(log);

            assertTrue(log.getKeys().isEmpty());
            assertEquals(columns, log.getNotKeys());
        }
    }

    @Test
    public void theColumnsOfAViewAreReadAsWell() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            DBTable view = tableNamed(meta.getTables(appSchema(meta), true), "VW_USER");

            List<DBColumn> columns = meta.getTableColumns(view);

            assertEquals(java.util.Arrays.asList("USER_ID", "USER_NAME"),
                    columns.stream().map(DBColumn::getColumn).collect(Collectors.toList()));
            assertTrue(view.getKeys().isEmpty(), "a view has no primary key");
        }
    }

    @Test
    public void theColumnsAreReadOnceAndCachedOnTheTable() throws Exception {
        try (DBMeta meta = new DBMeta(plainDriver(), connection())) {
            DBTable user = tableNamed(meta.getTables(appSchema(meta), false), "TB_USER");
            assertNull(user.getColumns());

            List<DBColumn> columns = meta.getTableColumns(user);

            assertSame(columns, meta.getTableColumns(user));
            assertSame(columns, user.getColumns());
        }
    }

    // ------------------------------------------------------- the shipped driver

    @Test
    public void theShippedH2DriverReadsItsTablesWithItsOwnQuery() throws Exception {
        JDBDriver driver = stockDriver();
        assertTrue(driver.isUseTables(), "the shipped definition replaces the table metadata");

        try (DBMeta meta = new DBMeta(driver, connection())) {
            List<DBTable> tables = meta.getTables(appSchema(meta), true);

            assertEquals(java.util.Arrays.asList("TB_LOG", "TB_USER", "VW_USER"), namesOf(tables),
                    "the custom query has to find what the metadata finds");
            assertEquals("TABLE", tableNamed(tables, "TB_USER").getType(),
                    "'BASE TABLE' is mapped by the query itself");
            assertEquals("VIEW", tableNamed(tables, "VW_USER").getType());
            assertEquals("the users", tableNamed(tables, "TB_USER").getRemarks());
            assertEquals("APP", tableNamed(tables, "TB_USER").getSchema());
        }
    }

    @Test
    public void theShippedH2DriverStillReadsItsColumnsThroughTheMetadata() throws Exception {
        JDBDriver driver = stockDriver();
        assertFalse(driver.isUseColumns());

        try (DBMeta meta = new DBMeta(driver, connection())) {
            DBTable user = tableNamed(meta.getTables(appSchema(meta), false), "TB_USER");

            List<DBColumn> columns = meta.getTableColumns(user);

            assertEquals(5, columns.size(),
                    "the tables of the custom query have to carry what getColumns() needs");
            assertEquals(2, user.getKeys().size());
        }
    }

    // -------------------------------------------------------- custom queries

    @Test
    public void aCustomColumnQueryBringsItsOwnKeyFlag() throws Exception {
        JDBDriver driver = plainDriver();
        driver.setUseColumns(true);
        driver.setColumnsSql(
                "select '${catalog}' as \"TABLE_CAT\", '${schema}' as \"TABLE_SCHEM\","
                + " '${table}' as \"TABLE_NAME\", 'PK_COL' as \"COLUMN_NAME\","
                + " cast(4 as smallint) as \"DATA_TYPE\", 'INTEGER' as \"TYPE_NAME\","
                + " cast(10 as int) as \"COLUMN_SIZE\", cast(0 as smallint) as \"NULLABLE\","
                + " 'the key' as \"REMARKS\", cast(null as varchar) as \"COLUMN_DEF\","
                + " 1 as \"IS_KEY\""
                + " union all select '${catalog}', '${schema}', '${table}', 'TXT_COL',"
                + " cast(12 as smallint), 'VARCHAR', cast(30 as int), cast(1 as smallint),"
                + " 'the text', cast(null as varchar), 0");

        try (DBMeta meta = new DBMeta(driver, connection())) {
            DBTable user = tableNamed(meta.getTables(appSchema(meta), false), "TB_USER");

            List<DBColumn> columns = meta.getTableColumns(user);

            assertEquals(java.util.Arrays.asList("PK_COL", "TXT_COL"),
                    columns.stream().map(DBColumn::getColumn).collect(Collectors.toList()));
            assertEquals(java.util.Arrays.asList(1, 2),
                    columns.stream().map(DBColumn::getNo).collect(Collectors.toList()));
            assertTrue(columns.get(0).isKey(), "IS_KEY is what marks a key here");
            assertFalse(columns.get(1).isKey());
            assertEquals(java.util.Arrays.asList(columns.get(0)), user.getKeys());
            assertEquals(java.util.Arrays.asList(columns.get(1)), user.getNotKeys());
            assertEquals("VARCHAR(30)", columns.get(1).getTypeString());
            assertEquals("the key", columns.get(0).getRemarks());
            assertEquals("TB_USER", columns.get(0).getTable(),
                    "the query is filled in from the table it is run for");
        }
    }

    @Test
    public void aCustomCommentQueryReplacesTheCommentsOfTheMetadata() throws Exception {
        JDBDriver driver = plainDriver();
        driver.setUseTableComments(true);
        driver.setTableCommentsSql("select 'TB_USER', 'the users of ${schema}'");
        driver.setUseColumnComments(true);
        driver.setColumnCommentsSql("select 'EMAIL', 'the address of ${table}'");

        try (DBMeta meta = new DBMeta(driver, connection())) {
            List<DBTable> tables = meta.getTables(appSchema(meta), false);
            DBTable user = tableNamed(tables, "TB_USER");
            assertEquals("the users of APP", user.getRemarks(),
                    "the query is filled in from the schema it is run for");
            assertEquals("the log", tableNamed(tables, "TB_LOG").getRemarks(),
                    "a table the query says nothing about keeps what the metadata said");

            List<DBColumn> columns = meta.getTableColumns(user);

            assertEquals("the address of TB_USER", columnNamed(columns, "EMAIL").getRemarks());
            assertNull(columnNamed(columns, "USER_NAME").getRemarks(),
                    "the comments of the metadata are replaced, not merged");
        }
    }

    // ------------------------------------------------------------- connecting

    @Test
    public void aUrlTheDriverDoesNotUnderstandIsReportedAsSuch() {
        JDBConnection conn = connection();
        conn.setConnectionUrl("jdbc:postgresql://localhost/test");

        SQLException ex = assertThrows(SQLException.class,
                () -> new DBMeta(plainDriver(), conn));

        assertTrue(ex.getMessage().contains("does not accept"), ex.getMessage());
        assertTrue(ex.getMessage().contains("jdbc:postgresql://localhost/test"), ex.getMessage());
    }

    @Test
    public void aDriverJarThatIsNotThereIsReported() {
        JDBDriver driver = plainDriver();
        driver.setJdbcJar(new File(jdbcJar.getParentFile(), "nowhere.jar").getAbsolutePath());

        assertThrows(ClassNotFoundException.class, () -> new DBMeta(driver, connection()));
    }

    @Test
    public void aDriverClassThatIsNotInTheJarIsReported() {
        JDBDriver driver = plainDriver();
        driver.setDriverClass("org.nosuch.Driver");

        assertThrows(ClassNotFoundException.class, () -> new DBMeta(driver, connection()));
    }

    @Test
    public void theConnectionIsReallyGoneAfterTheMetadataWasClosed() throws Exception {
        DBMeta meta = new DBMeta(plainDriver(), connection());
        assertFalse(meta.getTables(appSchema(meta), false).isEmpty());
        meta.close();
        meta.close(); // a failed generation run closes what it has twice

        // a file database of h2 only opens when nothing else holds it, so this
        // only works when the connection above was really given up
        try (DBMeta second = new DBMeta(plainDriver(), connection())) {
            assertFalse(second.getTables(appSchema(second), false).isEmpty(),
                    "every connection reads the metadata of its own");
        }
    }

    @Test
    public void everyMetadataCallOnAClosedConnectionIsRefused() throws Exception {
        DBMeta meta = new DBMeta(plainDriver(), connection());
        DBSchema schema = appSchema(meta);
        DBTable user = tableNamed(meta.getTables(schema, false), "TB_USER");
        DBSchema fresh = new DBSchema("APP", "APP", catalog, null);
        DBTable freshTable = new DBTable();
        freshTable.setCatalog(catalog);
        freshTable.setSchema("APP");
        freshTable.setTable("TB_LOG");
        meta.close();

        // the driver class loader goes with the connection, so a call that got
        // this far would fail inside the driver with a NoClassDefFoundError
        for (Executable call: new Executable[] {
                meta::getSchemas,
                meta::getSchemaTree,
                () -> meta.getTables(fresh, false),
                () -> meta.getTableColumns(freshTable) }) {
            SQLException ex = assertThrows(SQLException.class, call);
            assertTrue(ex.getMessage().contains("closed"), ex.getMessage());
        }

        // what was read before the connection was closed is still there
        assertFalse(schema.getTables().isEmpty());
        assertEquals("TB_USER", user.getTable());
    }

    @Test
    public void aConnectionWithoutCredentialsOpensTheDatabase() throws Exception {
        // the sample connection of a fresh installation carries neither a user
        // name nor a password, and Properties refuses a null value
        String noAuthUrl = "jdbc:h2:" + fixtureDir.resolve("jdbgen_noauth")
                .toAbsolutePath().toString().replace('\\', '/');
        try (URLClassLoader loader = new URLClassLoader(new URL[]{ jdbcJar.toURI().toURL() },
                DBMetaH2Test.class.getClassLoader())) {
            Driver driver = (Driver)Class.forName("org.h2.Driver", true, loader)
                    .getDeclaredConstructor().newInstance();
            try (Connection conn = driver.connect(noAuthUrl, new Properties());
                 Statement stmt = conn.createStatement()) {
                stmt.execute("create table TB_ANON (ID int not null primary key)");
            }
        }

        JDBConnection conn = connection();
        conn.setConnectionUrl(noAuthUrl);
        conn.setUserName(null);
        conn.setUserPassword(null);

        try (DBMeta meta = new DBMeta(plainDriver(), conn)) {
            assertFalse(meta.getSchemas().isEmpty());
        }
    }
}

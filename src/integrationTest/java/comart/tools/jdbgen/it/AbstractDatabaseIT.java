package comart.tools.jdbgen.it;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import comart.tools.jdbgen.types.JDBConnection;
import comart.tools.jdbgen.types.JDBDriver;
import comart.tools.jdbgen.types.db.DBColumn;
import comart.tools.jdbgen.types.db.DBMeta;
import comart.tools.jdbgen.types.db.DBSchema;
import comart.tools.jdbgen.types.db.DBTable;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.JdbcDatabaseContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every database integration test does: seed a database running in a
 * container, open it through {@link DBMeta} with the <em>stock</em> driver
 * definition of <code>defaultConfig.json</code> - only the jar path is filled
 * in, everything else is what a user gets out of the box - and read the seeded
 * schema back.
 *
 * <p>The subclasses contribute the container, the connection settings and their
 * own DDL dialect; the assertions live here, so that every database is checked
 * the same way. The tests run against one container per class
 * ({@link TestInstance.Lifecycle#PER_CLASS}, so the seeding runs once).</p>
 *
 * @author comart
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDatabaseIT {

    /** system property naming the directory the driver jars were copied to. */
    public static final String DRIVERS_DIR_PROPERTY = "intdrivers.dir";

    /** the seeded table with a single column primary key. */
    protected static final String CUSTOMER = "customer";
    /** the seeded table with a two column primary key. */
    protected static final String ORDER_ITEM = "order_item";
    /** the seeded view over {@link #CUSTOMER}. */
    protected static final String CUSTOMER_VIEW = "customer_view";

    /** comment of the {@link #CUSTOMER} table. */
    protected static final String CUSTOMER_REMARKS = "customer master";
    /** comment of the {@link #ORDER_ITEM} table. */
    protected static final String ORDER_ITEM_REMARKS = "one line of an order";
    /** comment of the customer_id column. */
    protected static final String CUSTOMER_ID_REMARKS = "customer identifier";
    /** comment of the customer_name column. */
    protected static final String CUSTOMER_NAME_REMARKS = "displayed customer name";

    /** columns of {@link #CUSTOMER}, in the order they are declared. */
    protected static final List<String> CUSTOMER_COLUMNS =
            Arrays.asList("customer_id", "customer_name", "email");
    /** columns of {@link #ORDER_ITEM}, in the order they are declared. */
    protected static final List<String> ORDER_ITEM_COLUMNS =
            Arrays.asList("order_id", "line_no", "product_name", "quantity");
    /** the primary key columns of {@link #ORDER_ITEM}. */
    protected static final List<String> ORDER_ITEM_KEYS =
            Arrays.asList("order_id", "line_no");

    // ---------------------------------------------------------------- contract

    /** the running container of this test, started by the subclass. */
    protected abstract JdbcDatabaseContainer<?> container();

    /** name of the stock driver definition in <code>defaultConfig.json</code>. */
    protected abstract String stockDriverName();

    /** file name prefix of the driver jar in <code>build/int-drivers</code>. */
    protected abstract String driverJarPrefix();

    /** the DDL seeding the tables, the view and the comments. */
    protected abstract List<String> seedStatements();

    /**
     * pick the schema the seed went into out of everything
     * {@link DBMeta#getSchemas()} reports.
     */
    protected abstract DBSchema seededSchema(List<DBSchema> schemas);

    /**
     * the connection URL, built from the mapped port of the container the way a
     * user builds it from the URL template of the driver.
     */
    protected abstract String connectionUrl();

    /**
     * an identifier the way the database stores it. Databases that fold
     * unquoted identifiers to upper case override this.
     *
     * @param name the identifier as it is written in the DDL, in lower case.
     * @return the identifier as the metadata reports it.
     */
    protected String stored(String name) {
        return name;
    }

    /** the user the container was created for. */
    protected String username() {
        return container().getUsername();
    }

    /** the password the container was created with. */
    protected String password() {
        return container().getPassword();
    }

    /**
     * the driver properties of the connection. By default the properties the
     * stock driver definition offers, which is what the user interface fills a
     * new connection with; subclasses add what their driver needs on top.
     */
    protected Map<String, String> connectionProps() {
        Map<String, String> props = new HashMap<>();
        Map<String, String> stock = stockDriver().getProps();
        if (stock != null)
            props.putAll(stock);
        return props;
    }

    // ----------------------------------------------------------------- seeding

    /**
     * run the DDL of the subclass. Seeding uses the same URL, credentials and
     * properties as the connection under test, so a database is only ever seeded
     * where it is read back from.
     */
    @BeforeAll
    protected void seed() throws Exception {
        Properties props = new Properties();
        props.setProperty("user", username());
        props.setProperty("password", password());
        props.putAll(connectionProps());
        try (Connection conn = DriverManager.getConnection(connectionUrl(), props);
             Statement stmt = conn.createStatement()) {
            for (String sql: seedStatements())
                stmt.execute(sql);
        }
    }

    // ------------------------------------------------------------------- tests

    /**
     * the stock driver definition opens the database, and the schema the seed
     * went into is among the schemas - both in the flat list and in the tree the
     * database explorer is built from.
     */
    @Test
    public void theStockDriverOpensTheDatabaseAndReportsTheSchema() throws Exception {
        try (DBMeta meta = openMeta()) {
            List<DBSchema> schemas = meta.getSchemas();
            assertFalse(schemas.isEmpty(), "no schema at all was reported");

            DBSchema schema = seededSchema(schemas);
            assertNotNull(schema, "the seeded schema is missing from getSchemas()");

            Map<String, List<DBSchema>> tree = meta.getSchemaTree();
            assertFalse(tree.isEmpty(), "the schema tree is empty");
            List<DBSchema> flattened = tree.values().stream()
                    .flatMap(List::stream).collect(Collectors.toList());
            assertTrue(flattened.stream().anyMatch(s -> s == schema),
                    "the seeded schema is missing from getSchemaTree()");
        }
    }

    /**
     * the table list holds the two seeded tables, and the seeded view only when
     * views are asked for.
     */
    @Test
    public void theTableListSeparatesTablesFromViews() throws Exception {
        try (DBMeta meta = openMeta()) {
            DBSchema schema = seededSchema(meta.getSchemas());

            List<String> tables = names(meta.getTables(schema, false));
            assertTrue(tables.contains(stored(CUSTOMER)), () -> stored(CUSTOMER) + " is missing: " + tables);
            assertTrue(tables.contains(stored(ORDER_ITEM)), () -> stored(ORDER_ITEM) + " is missing: " + tables);
            assertFalse(tables.contains(stored(CUSTOMER_VIEW)),
                    "the view must not be listed when views are not asked for");

            List<String> withViews = names(meta.getTables(schema, true));
            assertTrue(withViews.contains(stored(CUSTOMER_VIEW)),
                    () -> stored(CUSTOMER_VIEW) + " is missing: " + withViews);
            assertTrue(withViews.contains(stored(CUSTOMER)));
            assertTrue(withViews.contains(stored(ORDER_ITEM)));
        }
    }

    /**
     * the columns of a table, and with them the primary key flags and the
     * key/non-key split the templates iterate over.
     */
    @Test
    public void theColumnsCarryThePrimaryKeyFlags() throws Exception {
        try (DBMeta meta = openMeta()) {
            DBSchema schema = seededSchema(meta.getSchemas());
            List<DBTable> tables = meta.getTables(schema, false);

            DBTable customer = table(tables, CUSTOMER);
            List<DBColumn> columns = meta.getTableColumns(customer);
            assertEquals(stored(CUSTOMER_COLUMNS), columnNames(columns));
            assertEquals(stored(Arrays.asList("customer_id")), columnNames(customer.getKeys()));
            assertEquals(stored(Arrays.asList("customer_name", "email")),
                    columnNames(customer.getNotKeys()));

            DBTable orderItem = table(tables, ORDER_ITEM);
            List<DBColumn> itemColumns = meta.getTableColumns(orderItem);
            assertEquals(stored(ORDER_ITEM_COLUMNS), columnNames(itemColumns));

            // Both columns of the composite key are flagged, and only those,
            // in the order the key was declared in. DatabaseMetaData.getPrimaryKeys()
            // reports them by column name - 'line_no' before 'order_id' -, so
            // DBMeta has to put them back into their KEY_SEQ order for the
            // templates that build a 'where' clause out of the key list.
            assertEquals(stored(ORDER_ITEM_KEYS), columnNames(orderItem.getKeys()));
            assertEquals(stored(Arrays.asList("product_name", "quantity")),
                    columnNames(orderItem.getNotKeys()));
            for (DBColumn c: itemColumns)
                assertEquals(stored(ORDER_ITEM_KEYS).contains(c.getColumn()), c.isKey(),
                        "wrong key flag on " + c.getColumn());

            // every column is in exactly one of the two lists
            assertEquals(itemColumns.size(),
                    orderItem.getKeys().size() + orderItem.getNotKeys().size());
        }
    }

    /**
     * the table and column comments of the seed, as the driver definition
     * reports them - through the driver metadata, or through the custom comment
     * queries of the definition.
     */
    @Test
    public void theCommentsOfTheSeedAreReported() throws Exception {
        try (DBMeta meta = openMeta()) {
            DBSchema schema = seededSchema(meta.getSchemas());
            List<DBTable> tables = meta.getTables(schema, false);

            DBTable customer = table(tables, CUSTOMER);
            assertEquals(CUSTOMER_REMARKS, customer.getRemarks(), "table comment");
            assertEquals(ORDER_ITEM_REMARKS, table(tables, ORDER_ITEM).getRemarks(),
                    "table comment");

            meta.getTableColumns(customer);
            assertEquals(CUSTOMER_ID_REMARKS,
                    column(customer, "customer_id").getRemarks(), "column comment");
            assertEquals(CUSTOMER_NAME_REMARKS,
                    column(customer, "customer_name").getRemarks(), "column comment");
        }
    }

    // ----------------------------------------------------------------- helpers

    /**
     * open the seeded database the way the application does: the stock driver
     * definition, with the jar path pointing at the driver jar of this build.
     */
    protected DBMeta openMeta() throws Exception {
        JDBDriver driver = stockDriver();
        driver.setJdbcJar(driverJar().getAbsolutePath());

        JDBConnection conn = new JDBConnection();
        conn.setName(getClass().getSimpleName());
        conn.setDriverType(driver.getName());
        conn.setConnectionUrl(connectionUrl());
        conn.setUserName(username());
        conn.setUserPassword(password());
        conn.setConnectionProps(connectionProps());
        return new DBMeta(driver, conn);
    }

    /**
     * the stock driver definition of this test, read out of the
     * <code>defaultConfig.json</code> that ships with the application. A fresh
     * copy every time, so that a test may change it without affecting another.
     */
    protected JDBDriver stockDriver() {
        String name = stockDriverName();
        for (JDBDriver driver: stockDrivers()) {
            if (name.equals(driver.getName()))
                return driver;
        }
        throw new IllegalStateException("no stock driver named '" + name +
                "' in defaultConfig.json");
    }

    /** every driver definition of the shipped default configuration. */
    private static List<JDBDriver> stockDrivers() {
        try (Reader r = new InputStreamReader(AbstractDatabaseIT.class
                .getResourceAsStream("/defaultConfig.json"), StandardCharsets.UTF_8)) {
            DefaultConfig conf = new Gson().fromJson(r,
                    new TypeToken<DefaultConfig>(){}.getType());
            assertNotNull(conf, "defaultConfig.json is empty");
            assertNotNull(conf.drivers, "defaultConfig.json declares no drivers");
            return conf.drivers;
        } catch (Exception e) {
            throw new IllegalStateException("cannot read defaultConfig.json", e);
        }
    }

    /** the part of <code>defaultConfig.json</code> the tests read. */
    private static final class DefaultConfig {
        private List<JDBDriver> drivers;
    }

    /** the driver jar copied into <code>build/int-drivers</code> by the build. */
    protected File driverJar() {
        String dir = System.getProperty(DRIVERS_DIR_PROPERTY);
        assertNotNull(dir, "the '" + DRIVERS_DIR_PROPERTY + "' system property is not set - " +
                "run the tests with the 'integrationTest' task");
        File[] jars = new File(dir).listFiles((d, n) ->
                n.startsWith(driverJarPrefix()) && n.endsWith(".jar"));
        assertTrue(jars != null && jars.length == 1,
                "expected exactly one '" + driverJarPrefix() + "*.jar' in " + dir +
                        ", found " + (jars == null ? "no directory" : Arrays.toString(jars)));
        return jars[0];
    }

    /** the names of the given tables. */
    protected static List<String> names(List<DBTable> tables) {
        return tables.stream().map(DBTable::getTable).collect(Collectors.toList());
    }

    /** the names of the given columns. */
    protected static List<String> columnNames(List<DBColumn> columns) {
        return columns.stream().map(DBColumn::getColumn).collect(Collectors.toList());
    }

    /** the seeded table of the given logical name, failing when it is missing. */
    protected DBTable table(List<DBTable> tables, String name) {
        String stored = stored(name);
        return tables.stream().filter(t -> stored.equals(t.getTable())).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "table '" + stored + "' is missing from " + names(tables)));
    }

    /** the column of the given logical name, failing when it is missing. */
    protected DBColumn column(DBTable table, String name) {
        String stored = stored(name);
        return table.getColumns().stream().filter(c -> stored.equals(c.getColumn()))
                .findFirst().orElseThrow(() -> new AssertionError("column '" + stored +
                        "' is missing from " + columnNames(table.getColumns())));
    }

    /** the given identifiers the way the database stores them. */
    protected List<String> stored(List<String> names) {
        List<String> res = new ArrayList<>(names.size());
        for (String name: names)
            res.add(stored(name));
        return res;
    }

    /** an identifier folded to upper case, for the databases that do that. */
    protected static String upper(String name) {
        return name.toUpperCase(Locale.ROOT);
    }
}

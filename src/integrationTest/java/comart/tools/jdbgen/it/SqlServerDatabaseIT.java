package comart.tools.jdbgen.it;

import comart.tools.jdbgen.types.db.DBSchema;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The stock <em>Microsoft SQL Server</em> driver definition against SQL Server
 * 2022.
 *
 * <p>This is the definition with custom comment queries: SQL Server keeps
 * comments in extended properties, which the driver metadata does not report at
 * all, so the definition reads them with
 * <code>fn_listextendedproperty('MS_DESCRIPTION', ...)</code>. The seed writes
 * the comments the way SQL Server takes them,
 * <code>sp_addextendedproperty</code>, which is what makes
 * {@link #theCommentsOfTheSeedAreReported()} a test of those queries.</p>
 *
 * @author comart
 */
@Testcontainers
public class SqlServerDatabaseIT extends AbstractDatabaseIT {

    /** the schema the seed goes into, inside the default database. */
    private static final String SCHEMA = "jdbgen";

    @Container
    private static final MSSQLServerContainer<?> SQLSERVER =
            new MSSQLServerContainer<>(
                    DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
                    .acceptLicense()
                    .withStartupTimeout(Duration.ofMinutes(5));

    @Override
    protected JdbcDatabaseContainer<?> container() {
        return SQLSERVER;
    }

    @Override
    protected String stockDriverName() {
        return "Microsoft SQL Server";
    }

    @Override
    protected String driverJarPrefix() {
        return "mssql-jdbc-";
    }

    /**
     * the container has no database of its own, so the seed goes into a schema
     * of 'master' - which is also the database the custom comment queries of the
     * driver definition are executed in.
     */
    @Override
    protected String connectionUrl() {
        return "jdbc:sqlserver://" + SQLSERVER.getHost() + ":" +
                SQLSERVER.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT) +
                ";databaseName=master";
    }

    @Override
    protected List<String> seedStatements() {
        return Arrays.asList(
            "CREATE SCHEMA " + SCHEMA,
            "CREATE TABLE " + SCHEMA + "." + CUSTOMER + " (" +
                "customer_id INT NOT NULL, " +
                "customer_name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(200), " +
                "CONSTRAINT pk_customer PRIMARY KEY (customer_id))",
            "CREATE TABLE " + SCHEMA + "." + ORDER_ITEM + " (" +
                "order_id INT NOT NULL, " +
                "line_no INT NOT NULL, " +
                "product_name VARCHAR(100), " +
                "quantity INT, " +
                "CONSTRAINT pk_order_item PRIMARY KEY (order_id, line_no))",
            "CREATE VIEW " + SCHEMA + "." + CUSTOMER_VIEW + " AS " +
                "SELECT customer_id, customer_name FROM " + SCHEMA + "." + CUSTOMER,
            tableComment(CUSTOMER, CUSTOMER_REMARKS),
            tableComment(ORDER_ITEM, ORDER_ITEM_REMARKS),
            columnComment(CUSTOMER, "customer_id", CUSTOMER_ID_REMARKS),
            columnComment(CUSTOMER, "customer_name", CUSTOMER_NAME_REMARKS)
        );
    }

    /** the extended property SQL Server keeps a table comment in. */
    private static String tableComment(String table, String comment) {
        return "EXEC sp_addextendedproperty " +
                "@name = N'MS_Description', @value = N'" + comment + "', " +
                "@level0type = N'SCHEMA', @level0name = N'" + SCHEMA + "', " +
                "@level1type = N'TABLE', @level1name = N'" + table + "'";
    }

    /** the extended property SQL Server keeps a column comment in. */
    private static String columnComment(String table, String column, String comment) {
        return "EXEC sp_addextendedproperty " +
                "@name = N'MS_Description', @value = N'" + comment + "', " +
                "@level0type = N'SCHEMA', @level0name = N'" + SCHEMA + "', " +
                "@level1type = N'TABLE', @level1name = N'" + table + "', " +
                "@level2type = N'COLUMN', @level2name = N'" + column + "'";
    }

    @Override
    protected DBSchema seededSchema(List<DBSchema> schemas) {
        return schemas.stream()
                .filter(s -> SCHEMA.equals(s.getSchema()) && "master".equals(s.getCatalog()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("schema '" + SCHEMA +
                        "' of 'master' is missing from " + schemas));
    }
}

package comart.tools.jdbgen.it;

import comart.tools.jdbgen.types.db.DBSchema;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The stock <em>MySQL</em> driver definition against a MySQL 8 server.
 *
 * <p>MySQL has no schemas below a database: it reports every database as a
 * catalog and nothing as a schema, so DBMeta falls back to a schema entry that
 * only carries the catalog name.</p>
 *
 * <p>Connector/J only reports table comments when it reads its metadata out of
 * <code>information_schema</code>, which is not its default. The stock driver
 * definition therefore carries <code>useInformationSchema=true</code> among its
 * properties, and this test takes it from there rather than adding one of its
 * own - so a stock definition that loses the entry fails the comment
 * assertion.</p>
 *
 * @author comart
 */
@Testcontainers
public class MySqlDatabaseIT extends AbstractDatabaseIT {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withStartupTimeout(Duration.ofMinutes(5));

    @Override
    protected JdbcDatabaseContainer<?> container() {
        return MYSQL;
    }

    @Override
    protected String stockDriverName() {
        return "MySQL";
    }

    @Override
    protected String driverJarPrefix() {
        return "mysql-connector-j-";
    }

    @Override
    protected String connectionUrl() {
        return "jdbc:mysql://" + MYSQL.getHost() + ":" +
                MYSQL.getMappedPort(MySQLContainer.MYSQL_PORT) + "/" +
                MYSQL.getDatabaseName();
    }

    /**
     * the stock properties are the only ones this connection needs; without
     * <code>useInformationSchema</code> among them Connector/J reads its
     * metadata with <code>SHOW</code> statements, which report column comments
     * but no table comment at all.
     */
    @Test
    public void theStockPropertiesAskConnectorJForTheInformationSchema() {
        assertEquals("true", connectionProps().get("useInformationSchema"),
                "the stock MySQL definition has to carry useInformationSchema");
    }

    @Override
    protected List<String> seedStatements() {
        return Arrays.asList(
            "CREATE TABLE " + CUSTOMER + " (" +
                "customer_id INT NOT NULL COMMENT '" + CUSTOMER_ID_REMARKS + "', " +
                "customer_name VARCHAR(100) NOT NULL COMMENT '" + CUSTOMER_NAME_REMARKS + "', " +
                "email VARCHAR(200), " +
                "PRIMARY KEY (customer_id)) COMMENT='" + CUSTOMER_REMARKS + "'",
            "CREATE TABLE " + ORDER_ITEM + " (" +
                "order_id INT NOT NULL, " +
                "line_no INT NOT NULL, " +
                "product_name VARCHAR(100), " +
                "quantity INT, " +
                "PRIMARY KEY (order_id, line_no)) COMMENT='" + ORDER_ITEM_REMARKS + "'",
            "CREATE VIEW " + CUSTOMER_VIEW + " AS " +
                "SELECT customer_id, customer_name FROM " + CUSTOMER
        );
    }

    /**
     * the database the seed went into, which MySQL reports as a catalog without
     * schemas - DBMeta represents it by a schema entry carrying only the catalog
     * name.
     */
    @Override
    protected DBSchema seededSchema(List<DBSchema> schemas) {
        String database = MYSQL.getDatabaseName();
        return schemas.stream().filter(s -> database.equals(s.getCatalog())).findFirst()
                .orElseThrow(() -> new AssertionError("the catalog '" + database +
                        "' is missing from " + schemas));
    }
}

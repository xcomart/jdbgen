package comart.tools.jdbgen.it;

import comart.tools.jdbgen.types.db.DBSchema;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The stock <em>MariaDB</em> driver definition against a MariaDB 11 server.
 * Like MySQL, MariaDB reports databases as catalogs and has no schemas below
 * them; unlike Connector/J, the MariaDB driver reads its metadata out of
 * <code>information_schema</code> on its own, so table comments arrive without
 * an extra connection property.
 *
 * @author comart
 */
@Testcontainers
public class MariaDbDatabaseIT extends AbstractDatabaseIT {

    @Container
    private static final MariaDBContainer<?> MARIADB =
            new MariaDBContainer<>(DockerImageName.parse("mariadb:11"))
                    .withStartupTimeout(Duration.ofMinutes(5));

    @Override
    protected JdbcDatabaseContainer<?> container() {
        return MARIADB;
    }

    @Override
    protected String stockDriverName() {
        return "MariaDB";
    }

    @Override
    protected String driverJarPrefix() {
        return "mariadb-java-client-";
    }

    @Override
    protected String connectionUrl() {
        return "jdbc:mariadb://" + MARIADB.getHost() + ":" +
                MARIADB.getMappedPort(3306) + "/" +
                MARIADB.getDatabaseName();
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

    /** the database the seed went into, reported as a catalog without schemas. */
    @Override
    protected DBSchema seededSchema(List<DBSchema> schemas) {
        String database = MARIADB.getDatabaseName();
        return schemas.stream().filter(s -> database.equals(s.getCatalog())).findFirst()
                .orElseThrow(() -> new AssertionError("the catalog '" + database +
                        "' is missing from " + schemas));
    }
}

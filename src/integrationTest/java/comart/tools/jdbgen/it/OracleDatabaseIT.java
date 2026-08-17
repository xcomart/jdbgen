package comart.tools.jdbgen.it;

import comart.tools.jdbgen.types.db.DBMeta;
import comart.tools.jdbgen.types.db.DBSchema;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stock <em>Oracle</em> driver definition against Oracle Database 23 Free.
 * Oracle folds unquoted identifiers to upper case, and it reports comments only
 * when the connection asks for them - which is what the
 * <code>remarksReporting</code> property of the stock definition does.
 *
 * @author comart
 */
@Testcontainers
public class OracleDatabaseIT extends AbstractDatabaseIT {

    @Container
    private static final OracleContainer ORACLE =
            new OracleContainer(DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart"))
                    .withStartupTimeout(Duration.ofMinutes(10));

    @Override
    protected JdbcDatabaseContainer<?> container() {
        return ORACLE;
    }

    @Override
    protected String stockDriverName() {
        return "Oracle";
    }

    @Override
    protected String driverJarPrefix() {
        return "ojdbc11-";
    }

    /**
     * the pluggable database is addressed by its service name, which is the
     * form the URL template of the stock definition is written in.
     */
    @Override
    protected String connectionUrl() {
        return "jdbc:oracle:thin:@//" + ORACLE.getHost() + ":" +
                ORACLE.getMappedPort(1521) + "/" + ORACLE.getDatabaseName();
    }

    /** Oracle stores unquoted identifiers in upper case. */
    @Override
    protected String stored(String name) {
        return upper(name);
    }

    @Override
    protected List<String> seedStatements() {
        return Arrays.asList(
            "CREATE TABLE " + CUSTOMER + " (" +
                "customer_id NUMBER(10) NOT NULL, " +
                "customer_name VARCHAR2(100) NOT NULL, " +
                "email VARCHAR2(200), " +
                "CONSTRAINT pk_customer PRIMARY KEY (customer_id))",
            "CREATE TABLE " + ORDER_ITEM + " (" +
                "order_id NUMBER(10) NOT NULL, " +
                "line_no NUMBER(10) NOT NULL, " +
                "product_name VARCHAR2(100), " +
                "quantity NUMBER(10), " +
                "CONSTRAINT pk_order_item PRIMARY KEY (order_id, line_no))",
            "CREATE VIEW " + CUSTOMER_VIEW + " AS " +
                "SELECT customer_id, customer_name FROM " + CUSTOMER,
            "COMMENT ON TABLE " + CUSTOMER + " IS '" + CUSTOMER_REMARKS + "'",
            "COMMENT ON TABLE " + ORDER_ITEM + " IS '" + ORDER_ITEM_REMARKS + "'",
            "COMMENT ON COLUMN " + CUSTOMER + ".customer_id IS '" + CUSTOMER_ID_REMARKS + "'",
            "COMMENT ON COLUMN " + CUSTOMER + ".customer_name IS '" + CUSTOMER_NAME_REMARKS + "'"
        );
    }

    /**
     * Oracle has no catalogs and its driver answers <code>getCatalogs()</code>
     * with an empty result set, so DBMeta asks for the schemas without one and
     * groups them under the <code>"Default Catalog"</code> placeholder. The
     * seed went into the schema of the connected user.
     */
    @Override
    protected DBSchema seededSchema(List<DBSchema> schemas) {
        String owner = stored(username());
        return schemas.stream().filter(s -> owner.equals(s.getSchema())).findFirst()
                .orElseThrow(() -> new AssertionError("the schema '" + owner +
                        "' is missing from " + schemas));
    }

    /**
     * a database without catalogs still reports its schemas: the schema list is
     * the users of the database, grouped under the catalog placeholder, rather
     * than the single <code>"Default Schema"</code> entry that carries neither
     * a catalog nor a schema name.
     */
    @Test
    public void theSchemasOfADatabaseWithoutCatalogsAreReported() throws Exception {
        try (DBMeta meta = openMeta()) {
            List<DBSchema> schemas = meta.getSchemas();

            assertTrue(schemas.size() > 1,
                    "an Oracle database has more than one schema: " + schemas);
            assertTrue(schemas.stream().noneMatch(s -> "Default Schema".equals(s.getName())),
                    "the placeholder is only for a database that reports nothing: " + schemas);
            assertTrue(schemas.stream().anyMatch(s -> stored("sys").equals(s.getSchema())),
                    "SYS is missing from " + schemas);

            Map<String, List<DBSchema>> tree = meta.getSchemaTree();
            assertEquals(new HashSet<>(Arrays.asList("Default Catalog")), tree.keySet(),
                    "the schemas of a catalog-less database go under one placeholder");
            assertEquals(schemas.size(), tree.get("Default Catalog").size());
        }
    }
}

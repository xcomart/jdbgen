package comart.tools.jdbgen.it;

import comart.tools.jdbgen.template.TemplateManager;
import comart.tools.jdbgen.types.JDBGenConfig;
import comart.tools.jdbgen.types.db.DBMeta;
import comart.tools.jdbgen.types.db.DBSchema;
import comart.tools.jdbgen.types.db.DBTable;
import comart.utils.AppDirs;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stock <em>PostgreSQL</em> driver definition against a PostgreSQL 16
 * server. PostgreSQL reports tables, views and comments through the driver
 * metadata, so the definition needs no custom queries.
 *
 * <p>This is also the database the generator is driven end to end on: the
 * shipped <code>java_model.java</code> template is applied to a table read out
 * of the container.</p>
 *
 * @author comart
 */
@Testcontainers
public class PostgresDatabaseIT extends AbstractDatabaseIT {

    /** the schema the seed goes into. */
    private static final String SCHEMA = "jdbgen";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withStartupTimeout(Duration.ofMinutes(5));

    @Override
    protected JdbcDatabaseContainer<?> container() {
        return POSTGRES;
    }

    @Override
    protected String stockDriverName() {
        return "PostgreSQL";
    }

    @Override
    protected String driverJarPrefix() {
        return "postgresql-";
    }

    @Override
    protected String connectionUrl() {
        return "jdbc:postgresql://" + POSTGRES.getHost() + ":" +
                POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" +
                POSTGRES.getDatabaseName();
    }

    @Override
    protected List<String> seedStatements() {
        return Arrays.asList(
            "CREATE SCHEMA " + SCHEMA,
            "CREATE TABLE " + SCHEMA + "." + CUSTOMER + " (" +
                "customer_id integer NOT NULL, " +
                "customer_name varchar(100) NOT NULL, " +
                "email varchar(200), " +
                "CONSTRAINT pk_customer PRIMARY KEY (customer_id))",
            "CREATE TABLE " + SCHEMA + "." + ORDER_ITEM + " (" +
                "order_id integer NOT NULL, " +
                "line_no integer NOT NULL, " +
                "product_name varchar(100), " +
                "quantity integer, " +
                "CONSTRAINT pk_order_item PRIMARY KEY (order_id, line_no))",
            "CREATE VIEW " + SCHEMA + "." + CUSTOMER_VIEW + " AS " +
                "SELECT customer_id, customer_name FROM " + SCHEMA + "." + CUSTOMER,
            "COMMENT ON TABLE " + SCHEMA + "." + CUSTOMER + " IS '" + CUSTOMER_REMARKS + "'",
            "COMMENT ON TABLE " + SCHEMA + "." + ORDER_ITEM + " IS '" + ORDER_ITEM_REMARKS + "'",
            "COMMENT ON COLUMN " + SCHEMA + "." + CUSTOMER + ".customer_id IS '" +
                CUSTOMER_ID_REMARKS + "'",
            "COMMENT ON COLUMN " + SCHEMA + "." + CUSTOMER + ".customer_name IS '" +
                CUSTOMER_NAME_REMARKS + "'"
        );
    }

    /**
     * pgjdbc reports every database of the server as a catalog but can only
     * look into the connected one, so the same schema list comes back for each
     * of them - the first entry named like the seeded schema is the seeded one.
     */
    @Override
    protected DBSchema seededSchema(List<DBSchema> schemas) {
        return schemas.stream().filter(s -> SCHEMA.equals(s.getSchema())).findFirst()
                .orElseThrow(() -> new AssertionError("schema '" + SCHEMA +
                        "' is missing from " + schemas));
    }

    // ------------------------------------------------------- generator end run

    /**
     * the configuration singleton the template engine reads its options from.
     * It is built from the shipped defaults, in a data directory of its own so
     * that the configuration of the user running the build is left alone.
     */
    @BeforeAll
    public void useTheDefaultConfiguration() throws Exception {
        Path data = Files.createTempDirectory("jdbgen-it-config");
        data.toFile().deleteOnExit();
        System.setProperty(AppDirs.DATA_DIR_PROPERTY, data.toString());
        JDBGenConfig.getInstance(true);
    }

    /**
     * the whole way from the database to a generated source file: the columns
     * read through DBMeta are handed to the shipped <code>java_model.java</code>
     * template, which has to render the class, its fields and the comments of
     * the seeded table.
     */
    @Test
    public void theShippedTemplateIsRenderedFromTheDatabase() throws Exception {
        File template = new File("templates/java_model.java");
        assertTrue(template.isFile(), "the shipped template " + template.getAbsolutePath() +
                " is missing - the test has to run with the project directory as its " +
                "working directory");
        String text = new String(Files.readAllBytes(template.toPath()), StandardCharsets.UTF_8);

        String generated;
        try (DBMeta meta = openMeta()) {
            DBTable customer = table(meta.getTables(seededSchema(meta.getSchemas()), false),
                    CUSTOMER);
            meta.getTableColumns(customer);

            Map<String, String> customVars = new HashMap<>();
            customVars.put("author", "Integration Test");
            generated = new TemplateManager(text, customVars).applyMapper(customer);
        }

        assertContains(generated, "public class CustomerModel");
        assertContains(generated, "@Alias(\"customer\")");
        // the table comment, used as the class description
        assertContains(generated, CUSTOMER_REMARKS);
        // the key column, from ${for:item=keys}
        assertContains(generated, "customerId");
        assertContains(generated, CUSTOMER_ID_REMARKS);
        // the remaining columns, from ${for:item=notKeys}
        assertContains(generated, "customerName");
        assertContains(generated, "email");
        assertContains(generated, CUSTOMER_NAME_REMARKS);
        // nothing of the template must be left unresolved
        assertTrue(!generated.contains("${"), "unresolved template variable in:\n" + generated);
    }

    /** assert that the generated source holds the given text. */
    private static void assertContains(String generated, String expected) {
        assertTrue(generated.contains(expected),
                "'" + expected + "' is missing from the generated source:\n" + generated);
    }
}

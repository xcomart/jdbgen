package comart.tools.jdbgen.types;

import comart.tools.jdbgen.types.maven.SearchResponseItem;
import comart.utils.StrUtils;
import comart.utils.AppDirs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Maven coordinates the shipped drivers carry. They are what turns the
 * download button of the driver manager into a single click, so a coordinate
 * that is not a coordinate would send the user back to the search dialog -
 * or, worse, download nothing at all.
 */
public class StockDriverArtifactsTest {

    /** the one shipped driver whose driver is not published on Maven Central. */
    private static final String NO_ARTIFACT = "CUBRID";

    private static List<JDBDriver> bundledDrivers() throws Exception {
        List<JDBDriver> drivers = JDBGenConfig.loadBundledDefaults().getDrivers();
        assertNotNull(drivers, "the bundled configuration ships the drivers");
        assertTrue(drivers.size() > 1);
        return drivers;
    }

    @Test
    public void everyShippedDriverOnMavenCentralNamesItsArtifact() throws Exception {
        for (JDBDriver d: bundledDrivers()) {
            assertTrue(d.isStockItem(), d.getName() + " is shipped with the release");
            if (NO_ARTIFACT.equals(d.getName())) {
                assertTrue(StrUtils.isEmpty(d.getMavenArtifact()),
                        d.getName() + " has no artifact on Maven Central");
                assertTrue(!StrUtils.isEmpty(d.getDefaultQuery()),
                        d.getName() + " has to fall back to the search dialog");
                continue;
            }
            SearchResponseItem item = SearchResponseItem.ofCoordinate(d.getMavenArtifact());
            assertNotNull(item, d.getName() + " carries '" + d.getMavenArtifact()
                    + "', which is no 'groupId:artifactId:version'");
            assertTrue(item.getFilePath().endsWith(".jar"));
            // the search dialog stays reachable for the drivers that have one
            assertTrue(!StrUtils.isEmpty(d.getDefaultQuery()));
        }
    }

    @Test
    public void aCoordinateNamesTheJarOfTheRepositoryLayout() {
        SearchResponseItem item = SearchResponseItem.ofCoordinate(
                "org.xerial:sqlite-jdbc:3.53.2.1");
        assertNotNull(item);
        assertEquals("org/xerial/sqlite-jdbc/3.53.2.1/sqlite-jdbc-3.53.2.1.jar",
                item.getFilePath());
    }

    /**
     * the SQL Server driver names the JDK it was built for in its version, so
     * a coordinate is not allowed to be cut at the last dot anywhere.
     */
    @Test
    public void theSqlServerVersionKeepsItsJreSuffix() {
        SearchResponseItem item = SearchResponseItem.ofCoordinate(
                "com.microsoft.sqlserver:mssql-jdbc:13.4.0.jre11");
        assertNotNull(item);
        assertTrue(item.getFilePath().endsWith("/mssql-jdbc-13.4.0.jre11.jar"),
                item.getFilePath());
    }

    @Test
    public void anythingThatIsNoCoordinateIsNone() {
        for (String s: new String[]{null, "", "   ", "sqlite-jdbc", "g:a",
                "g:a:v:extra", "g::v", ":a:v", "g:a:"})
            assertNull(SearchResponseItem.ofCoordinate(s), "'" + s + "'");
    }

    /**
     * a configuration written before the coordinates existed: the shipped
     * drivers get theirs, everything the user owns is left alone.
     */
    @Test
    public void anOlderConfigurationGetsTheCoordinatesOfTheShippedDrivers() {
        JDBDriver stock = JDBDriver.builder().name("SQLite").stockItem(true).build();
        JDBDriver edited = JDBDriver.builder().name("PostgreSQL").stockItem(true)
                .mavenArtifact("org.postgresql:postgresql:9.9.9").build();
        JDBDriver own = JDBDriver.builder().name("MySQL").stockItem(false).build();
        JDBDriver unknown = JDBDriver.builder().name("Informix").stockItem(true).build();
        List<JDBDriver> drivers = new ArrayList<>();
        drivers.add(stock);
        drivers.add(edited);
        drivers.add(own);
        drivers.add(unknown);

        JDBGenConfig.fillStockMavenArtifacts(drivers);

        assertEquals("org.xerial:sqlite-jdbc:3.53.2.1", stock.getMavenArtifact());
        assertEquals("org.postgresql:postgresql:9.9.9", edited.getMavenArtifact(),
                "a coordinate the user changed is never overwritten");
        assertNull(own.getMavenArtifact(), "a driver of the user's own is left alone");
        assertNull(unknown.getMavenArtifact());
    }

    /**
     * the shipped H2 driver names the jar bundled below the installation, and
     * the file name of that jar carries the version of its Maven coordinate -
     * build.gradle copies exactly that artifact into 'drivers/'.
     */
    @Test
    public void theShippedH2DriversNameTheBundledJar() throws Exception {
        int seen = 0;
        for (JDBDriver d: bundledDrivers()) {
            if (!"org.h2.Driver".equals(d.getDriverClass()))
                continue;
            seen++;
            SearchResponseItem item = SearchResponseItem.ofCoordinate(d.getMavenArtifact());
            assertNotNull(item, d.getName());
            assertEquals("drivers/" + item.getA() + "-" + item.getV() + ".jar", d.getJdbcJar(),
                    d.getName() + " names the jar of its own coordinate");
        }
        assertEquals(2, seen, "H2 Embedded and H2 Server");
    }

    /**
     * a configuration without a jar for a shipped driver picks up the bundled
     * one - but only when the installation actually carries it.
     */
    @Test
    public void aShippedDriverWithoutAJarTakesTheBundledOne(@TempDir Path install) throws Exception {
        // both places a relative jar path is looked up in are redirected, the
        // user data directory of this machine may well hold a downloaded H2
        System.setProperty(AppDirs.RESOURCE_BASE_PROPERTY, install.resolve("install").toString());
        System.setProperty(AppDirs.DATA_DIR_PROPERTY, install.resolve("data").toString());
        try {
            JDBDriver embedded = JDBDriver.builder().name("H2 Embedded").stockItem(true).build();
            JDBDriver own = JDBDriver.builder().name("H2 Server").stockItem(true)
                    .jdbcJar("drivers/h2-2.3.232.jar").build();
            List<JDBDriver> drivers = new ArrayList<>();
            drivers.add(embedded);
            drivers.add(own);

            JDBGenConfig.fillStockMavenArtifacts(drivers);
            assertNull(embedded.getJdbcJar(), "an installation without the jar leaves it empty");

            String bundled = bundledDrivers().stream()
                    .filter(d -> "H2 Embedded".equals(d.getName()))
                    .findFirst().get().getJdbcJar();
            Path jar = install.resolve("install").resolve(bundled);
            Files.createDirectories(jar.getParent());
            Files.write(jar, new byte[]{ 'P', 'K' });

            JDBGenConfig.fillStockMavenArtifacts(drivers);
            assertEquals(bundled, embedded.getJdbcJar());
            assertEquals("drivers/h2-2.3.232.jar", own.getJdbcJar(),
                    "a jar the user has is never replaced");
        } finally {
            System.clearProperty(AppDirs.RESOURCE_BASE_PROPERTY);
            System.clearProperty(AppDirs.DATA_DIR_PROPERTY);
        }
    }

    @Test
    public void anEmptyDriverListIsNoFailure() {
        JDBGenConfig.fillStockMavenArtifacts(null);
        JDBGenConfig.fillStockMavenArtifacts(new ArrayList<>());
    }
}

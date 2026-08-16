package comart.tools.jdbgen.types;

import comart.tools.jdbgen.types.maven.SearchResponseItem;
import comart.utils.StrUtils;
import java.util.ArrayList;
import java.util.List;
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

    @Test
    public void anEmptyDriverListIsNoFailure() {
        JDBGenConfig.fillStockMavenArtifacts(null);
        JDBGenConfig.fillStockMavenArtifacts(new ArrayList<>());
    }
}

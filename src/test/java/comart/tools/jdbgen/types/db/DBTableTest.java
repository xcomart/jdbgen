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

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The table list drives the tree of the main window, which only tells tables
 * and views apart - so whatever a database calls its table types has to end up
 * as one of those two.
 */
public class DBTableTest {

    /** one row of a <code>getTables()</code> result set. */
    private static MapResultSet row(String type) {
        return new MapResultSet()
                .with("TABLE_CAT", "TESTDB")
                .with("TABLE_SCHEM", "PUBLIC")
                .with("TABLE_NAME", "TB_USER")
                .with("TABLE_TYPE", type)
                .with("REMARKS", "the users");
    }

    @Test
    public void everyColumnOfTheResultSetIsMapped() throws Exception {
        DBTable table = new DBTable(row("TABLE"));

        assertEquals("TESTDB", table.getCatalog());
        assertEquals("PUBLIC", table.getSchema());
        assertEquals("TB_USER", table.getTable());
        assertEquals("TB_USER", table.getName(), "the display name starts as the table name");
        assertEquals("TABLE", table.getType());
        assertEquals("the users", table.getRemarks());
        assertNull(table.getColumns(), "the columns are read on demand");
        assertNull(table.getKeys());
        assertNull(table.getNotKeys());
    }

    @ParameterizedTest
    @CsvSource({
        "TABLE, TABLE",
        "VIEW, VIEW",
        "SYSTEM TABLE, TABLE",
        "GLOBAL TEMPORARY TABLE, TABLE",
        "BASE TABLE, TABLE",
        "MATERIALIZED VIEW, VIEW",
        "SYSTEM VIEW, VIEW"
    })
    public void aCompoundTableTypeIsReducedToTheKindItHolds(String reported, String expected)
            throws Exception {
        assertEquals(expected, new DBTable(row(reported)).getType());
    }

    @Test
    public void aCustomQueryMayOmitTheTableType() throws Exception {
        assertEquals("TABLE", new DBTable(row(null)).getType(),
                "a custom table query usually only returns tables");
    }

    @Test
    public void aTypeThatIsNeitherATableNorAViewIsLeftAsItIs() throws Exception {
        // nothing to map it onto; the tree simply does not show it
        assertEquals("SEQUENCE", new DBTable(row("SEQUENCE")).getType());
    }

    @Test
    public void theTitleIsTheTableNameAsTheDatabaseReportsIt() throws Exception {
        DBTable table = new DBTable(row("TABLE"));
        table.setName("User");

        assertEquals("TB_USER", table.getTitle(),
                "renaming the model must not rename the entry of the table list");
    }

    @ParameterizedTest
    @ValueSource(strings = {"TABLE", "VIEW"})
    public void theIconIsAFontAwesomeGlyph(String type) throws Exception {
        assertTrue(new DBTable(row(type)).getIcon().startsWith("fa:"));
    }

    @Test
    public void aViewIsShownWithAnotherIconThanATable() throws Exception {
        assertNotEquals(new DBTable(row("TABLE")).getIcon(), new DBTable(row("VIEW")).getIcon());
    }

    @Test
    public void aTableTheQueryDoesNotDescribeAtAllIsReported() {
        MapResultSet rs = new MapResultSet()
                .with("TABLE_CAT", "TESTDB")
                .with("TABLE_SCHEM", "PUBLIC")
                .with("TABLE_NAME", "TB_USER");

        assertThrows(SQLException.class, () -> new DBTable(rs),
                "a query missing a required column has to say so");
    }

    @Test
    public void aTableDescribesItselfForTheLog() throws Exception {
        DBTable table = new DBTable(row("TABLE"));

        assertTrue(table.toString().contains("TB_USER"), table.toString());
        assertTrue(table.toString().contains("the users"), table.toString());
    }
}

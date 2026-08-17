package comart.tools.jdbgen.ui;

import comart.tools.jdbgen.types.db.DBTable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The table list filter of the main window narrows the list box down to the
 * tables whose name contains the typed text. The matching itself is pure -
 * no window involved - so it is checked here directly against
 * {@link JDBGeneratorMain#filterTables(List, String)}.
 */
public class TableListFilterTest {

    private static DBTable table(String name) {
        DBTable t = new DBTable();
        t.setTable(name);
        t.setName(name);
        return t;
    }

    private static List<DBTable> tablesOf(String... names) {
        List<DBTable> res = new ArrayList<>();
        for (String name: names)
            res.add(table(name));
        return res;
    }

    private static List<String> names(List<DBTable> tables) {
        List<String> res = new ArrayList<>();
        for (DBTable t: tables)
            res.add(t.getTable());
        return res;
    }

    @Test
    public void aBlankFilterKeepsEveryTableInOrder() {
        List<DBTable> tables = tablesOf("USERS", "ORDERS", "PRODUCTS");

        assertEquals(Arrays.asList("USERS", "ORDERS", "PRODUCTS"),
                names(JDBGeneratorMain.filterTables(tables, "")));
        assertEquals(Arrays.asList("USERS", "ORDERS", "PRODUCTS"),
                names(JDBGeneratorMain.filterTables(tables, null)));
    }

    @Test
    public void aNullTableListFiltersToNothing() {
        assertTrue(JDBGeneratorMain.filterTables(null, "any").isEmpty());
        assertTrue(JDBGeneratorMain.filterTables(null, null).isEmpty());
    }

    @Test
    public void onlyNamesContainingTheFilterTextSurvive() {
        List<DBTable> tables = tablesOf("USERS", "ORDERS", "PRODUCTS");

        assertEquals(Arrays.asList("USERS", "ORDERS"),
                names(JDBGeneratorMain.filterTables(tables, "ers")));
        assertEquals(Arrays.asList("PRODUCTS"),
                names(JDBGeneratorMain.filterTables(tables, "prod")));
    }

    @Test
    public void theMatchIsCaseInsensitive() {
        List<DBTable> tables = tablesOf("Users", "Orders");

        assertEquals(Arrays.asList("Users"),
                names(JDBGeneratorMain.filterTables(tables, "USE")));
        assertEquals(Arrays.asList("Users"),
                names(JDBGeneratorMain.filterTables(tables, "use")));
    }

    @Test
    public void aFilterThatMatchesNothingYieldsAnEmptyListNotNull() {
        List<DBTable> tables = tablesOf("USERS", "ORDERS");

        assertTrue(JDBGeneratorMain.filterTables(tables, "zzz").isEmpty());
    }

    @Test
    public void theOriginalListIsNeverModified() {
        List<DBTable> tables = tablesOf("USERS", "ORDERS");

        JDBGeneratorMain.filterTables(tables, "USER");

        assertEquals(2, tables.size());
    }
}

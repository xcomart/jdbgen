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
package comart.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The key/value table helpers of {@link UIUtils}. They work on the table model
 * alone - no component, no window - which is what the connection properties and
 * the custom variables of a connection are edited through.
 */
public class UIUtilsTableTest {

    private static DefaultTableModel model(Object[]... rows) {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"key", "value"}, 0);
        for (Object[] row: rows)
            model.addRow(row);
        return model;
    }

    private static List<String> keysOf(Map<String, String> map) {
        return new ArrayList<>(map.keySet());
    }

    @Test
    public void aTableIsReadIntoAMapInTheOrderOfItsRows() {
        DefaultTableModel model = model(
                new Object[]{"ssl", "true"},
                new Object[]{"charset", "utf8"},
                new Object[]{"applicationName", "jdbgen"});

        Map<String, String> props = UIUtils.applyTableToMap(model);

        assertEquals(3, props.size());
        assertEquals("true", props.get("ssl"));
        assertEquals("utf8", props.get("charset"));
        // the rows are handed to the driver in the order they were typed in
        assertEquals(java.util.Arrays.asList("ssl", "charset", "applicationName"),
                keysOf(props));
    }

    @Test
    public void aHalfFilledRowIsNoEntry() {
        DefaultTableModel model = model(
                new Object[]{"ssl", "true"},
                new Object[]{"", "orphan value"},
                new Object[]{"orphan key", ""},
                new Object[]{"   ", "   "},
                new Object[]{null, null});

        Map<String, String> props = UIUtils.applyTableToMap(model);

        assertEquals(1, props.size(), "only the complete row is an entry: " + props);
        assertEquals("true", props.get("ssl"));
        assertFalse(props.containsKey("orphan key"));
    }

    @Test
    public void aTableAlwaysEndsInARowToTypeInto() {
        DefaultTableModel model = model(new Object[]{"ssl", "true"});

        UIUtils.tableSetLastEmpty(model);

        assertEquals(2, model.getRowCount());
        assertEquals("", model.getValueAt(1, 0));
        assertEquals("", model.getValueAt(1, 1));

        // the empty row that is already there is not doubled
        UIUtils.tableSetLastEmpty(model);
        assertEquals(2, model.getRowCount());
    }

    @Test
    public void anEmptyTableIsGivenItsFirstRow() {
        DefaultTableModel model = model();

        UIUtils.tableSetLastEmpty(model);

        assertEquals(1, model.getRowCount());
        assertEquals("", model.getValueAt(0, 0));
    }

    @Test
    public void theCheckBoxColumnsBeforeTheKeyAreFilledIn() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"use", "name", "value"}, 0);
        model.addRow(new Object[]{Boolean.TRUE, "ssl", "true"});

        UIUtils.tableSetLastEmpty(model, 1);

        assertEquals(2, model.getRowCount());
        assertEquals(Boolean.FALSE, model.getValueAt(1, 0),
                "a check box column cannot hold the empty string");
        assertEquals("", model.getValueAt(1, 1));
        assertEquals("", model.getValueAt(1, 2));

        UIUtils.tableSetLastEmpty(model, 1);
        assertEquals(2, model.getRowCount());
    }

    @Test
    public void onlyTheLastRowDecidesWhetherAnotherOneIsNeeded() {
        // an empty row in the middle is left alone - the user is editing it
        DefaultTableModel model = model(
                new Object[]{"", ""},
                new Object[]{"ssl", "true"});

        UIUtils.tableSetLastEmpty(model);

        assertEquals(3, model.getRowCount());
        assertTrue(UIUtils.applyTableToMap(model).containsKey("ssl"));
    }
}

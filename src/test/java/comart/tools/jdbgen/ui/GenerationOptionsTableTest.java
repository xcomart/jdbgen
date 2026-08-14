package comart.tools.jdbgen.ui;

import comart.tools.jdbgen.types.JDBTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The template table of the main window is the only place the templates of a
 * connection are edited, so what is read out of it is exactly what is stored.
 * Both helpers work on the table model alone - no window involved.
 */
public class GenerationOptionsTableTest {

    private static DefaultTableModel model() {
        return new DefaultTableModel(
                new Object[]{"select", "name", "file", "out"}, 0);
    }

    @Test
    public void theTableIsFilledInTheOrderOfTheConnectionAndKeepsTheTicks() {
        DefaultTableModel m = model();
        // a leftover row of a previous connection must not survive
        m.addRow(new Object[]{Boolean.TRUE, "stale", "stale.tpl", "stale.java"});

        JDBGeneratorMain.fillTemplateTable(m, Arrays.asList(
                new JDBTemplate("model", "model.tpl", "${name}.java", true),
                new JDBTemplate("mapper", "mapper.tpl", "${name}.xml")));

        assertEquals(2, m.getRowCount());
        assertEquals(Boolean.TRUE, m.getValueAt(0, 0));
        assertEquals("model", m.getValueAt(0, 1));
        assertEquals("model.tpl", m.getValueAt(0, 2));
        assertEquals("${name}.java", m.getValueAt(0, 3));
        assertEquals(Boolean.FALSE, m.getValueAt(1, 0));
        assertEquals("mapper", m.getValueAt(1, 1));
    }

    @Test
    public void aNullTemplateListEmptiesTheTable() {
        DefaultTableModel m = model();
        m.addRow(new Object[]{Boolean.TRUE, "stale", "stale.tpl", "stale.java"});

        JDBGeneratorMain.fillTemplateTable(m, null);

        assertEquals(0, m.getRowCount());
    }

    @Test
    public void whatWasFilledInIsWhatIsReadBack() {
        List<JDBTemplate> tpls = Arrays.asList(
                new JDBTemplate("model", "model.tpl", "${name}.java", true),
                new JDBTemplate("mapper", "mapper.tpl", "${name}.xml"));
        DefaultTableModel m = model();

        JDBGeneratorMain.fillTemplateTable(m, tpls);
        List<JDBTemplate> back = JDBGeneratorMain.readTemplateTable(m);

        assertEquals(new ArrayList<>(tpls), back);
    }

    @Test
    public void aTickTypedIntoTheTableIsRead() {
        DefaultTableModel m = model();
        JDBGeneratorMain.fillTemplateTable(m,
                Arrays.asList(new JDBTemplate("model", "model.tpl", "${name}.java")));

        m.setValueAt(Boolean.TRUE, 0, 0);

        assertTrue(JDBGeneratorMain.readTemplateTable(m).get(0).isSelected());
    }

    @Test
    public void anEmptyRowIsNoTemplate() {
        DefaultTableModel m = model();
        m.addRow(new Object[]{null, null, null, null});
        m.addRow(new Object[]{Boolean.TRUE, "model", "model.tpl", "${name}.java"});
        m.addRow(new Object[]{Boolean.FALSE, "", "", ""});

        List<JDBTemplate> back = JDBGeneratorMain.readTemplateTable(m);

        assertEquals(1, back.size());
        assertEquals("model", back.get(0).getName());
    }

    @Test
    public void aRowWithoutAValueIsReadAsEmptyTextAndNotAsTheWordNull() {
        DefaultTableModel m = model();
        m.addRow(new Object[]{null, "model", null, null});

        JDBTemplate back = JDBGeneratorMain.readTemplateTable(m).get(0);

        assertEquals("", back.getTemplateFile());
        assertEquals("", back.getOutTemplate());
        assertFalse(back.isSelected(), "a missing tick is not a tick");
    }
}

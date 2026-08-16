package comart.tools.jdbgen.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import comart.utils.StrUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A configuration written by one run must be readable by the next one, where
 * the master password is entered again from scratch.
 */
public class ConfigRoundTripTest {

    @Test
    public void connectionSurvivesRestart() {
        StrUtils.setMaster("correct horse battery staple");

        JDBConnection conn = new JDBConnection();
        conn.setName("sample");
        conn.setUserName("scott");
        conn.setUserPassword("tiger");

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(conn);

        // a new run: same password, but a fresh session salt and empty caches
        StrUtils.setMaster("correct horse battery staple");

        JDBConnection back = new Gson().fromJson(json, JDBConnection.class);
        assertEquals("tiger", back.getUserPassword());
        assertEquals("scott", back.getUserName());
    }

    @Test
    public void emptyAndUnsetPasswordsSurviveRestart() {
        StrUtils.setMaster("pw");

        JDBConnection conn = new JDBConnection();
        conn.setName("sample");
        conn.setUserPassword("");

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(conn);

        StrUtils.setMaster("pw");

        JDBConnection back = new Gson().fromJson(json, JDBConnection.class);
        assertEquals("", back.getUserPassword());
    }

    /**
     * The templates a connection generates, including which of them the main
     * window has ticked: the ticks are what the next start offers again.
     */
    @Test
    public void theTemplatesAndTheirTicksSurviveRestart() {
        StrUtils.setMaster("pw");

        JDBConnection conn = new JDBConnection();
        conn.setName("sample");
        conn.setOutputDir("output");
        conn.setAuthor("scott");
        conn.setTemplates(java.util.Arrays.asList(
                new JDBTemplate("model", "templates/model.tpl", "${name.pascal}.java", true),
                new JDBTemplate("mapper", "templates/mapper.tpl", "${name.pascal}Mapper.xml")));

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(conn);

        StrUtils.setMaster("pw");

        JDBConnection back = new Gson().fromJson(json, JDBConnection.class);
        assertEquals(2, back.getTemplates().size());
        JDBTemplate model = back.getTemplates().get(0);
        assertEquals("model", model.getName());
        assertEquals("templates/model.tpl", model.getTemplateFile());
        assertEquals("${name.pascal}.java", model.getOutTemplate());
        assertTrue(model.isSelected(), "the ticked template comes back ticked");
        assertFalse(back.getTemplates().get(1).isSelected());
        assertEquals("output", back.getOutputDir());
        assertEquals("scott", back.getAuthor());
    }

    /**
     * A configuration written before the tick was stored has no such field. It
     * has to read back as "not ticked" instead of failing, otherwise an update
     * of jdbgen would lose every connection.
     */
    @Test
    public void aTemplateWithoutTheTickReadsAsUnticked() {
        JDBTemplate back = new Gson().fromJson(
                "{\"name\":\"model\",\"templateFile\":\"t.tpl\",\"outTemplate\":\"o.java\"}",
                JDBTemplate.class);

        assertEquals("model", back.getName());
        assertFalse(back.isSelected());
    }

    /**
     * Where the main window was and how it was divided: the next start has to
     * come up the same way.
     */
    @Test
    public void theWindowStateSurvivesRestart() {
        StrUtils.setMaster("pw");

        JDBGenConfig conf = new JDBGenConfig();
        WindowState state = new WindowState();
        state.setWidth(1200);
        state.setHeight(800);
        state.setX(-40);
        state.setY(120);
        state.setMaximized(true);
        state.setSchemaDivider(210);
        state.setOptionsDivider(640);
        conf.setMainWindow(state);

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(conf);

        StrUtils.setMaster("pw");

        WindowState back = new Gson().fromJson(json, JDBGenConfig.class).getMainWindow();
        assertNotNull(back);
        assertEquals(1200, back.getWidth());
        assertEquals(800, back.getHeight());
        assertEquals(-40, back.getX(), "a screen left of the primary one keeps its negative x");
        assertEquals(120, back.getY());
        assertTrue(back.isMaximized());
        assertEquals(210, back.getSchemaDivider());
        assertEquals(640, back.getOptionsDivider());
        assertTrue(back.hasBounds());
    }

    /**
     * A configuration written before the window state was stored has no such
     * entry. It has to read back as "nothing stored" so that the window keeps
     * its packed size and its default dividers.
     */
    @Test
    public void aConfigWithoutTheWindowStateKeepsTheDefaults() {
        WindowState fresh = new WindowState();
        assertFalse(fresh.hasBounds());
        assertFalse(fresh.isMaximized());
        assertTrue(fresh.getSchemaDivider() <= 0);
        assertTrue(fresh.getOptionsDivider() <= 0);

        WindowState back = new Gson().fromJson("{}", WindowState.class);
        assertFalse(back.hasBounds());
        assertTrue(back.getSchemaDivider() <= 0, "an absent divider must not be applied");
        assertTrue(back.getOptionsDivider() <= 0, "an absent divider must not be applied");
    }

    /**
     * The bundled default configuration has to survive the very first save,
     * otherwise no config.json is ever written and every launch asks to set up
     * a new master password again.
     */
    @Test
    public void bundledDefaultConfigCanBeSerialized() throws Exception {
        StrUtils.setMaster("pw");

        JDBGenConfig conf;
        try (InputStreamReader ir = new InputStreamReader(
                JDBGenConfig.class.getResourceAsStream("/defaultConfig.json"),
                StandardCharsets.UTF_8)) {
            conf = new Gson().fromJson(ir, JDBGenConfig.class);
        }
        assertNotNull(conf);

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(conf);
        assertTrue(json.length() > 0);

        // and it has to read back
        StrUtils.setMaster("pw");
        assertNotNull(new Gson().fromJson(json, JDBGenConfig.class));
    }
}

package comart.tools.jdbgen.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import comart.utils.StrUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

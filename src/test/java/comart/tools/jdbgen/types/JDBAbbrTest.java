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
package comart.tools.jdbgen.types;

import comart.utils.AppDirs;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The abbreviation rules are edited as a table and applied through two lookup
 * maps, so what the table holds and what the maps hold have to stay the same
 * thing.
 */
public class JDBAbbrTest {

    @BeforeAll
    public static void isolateConfiguration(@TempDir Path dir) {
        String data = System.getProperty(AppDirs.DATA_DIR_PROPERTY);
        String base = System.getProperty(AppDirs.RESOURCE_BASE_PROPERTY);
        System.setProperty(AppDirs.DATA_DIR_PROPERTY, dir.resolve("data").toString());
        System.setProperty(AppDirs.RESOURCE_BASE_PROPERTY, dir.resolve("install").toString());
        try {
            JDBGenConfig.getInstance(true);
        } finally {
            if (data == null) System.clearProperty(AppDirs.DATA_DIR_PROPERTY);
            else System.setProperty(AppDirs.DATA_DIR_PROPERTY, data);
            if (base == null) System.clearProperty(AppDirs.RESOURCE_BASE_PROPERTY);
            else System.setProperty(AppDirs.RESOURCE_BASE_PROPERTY, base);
        }
    }

    @AfterEach
    public void restoreConfiguration() {
        JDBGenConfig.getInstance(true).setAbbrs(new ArrayList<>());
        JDBAbbr.buildMap();
    }

    /** build the lookup maps out of <code>rules</code>. */
    private static void install(JDBAbbr... rules) {
        JDBGenConfig.getInstance(true).setAbbrs(new ArrayList<>(Arrays.asList(rules)));
        JDBAbbr.buildMap();
    }

    @Test
    public void aRuleIsShownAsOneRowOfTheAbbreviationTable() {
        JDBAbbr abbr = new JDBAbbr(true, false, "usr", "user");

        assertArrayEquals(new Object[]{ true, false, "usr", "user" }, abbr.getRowArray());
    }

    @Test
    public void aRuleDescribesItselfAsWhatItReplaces() {
        assertEquals("{usr:user}", new JDBAbbr(true, false, "usr", "user").toString());
    }

    @Test
    public void aWordRuleAndAWholeNameRuleGoIntoSeparateMaps() {
        install(new JDBAbbr(true, false, "usr", "user"),
                new JDBAbbr(true, true, "tb_usr", "userTable"));

        assertEquals("user", JDBAbbr.abbrMap.get("usr"));
        assertFalse(JDBAbbr.abbrMap.containsKey("tb_usr"));
        assertEquals("userTable", JDBAbbr.abbrNameMap.get("tb_usr"));
        assertFalse(JDBAbbr.abbrNameMap.containsKey("usr"));
    }

    @Test
    public void aRuleThatIsTurnedOffIsNotApplied() {
        install(new JDBAbbr(false, false, "usr", "user"),
                new JDBAbbr(null, false, "acct", "account"));

        assertTrue(JDBAbbr.abbrMap.isEmpty(), "an unchecked rule stays in the table but is unused");
        assertTrue(JDBAbbr.abbrNameMap.isEmpty());
    }

    @Test
    public void aRuleIsLookedUpIgnoringTheCase() {
        install(new JDBAbbr(true, false, "USR", "user"));

        assertEquals("user", JDBAbbr.abbrMap.get("usr"),
                "identifiers are lower cased before they are looked up");
    }

    @Test
    public void aRuleWithoutAScopeCountsAsAWordRule() {
        JDBAbbr abbr = new JDBAbbr(true, null, "usr", "user");
        install(abbr);

        assertEquals("user", JDBAbbr.abbrMap.get("usr"));
        assertEquals(Boolean.FALSE, abbr.getTotalName(),
                "the missing scope is filled in, so the table shows what is applied");
    }

    @Test
    public void theLastRuleOfADuplicateWins() {
        install(new JDBAbbr(true, false, "usr", "user"),
                new JDBAbbr(true, false, "usr", "person"));

        assertEquals("person", JDBAbbr.abbrMap.get("usr"));
    }

    @Test
    public void rebuildingTheMapsForgetsTheRulesThatWereRemoved() {
        install(new JDBAbbr(true, false, "usr", "user"));
        assertEquals(1, JDBAbbr.abbrMap.size());

        install(new JDBAbbr(true, false, "acct", "account"));

        assertFalse(JDBAbbr.abbrMap.containsKey("usr"), "the maps are built from scratch");
        assertEquals("account", JDBAbbr.abbrMap.get("acct"));
    }

    @Test
    public void aConfigurationWithoutAnyRuleLeavesTheMapsEmpty() {
        List<JDBAbbr> none = new ArrayList<>();
        JDBGenConfig.getInstance(true).setAbbrs(none);

        JDBAbbr.buildMap();

        assertTrue(JDBAbbr.abbrMap.isEmpty());
        assertTrue(JDBAbbr.abbrNameMap.isEmpty());
    }
}

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

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The list editors of the dialogs work on copies of what they edit, so that
 * cancelling an edit really leaves the configuration alone. That only holds as
 * far as the copies are deep enough.
 */
public class JDBListTypesTest {

    @Test
    public void anEntryIsShownInTheListUnderItsName() {
        JDBListBase item = new JDBListBase("Sample", "fa:DATABASE");

        assertEquals("Sample", item.getTitle());
        assertEquals("Sample", item.toString(),
                "a plain list model renders an entry with toString()");
        assertEquals("fa:DATABASE", item.getIcon());
    }

    @Test
    public void anEntryWithoutANameIsRenderedAsNothing() {
        assertNull(new JDBListBase().getTitle());
        assertNull(new JDBListBase().toString());
    }

    @Test
    public void theCopyOfAnEntryCarriesTheSameValues() throws Exception {
        JDBConnection conn = new JDBConnection();
        conn.setName("Sample");
        conn.setIcon("stock:h2.png");
        conn.setDriverType("H2 Embedded");
        conn.setConnectionUrl("jdbc:h2:mem:test");
        conn.setOutputDir("out");

        JDBConnection copy = (JDBConnection)conn.clone();

        assertNotSame(conn, copy);
        assertEquals(conn, copy, "the copy is only useful when it is equal to start with");
        copy.setName("Other");
        assertEquals("Sample", conn.getName(), "editing the copy leaves the original alone");
    }

    @Test
    public void theCopyOfAConnectionSharesItsTemplateList() throws Exception {
        // JDBListBase.clone() is shallow: everything a subclass holds beyond
        // its own strings stays shared until the subclass copies it itself
        JDBConnection conn = new JDBConnection();
        conn.setName("Sample");
        conn.setTemplates(new ArrayList<>(Arrays.asList(
                new JDBTemplate("Model", "model.java", "${name}.java"))));

        JDBConnection copy = (JDBConnection)conn.clone();

        assertSame(conn.getTemplates(), copy.getTemplates());
    }

    @Test
    public void aTemplateIsShownAsOneRowOfTheTemplateTable() {
        JDBTemplate tpl = new JDBTemplate("Java Model", "templates/java_model.java",
                "${name.suffix.pascal}Model.java");

        assertArrayEquals(new Object[]{ "Java Model", "templates/java_model.java",
                "${name.suffix.pascal}Model.java" }, tpl.getRowArray());
    }

    @Test
    public void theCopyOfATemplateIsIndependentOfIt() throws Exception {
        JDBTemplate tpl = new JDBTemplate("Java Model", "model.java", "${name}.java");

        JDBTemplate copy = (JDBTemplate)tpl.clone();

        assertNotSame(tpl, copy);
        assertEquals(tpl, copy);
        copy.setOutTemplate("${name}.txt");
        assertEquals("${name}.java", tpl.getOutTemplate());
    }

    @Test
    public void theCopyOfAPresetCopiesEveryTemplateInIt() throws Exception {
        JDBPreset preset = new JDBPreset();
        preset.setName("Java");
        preset.setTemplates(new ArrayList<>(Arrays.asList(
                new JDBTemplate("Model", "model.java", "${name}.java"),
                new JDBTemplate("Mapper", "mapper.xml", "${name}.xml"))));

        JDBPreset copy = (JDBPreset)preset.clone();

        assertEquals(preset, copy);
        assertNotSame(preset.getTemplates(), copy.getTemplates());
        for (int i = 0; i < preset.getTemplates().size(); i++)
            assertNotSame(preset.getTemplates().get(i), copy.getTemplates().get(i),
                    "editing a template of the copy must not edit the original");

        copy.getTemplates().get(0).setName("Renamed");
        copy.getTemplates().remove(1);

        assertEquals(2, preset.getTemplates().size());
        assertEquals("Model", preset.getTemplates().get(0).getName());
    }

    @Test
    public void theCopyOfAPresetWithoutTemplatesIsUsableRightAway() throws Exception {
        JDBPreset preset = new JDBPreset();
        preset.setName("Empty");

        JDBPreset copy = (JDBPreset)preset.clone();

        assertNull(preset.getTemplates());
        assertEquals(new ArrayList<JDBTemplate>(), copy.getTemplates(),
                "the editor may add to the list without checking it first");
    }

    @Test
    public void aPresetIsShownUnderItsNameLikeEveryOtherEntry() {
        JDBPreset preset = new JDBPreset();
        preset.setName("Java");
        preset.setIcon("fa:COFFEE");

        assertEquals("Java", preset.getTitle());
        assertEquals("fa:COFFEE", preset.getIcon());
    }

    @Test
    public void onlyTheTitleOfASubclassIsItsName() {
        // JDBListBase.toString() hands out the title, but every subclass is a
        // @Data class and brings a generated toString of its own - so a
        // renderer has to ask for the title, it cannot print the entry
        JDBPreset preset = new JDBPreset();
        preset.setName("Java");

        assertEquals("Java", preset.getTitle());
        assertTrue(preset.toString().startsWith("JDBPreset("), preset.toString());
    }

    @Test
    public void twoEntriesDifferAsSoonAsAnySettingDoes() {
        JDBDriver one = JDBDriver.builder().name("H2").driverClass("org.h2.Driver")
                .jdbcJar("h2.jar").build();
        JDBDriver other = JDBDriver.builder().name("H2").driverClass("org.h2.Driver")
                .jdbcJar("h2.jar").build();

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());

        other.setUseTables(true);
        assertTrue(!one.equals(other), "a list may hold two drivers of the same name");
    }

    @Test
    public void aBuilderCopyMayBeEditedBeforeItIsBuilt() {
        JDBDriver driver = JDBDriver.builder().name("H2").jdbcJar("h2.jar")
                .driverClass("org.h2.Driver").stockItem(true).build();

        JDBDriver derived = driver.toBuilder().name("H2 of my own").stockItem(false).build();

        assertEquals("H2 of my own", derived.getName());
        assertEquals("h2.jar", derived.getJdbcJar(), "everything else is taken over");
        assertEquals("H2", driver.getName());
        assertTrue(driver.isStockItem());
    }
}

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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the reflective accessors of {@link ObjUtils}.
 *
 * @author comart
 */
public class ObjUtilsTest {

    /**
     * A bean whose setters take primitives, exactly like the ones Lombok
     * generates for the database meta models.
     */
    public static class PrimitiveBean {
        private int no;
        private boolean flag;
        private long size;
        private String name;

        public int getNo() { return no; }
        public void setNo(int no) { this.no = no; }

        public boolean isFlag() { return flag; }
        public void setFlag(boolean flag) { this.flag = flag; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class DerivedBean extends PrimitiveBean {
        private String extra;
        public String getExtra() { return extra; }
        public void setExtra(String extra) { this.extra = extra; }
    }

    @Test
    public void testSetValueMapsBoxedArgumentToPrimitiveSetter() throws Exception {
        PrimitiveBean bean = new PrimitiveBean();

        // Integer.class does not match setNo(int) - the lookup used to fail
        // silently and leave the property untouched
        ObjUtils.setValue(bean, "no", 7);
        assertEquals(7, bean.getNo());

        ObjUtils.setValue(bean, "flag", true);
        assertTrue(bean.isFlag());

        ObjUtils.setValue(bean, "size", 42L);
        assertEquals(42L, bean.getSize());

        ObjUtils.setValue(bean, "name", "sample");
        assertEquals("sample", bean.getName());
    }

    @Test
    public void testSetValueWalksUpTheClassHierarchy() throws Exception {
        DerivedBean bean = new DerivedBean();
        ObjUtils.setValue(bean, "no", 3);
        assertEquals(3, bean.getNo());
        ObjUtils.setValue(bean, "extra", "x");
        assertEquals("x", bean.getExtra());
    }

    @Test
    public void testGetValueFindsBothGetterStyles() throws Exception {
        PrimitiveBean bean = new PrimitiveBean();
        bean.setNo(5);
        bean.setFlag(true);
        bean.setName("sample");

        assertEquals(5, ObjUtils.getValue(bean, "no"));
        // 'flag' has no getFlag() - the isFlag() fallback must be found
        assertEquals(Boolean.TRUE, ObjUtils.getValue(bean, "flag"));
        assertEquals("sample", ObjUtils.getValue(bean, "name"));
        assertNull(ObjUtils.getValue(bean, "noSuchProperty"));
    }

    @Test
    public void testGetValueNavigatesNestedProperties() throws Exception {
        PrimitiveBean inner = new PrimitiveBean();
        inner.setName("inner");
        Map<String, Object> outer = new HashMap<>();
        outer.put("bean", inner);

        assertEquals("inner", ObjUtils.getValue(outer, "bean.name"));
    }

    /**
     * a bean whose writable property is named without the <code>set</code>
     * prefix, and one that carries a public field.
     */
    public static class PlainBean {
        public String visible;
        private String title;

        public String getVisible() { return "from the getter"; }
        public String getTitle() { return title; }
        public void title(String title) { this.title = title; }
    }

    @Test
    public void testSetValueIgnoresANullValue() throws Exception {
        PrimitiveBean bean = new PrimitiveBean();
        bean.setName("kept");

        // no setter takes a null typed argument, so nothing is written - the
        // property must not be cleared by a value that is not there
        ObjUtils.setValue(bean, "name", null);
        assertEquals("kept", bean.getName());
    }

    @Test
    public void testSetValueIsANoOpWhenNoSetterMatches() throws Exception {
        PrimitiveBean bean = new PrimitiveBean();
        bean.setNo(5);

        // there is a 'no' property but no setNo(String)
        ObjUtils.setValue(bean, "no", "not a number");
        assertEquals(5, bean.getNo());

        // and no property of that name at all
        ObjUtils.setValue(bean, "noSuchProperty", "x");
        assertEquals(5, bean.getNo());
    }

    @Test
    public void testSetValueFindsASetterWithoutThePrefix() throws Exception {
        PlainBean bean = new PlainBean();
        ObjUtils.setValue(bean, "title", "written");
        assertEquals("written", bean.getTitle());
    }

    @Test
    public void testGetValuePrefersAPublicField() throws Exception {
        PlainBean bean = new PlainBean();
        bean.visible = "field value";
        // the field is read directly, the getter is only the fallback
        assertEquals("field value", ObjUtils.getValue(bean, "visible"));
    }

    @Test
    public void testGetValueOnAPathThatBreaksOffYieldsNull() throws Exception {
        Map<String, Object> outer = new HashMap<>();
        outer.put("bean", new PrimitiveBean());

        // the middle segment resolves to nothing, so the rest of the path has
        // nothing to be read from
        assertNull(ObjUtils.getValue(outer, "missing.name"));
        assertNull(ObjUtils.getValue(outer, "bean.name.length"));
        assertNull(ObjUtils.getValue(null, "anything"));
        assertNull(ObjUtils.getValue(outer, "missing"));
    }

    @Test
    public void testGetValueOfAnEmptyNameIsNullInEveryContext() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("", "an entry under the empty key");

        // reading a bean used to capitalize the name and die on substring(0, 1)
        assertNull(ObjUtils.getValue(new PrimitiveBean(), ""));
        // and a map answered whatever happened to sit under the empty key, so
        // the '${}' of a template behaved differently per kind of context
        assertNull(ObjUtils.getValue(map, ""));
        assertNull(ObjUtils.getValue(map, "bean."));
        assertNull(ObjUtils.getValue(new PrimitiveBean(), "name."));
        assertNull(ObjUtils.getValue(null, ""));
        // a name that is not there at all is nothing to read either
        assertNull(ObjUtils.getValue(new PrimitiveBean(), null));
        assertNull(ObjUtils.getValue(map, null));
        // the fallback overload sees the same nothing
        assertEquals("fallback", ObjUtils.getValue(map, "", "fallback"));
        assertEquals("fallback", ObjUtils.getValue(new PrimitiveBean(), "", "fallback"));
    }

    @Test
    public void testGetValueFallsBackToTheDefault() throws Exception {
        PrimitiveBean bean = new PrimitiveBean();
        bean.setName("sample");

        assertEquals("sample", ObjUtils.getValue(bean, "name", "fallback"));
        assertEquals("fallback", ObjUtils.getValue(bean, "noSuchProperty", "fallback"));
        // a property that is there but null takes the fallback too
        assertEquals("fallback", ObjUtils.getValue(new PrimitiveBean(), "name", "fallback"));
    }

    @Test
    public void testObjToMapFlattensTheWholeHierarchy() {
        DerivedBean bean = new DerivedBean();
        bean.setNo(7);
        bean.setFlag(true);
        bean.setName("sample");
        bean.setExtra("x");

        Map<String, Object> map = ObjUtils.objToMap(bean);

        assertEquals("x", map.get("extra"));
        assertEquals(Integer.valueOf(7), map.get("no"),
                "the fields of the super class are in there too");
        assertEquals(Boolean.TRUE, map.get("flag"));
        assertEquals("sample", map.get("name"));
        assertEquals(Long.valueOf(0L), map.get("size"));
        assertFalse(map.containsKey("noSuchProperty"));
    }

    @Test
    public void testSetFieldsToOverwritesWhatIsAlreadyThere() {
        PrimitiveBean bean = new PrimitiveBean();
        bean.setName("from the bean");
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("name", "from the map");
        vars.put("untouched", "kept");

        ObjUtils.setFieldsTo(bean, vars);

        assertEquals("from the bean", vars.get("name"));
        assertEquals("kept", vars.get("untouched"),
                "an entry the object has no field for stays as it is");
    }

    @Test
    public void testFileContentsRoundTrip(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("nested/sample.txt");
        String content = "line one\n한글 두 번째 줄\n";

        ObjUtils.writeFile(file.toString(), content);

        assertTrue(Files.isRegularFile(file), "the parent directories are created");
        assertEquals(content, ObjUtils.getFileContents(file.toString()));
        // the file is UTF-8, whatever the platform encoding is
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file));

        ObjUtils.writeFile(file.toString(), "replaced");
        assertEquals("replaced", ObjUtils.getFileContents(file.toString()));
    }

    @Test
    public void testGetLoginUserIdIsTheAccountTheJvmRunsUnder() {
        assertEquals(System.getProperty("user.name"), ObjUtils.getLoginUserId());
    }
}

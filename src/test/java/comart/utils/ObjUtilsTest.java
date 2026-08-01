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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
}

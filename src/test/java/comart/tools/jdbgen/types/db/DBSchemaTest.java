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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <code>DatabaseMetaData.getSchemas()</code> reports the schema in the first
 * and the catalog in the second column, and is read by position because that is
 * all the specification promises.
 */
public class DBSchemaTest {

    @Test
    public void theSchemaIsReadByPositionNotByName() throws Exception {
        // the labels are the ones of the specification, but a driver is free to
        // report them differently - only the order is fixed
        MapResultSet rs = new MapResultSet()
                .with("TABLE_SCHEM", "PUBLIC")
                .with("TABLE_CATALOG", "TESTDB");

        DBSchema schema = new DBSchema(rs);

        assertEquals("PUBLIC", schema.getSchema());
        assertEquals("PUBLIC", schema.getName(), "the display name starts as the schema name");
        assertEquals("TESTDB", schema.getCatalog());
        assertNull(schema.getTables(), "the tables are read on demand");
    }

    @Test
    public void aDatabaseWithoutCatalogsReportsNoneForTheSchema() throws Exception {
        MapResultSet rs = new MapResultSet()
                .with("TABLE_SCHEM", "PUBLIC")
                .with("TABLE_CATALOG", null);

        DBSchema schema = new DBSchema(rs);

        assertEquals("PUBLIC", schema.getSchema());
        assertNull(schema.getCatalog());
    }

    @Test
    public void aPlaceholderSchemaOnlyCarriesWhatItWasGiven() {
        // what DBMeta builds for a database reporting neither catalog nor schema
        DBSchema schema = new DBSchema();
        schema.setName("Default Schema");

        assertEquals("Default Schema", schema.getName());
        assertNull(schema.getSchema(), "there is no schema to ask the driver about");
        assertNull(schema.getCatalog());
    }
}

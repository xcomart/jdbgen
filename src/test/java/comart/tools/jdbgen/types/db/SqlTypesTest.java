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

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The type tables are what a generated model is typed with, so a wrong entry
 * shows up in every generated source.
 */
public class SqlTypesTest {

    @ParameterizedTest
    @CsvSource({
        "CHAR, CHAR, String",
        "VARCHAR, VARCHAR, String",
        "NVARCHAR, NVARCHAR, String",
        "LONGVARCHAR, LONGVARCHAR, String",
        "CLOB, CLOB, String",
        "NCLOB, NCLOB, String",
        "INTEGER, INTEGER, Integer",
        "SMALLINT, SMALLINT, Short",
        "TINYINT, TINYINT, Short",
        "BIGINT, BIGINT, Long",
        "NUMERIC, NUMERIC, Integer",
        "DECIMAL, DECIMAL, Integer",
        "FLOAT, FLOAT, Float",
        "REAL, REAL, Float",
        "DOUBLE, DOUBLE, Double",
        "BOOLEAN, BOOLEAN, Boolean",
        "BIT, BIT, Boolean",
        "DATE, DATE, Date",
        "TIME, TIME, Time",
        "TIMESTAMP, TIMESTAMP, String",
        "BINARY, BINARY, byte[]",
        "VARBINARY, VARBINARY, byte[]",
        "LONGVARBINARY, LONGVARBINARY, byte[]",
        "BLOB, BLOB, byte[]",
        "SQLXML, SQLXML, String",
        "OTHER, OTHER, String"
    })
    public void aTypeCodeIsTranslatedIntoItsNameAndItsJavaType(
            String constant, String jdbcType, String javaType) throws Exception {
        int code = Types.class.getField(constant).getInt(null);

        assertEquals(jdbcType, SqlTypes.getJDBCType(code), constant);
        assertEquals(javaType, SqlTypes.getJavaType(code), constant);
    }

    @ParameterizedTest
    @ValueSource(ints = {9999, -100000, Integer.MAX_VALUE, Integer.MIN_VALUE})
    public void aCodeThatIsNoSqlTypeIsAnsweredWithNothing(int code) {
        assertNull(SqlTypes.getJDBCType(code));
        assertNull(SqlTypes.getJavaType(code));
    }

    @Test
    public void everyTypeOfTheJdbcSpecificationIsCovered() throws Exception {
        List<String> missing = new ArrayList<>();
        for (Field f: Types.class.getFields()) {
            if (f.getType() != int.class)
                continue;
            int code = f.getInt(null);
            // 'REF_CURSOR' and the time zone types were added after this table
            // was written and are deliberately left out of the check
            if (SqlTypes.getJDBCType(code) == null || SqlTypes.getJavaType(code) == null)
                missing.add(f.getName());
        }

        assertEquals(java.util.Arrays.asList(
                "REF_CURSOR", "TIMESTAMP_WITH_TIMEZONE", "TIME_WITH_TIMEZONE"),
                sorted(missing),
                "a type that is not in the tables renders as an empty java type");
    }

    @Test
    public void theTwoTablesDescribeTheSameTypes() throws Exception {
        for (Field f: Types.class.getFields()) {
            if (f.getType() != int.class)
                continue;
            int code = f.getInt(null);
            if (SqlTypes.getJDBCType(code) == null)
                continue;
            assertNotNull(SqlTypes.getJavaType(code),
                    f.getName() + " has a jdbc type but no java type");
        }
    }

    @Test
    public void theJdbcTypeIsTheNameOfTheConstantItself() throws Exception {
        for (Field f: Types.class.getFields()) {
            if (f.getType() != int.class)
                continue;
            String jdbc = SqlTypes.getJDBCType(f.getInt(null));
            if (jdbc != null)
                assertEquals(f.getName(), jdbc);
        }
    }

    @Test
    public void theArrayAndStructTypesAreNoJavaTypes() {
        // they are placeholders rather than something a model can be typed with
        assertEquals("array", SqlTypes.getJavaType(Types.ARRAY));
        assertEquals("struct", SqlTypes.getJavaType(Types.STRUCT));
        assertEquals("ref", SqlTypes.getJavaType(Types.REF));
        assertTrue(SqlTypes.getJavaType(Types.NULL).equals("null"));
    }

    private static List<String> sorted(List<String> names) {
        List<String> res = new ArrayList<>(names);
        res.sort(String::compareTo);
        return res;
    }
}

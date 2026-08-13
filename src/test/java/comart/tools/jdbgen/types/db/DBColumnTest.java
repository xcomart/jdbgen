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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the templates see of a column is derived while the metadata is read, so
 * the derivation is what has to be right: the type as it is written in DDL, the
 * character type flag and the two type names.
 */
public class DBColumnTest {

    /** one row of a <code>getColumns()</code> result set. */
    private static MapResultSet row(String typeName, int dataType, int size) {
        return new MapResultSet()
                .with("TABLE_CAT", "TESTDB")
                .with("TABLE_SCHEM", "PUBLIC")
                .with("TABLE_NAME", "TB_USER")
                .with("COLUMN_NAME", "USER_NAME")
                .with("DATA_TYPE", dataType)
                .with("TYPE_NAME", typeName)
                .with("COLUMN_SIZE", size)
                .with("NULLABLE", DatabaseMetaData.columnNullable)
                .with("REMARKS", "the name")
                .with("COLUMN_DEF", "'anonymous'");
    }

    @Test
    public void everyColumnOfTheResultSetIsMapped() throws Exception {
        DBColumn col = new DBColumn(row("VARCHAR", Types.VARCHAR, 40));

        assertEquals("TESTDB", col.getCatalog());
        assertEquals("PUBLIC", col.getSchema());
        assertEquals("TB_USER", col.getTable());
        assertEquals("USER_NAME", col.getColumn());
        assertEquals("USER_NAME", col.getName(), "the display name starts as the column name");
        assertEquals(Types.VARCHAR, col.getDataType());
        assertEquals("VARCHAR", col.getTypeName());
        assertEquals(40, col.getLength());
        assertEquals(DatabaseMetaData.columnNullable, col.getNullable());
        assertEquals("the name", col.getRemarks());
        assertEquals("'anonymous'", col.getDefVal());
        assertFalse(col.isKey(), "the primary key is read separately");
        assertNull(col.getNvlColName(), "only a template fills this in");
    }

    @Test
    public void aCharacterTypeCarriesItsLength() throws Exception {
        assertEquals("VARCHAR(40)", new DBColumn(row("varchar", Types.VARCHAR, 40)).getTypeString());
        assertEquals("CHAR(2)", new DBColumn(row("char", Types.CHAR, 2)).getTypeString());
        assertEquals("VARBINARY(16)",
                new DBColumn(row("varbinary", Types.VARBINARY, 16)).getTypeString());
    }

    @Test
    public void aTypeWithoutALengthIsWrittenAsItIs() throws Exception {
        assertEquals("INTEGER", new DBColumn(row("integer", Types.INTEGER, 10)).getTypeString());
        assertEquals("TIMESTAMP",
                new DBColumn(row("timestamp", Types.TIMESTAMP, 26)).getTypeString());
        assertEquals("CLOB", new DBColumn(row("clob", Types.CLOB, 2147483647)).getTypeString());
    }

    @Test
    public void anUnreasonableLengthIsWrittenAsMax() throws Exception {
        // sql server reports varchar(max) as a length of 2^31-1
        assertEquals("VARCHAR(max)",
                new DBColumn(row("varchar", Types.VARCHAR, 2147483647)).getTypeString());
        assertEquals("VARCHAR(1000000)",
                new DBColumn(row("varchar", Types.VARCHAR, 1000000)).getTypeString(),
                "the switch is above a million, not at it");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CHAR", "VARCHAR", "NVARCHAR", "LONGVARCHAR", "CLOB", "NCLOB", "TEXT"})
    public void everyTextTypeIsRecognizedAsACharacterType(String typeName) throws Exception {
        assertTrue(new DBColumn(row(typeName, Types.VARCHAR, 10)).isCharType(), typeName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INTEGER", "NUMBER", "BLOB", "DATE", "VARBINARY"})
    public void everyOtherTypeIsNotACharacterType(String typeName) throws Exception {
        assertFalse(new DBColumn(row(typeName, Types.OTHER, 10)).isCharType(), typeName);
    }

    @Test
    public void theTypeIsRecognizedRegardlessOfTheCaseTheDriverReportsItIn() throws Exception {
        assertTrue(new DBColumn(row("nVarChar", Types.NVARCHAR, 10)).isCharType());
        assertEquals("NVARCHAR(10)",
                new DBColumn(row("nVarChar", Types.NVARCHAR, 10)).getTypeString());
    }

    @ParameterizedTest
    @CsvSource({
        "4, INTEGER, Integer",
        "12, VARCHAR, String",
        "93, TIMESTAMP, String",
        "-5, BIGINT, Long",
        "2004, BLOB, byte[]"
    })
    public void theJdbcAndJavaTypesComeFromTheTypeCode(
            int dataType, String jdbcType, String javaType) throws Exception {
        DBColumn col = new DBColumn(row("whatever", dataType, 10));

        assertEquals(jdbcType, col.getJdbcType());
        assertEquals(javaType, col.getJavaType());
    }

    @Test
    public void aTypeCodeNoJdbcVersionKnowsLeavesTheTypeNamesEmpty() throws Exception {
        DBColumn col = new DBColumn(row("GEOMETRY", 9999, 10));

        assertNull(col.getJdbcType());
        assertNull(col.getJavaType());
        assertEquals("GEOMETRY", col.getTypeString(), "the database type is still usable");
    }

    @Test
    public void aCustomQueryMayOmitTheTypeName() throws Exception {
        // 'TYPE_NAME' is null rather than absent: the column is selected, the
        // query just has nothing to put into it
        DBColumn col = new DBColumn(row(null, Types.VARCHAR, 10));

        assertEquals("", col.getTypeName(), "a null type name would break every template");
        assertEquals("", col.getTypeString());
        assertFalse(col.isCharType());
        assertEquals("VARCHAR", col.getJdbcType(), "the type code still tells what it is");
    }

    @Test
    public void aColumnTheQueryDoesNotSelectAtAllIsReported() {
        MapResultSet rs = new MapResultSet()
                .with("TABLE_CAT", "TESTDB")
                .with("TABLE_SCHEM", "PUBLIC")
                .with("TABLE_NAME", "TB_USER")
                .with("COLUMN_NAME", "USER_NAME");

        assertThrows(SQLException.class, () -> new DBColumn(rs),
                "a query missing a required column has to say so");
    }

    @Test
    public void aColumnIsNumberedAndDescribedForTheLog() throws Exception {
        DBColumn col = new DBColumn(row("VARCHAR", Types.VARCHAR, 40));
        col.setNo(7);

        assertEquals(7, col.getNo(), "the position inside the table is filled in while reading");
        assertTrue(col.toString().contains("USER_NAME"), col.toString());
        assertTrue(col.toString().contains("VARCHAR(40)"),
                "the log has to show what the templates work with: " + col);
    }
}

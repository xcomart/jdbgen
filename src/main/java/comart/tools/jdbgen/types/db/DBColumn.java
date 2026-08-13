/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * One column of a database table, read from a
 * <code>DatabaseMetaData.getColumns()</code> result set or from the custom
 * column query of the driver. Besides the raw metadata it carries the derived
 * values the templates work with: the type as it is written in DDL, the JDBC
 * type name and the matching Java type.
 *
 * @author comart
 */
@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
public class DBColumn extends DBMetaModel {
    /** catalog of the owning table (<code>TABLE_CAT</code>). */
    private String catalog;
    /** schema of the owning table (<code>TABLE_SCHEM</code>). */
    private String schema;
    /** name of the owning table (<code>TABLE_NAME</code>). */
    private String table;
    /** column name (<code>COLUMN_NAME</code>). */
    private String column;
    /** display name of this column, initially the same as <code>column</code>. */
    private String name;
    /** SQL type code of <code>java.sql.Types</code> (<code>DATA_TYPE</code>). */
    private short dataType;
    /** database specific type name (<code>TYPE_NAME</code>), never <code>null</code>. */
    private String typeName;
    /** column size (<code>COLUMN_SIZE</code>). */
    private int length;
    /** nullability as one of the <code>DatabaseMetaData.columnNo...</code> constants. */
    private short nullable;
    /** column comment (<code>REMARKS</code>). */
    private String remarks;
    /** whether this column takes part in the primary key. */
    private boolean isKey;
    /** default value of the column (<code>COLUMN_DEF</code>). */
    private String defVal;
    
    /** the type as it is written in DDL, that is the upper cased type name with a length for character and binary types. */
    private String typeString;
    /** whether the type name marks a character type (<code>CHAR</code>, <code>CLOB</code> or <code>TEXT</code>). */
    private boolean isCharType;
    /** name a template may use for the null replacement of this column; not filled in while the metadata is read. */
    private String nvlColName;
    /** JDBC type name of {@link #dataType}, see {@link SqlTypes#getJDBCType(int)}. */
    private String jdbcType;
    /** Java type matching {@link #dataType}, see {@link SqlTypes#getJavaType(int)}. */
    private String javaType;
    
    /**
     * read a column out of the current row of a column result set and derive
     * the template values from it. A missing <code>TYPE_NAME</code>, which a
     * custom column query may well omit, is taken as an empty type name.
     *
     * @param rs result set positioned on the row to read.
     * @throws SQLException if the columns cannot be read.
     */
    public DBColumn(ResultSet rs) throws SQLException {
        catalog = rs.getString("TABLE_CAT");
        schema = rs.getString("TABLE_SCHEM");
        table = rs.getString("TABLE_NAME");
        column = rs.getString("COLUMN_NAME");
        name = column;
        dataType = rs.getShort("DATA_TYPE");
        typeName = rs.getString("TYPE_NAME");
        // custom column queries may omit TYPE_NAME entirely
        if (typeName == null)
            typeName = "";
        length = rs.getInt("COLUMN_SIZE");
        nullable = rs.getShort("NULLABLE");
        remarks = rs.getString("REMARKS");
        defVal = rs.getString("COLUMN_DEF");
        
        String tname = typeName.toUpperCase();
        isCharType = tname.contains("CHAR") ||
                tname.contains("CLOB") ||
                tname.contains("TEXT");
        if (tname.contains("CHAR") || tname.contains("BINARY")) {
            if (length > 1000000)
                tname += "(max)";
            else
                tname += "("+length+")";
        }
        typeString = tname;
        jdbcType = SqlTypes.getJDBCType(dataType);
        javaType = SqlTypes.getJavaType(dataType);
    }
}

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
 *
 * @author comart
 */
@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
public class DBColumn extends DBMetaModel {
    private String catalog;
    private String schema;
    private String table;
    private String column;
    private String name;
    private short dataType;
    private String typeName;
    private int length;
    private short nullable;
    private String remarks;
    private boolean isKey;
    private String defVal;
    
    private String typeString;
    private boolean isCharType;
    private String nvlColName;
    private String jdbcType;
    private String javaType;
    
    public DBColumn(ResultSet rs) throws SQLException {
        catalog = rs.getString("TABLE_CAT");
        schema = rs.getString("TABLE_SCHEM");
        table = rs.getString("TABLE_NAME");
        column = rs.getString("COLUMN_NAME");
        name = column;
        dataType = rs.getShort("DATA_TYPE");
        typeName = rs.getString("TYPE_NAME");
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

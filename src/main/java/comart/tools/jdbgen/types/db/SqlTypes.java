/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.db;

import java.sql.Types;
import java.util.HashMap;

/**
 *
 * @author comart
 */
public final class SqlTypes {
    private static final HashMap<Integer,String> jdbcTypes = new HashMap<>();
    private static final HashMap<Integer,String> javaTypes = new HashMap<>();
    static {
        jdbcTypes.put(Types.ARRAY, "ARRAY");
        jdbcTypes.put(Types.BIGINT, "BIGINT");
        jdbcTypes.put(Types.BINARY, "BINARY");
        jdbcTypes.put(Types.BIT, "BIT");
        jdbcTypes.put(Types.BLOB, "BLOB");
        jdbcTypes.put(Types.BOOLEAN, "BOOLEAN");
        jdbcTypes.put(Types.CHAR, "CHAR");
        jdbcTypes.put(Types.CLOB, "CLOB");
        jdbcTypes.put(Types.DATALINK, "DATALINK");
        jdbcTypes.put(Types.DATE, "DATE");
        jdbcTypes.put(Types.DECIMAL, "DECIMAL");
        jdbcTypes.put(Types.DISTINCT, "DISTINCT");
        jdbcTypes.put(Types.DOUBLE, "DOUBLE");
        jdbcTypes.put(Types.FLOAT, "FLOAT");
        jdbcTypes.put(Types.INTEGER, "INTEGER");
        jdbcTypes.put(Types.JAVA_OBJECT, "JAVA_OBJECT");
        jdbcTypes.put(Types.LONGNVARCHAR, "LONGNVARCHAR");
        jdbcTypes.put(Types.LONGVARBINARY, "LONGVARBINARY");
        jdbcTypes.put(Types.LONGVARCHAR, "LONGVARCHAR");
        jdbcTypes.put(Types.NCHAR, "NCHAR");
        jdbcTypes.put(Types.NCLOB, "NCLOB");
        jdbcTypes.put(Types.NULL, "NULL");
        jdbcTypes.put(Types.NUMERIC, "NUMERIC");
        jdbcTypes.put(Types.NVARCHAR, "NVARCHAR");
        jdbcTypes.put(Types.OTHER, "OTHER");
        jdbcTypes.put(Types.REAL, "REAL");
        jdbcTypes.put(Types.REF, "REF");
        jdbcTypes.put(Types.ROWID, "ROWID");
        jdbcTypes.put(Types.SMALLINT, "SMALLINT");
        jdbcTypes.put(Types.SQLXML, "SQLXML");
        jdbcTypes.put(Types.STRUCT, "STRUCT");
        jdbcTypes.put(Types.TIME, "TIME");
        jdbcTypes.put(Types.TIMESTAMP, "TIMESTAMP");
        jdbcTypes.put(Types.TINYINT, "TINYINT");
        jdbcTypes.put(Types.VARBINARY, "VARBINARY");
        jdbcTypes.put(Types.VARCHAR, "VARCHAR");

        javaTypes.put(Types.ARRAY, "array");
        javaTypes.put(Types.BIGINT, "Long");
        javaTypes.put(Types.BINARY, "byte[]");
        javaTypes.put(Types.BIT, "Boolean");
        javaTypes.put(Types.BLOB, "byte[]");
        javaTypes.put(Types.BOOLEAN, "Boolean");
        javaTypes.put(Types.CHAR, "String");
        javaTypes.put(Types.CLOB, "String");
        javaTypes.put(Types.DATALINK, "String");
        javaTypes.put(Types.DATE, "Date");
        javaTypes.put(Types.DECIMAL, "Integer");
        javaTypes.put(Types.DISTINCT, "String");
        javaTypes.put(Types.DOUBLE, "Double");
        javaTypes.put(Types.FLOAT, "Float");
        javaTypes.put(Types.INTEGER, "Integer");
        javaTypes.put(Types.JAVA_OBJECT, "String");
        javaTypes.put(Types.LONGNVARCHAR, "String");
        javaTypes.put(Types.LONGVARBINARY, "byte[]");
        javaTypes.put(Types.LONGVARCHAR, "String");
        javaTypes.put(Types.NCHAR, "String");
        javaTypes.put(Types.NCLOB, "String");
        javaTypes.put(Types.NULL, "null");
        javaTypes.put(Types.NUMERIC, "Integer");
        javaTypes.put(Types.NVARCHAR, "String");
        javaTypes.put(Types.OTHER, "String");
        javaTypes.put(Types.REAL, "Float");
        javaTypes.put(Types.REF, "ref");
        javaTypes.put(Types.ROWID, "Integer");
        javaTypes.put(Types.SMALLINT, "Short");
        javaTypes.put(Types.SQLXML, "String");
        javaTypes.put(Types.STRUCT, "struct");
        javaTypes.put(Types.TIME, "Time");
        javaTypes.put(Types.TIMESTAMP, "String");
        javaTypes.put(Types.TINYINT, "Short");
        javaTypes.put(Types.VARBINARY, "byte[]");
        javaTypes.put(Types.VARCHAR, "String");
    }
    
    public static String getJDBCType(int sqlType) {
        return jdbcTypes.get(sqlType);
    }
    
    public static String getJavaType(int sqlType) {
        return javaTypes.get(sqlType);
    }
}

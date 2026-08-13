/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * One schema of the connected database, as reported by
 * <code>DatabaseMetaData.getSchemas()</code>. Databases without schemas are
 * represented by a placeholder that only carries the catalog or a default name.
 *
 * @author comart
 */
@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
@AllArgsConstructor
public class DBSchema extends DBMetaModel {
    /** display name of this schema, initially the same as <code>schema</code>. */
    private String name;
    /** schema name as reported by the driver. */
    private String schema;
    /** catalog this schema belongs to. */
    private String catalog;
    
    /** the tables of this schema, <code>null</code> until they are read. */
    private List<DBTable> tables = null;
    
    /**
     * read a schema out of the current row of a
     * <code>DatabaseMetaData.getSchemas()</code> result set, which reports the
     * schema name in the first and the catalog in the second column.
     *
     * @param rs result set positioned on the row to read.
     * @throws SQLException if the columns cannot be read.
     */
    public DBSchema(ResultSet rs) throws SQLException {
        schema = rs.getString(1);
        name = schema;
        catalog = rs.getString(2);
    }
}

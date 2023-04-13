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
 *
 * @author comart
 */
@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
@AllArgsConstructor
public class DBSchema extends DBMetaModel {
    private String name;
    private String schema;
    private String catalog;
    
    private List<DBTable> tables = null;
    
    public DBSchema(ResultSet rs) throws SQLException {
        schema = rs.getString("TABLE_SCHEM");
        name = schema;
        catalog = rs.getString("TABLE_CATALOG");
    }
}

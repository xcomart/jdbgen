/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.db;

import comart.tools.jdbgen.types.HasIcon;
import comart.tools.jdbgen.types.HasTitle;
import comart.utils.StrUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import jiconfont.icons.font_awesome.FontAwesome;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * One table or view of the connected database, read from a
 * <code>DatabaseMetaData.getTables()</code> result set or from the custom table
 * query of the driver. Its columns are loaded lazily and, once loaded, are also
 * kept split into key and non-key columns for the templates.
 *
 * @author comart
 */
@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
public class DBTable extends DBMetaModel implements HasTitle, HasIcon {
    /** catalog of this table (<code>TABLE_CAT</code>). */
    private String catalog;
    /** schema of this table (<code>TABLE_SCHEM</code>). */
    private String schema;
    /** display name of this table, initially the same as <code>table</code>. */
    private String name;
    /** table name (<code>TABLE_NAME</code>). */
    private String table;
    /** either <code>"TABLE"</code> or <code>"VIEW"</code>. */
    private String type;
    /** all columns of this table, <code>null</code> until they are read. */
    private List<DBColumn> columns = null;
    /** the primary key columns, <code>null</code> until the columns are read. */
    private List<DBColumn> keys = null;
    /** the columns outside the primary key, <code>null</code> until the columns are read. */
    private List<DBColumn> notKeys = null;
    /** free form field a template may fill in; not filled in while the metadata is read. */
    private String source;
    /** table comment (<code>REMARKS</code>). */
    private String remarks;
    
    
    /**
     * read a table out of the current row of a table result set. The table type
     * is normalized to <code>"TABLE"</code> or <code>"VIEW"</code>: a missing
     * type, which a custom table query may well omit, becomes
     * <code>"TABLE"</code>, and a compound type such as
     * <code>"SYSTEM TABLE"</code> is reduced to the kind it contains.
     *
     * @param rs result set positioned on the row to read.
     * @throws SQLException if the columns cannot be read.
     */
    public DBTable(ResultSet rs) throws SQLException {
        catalog = rs.getString("TABLE_CAT");
        schema = rs.getString("TABLE_SCHEM");
        table = rs.getString("TABLE_NAME");
        setName(table);
        type = rs.getString("TABLE_TYPE");
        if (type == null) {
            // custom table queries may omit TABLE_TYPE entirely
            type = "TABLE";
        } else if (!StrUtils.contains(new String[]{"TABLE", "VIEW"}, type)) {
            // TODO: need more specific
            if (type.contains("TABLE"))
                type = "TABLE";
            else if (type.contains("VIEW"))
                type = "VIEW";
        }
        remarks = rs.getString("REMARKS");
    }

    /**
     * set the display name of this table.
     *
     * @param name the new display name.
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * label of this table in the table list, which is the table name as the
     * database reports it.
     *
     * @return the value of <code>table</code>.
     */
    @Override
    public String getTitle() {
        return table;
    }
    
    /**
     * icon of this item, a Font Awesome glyph telling tables and views apart.
     *
     * @return a <code>fa:</code> icon locator with the table glyph for a table
     *         and the eye glyph for a view.
     */
    @Override
    public String getIcon() {
        return "fa:"+("TABLE".equals(type) ? FontAwesome.TABLE:FontAwesome.EYE);
    }
}

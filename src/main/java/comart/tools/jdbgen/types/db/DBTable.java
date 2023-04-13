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
 *
 * @author comart
 */
@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
public class DBTable extends DBMetaModel implements HasTitle, HasIcon {
    private String catalog;
    private String schema;
    private String name;
    private String table;
    private String type;
    private List<DBColumn> columns = null;
    private List<DBColumn> keys = null;
    private List<DBColumn> notKeys = null;
    private String source;
    private String remarks;
    
    
    private String nameCamelCase;
    private String namePascalCase;
    private String nameSuffix;
    private String nameSuffixCamelCase;
    private String nameSuffixPascal;
    private String nameSuffixLower;
    
    public DBTable(ResultSet rs) throws SQLException {
        catalog = rs.getString("TABLE_CAT");
        schema = rs.getString("TABLE_SCHEM");
        table = rs.getString("TABLE_NAME");
        setName(table);
        type = rs.getString("TABLE_TYPE");
        remarks = rs.getString("REMARKS");
    }

    public final void setName(String name) {
        this.name = name;
        this.nameCamelCase = StrUtils.toCamelCase(name);
        namePascalCase = nameCamelCase.substring(0,1).toUpperCase();
        if (nameCamelCase.length() > 1)
            namePascalCase += nameCamelCase.substring(1);
        // guarantee start with 0 if no under bar is presents.
        int idx = name.indexOf('_') + 1;
        this.nameSuffix = name.substring(idx);
        this.nameSuffixCamelCase = StrUtils.toCamelCase(this.nameSuffix);
        this.nameSuffixPascal = nameSuffixCamelCase.substring(0,1).toUpperCase();
        if (nameSuffixCamelCase.length() > 1)
            this.nameSuffixPascal += nameSuffixCamelCase.substring(1);
        this.nameSuffixLower = nameSuffixCamelCase.toLowerCase();
    }

    @Override
    public String getTitle() {
        return table;
    }
    
    @Override
    public String getIcon() {
        return "FA:"+("TABLE".equals(type) ? FontAwesome.TABLE:FontAwesome.EYE);
    }
}

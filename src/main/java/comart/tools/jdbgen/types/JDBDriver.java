/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author comart
 */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder=true)
@NoArgsConstructor
@AllArgsConstructor
public class JDBDriver extends JDBListBase {
    private boolean stockItem;
    private String urlTemplate;
    private String jdbcJar;
    private String driverClass;
    private Map<String, String> props;
    private boolean noAuth;
    private boolean useCatalogSql;
    private String catalogSql;
    private boolean useSchemaSql;
    private String schemaSql;
    private boolean useTableSql;
    private String tableSql;
    private boolean useColumnSql;
    private String columnSql;
}

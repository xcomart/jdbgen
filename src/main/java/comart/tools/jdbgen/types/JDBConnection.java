/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types;

import java.util.List;
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
@EqualsAndHashCode(callSuper=false)
@SuperBuilder(toBuilder=true)
@NoArgsConstructor
@AllArgsConstructor
public class JDBConnection extends JDBListBase {
    private String driverType;
    private String connectionUrl;
    private String userName;
    private String userPassword;
    private Map<String, String> connectionProps;
    private boolean useKeepAlive;
    private String keepAliveQuery;
    private List<JDBTemplate> templates;
    private String outputDir;
    private String author;
    private boolean dropFirstWord;
    private boolean buildJavaPackage;
    private boolean buildSqlNamespace;
}

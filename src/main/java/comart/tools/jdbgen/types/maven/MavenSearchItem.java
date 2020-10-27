/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.maven;

import comart.tools.jdbgen.types.JDBListBase;
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
public class MavenSearchItem extends JDBListBase {
    private String groupId;
    private String artifactId;
    private String description;
    
    public String getToolTip() {
        return groupId + " > " + artifactId + "\n" + description;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author comart
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JDBTemplate {
    private TemplateType type;
    private String name;
    private String templateFile;
    private String outTemplate;
}

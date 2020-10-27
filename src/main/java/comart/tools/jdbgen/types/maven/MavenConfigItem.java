/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.maven;

import java.util.List;
import lombok.Data;

/**
 *
 * @author comart
 */
@Data
public class MavenConfigItem {
    private String url;
    private List<MavenTemplate> templates;
}

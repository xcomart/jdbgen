/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.maven;

import comart.utils.XNode;
import java.util.List;
import lombok.Data;

/**
 *
 * @author comart
 */
@Data
public class MavenTemplate {
    private String name;
    private String type;
    private String attr;
    private String key;
    private boolean repeat;
    private List<MavenTemplateItem> items;
    
    public boolean isMatch(XNode node) {
        if ("findAttr".equalsIgnoreCase(type)) {
            return key.equals(node.getAttribute(attr));
        }
        // not defined other type currently.
        return false;
    }
}

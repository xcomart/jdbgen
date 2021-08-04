/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.maven;

import comart.utils.tuple.Pair;
import lombok.Data;

/**
 *
 * @author comart
 */
@Data
public class MavenTemplateItem {
    private String name;
    private String type;
    private boolean repeat;
    private String location;
    private String attr;
    private String format;
    private String formatData;
    private String delimiter;
    private Pair<String, String> attrPair;
}

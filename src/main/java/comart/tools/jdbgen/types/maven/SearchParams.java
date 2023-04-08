/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package comart.tools.jdbgen.types.maven;

import lombok.Data;

/**
 *
 * @author comart
 */
@Data
public class SearchParams {
    String q;
    String core;
    String defType;
    String qf;
    String indent;
    String spellcheck;
    String fl;
    String start;
    String spellcheck_count;
    String sort;
    String rows;
    String wt = "json";
    String version;
}

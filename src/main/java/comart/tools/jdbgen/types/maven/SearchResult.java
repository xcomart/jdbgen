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
public class SearchResult {
    int status;
    int QTime;
    SearchParams params;
    SearchResponse response;
}

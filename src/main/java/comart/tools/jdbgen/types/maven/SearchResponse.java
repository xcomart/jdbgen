/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package comart.tools.jdbgen.types.maven;

import java.util.List;
import lombok.Data;

/**
 *
 * @author comart
 */
@Data
public class SearchResponse {
    int numFound;
    int start;
    SearchResponseItem[] docs;
}

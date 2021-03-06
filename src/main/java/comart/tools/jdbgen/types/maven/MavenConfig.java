/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.maven;

import lombok.Data;

/**
 *
 * @author comart
 */
@Data
public class MavenConfig {
    private String urlBase;
    private MavenConfigItem search;
    private MavenConfigItem repository;
    private MavenConfigItem version;
    private MavenConfigItem download;
}

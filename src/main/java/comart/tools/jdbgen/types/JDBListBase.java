/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author comart
 */
@Data
@SuperBuilder(toBuilder=true)
@NoArgsConstructor
@AllArgsConstructor
public class JDBListBase implements HasIcon, HasTitle {
    private String name;
    private String icon;
    
    @Override
    public String getTitle() {
        return getName();
    }
    
    @Override
    public String toString() {
        return getTitle();
    }
}

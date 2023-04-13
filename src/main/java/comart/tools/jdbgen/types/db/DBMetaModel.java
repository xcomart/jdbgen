/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.db;

import com.google.gson.Gson;
import lombok.Data;

/**
 *
 * @author comart
 */
@Data
public class DBMetaModel {
    private int no;
    
    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.db;

import com.google.gson.Gson;
import lombok.Data;

/**
 * Common base of the database metadata models. It only carries the ordinal
 * number of the item inside its parent and renders itself as JSON, which makes
 * the models easy to inspect in the logs and in the template preview.
 *
 * @author comart
 */
@Data
public class DBMetaModel {
    /** one based position of this item inside its parent, as far as it is filled in. */
    private int no;
    
    /**
     * this model serialized as JSON.
     *
     * @return the JSON representation of all fields of this instance.
     */
    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}

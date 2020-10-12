/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.ui;

import comart.tools.jdbgen.types.JDBListBase;
import java.util.List;

/**
 *
 * @author comart
 */
public class NamingUtils {
    public static boolean nameExists(List<? extends JDBListBase> list, String name) {
        return list.stream().anyMatch((d) -> {
            return name.equals(d.getName());
        });
    }

    public static String nextNameOf(List<? extends JDBListBase> list, String name) {
        int count = 0;

        String ret;
        for(ret = name; nameExists(list, ret); ret = name + " - " + count) {
            ++count;
        }

        return ret;
    }
    
}

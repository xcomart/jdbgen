/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen;

import comart.tools.jdbgen.types.JDBGenConfig;
import comart.tools.jdbgen.ui.JDBGeneratorMain;
import comart.utils.UIUtils;

/**
 *
 * @author comart
 */
public class JDBGenerator {
    public static void main(final String[] args) {
        if (JDBGenConfig.getInstance().isDarkUI()) {
            UIUtils.setFlatDarkLaf();
        } else {
            UIUtils.setFlatLightLaf();
        }
        JDBGeneratorMain win = new JDBGeneratorMain();
        win.setLocationRelativeTo(null);
        win.setVisible(true);
    }
}

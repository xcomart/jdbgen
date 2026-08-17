/*
 * MIT License
 * 
 * Copyright (c) 2020 Dennis Soungjin Park
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package comart.tools.jdbgen;

import comart.tools.jdbgen.types.JDBGenConfig;
import comart.tools.jdbgen.ui.JDBGeneratorMain;
import comart.utils.AppDirs;
import comart.utils.I18n;
import comart.utils.PlatformUtils;
import comart.utils.UIUtils;
import java.awt.Font;
import javax.swing.UIManager;

/**
 * Entry point of the JDBGen desktop application. It brings the environment up
 * in the order the rest of the application expects: legacy data of releases up
 * to 0.3.0 is migrated into the user data directory, the language is applied,
 * the look and feel is installed, an update check is run and the main window
 * {@link comart.tools.jdbgen.ui.JDBGeneratorMain} is finally shown.
 *
 * @author comart
 */
public class JDBGenerator {
    /**
     * start the application. The steps are ordered on purpose: the legacy data
     * has to be in place before anything reads it, and the language has to be
     * settled before the first dialog - the master password prompt of
     * <code>JDBGenConfig</code> - can appear.
     *
     * @param args
     *            command line arguments; they are not evaluated.
     */
    public static void main(final String[] args) {
        // a release up to 0.3.0 kept the configuration and the driver jars next
        // to the application; they are carried over into the user data
        // directory before anything reads them.
        AppDirs.migrateLegacyData();
        // the language has to be settled before the first dialog can appear,
        // and the master password prompt of JDBGenConfig is one. The setting is
        // therefore read straight out of the configuration file, which parses
        // without a password.
        I18n.applyLanguage(JDBGenConfig.peekLanguage());
        PlatformUtils.setDockIcon();
        UIUtils.setFlatLightLaf();
        PlatformUtils.updateCheck();
        if (JDBGenConfig.getInstance().isDarkUI()) {
            UIUtils.setFlatDarkLaf();
        }
        UIManager.put("ToolTip.font", new Font("Monospaced", Font.PLAIN, 13));
        JDBGeneratorMain win = new JDBGeneratorMain();
        // the window puts itself back where it was last closed; it is only
        // centered when there is no usable stored position.
        if (!win.isLocationRestored())
            win.setLocationRelativeTo(null);
        win.setVisible(true);
    }
}

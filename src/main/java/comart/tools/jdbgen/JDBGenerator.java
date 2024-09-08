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
import comart.utils.PlatformUtils;
import comart.utils.UIUtils;
import java.awt.Font;
import javax.swing.UIManager;

/**
 *
 * @author comart
 */
public class JDBGenerator {
    public static void main(final String[] args) {
        PlatformUtils.setDockIcon();
        UIUtils.setFlatLightLaf();
        PlatformUtils.updateCheck();
        if (JDBGenConfig.getInstance().isDarkUI()) {
            UIUtils.setFlatDarkLaf();
        }
        UIManager.put("ToolTip.font", new Font("Monospaced", Font.PLAIN, 13));
        JDBGeneratorMain win = new JDBGeneratorMain();
        win.setLocationRelativeTo(null);
        win.setVisible(true);
    }
}

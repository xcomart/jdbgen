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

package comart.utils;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import comart.tools.jdbgen.types.HasIcon;
import comart.tools.jdbgen.types.HasTitle;
import comart.utils.tuple.Pair;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import jiconfont.IconCode;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.ObjectUtils;

public class UIUtils {
    private static final Logger logger = Logger.getLogger(UIUtils.class.getName());
    private static int fontSize = 14;
    private static Color color = null;
    private static final Set<Pair<JComponent, IconCode>> items = new HashSet();
    private static final Set<Container> frames = new HashSet<>();
    private static final Map<String, Icon> cachedIcon = new HashMap<>();
    private static UIDefaults uiDefaults = null;

    static {
        IconFontSwing.register(FontAwesome.getIconFont());
    }

    public UIUtils() {
    }

    @SuppressWarnings("UseSpecificCatch")
    public static void setLAF(String className) {
        boolean hasSet = false;

        try {
            UIManager.setLookAndFeel(className);
            hasSet = true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, (String)null, e);
        }

        if (!hasSet) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                logger.log(Level.SEVERE, (String)null, e);
            }
        }
        
        uiDefaults = UIManager.getLookAndFeelDefaults();

        color = uiDefaults.getColor("Button.foreground");
        fontSize = uiDefaults.getFont("Button.font").getSize();
        items.forEach((t) -> {
            addIconPrivate((JComponent)t.getFirst(), (IconCode)t.getSecond());
        });
        frames.forEach((f) -> {
            try {
                SwingUtilities.updateComponentTreeUI(f);
            } catch (Exception var2) {
            }

        });
    }

    public static Image resize(Image image) {
        return image.getScaledInstance((int)(fontSize * 1.2), (int)(fontSize * 1.2), 4);
    }

    public static void setFlatDarkLaf() {
        setLAF(FlatDarkLaf.class.getName());
    }

    public static void setFlatLightLaf() {
        setLAF(FlatLightLaf.class.getName());
    }

    public static void registerFrame(Container frame) {
        frames.add(frame);
    }

    public static void fontSizeChanged(int size) {
        fontSize = UIManager.getFont("Button.font").getSize();
        items.forEach((t) -> {
            addIconPrivate((JComponent)t.getFirst(), (IconCode)t.getSecond());
        });
    }

    public static void applyIcon(JComponent button, IconCode code, String text) {
        try {
            Method setText = button.getClass().getMethod("setText", new Class[]{String.class});
            setText.invoke(button, text);
        } catch(Throwable ignored) {}
        addIcon(button, code);
    }

    public static void applyIcon(JComponent button, IconCode code) {
        applyIcon(button, code, "");
    }

    private static void addIconPrivate(JComponent button, IconCode code) {
        try {
            Method setIcon = button.getClass().getMethod("setIcon", new Class[]{Icon.class});
            setIcon.invoke(button, IconFontSwing.buildIcon(code, (float)fontSize, color));
        } catch (Exception ignored) {}

    }

    public static void addIcon(JComponent button, IconCode code) {
        addIconPrivate(button, code);
        items.add(new Pair(button, code));
    }
    
    @SuppressWarnings({"null", "UseSpecificCatch"})
    public static synchronized Icon getIcon(String path) {
        Icon res = null;
        if (!cachedIcon.containsKey(path)) {
            boolean isStock = path.toLowerCase().startsWith("stock:");
            boolean isUrl = path.toLowerCase().startsWith("http");
            boolean isFA = path.toLowerCase().startsWith("fa:");
            String npath = path;
            if (isStock) {
                npath = "/icons/" + path.substring(6);
            }

            try {
                if (isUrl) {
                    OkHttpClient client = HttpUtils.getClient();
                    Request req = new Request.Builder().url(path).build();
                    try (Response response = client.newCall(req).execute()) {
                        res = new ImageIcon(resize(ImageIO.read(response.body().byteStream())));
                    }
                } else if (isFA) {
                    IconCode code = FontAwesome.valueOf(npath.substring(3).toUpperCase());
                    res = IconFontSwing.buildIcon( code, (float)fontSize, color);
                } else {
                    if (StrUtils.isEmpty(path))
                        npath = "/icons/generic.png";
                    try (InputStream is = isStock ? UIUtils.class.getResourceAsStream(npath) : new FileInputStream(path)) {
                        res = new ImageIcon(resize(ImageIO.read((InputStream)is)));
                    }
                }
            } catch (Exception e) {
                if (!"/icons/generic.png".equals(npath)) {
                    logger.info("Icon not found. Falling back to use default icon.");
                    res = getIcon("stock:generic.png");
                } else {
                    logger.log(Level.SEVERE, "cannot read default icon. installation may corrupted.", e);
                }
            }

            if (res != null) {
                cachedIcon.put(path, res);
            }
        } else {
            res = (Icon)cachedIcon.get(path);
        }

        return (Icon)res;
    }
    
    private static class MyListCellRenderer<T> extends DefaultListCellRenderer {
        private final Function<T,HasTitle> func;
        public MyListCellRenderer(Function<T,HasTitle> func) {
            this.func = func;
        }
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            HasTitle lb = func.apply((T)value);
            if (lb != null) {
                if (lb instanceof HasIcon) {
                    HasIcon hi = (HasIcon)lb;
                    String icon = hi.getIcon();
                    if (icon.startsWith("FA:")) {
                        UIUtils.addIcon(label, FontAwesome.valueOf(icon.substring(3)));
                    } else {
                        label.setIcon(UIUtils.getIcon(hi.getIcon()));
                    }
                }
                label.setText(lb.getTitle());
            }
            return label;
        }
        
    }
    
    public static <T> ListCellRenderer<T> getListCellRenderer(Function<T,HasTitle> func) {
        return (ListCellRenderer<T>) new MyListCellRenderer<>(func);
    }
    
    public static void applyTableEdit(JTable table) {
        DefaultTableModel tmodel = (DefaultTableModel)table.getModel();
        tmodel.addTableModelListener(e -> {
            int ridx = tmodel.getRowCount() - 1;
            boolean needToAdd = ridx < 0;

            if (!needToAdd) {
                for(int i = 0; i < tmodel.getColumnCount(); ++i) {
                    if (ObjectUtils.isNotEmpty(tmodel.getValueAt(ridx, i))) {
                        needToAdd = true;
                        break;
                    }
                }
            }

            if (needToAdd) {
                EventQueue.invokeLater(() -> {
                    tmodel.addRow(new String[tmodel.getColumnCount()]);
                });
            }
        });
    }
    
    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent, message, "Error", JOptionPane.ERROR_MESSAGE);
        logger.warning(message);
    }
    
    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
        logger.info(message);
    }
    
    public static boolean confirm(Component parent, String title, String message) {
        boolean res = JOptionPane.showConfirmDialog(
                parent, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
        logger.log(Level.INFO, "{0}: {1}", new Object[]{message, res});
        return res;
    }
    
    private static class PasswordPanel extends JPanel {
        private final String password;

        private PasswordPanel(String prompt) {
            super(new FlowLayout());
            JPasswordField pwdField = new JPasswordField(20);
            add(pwdField);
            JOptionPane joptionPane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            boolean responseOK = configure(joptionPane, prompt, pwdField).equals(JOptionPane.OK_OPTION);
            this.password = responseOK ? String.valueOf(pwdField.getPassword()) : null;
        }

        public static String getPassword(String message){
            return new PasswordPanel(message).password;
        }

        private Object configure(JOptionPane jOptionPane, String prompt, JComponent pwdField) {
            JDialog jDialog = promptDialog(prompt, jOptionPane, pwdField);
            Object result = jOptionPane.getValue();
            jDialog.dispatchEvent(new WindowEvent(jDialog, WindowEvent.WINDOW_CLOSING));
            jDialog.dispose();
            return result;
        }

        private JDialog promptDialog(String message, JOptionPane jOptionPane, JComponent pwdField) {
            JDialog dialog = jOptionPane.createDialog(message);
            dialog.addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowGainedFocus(WindowEvent e) {
                    pwdField.requestFocusInWindow();
                }
            });
            dialog.setVisible(true);
            return dialog;
        }
    }

    public static String password(String message) {
        String passwordText = PasswordPanel.getPassword(message);
        if (null != passwordText) {
            return passwordText;
        } else {
            return null;
        }
    }
}

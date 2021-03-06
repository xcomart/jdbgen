
package comart.utils;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import comart.tools.jdbgen.types.JDBListBase;
import comart.utils.tuple.Pair;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Image;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
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
    private static final Set<Pair<AbstractButton, IconCode>> items = new HashSet();
    private static final Set<Container> frames = new HashSet<>();
    private static final Map<String, Icon> cachedIcon = new HashMap<>();
    private static UIDefaults uiDefaults = null;

    static {
        IconFontSwing.register(FontAwesome.getIconFont());
    }

    public UIUtils() {
    }

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
            addIconPrivate((AbstractButton)t.getFirst(), (IconCode)t.getSecond());
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
            addIconPrivate((AbstractButton)t.getFirst(), (IconCode)t.getSecond());
        });
    }

    public static void applyIcon(AbstractButton button, IconCode code, String text) {
        button.setText(text);
        addIcon(button, code);
    }

    public static void applyIcon(AbstractButton button, IconCode code) {
        applyIcon(button, code, "");
    }

    private static void addIconPrivate(AbstractButton button, IconCode code) {
        try {
            button.setIcon(IconFontSwing.buildIcon(code, (float)fontSize, color));
        } catch (Exception ignored) {}

    }

    public static void addIcon(AbstractButton button, IconCode code) {
        addIconPrivate(button, code);
        items.add(new Pair(button, code));
    }
    
    public static synchronized Icon getIcon(String path) {
        Icon res = null;
        if (!cachedIcon.containsKey(path)) {
            boolean isStock = path.startsWith("stock:");
            boolean isUrl = path.startsWith("http");
            String npath = path;
            if (isStock) {
                npath = "/icons/" + path.substring(6);
            }

            try {
                if (isUrl) {
                    OkHttpClient client = MavenUtils.getHttpClient();
                    Request req = new Request.Builder().url(path).build();
                    try (Response response = client.newCall(req).execute()) {
                        res = new ImageIcon(resize(ImageIO.read(response.body().byteStream())));
                    }
                } else {
                    try (InputStream is = isStock ? UIUtils.class.getResourceAsStream(npath) : new FileInputStream(path)) {
                        res = new ImageIcon(resize(ImageIO.read((InputStream)is)));
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, (String)null, e);
                if (!"/icons/generic.png".equals(npath)) {
                    res = getIcon("stock:generic.png");
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
        private final Function<T,JDBListBase> func;
        public MyListCellRenderer(Function<T,JDBListBase> func) {
            this.func = func;
        }
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            JDBListBase lb = func.apply((T)value);
            if (lb != null) {
                label.setIcon(UIUtils.getIcon(lb.getIcon()));
                label.setText(lb.getName());
            }
            return label;
        }
        
    }
    
    public static <T> ListCellRenderer<T> getListCellRenderer(Function<T,JDBListBase> func) {
        return new MyListCellRenderer(func);
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
                parent, message, "Confirm", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
        logger.log(Level.INFO, "{0}: {1}", new Object[]{message, res});
        return res;
    }
}

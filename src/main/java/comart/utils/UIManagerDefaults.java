
package comart.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Painter;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class UIManagerDefaults implements ActionListener, ItemListener {
    private static final Logger logger = Logger.getLogger(UIManagerDefaults.class.getName());
    private static final String[] COLUMN_NAMES = new String[]{"Key", "Value", "Sample"};
    private static String selectedItem;
    private final JComponent contentPane = new JPanel(new BorderLayout());
    private JMenuBar menuBar;
    private JComboBox<String> comboBox;
    private JRadioButton byComponent;
    private JTable table;
    private final TreeMap<String, TreeMap<String, Object>> items = new TreeMap();
    private final HashMap<String, DefaultTableModel> models = new HashMap();

    public UIManagerDefaults() {
        this.contentPane.add(this.buildNorthComponent(), "North");
        this.contentPane.add(this.buildCenterComponent(), "Center");
        this.resetComponents();
    }

    public JComponent getContentPane() {
        return this.contentPane;
    }

    public JMenuBar getMenuBar() {
        if (this.menuBar == null) {
            this.menuBar = this.createMenuBar();
        }

        return this.menuBar;
    }

    private JComponent buildNorthComponent() {
        this.comboBox = new JComboBox();
        JLabel label = new JLabel("Select Item:");
        label.setDisplayedMnemonic('S');
        label.setLabelFor(this.comboBox);
        this.byComponent = new JRadioButton("By Component", true);
        this.byComponent.setMnemonic('C');
        this.byComponent.addActionListener(this);
        JRadioButton byValueType = new JRadioButton("By Value Type");
        byValueType.setMnemonic('V');
        byValueType.addActionListener(this);
        ButtonGroup group = new ButtonGroup();
        group.add(this.byComponent);
        group.add(byValueType);
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(15, 0, 15, 0));
        panel.add(label);
        panel.add(this.comboBox);
        panel.add(this.byComponent);
        panel.add(byValueType);
        return panel;
    }

    private JComponent buildCenterComponent() {
        DefaultTableModel model = new DefaultTableModel(COLUMN_NAMES, 0);
        this.table = new JTable(model);
        this.table.setAutoCreateColumnsFromModel(false);
        this.table.getColumnModel().getColumn(0).setPreferredWidth(250);
        this.table.getColumnModel().getColumn(1).setPreferredWidth(500);
        this.table.getColumnModel().getColumn(2).setPreferredWidth(100);
        this.table.getColumnModel().getColumn(2).setCellRenderer(new UIManagerDefaults.SampleRenderer());
        Dimension d = this.table.getPreferredSize();
        d.height = 350;
        this.table.setPreferredScrollableViewportSize(d);
        return new JScrollPane(this.table);
    }

    public final void resetComponents() {
        this.items.clear();
        this.models.clear();
        ((DefaultTableModel)this.table.getModel()).setRowCount(0);
        this.buildItemsMap();
        ArrayList<String> comboBoxItems = new ArrayList(50);
        this.items.keySet().forEach((key) -> {
            comboBoxItems.add(key);
        });
        this.comboBox.removeItemListener(this);
        this.comboBox.setModel(new DefaultComboBoxModel(comboBoxItems.toArray(new String[comboBoxItems.size()])));
        this.comboBox.setSelectedIndex(-1);
        this.comboBox.addItemListener(this);
        this.comboBox.requestFocusInWindow();
        if (selectedItem != null) {
            this.comboBox.setSelectedItem(selectedItem);
        }

    }

    private void buildItemsMap() {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Enumeration enumm = defaults.keys();

        while(enumm.hasMoreElements()) {
            Object key = enumm.nextElement();
            Object value = defaults.get(key);
            String itemName = this.getItemName(key.toString(), value);
            if (itemName != null) {
                TreeMap<String, Object> attributeMap = (TreeMap)this.items.computeIfAbsent(itemName, (k) -> {
                    return new TreeMap();
                });
                attributeMap.put(key.toString(), value);
            }
        }

    }

    private String getItemName(String key, Object value) {
        if (!key.startsWith("class") && !key.startsWith("javax")) {
            return this.byComponent.isSelected() ? this.getComponentName(key, value) : this.getValueName(key, value);
        } else {
            return null;
        }
    }

    private String getComponentName(String key, Object value) {
        int pos = this.componentNameEndOffset(key);
        String componentName;
        if (pos != -1) {
            componentName = key.substring(0, pos);
        } else if (key.endsWith("UI")) {
            componentName = key.substring(0, key.length() - 2);
        } else if (value instanceof ColorUIResource) {
            componentName = "System Colors";
        } else {
            componentName = "Miscellaneous";
        }

        if (componentName.equals("Checkbox")) {
            componentName = "CheckBox";
        }

        return componentName;
    }

    private int componentNameEndOffset(String key) {
        if (key.startsWith("\"")) {
            return key.indexOf("\"", 1) + 1;
        } else {
            int pos = key.indexOf(":");
            if (pos != -1) {
                return pos;
            } else {
                pos = key.indexOf("[");
                return pos != -1 ? pos : key.indexOf(".");
            }
        }
    }

    private String getValueName(String key, Object value) {
        if (value instanceof Icon) {
            return "Icon";
        } else if (value instanceof Font) {
            return "Font";
        } else if (value instanceof Border) {
            return "Border";
        } else if (value instanceof Color) {
            return "Color";
        } else if (value instanceof Insets) {
            return "Insets";
        } else if (value instanceof Boolean) {
            return "Boolean";
        } else if (value instanceof Dimension) {
            return "Dimension";
        } else if (value instanceof Number) {
            return "Number";
        } else if (value instanceof Painter) {
            return "Painter";
        } else if (key.endsWith("UI")) {
            return "UI";
        } else if (key.endsWith("InputMap")) {
            return "InputMap";
        } else if (key.endsWith("RightToLeft")) {
            return "InputMap";
        } else {
            return key.endsWith("radient") ? "Gradient" : "The Rest";
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar newMenuBar = new JMenuBar();
        newMenuBar.add(this.createFileMenu());
        newMenuBar.add(this.createLAFMenu());
        return newMenuBar;
    }

    private JMenu createFileMenu() {
        JMenu menu = new JMenu("Application");
        menu.setMnemonic('A');
        menu.addSeparator();
        menu.add(new UIManagerDefaults.ExitAction());
        return menu;
    }

    private JMenu createLAFMenu() {
        ButtonGroup bg = new ButtonGroup();
        JMenu menu = new JMenu("Look & Feel");
        menu.setMnemonic('L');
        String lafId = UIManager.getLookAndFeel().getID();
        LookAndFeelInfo[] lafInfo = UIManager.getInstalledLookAndFeels();
        LookAndFeelInfo[] var5 = lafInfo;
        int var6 = lafInfo.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            LookAndFeelInfo lookAndFeelInfo = var5[var7];
            String laf = lookAndFeelInfo.getClassName();
            String name = lookAndFeelInfo.getName();
            Action action = new UIManagerDefaults.ChangeLookAndFeelAction(this, laf, name);
            JRadioButtonMenuItem mi = new JRadioButtonMenuItem(action);
            menu.add(mi);
            bg.add(mi);
            if (name.equals(lafId)) {
                mi.setSelected(true);
            }
        }

        return menu;
    }

    public void actionPerformed(ActionEvent e) {
        selectedItem = null;
        this.resetComponents();
        this.comboBox.requestFocusInWindow();
    }

    public void itemStateChanged(ItemEvent e) {
        String itemName = (String)e.getItem();
        this.changeTableModel(itemName);
        this.updateRowHeights();
        selectedItem = itemName;
    }

    private void changeTableModel(String itemName) {
        DefaultTableModel model = (DefaultTableModel)this.models.get(itemName);
        if (model != null) {
            this.table.setModel(model);
        } else {
            model = new DefaultTableModel(COLUMN_NAMES, 0);
            Map<String, Object> attributes = (Map)this.items.get(itemName);

            ArrayList row;
            for(Iterator var4 = attributes.keySet().iterator(); var4.hasNext(); model.addRow(row.toArray(new Object[row.size()]))) {
                String attribute = (String)var4.next();
                Object value = attributes.get(attribute);
                row = new ArrayList(3);
                row.add(attribute);
                if (value != null) {
                    row.add(value.toString());
                    if (value instanceof Icon) {
                        value = new UIManagerDefaults.SafeIcon((Icon)value);
                    }

                    row.add(value);
                } else {
                    row.add("null");
                    row.add("");
                }
            }

            this.table.setModel(model);
            this.models.put(itemName, model);
        }
    }

    private void updateRowHeights() {
        try {
            for(int row = 0; row < this.table.getRowCount(); ++row) {
                int rowHeight = this.table.getRowHeight();

                for(int column = 0; column < this.table.getColumnCount(); ++column) {
                    Component comp = this.table.prepareRenderer(this.table.getCellRenderer(row, column), row, column);
                    rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
                }

                this.table.setRowHeight(row, rowHeight);
            }
        } catch (ClassCastException var5) {
        }

    }

    private static void createAndShowGUI() {
        UIManagerDefaults application = new UIManagerDefaults();
        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new JFrame("UIManager Defaults");
        frame.setDefaultCloseOperation(3);
        frame.setJMenuBar(application.getMenuBar());
        frame.getContentPane().add(application.getContentPane());
        frame.pack();
        frame.setLocationRelativeTo((Component)null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UIManagerDefaults::createAndShowGUI);
    }

    static class ExitAction extends AbstractAction {
        public ExitAction() {
            this.putValue("Name", "Exit");
            this.putValue("ShortDescription", this.getValue("Name"));
            this.putValue("MnemonicKey", 88);
        }

        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }

    static class ChangeLookAndFeelAction extends AbstractAction {
        private final UIManagerDefaults defaults;
        private final String laf;

        protected ChangeLookAndFeelAction(UIManagerDefaults defaults, String laf, String name) {
            this.defaults = defaults;
            this.laf = laf;
            this.putValue("Name", name);
            this.putValue("ShortDescription", this.getValue("Name"));
        }

        public void actionPerformed(ActionEvent e) {
            try {
                UIManager.setLookAndFeel(this.laf);
                this.defaults.resetComponents();
                JMenuItem mi = (JMenuItem)e.getSource();
                JPopupMenu popup = (JPopupMenu)mi.getParent();
                JRootPane rootPane = SwingUtilities.getRootPane(popup.getInvoker());
                SwingUtilities.updateComponentTreeUI(rootPane);
                JFrame frame = (JFrame)SwingUtilities.windowForComponent(rootPane);
                frame.dispose();
                if (UIManager.getLookAndFeel().getSupportsWindowDecorations()) {
                    frame.setUndecorated(true);
                    frame.getRootPane().setWindowDecorationStyle(1);
                } else {
                    frame.setUndecorated(false);
                }

                frame.setVisible(true);
            } catch (IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException | ClassNotFoundException var6) {
                System.out.println("Failed loading L&F: " + this.laf);
                UIManagerDefaults.logger.log(Level.SEVERE, (String)null, var6);
            }

        }
    }

    static class SampleRenderer extends JLabel implements TableCellRenderer {
        public SampleRenderer() {
            this.setHorizontalAlignment(0);
            this.setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object sample, boolean isSelected, boolean hasFocus, int row, int column) {
            this.setBackground((Color)null);
            this.setBorder((Border)null);
            this.setIcon((Icon)null);
            this.setText("");
            if (sample instanceof Color) {
                this.setBackground((Color)sample);
            } else if (sample instanceof Border) {
                this.setBorder((Border)sample);
            } else if (sample instanceof Font) {
                this.setText("Sample");
                this.setFont((Font)sample);
            } else if (sample instanceof Icon) {
                this.setIcon((Icon)sample);
            }

            return this;
        }

        public void paint(Graphics g) {
            try {
                super.paint(g);
            } catch (Exception var3) {
                UIManagerDefaults.logger.log(Level.SEVERE, (String)null, var3);
            }

        }
    }

    public static class SafeIcon implements Icon {
        private final Icon wrappee;
        private Icon standIn;

        public SafeIcon(Icon wrappee) {
            this.wrappee = wrappee;
        }

        public int getIconHeight() {
            return this.wrappee.getIconHeight();
        }

        public int getIconWidth() {
            return this.wrappee.getIconWidth();
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (this.standIn == this) {
                this.paintFallback(g, x, y);
            } else if (this.standIn != null) {
                this.standIn.paintIcon(c, g, x, y);
            } else {
                try {
                    this.wrappee.paintIcon(c, g, x, y);
                } catch (ClassCastException var6) {
                    this.createStandIn(var6);
                    this.standIn.paintIcon(c, g, x, y);
                }
            }

        }

        private void createStandIn(ClassCastException e) {
            try {
                Class<?> clazz = this.getClass(e);
                JComponent standInComponent = this.getSubstitute(clazz);
                this.standIn = this.createImageIcon(standInComponent);
            } catch (IllegalAccessException | ClassNotFoundException var4) {
                UIManagerDefaults.logger.log(Level.SEVERE, (String)null, var4);
                this.standIn = this;
            }

        }

        private Icon createImageIcon(JComponent standInComponent) {
            BufferedImage image = new BufferedImage(this.getIconWidth(), this.getIconHeight(), 2);
            Graphics2D g = image.createGraphics();

            ImageIcon var4;
            try {
                this.wrappee.paintIcon(standInComponent, g, 0, 0);
                var4 = new ImageIcon(image);
            } finally {
                g.dispose();
            }

            return var4;
        }

        private JComponent getSubstitute(Class<?> clazz) throws IllegalAccessException {
            Object standInComponent;
            try {
                standInComponent = (JComponent)clazz.newInstance();
            } catch (InstantiationException var4) {
                standInComponent = new AbstractButton() {
                };
                ((AbstractButton)standInComponent).setModel(new DefaultButtonModel());
            }

            return (JComponent)standInComponent;
        }

        private Class<?> getClass(ClassCastException e) throws ClassNotFoundException {
            String className = e.getMessage();
            className = className.substring(className.lastIndexOf(" ") + 1);
            return Class.forName(className);
        }

        private void paintFallback(Graphics g, int x, int y) {
            g.drawRect(x, y, this.getIconWidth(), this.getIconHeight());
            g.drawLine(x, y, x + this.getIconWidth(), y + this.getIconHeight());
            g.drawLine(x + this.getIconWidth(), y, x, y + this.getIconHeight());
        }
    }
}

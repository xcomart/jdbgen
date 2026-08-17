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
package comart.tools.jdbgen.ui;

import comart.tools.jdbgen.template.TemplateManager;
import comart.tools.jdbgen.types.JDBAbbr;
import comart.tools.jdbgen.types.JDBConnection;
import comart.tools.jdbgen.types.JDBDriver;
import comart.tools.jdbgen.types.JDBGenConfig;
import comart.tools.jdbgen.types.JDBTemplate;
import comart.tools.jdbgen.types.WindowState;
import comart.tools.jdbgen.types.db.DBMeta;
import comart.tools.jdbgen.types.db.DBSchema;
import comart.tools.jdbgen.types.db.DBTable;
import comart.utils.AppDirs;
import comart.utils.I18n;
import comart.utils.ObjUtils;
import comart.utils.PlatformUtils;
import comart.utils.StrUtils;
import comart.utils.UIUtils;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.LayerUI;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import jiconfont.icons.font_awesome.FontAwesome;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;

/**
 * main window of the application. It opens the connection chosen in the
 * connection manager, shows its catalog/schema tree and the tables of the
 * selected schema, and runs the code generation for the tables and templates
 * the user ticked.
 * <p>
 * It also hosts the global settings that are stored in the configuration: the
 * dark user interface flag, the interface language and whether the
 * abbreviation mapping is applied. Opening a connection and generating code are
 * both done on background threads, the window is left in a disabled state
 * meanwhile.
 *
 * @author comart
 */
@Slf4j
public class JDBGeneratorMain extends javax.swing.JFrame {

    /**
     * label user interface that keeps a label from growing with its text.
     * The layout is computed for at most the first 20 characters, so a long
     * connection url cannot widen the label, while the full text is still
     * returned for painting.
     */
    private static class MyLabelUI extends BasicLabelUI {
        /**
         * lay the label out for at most the first 20 characters, but return the
         * full text so that it is painted unclipped.
         *
         * @param label
         *            the label being laid out.
         * @param fontMetrics
         *            the metrics of the label's font.
         * @param text
         *            the label's text, may be <code>null</code>.
         * @param icon
         *            the label's icon, may be <code>null</code>.
         * @param viewR
         *            the area available to the label, filled in by the caller.
         * @param iconR
         *            receives the computed icon bounds.
         * @param textR
         *            receives the computed text bounds.
         * @return the unmodified <code>text</code>.
         */
        @Override
        protected String layoutCL(
                JLabel label, FontMetrics fontMetrics, String text, Icon icon,
                Rectangle viewR, Rectangle iconR, Rectangle textR) {
            
            String clipped = text == null ?
                    "" : text.substring(0, Math.min(20, text.length()));
            super.layoutCL(
                    label, fontMetrics, clipped, icon, viewR, iconR, textR);
            return text;
        }
    }

    /**
     * Give the connection info label the UI that caps its width. A
     * look-and-feel change replaces every UI delegate, so this has to run
     * again afterwards or a long url widens the label and pushes the panels
     * on the right out of the window.
     */
    private void applyConnectionInfoUI() {
        lblConnectionInfo.setUI(new MyLabelUI());
        lblConnectionInfo.setAutoscrolls(true);
    }

    /**
     * the stored <code>language</code> value of every entry of the language
     * menu, in the order the entries appear. <code>null</code> is the
     * operating system locale.
     */
    private static final String[] LANGUAGES = { null, "en", "ko", "es", "ja", "zh-CN" };

    /**
     * the configuration the connections, the drivers and the global settings
     * are read from and written back to.
     */
    private final JDBGenConfig conf;
    /**
     * the connections by name, used by the combo box renderer to find the icon
     * of an entry.
     */
    private final Map<String, JDBConnection> connMap = new HashMap<>();
    /**
     * the connection currently applied to the window, or <code>null</code>
     * before the first one has been chosen.
     */
    private JDBConnection currConn = null;
    /**
     * the metadata of the open connection, or <code>null</code> while none is
     * open. Everything that reads the database has to check this first.
     */
    private DBMeta dbmeta = null;
    /**
     * the tables of the schema selected in the tree, in the order they are
     * shown in the table list. <code>null</code> while no schema is selected.
     */
    private List<DBTable> tables;
    /**
     * the tables of {@link #tables} that pass the current text of
     * {@link #txtTableFilter}, in the order they are shown in the table list.
     * The list box only ever displays this subset, so every consumer that
     * resolves a list index - a selection, a mouse position - has to read this
     * field instead of <code>tables</code>. <code>null</code> while no schema
     * is selected.
     */
    private List<DBTable> visibleTables;
    /**
     * guard against feedback while the variable table is being filled
     * programmatically. While <code>false</code>, the table model listener does
     * not append a trailing empty row.
     */
    private boolean autoReset = true;
    /** true while a connection is being opened on a background thread */
    private boolean connecting = false;
    /**
     * whether the stored window position was restored while the window was
     * built, see {@link #isLocationRestored()}. When it was not, the caller
     * that shows the window centers it.
     */
    private boolean locationRestored = false;
    
    /**
     * the main window, assigned at the end of its construction so that the
     * other dialogs can reach it. <code>null</code> until then.
     */
    public static JDBGeneratorMain INSTANCE = null;
    
    /**
     * the tables of the schema currently selected in the tree.
     *
     * @return the table list shown in the table list box, or <code>null</code>
     *         while no schema is selected.
     */
    public List<DBTable> getTables() {
        return tables;
    }
    
    /**
     * Build the main window. Restores the stored settings, asks for a
     * connection through the connection manager - the application exits when
     * that dialog is cancelled - and registers the platform handlers for the
     * about menu entry.
     */
    public JDBGeneratorMain() {
        initComponents();
        conf = JDBGenConfig.getInstance();
        chkApplyAbbr.setSelected(conf.isApplyAbbr());
        buildMenuBar();
        initTableListPopup();
        treSchemas.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        UIUtils.setApplicationIcon(this);

        applyConnectionInfoUI();

        EventQueue.invokeLater(() -> {
            JDBConnectionManager cm = JDBConnectionManager.getInstance();
            cm.setLocationRelativeTo(this);
            cm.setVisible(true);
            if (cm.selectedConnection == null)
                System.exit(0);
            applyConnection(cm.selectedConnection);
        });

        initTemplates();
        applyIcons();
        applyTooltips();
        clearContents();

        tabVars.getModel().addTableModelListener((evt) -> {
            if (autoReset) {
                UIUtils.tableSetLastEmpty(tabVars.getModel());
            }
        });
        UIUtils.setCommitOnLostFocus(tabVars);
        initTemplateActions();
        // the window is EXIT_ON_CLOSE, and this runs before it exits: the
        // generation options and the window geometry are stored whichever way
        // the window is left. The dividers can only be placed once the window
        // is on screen, which windowOpened is the first moment of.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveOnExit();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                applyStoredDividers();
            }
        });

        PlatformUtils.registerHandlers(e -> showAbout(), null, null, null);
        log.info("before pack");
        this.pack();
        log.info("after pack");
        // below its minimum the table list is squeezed to nothing and the
        // panels beside it start losing their content: do not let the window
        // be resized to where that happens. The computed minimum has been
        // measured to come out one layout gap short of what actually fits,
        // hence the extra slack on top of it.
        Dimension minSize = getMinimumSize();
        setMinimumSize(new Dimension(minSize.width + 12, minSize.height));
        // whatever the last run left behind wins over the packed size; without
        // a stored geometry nothing here changes the window.
        locationRestored = restoreWindowState();
        INSTANCE = this;
    }
    
    /**
     * show the about dialog.
     */
    private void showAbout() {
        JDBAbout.getInstance(this).setVisible(true);
    }

    /**
     * open the driver manager on top of this window. The connection manager
     * opens the same shared dialog, this is only the second way in.
     */
    private void showDriverManager() {
        JDBDriverManager dm = JDBDriverManager.getInstance();
        dm.setModal(true);
        dm.setLocationRelativeTo(this);
        dm.setVisible(true);
    }

    /**
     * open the abbreviation mapper on top of this window.
     */
    private void showAbbreviationMapper() {
        JDBAbbreviationMapper.getInstance(this).setVisible(true);
    }

    /**
     * the dark user interface entry of the view menu. It is the one place the
     * flag is shown and switched, so it is also what its state is read from.
     * <code>null</code> until the menu bar is built.
     */
    private JCheckBoxMenuItem miDarkUI;

    /**
     * a menu item with its text taken from the resource bundle.
     *
     * @param key
     *            the resource key of the item text.
     * @param action
     *            what the item does when it is chosen.
     * @return the new item, not attached to a menu yet.
     */
    private static JMenuItem menuItem(String key, java.awt.event.ActionListener action) {
        JMenuItem item = new JMenuItem(I18n.t(key));
        item.addActionListener(action);
        return item;
    }

    /**
     * Build the menu bar of the window. Every entry calls the same method the
     * control it belongs to calls, so that both ways in stay in step by
     * construction. Call after <code>initComponents()</code> and after
     * <code>conf</code> has been read, both are used here.
     */
    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenu mnuFile = new JMenu(I18n.t("generatorMain.menu.file"));
        JMenuItem miGenerate = menuItem("generatorMain.menu.file.generate",
                e -> generate());
        miGenerate.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, shortcut));
        // the generate button is disabled while a connection is being opened,
        // mirror that instead of letting the accelerator through meanwhile
        miGenerate.setEnabled(btnGenerate.isEnabled());
        btnGenerate.addPropertyChangeListener("enabled",
                e -> miGenerate.setEnabled(btnGenerate.isEnabled()));
        mnuFile.add(miGenerate);
        mnuFile.addSeparator();
        mnuFile.add(menuItem("generatorMain.menu.file.close",
                e -> closeApplication()));
        menuBar.add(mnuFile);

        JMenu mnuTools = new JMenu(I18n.t("generatorMain.menu.tools"));
        mnuTools.add(menuItem("generatorMain.menu.tools.connectionManager",
                e -> showConnectionManager()));
        mnuTools.add(menuItem("generatorMain.menu.tools.driverManager",
                e -> showDriverManager()));
        mnuTools.addSeparator();
        mnuTools.add(menuItem("generatorMain.menu.tools.abbreviationMapper",
                e -> showAbbreviationMapper()));
        menuBar.add(mnuTools);

        JMenu mnuView = new JMenu(I18n.t("generatorMain.menu.view"));
        miDarkUI = new JCheckBoxMenuItem(I18n.t("generatorMain.menu.view.darkUI"));
        miDarkUI.setSelected(conf.isDarkUI());
        miDarkUI.addActionListener(e -> applyDarkUI(miDarkUI.isSelected()));
        mnuView.add(miDarkUI);
        mnuView.add(buildLanguageMenu());
        menuBar.add(mnuView);

        JMenu mnuHelp = new JMenu(I18n.t("generatorMain.menu.help"));
        mnuHelp.add(menuItem("generatorMain.menu.help.about", e -> showAbout()));
        mnuHelp.add(menuItem("generatorMain.menu.help.templateReference",
                e -> PlatformUtils.openDoc("template-reference.md")));
        menuBar.add(mnuHelp);

        setJMenuBar(menuBar);
    }

    /**
     * the language submenu, one radio entry per supported language. It is the
     * only place the language is chosen, so the radio group is also what shows
     * which one is stored.
     *
     * @return the language submenu, not attached to a menu yet.
     */
    private JMenu buildLanguageMenu() {
        JMenu mnuLanguage = new JMenu(I18n.t("generatorMain.menu.view.language"));
        mnuLanguage.setToolTipText(I18n.t("common.language.tooltip"));
        ButtonGroup group = new ButtonGroup();
        String[] names = languageNames();
        int selected = languageIndex(conf.getLanguage());
        for (int i=0; i<names.length; i++) {
            final int idx = i;
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(names[i]);
            item.setSelected(i == selected);
            item.addActionListener(e -> applyLanguage(idx));
            group.add(item);
            mnuLanguage.add(item);
        }
        return mnuLanguage;
    }

    /**
     * Give the table list a "select all" / "clear selection" popup. Both act
     * on what is currently shown, i.e. on <code>lstTables</code>'s own model,
     * so they automatically respect whatever the filter field narrowed the
     * list down to. This listener is added on top of the one the list already
     * has for the double click, Swing dispatches to every listener of a
     * component so the two do not interfere with each other.
     */
    private void initTableListPopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(menuItem("generatorMain.lstTables.popup.selectAll", e -> {
            int size = lstTables.getModel().getSize();
            if (size > 0)
                lstTables.setSelectionInterval(0, size - 1);
        }));
        menu.add(menuItem("generatorMain.lstTables.popup.clearSelection",
                e -> lstTables.clearSelection()));
        lstTables.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }
            /** the popup trigger is platform dependent, hence both handlers. */
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger())
                    menu.show(lstTables, e.getX(), e.getY());
            }
        });
    }

    /**
     * the tables of <code>tables</code> whose name contains
     * <code>filterText</code>, matched case-insensitively. This is the only
     * place the table list filter matches a name, so the list box and its
     * tests apply exactly the same rule.
     *
     * @param tables
     *            the full table list of the selected schema, may be
     *            <code>null</code>.
     * @param filterText
     *            the text typed into the filter field; every table is kept
     *            when it is <code>null</code> or blank.
     * @return the tables that pass the filter, in the order of
     *         <code>tables</code>. Never <code>null</code>, empty when
     *         <code>tables</code> is <code>null</code>.
     */
    static List<DBTable> filterTables(List<DBTable> tables, String filterText) {
        List<DBTable> res = new ArrayList<>();
        if (tables == null)
            return res;
        if (StrUtils.isEmpty(filterText)) {
            res.addAll(tables);
            return res;
        }
        String needle = filterText.toLowerCase();
        for (DBTable t: tables) {
            if (t.getTable() != null && t.getTable().toLowerCase().contains(needle))
                res.add(t);
        }
        return res;
    }

    /**
     * narrow the table list box to the tables of <code>tables</code> that
     * pass the current text of {@link #txtTableFilter}, and rebuild
     * {@link #visibleTables} to match. Every place that resolves a list index
     * of <code>lstTables</code> - the generate button, the double click, the
     * hover tooltip - reads <code>visibleTables</code> instead of
     * <code>tables</code>, since the list box only ever shows this subset.
     * Call whenever <code>tables</code> changes and whenever the filter text
     * changes.
     */
    private void applyTableFilter() {
        visibleTables = filterTables(tables, txtTableFilter.getText());
        DefaultListModel<String> listModel = new DefaultListModel<>();
        visibleTables.forEach(t -> listModel.addElement(t.getTable()));
        lstTables.setModel(listModel);
        lstTables.setCellRenderer(UIUtils.getListCellRenderer(
                s -> visibleTables.stream()
                    .filter(t -> s.equals(t.getName()))
                    .findFirst().orElse(null)));
    }

    /**
     * empty the template table, the variable table, the table list and the
     * author and output directory fields.
     */
    private void clearContents() {
        ((DefaultTableModel)this.tabTemplates.getModel()).setRowCount(0);
        ((DefaultTableModel)this.tabVars.getModel()).setRowCount(0);
        this.lstTables.removeAll();
        this.txtAuthor.setText("");
        this.txtOutputDir.setText("");
    }

    /**
     * The column names a table model is built with are untranslated
     * placeholders, the shown ones are set here.
     *
     * @param table
     *            the table whose column headers are replaced.
     * @param keys
     *            the resource keys of the header texts, one per column,
     *            starting at the first column. Keys beyond the last column are
     *            ignored.
     */
    private static void applyHeaders(JTable table, String... keys) {
        TableColumnModel colModel = table.getColumnModel();
        for (int i=0; i<keys.length && i<colModel.getColumnCount(); i++)
            colModel.getColumn(i).setHeaderValue(I18n.t(keys[i]));
    }

    /**
     * set up the template and variable tables: fixed column widths, translated
     * headers and a click on the header of the check box column that ticks or
     * unticks every template at once.
     */
    private void initTemplates() {
        tabTemplates.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColumnModel colModel = tabTemplates.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(50);
        colModel.getColumn(1).setPreferredWidth(130);
        colModel.getColumn(2).setPreferredWidth(130);
        colModel.getColumn(3).setPreferredWidth(130);
        applyHeaders(tabTemplates,
                "generatorMain.tabTemplates.column.select",
                "generatorMain.tabTemplates.column.name",
                "generatorMain.tabTemplates.column.templateFile",
                "generatorMain.tabTemplates.column.outTemplate");
        applyHeaders(tabVars,
                "generatorMain.tabVars.column.name",
                "generatorMain.tabVars.column.value");

        DefaultTableModel tpls = (DefaultTableModel)this.tabTemplates.getModel();
        JTableHeader tplHeader = this.tabTemplates.getTableHeader();
        tplHeader.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                if (tabTemplates.columnAtPoint(p) == 0) {
                    if (tpls.getRowCount() > 0) {
                        Object val = tpls.getValueAt(0, 0);
                        boolean checked = val != null ? (boolean)val:false;
                        for (int i=0; i<tpls.getRowCount(); i++)
                            tpls.setValueAt(!checked, i, 0);
                    }
                }
            }
        });
    }
    
    /**
     * apply the font icons of the buttons of this window.
     */
    private void applyIcons() {
        UIUtils.addIcon(btnManageConn, FontAwesome.COG);
        UIUtils.applyIcon(btnDelVar, FontAwesome.MINUS);
        UIUtils.addIcon(btnGenerate, FontAwesome.COGS);
        UIUtils.addIcon(btnClose, FontAwesome.TIMES);
        UIUtils.applyIcon(btnBrowseOutput, FontAwesome.FOLDER_O);
    }

    /**
     * an example of an output file name, shown in the tool tip of the template
     * table. It is not translated and it is handed to the message as an
     * argument, because the braces of a template variable are not something a
     * message pattern can hold literally.
     */
    private static final String OUT_TEMPLATE_EXAMPLE = "${name.pascal}.java";

    /**
     * describe the inputs that are not self explanatory. The template table
     * itself already shows the row under the mouse, so its explanation goes on
     * the table header instead of overwriting that.
     */
    private void applyTooltips() {
        tabTemplates.getTableHeader().setToolTipText(
                I18n.t("generatorMain.tabTemplates.header.toolTipText",
                        OUT_TEMPLATE_EXAMPLE));
        tabVars.setToolTipText(I18n.t("generatorMain.tabVars.toolTipText"));
        txtOutputDir.setToolTipText(I18n.t("generatorMain.txtOutputDir.toolTipText"));
    }

    /**
     * remove the selected custom variable, or clear it when it is the only row
     * left. The table model listener puts the trailing input row back.
     */
    private void removeSelectedVar() {
        int row = tabVars.getSelectedRow();
        if (row < 0)
            return;
        DefaultTableModel model = (DefaultTableModel)tabVars.getModel();
        if (model.getRowCount() > 1) {
            model.removeRow(row);
        } else {
            for (int i=0; i<model.getColumnCount(); i++)
                model.setValueAt("", row, i);
        }
    }

    /**
     * Give the template table its popup menu and its double click. The
     * templates of a connection are edited here and nowhere else, so adding,
     * editing, removing and the presets all hang off this table.
     */
    private void initTemplateActions() {
        // an empty table is otherwise zero pixels high inside its scroll pane,
        // and the popup below could never be reached to add the first template
        tabTemplates.setFillsViewportHeight(true);
        JPopupMenu menu = new JPopupMenu();
        menu.add(menuItem("generatorMain.tabTemplates.popup.add", e -> addTemplate()));
        menu.add(menuItem("generatorMain.tabTemplates.popup.edit", e -> editTemplate()));
        menu.add(menuItem("generatorMain.tabTemplates.popup.delete", e -> removeTemplate()));
        menu.addSeparator();
        menu.add(menuItem("generatorMain.tabTemplates.popup.presets", e -> showPresets()));
        tabTemplates.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                // a click in the tick column is the tick itself, everywhere
                // else a double click opens the row
                if (e.getClickCount() == 2 && !e.isPopupTrigger()
                        && tabTemplates.columnAtPoint(e.getPoint()) > 0)
                    editTemplate();
            }
            /** the popup trigger is platform dependent, hence both handlers. */
            private void showPopup(MouseEvent e) {
                if (!e.isPopupTrigger())
                    return;
                int row = tabTemplates.rowAtPoint(e.getPoint());
                if (row > -1)
                    tabTemplates.setRowSelectionInterval(row, row);
                else
                    tabTemplates.clearSelection();
                menu.show(tabTemplates, e.getX(), e.getY());
            }
        });
        installEmptyTemplateHint();
    }

    /**
     * a freshly opened connection has no templates yet, and the only way to
     * add one is the popup menu above, which a user can't discover just by
     * looking at an empty table. Wrap the table's view in a {@link JLayer} so
     * a translucent hint can be painted over it while it is empty; the
     * {@link JLayer} still delegates {@link Scrollable} to the table, so
     * scrolling behaves exactly as before.
     */
    private void installEmptyTemplateHint() {
        LayerUI<JTable> hintUI = new LayerUI<JTable>() {
            @Override
            public void paint(Graphics g, JComponent c) {
                super.paint(g, c);
                if (tabTemplates.getModel().getRowCount() != 0)
                    return;
                String hint = I18n.t("generatorMain.tabTemplates.emptyHint");
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    Object desktopHints = Toolkit.getDefaultToolkit()
                            .getDesktopProperty("awt.font.desktophints");
                    if (desktopHints instanceof Map)
                        g2.setRenderingHints((Map<?, ?>) desktopHints);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    Color fg = UIManager.getColor("Label.disabledForeground");
                    g2.setColor(fg != null ? fg : Color.GRAY);
                    g2.setFont(tabTemplates.getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (c.getWidth() - fm.stringWidth(hint)) / 2;
                    int y = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(hint, Math.max(x, 0), y);
                } finally {
                    g2.dispose();
                }
            }
        };
        JLayer<JTable> layer = new JLayer<>(tabTemplates, hintUI);
        scrTemplates.setViewportView(layer);
        // repaint the layer explicitly on every model change: it overlays the
        // table rather than being painted by it, so a plain table repaint is
        // not guaranteed to reach it in every look and feel.
        tabTemplates.getModel().addTableModelListener(e -> layer.repaint());
    }

    /**
     * ask for a new template and append it to the table, ticked.
     */
    private void addTemplate() {
        JDBTemplate tpl = templateDialog(
                I18n.t("generatorMain.template.add.title"), null);
        if (tpl == null)
            return;
        DefaultTableModel model = (DefaultTableModel)tabTemplates.getModel();
        model.addRow(new Object[]{ tpl.isSelected(), tpl.getName(),
            tpl.getTemplateFile(), tpl.getOutTemplate() });
        int row = model.getRowCount() - 1;
        tabTemplates.setRowSelectionInterval(row, row);
        saveGenerationOptions();
    }

    /**
     * edit the selected template. Its tick is left as it is.
     */
    private void editTemplate() {
        int row = tabTemplates.getSelectedRow();
        if (row < 0)
            return;
        DefaultTableModel model = (DefaultTableModel)tabTemplates.getModel();
        Object sel = model.getValueAt(row, 0);
        JDBTemplate cur = new JDBTemplate(
                str(model.getValueAt(row, 1)),
                str(model.getValueAt(row, 2)),
                str(model.getValueAt(row, 3)),
                sel instanceof Boolean && (Boolean)sel);
        JDBTemplate tpl = templateDialog(
                I18n.t("generatorMain.template.edit.title"), cur);
        if (tpl == null)
            return;
        model.setValueAt(tpl.getName(), row, 1);
        model.setValueAt(tpl.getTemplateFile(), row, 2);
        model.setValueAt(tpl.getOutTemplate(), row, 3);
        saveGenerationOptions();
    }

    /**
     * remove the selected template. Nothing is asked - a template that was
     * removed by accident is three fields away from being back.
     */
    private void removeTemplate() {
        int row = tabTemplates.getSelectedRow();
        if (row < 0)
            return;
        ((DefaultTableModel)tabTemplates.getModel()).removeRow(row);
        saveGenerationOptions();
    }

    /**
     * open the preset dialog on the template table of this window. The dialog
     * writes into the table directly, so whatever it left there is stored when
     * it closes.
     */
    private void showPresets() {
        JDBPresets presets = new JDBPresets(tabTemplates, 1);
        presets.setModal(true);
        presets.setLocationRelativeTo(this);
        presets.setVisible(true);
        saveGenerationOptions();
    }

    /**
     * ask for the three fields of a template. The dialog is repeated until
     * every field is filled in or the user cancels.
     *
     * @param title
     *            the title of the dialog.
     * @param current
     *            the template to edit, or <code>null</code> to describe a new
     *            one. A new template comes back ticked.
     * @return the entered template, or <code>null</code> when the dialog was
     *         cancelled.
     */
    private JDBTemplate templateDialog(String title, JDBTemplate current) {
        JTextField txtName = new JTextField(
                current == null ? "" : current.getName(), 30);
        JTextField txtFile = new JTextField(
                current == null ? "" : current.getTemplateFile(), 30);
        JTextField txtOut = new JTextField(
                current == null ? "" : current.getOutTemplate(), 30);
        JButton btnBrowse = new JButton("...");
        UIUtils.applyIcon(btnBrowse, FontAwesome.FOLDER_O);
        btnBrowse.addActionListener(e -> {
            // the chooser is parented on the button, not on this window: that
            // makes it a child of the modal dialog it is opened from instead of
            // a second modal window beside it
            String path = UIUtils.openFileDlg(btnBrowse,
                    AppDirs.installResourceFile("templates").getAbsolutePath(), true);
            if (!StrUtils.isEmpty(path))
                txtFile.setText(path);
        });
        JPanel filePanel = new JPanel(new BorderLayout(4, 0));
        filePanel.add(txtFile, BorderLayout.CENTER);
        filePanel.add(btnBrowse, BorderLayout.EAST);
        Object[] content = {
            I18n.t("generatorMain.template.name"), txtName,
            I18n.t("generatorMain.template.file"), filePanel,
            I18n.t("generatorMain.template.outTemplate"), txtOut
        };
        while (true) {
            int res = JOptionPane.showConfirmDialog(this, content, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION)
                return null;
            if (StrUtils.isEmpty(txtName.getText())
                    || StrUtils.isEmpty(txtFile.getText())
                    || StrUtils.isEmpty(txtOut.getText())) {
                UIUtils.error(this, I18n.t("generatorMain.msg.templateRequired"));
                continue;
            }
            return new JDBTemplate(txtName.getText(), txtFile.getText(),
                    txtOut.getText(), current == null || current.isSelected());
        }
    }

    /**
     * while <code>true</code>, a change of the connection combo box does not
     * open a connection. Set while the combo box is being rebuilt, because
     * selecting an entry is what triggers the connect.
     */
    private boolean suppressCboConnEvent = false;
    /**
     * reload the connection combo box from the configuration and show the
     * templates, the author and the custom variables of the given connection.
     * Selecting the connection in the combo box is what triggers the actual
     * connect, so a <code>null</code> argument only refreshes the list and
     * restores the previous entry without connecting again.
     *
     * @param conn
     *            the connection to apply, or <code>null</code> to just reload
     *            the list of connections.
     */
    private void applyConnection(JDBConnection conn) {
        suppressCboConnEvent = conn == null;
        String preName = null;
        if (suppressCboConnEvent)
            preName = (String)cboConnection.getSelectedItem();

        connMap.clear();

        boolean back = suppressCboConnEvent;
        suppressCboConnEvent = true;
        cboConnection.removeAllItems();
        conf.getConnections().forEach(c -> {
            connMap.put(c.getName(), c);
            cboConnection.addItem(c.getName());
        });
        cboConnection.setRenderer(UIUtils.getListCellRenderer(s -> connMap.get(s)));
        suppressCboConnEvent = back;
        if (conn != null)
            cboConnection.setSelectedItem(conn.getName());
        else if (suppressCboConnEvent) {
            cboConnection.setSelectedItem(preName);
            suppressCboConnEvent = false;
        }
        // selecting the entry above already ran the combo handler, which shows
        // the options of the connection it switched to. Doing it again here is
        // what covers the paths where that handler bailed out early, and it
        // costs nothing where it did not.
        currConn = conn;
        showGenerationOptions(conn);
    }

    /**
     * Fill the generation options panel - the templates with their ticks, the
     * output directory, the author and the custom variables - from a
     * connection. A <code>null</code> connection empties the panel.
     *
     * @param conn
     *            the connection to show, or <code>null</code> for none.
     */
    private void showGenerationOptions(JDBConnection conn) {
        DefaultTableModel tplModel = (DefaultTableModel)tabTemplates.getModel();
        tplModel.setRowCount(0);
        DefaultTableModel cstModel = (DefaultTableModel)tabVars.getModel();
        autoReset = false;
        cstModel.setRowCount(0);
        if (conn != null) {
            fillTemplateTable(tplModel, conn.getTemplates());
            txtAuthor.setText(conn.getAuthor());
            txtOutputDir.setText(conn.getOutputDir());
            if (conn.getCustomVars() != null)
                conn.getCustomVars().forEach((k, v) -> {if (!StrUtils.isEmpty(k)) cstModel.addRow(new String[]{k, v});});
            cstModel.addRow(new String[]{"", ""});
        } else {
            txtAuthor.setText("");
            txtOutputDir.setText("");
        }
        autoReset = true;
    }

    /**
     * Show the templates of a connection in the four column template table of
     * this window, ticking the ones the connection has stored as selected.
     *
     * @param model
     *            the model of the template table, emptied first.
     * @param tpls
     *            the templates to show, may be <code>null</code>.
     */
    static void fillTemplateTable(DefaultTableModel model, List<JDBTemplate> tpls) {
        model.setRowCount(0);
        if (tpls == null)
            return;
        for (JDBTemplate t: tpls)
            model.addRow(new Object[]{ t.isSelected(), t.getName(),
                t.getTemplateFile(), t.getOutTemplate() });
    }

    /**
     * Read the four column template table of this window back into template
     * objects. The table is the whole truth about the templates of the current
     * connection - it holds every field of them, including the tick - so the
     * connection's list is rebuilt from it instead of being matched up row by
     * row.
     *
     * @param model
     *            the model of the template table.
     * @return the templates shown, in table order. A row without a name is not
     *         a template and is dropped.
     */
    static List<JDBTemplate> readTemplateTable(TableModel model) {
        List<JDBTemplate> res = new ArrayList<>();
        for (int i=0; i<model.getRowCount(); i++) {
            String name = str(model.getValueAt(i, 1));
            if (StrUtils.isEmpty(name))
                continue;
            Object sel = model.getValueAt(i, 0);
            res.add(new JDBTemplate(name, str(model.getValueAt(i, 2)),
                    str(model.getValueAt(i, 3)),
                    sel instanceof Boolean && (Boolean)sel));
        }
        return res;
    }

    /**
     * the text of a table cell, with a missing value read as the empty string
     * instead of the word "null".
     *
     * @param value
     *            the cell value, may be <code>null</code>.
     * @return the value as text.
     */
    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * Write the generation options panel back into the connection it belongs
     * to and store the configuration. This is the only place the panel is
     * saved from, so every way out of it - generating, switching the
     * connection, opening the connection manager and closing the window - has
     * to come through here.
     */
    private void saveGenerationOptions() {
        if (!applyGenerationOptions())
            return;
        JDBGenConfig.saveInstance(this);
    }

    /**
     * Write the generation options panel back into the connection it belongs
     * to, without storing the configuration - the caller does that.
     *
     * @return <code>true</code> when the options were taken over,
     *         <code>false</code> while no connection has been chosen yet and
     *         there is nothing to write them into.
     */
    private boolean applyGenerationOptions() {
        if (currConn == null)
            return false;
        currConn.setTemplates(readTemplateTable(tabTemplates.getModel()));
        currConn.setOutputDir(txtOutputDir.getText());
        currConn.setAuthor(txtAuthor.getText());
        currConn.setCustomVars(UIUtils.applyTableToMap(tabVars.getModel()));
        return true;
    }

    /**
     * Store everything the next start restores and write the configuration
     * once. Unlike {@link #saveGenerationOptions()} this also runs when no
     * connection has been chosen yet: the window geometry is worth keeping
     * either way.
     */
    private void saveOnExit() {
        applyGenerationOptions();
        storeWindowState();
        JDBGenConfig.saveInstance(this);
    }

    /**
     * Take the geometry of the window and the positions of the two work area
     * dividers over into the configuration.
     *
     * <p>While the window is maximized its bounds are the ones of the screen,
     * which is not what restoring it down has to come back to: only the
     * maximized flag is updated then, the stored size and position stay at the
     * values the user last chose.</p>
     */
    private void storeWindowState() {
        WindowState state = conf.getMainWindow();
        if (state == null) {
            state = new WindowState();
            conf.setMainWindow(state);
        }
        boolean maximized = (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
        state.setMaximized(maximized);
        if (!maximized) {
            Rectangle bounds = getBounds();
            state.setWidth(bounds.width);
            state.setHeight(bounds.height);
            state.setX(bounds.x);
            state.setY(bounds.y);
        }
        if (splSchema != null)
            state.setSchemaDivider(splSchema.getDividerLocation());
        if (splOptions != null)
            state.setOptionsDivider(splOptions.getDividerLocation());
    }

    /**
     * Bring the window back to where it was left, right after
     * <code>pack()</code> has settled its default size.
     *
     * <p>A stored size below the minimum of the window is raised to it, and a
     * stored position is only taken when it still lies on one of the screens
     * currently attached - a window restored onto a monitor that is gone would
     * be invisible. The dividers are not touched here: they are applied once
     * the window has been shown, see {@link #applyStoredDividers()}.</p>
     *
     * @return <code>true</code> when the position was restored, so that the
     *         caller knows it must not center the window any more.
     */
    private boolean restoreWindowState() {
        WindowState state = conf.getMainWindow();
        if (state == null)
            return false;
        boolean located = false;
        if (state.hasBounds()) {
            Dimension min = getMinimumSize();
            setSize(Math.max(state.getWidth(), min.width),
                    Math.max(state.getHeight(), min.height));
            if (isOnAnyScreen(new Rectangle(state.getX(), state.getY(),
                    getWidth(), getHeight()))) {
                setLocation(state.getX(), state.getY());
                located = true;
            }
        }
        if (state.isMaximized())
            setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
        return located;
    }

    /**
     * whether a window would be visible at all with the given bounds.
     *
     * @param bounds
     *            the bounds a window is about to be restored to.
     * @return <code>true</code> when they overlap one of the screens currently
     *         attached; <code>false</code> without a screen to ask, in which
     *         case the caller keeps the default position.
     */
    private static boolean isOnAnyScreen(Rectangle bounds) {
        try {
            if (GraphicsEnvironment.isHeadless())
                return false;
            for (GraphicsDevice gd : GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getScreenDevices()) {
                if (gd.getDefaultConfiguration().getBounds().intersects(bounds))
                    return true;
            }
        } catch (Exception e) {
            log.warn("cannot read the screen configuration: {}", e.getLocalizedMessage());
        }
        return false;
    }

    /**
     * Put the two work area dividers back where they were left. This runs once
     * the window has been shown: before that the split panes have no width yet
     * and the layout would move the dividers again. Positions that were never
     * stored are left alone, which keeps the default split of a fresh
     * configuration.
     */
    private void applyStoredDividers() {
        WindowState state = conf.getMainWindow();
        if (state == null)
            return;
        if (state.getSchemaDivider() > 0 && splSchema != null)
            splSchema.setDividerLocation(state.getSchemaDivider());
        if (state.getOptionsDivider() > 0 && splOptions != null) {
            // moving the outer divider resizes the inner split, and the layout
            // that follows moves the inner divider along with it. That layout
            // is scheduled on the event queue, so the inner position is put
            // back behind it - setting it right here would be overwritten.
            EventQueue.invokeLater(
                    () -> splOptions.setDividerLocation(state.getOptionsDivider()));
        }
    }

    /**
     * whether the window came up at the position it was last closed at.
     *
     * @return <code>true</code> when the stored position was restored, so that
     *         the caller must not center the window itself.
     * @see #restoreWindowState()
     */
    public boolean isLocationRestored() {
        return locationRestored;
    }

    /**
     * Build the catalog/schema node hierarchy. This queries the database, so it
     * must NOT be called on the EDT. The returned nodes are not attached to any
     * component yet, so building them off the EDT is safe.
     *
     * @param meta
     *            the metadata of the open connection.
     * @return the root node of the tree. A database with a single catalog uses
     *         that catalog as the root, otherwise a generic root holds one node
     *         per catalog.
     * @throws SQLException
     *             if the schema list cannot be read.
     */
    private static DefaultMutableTreeNode buildSchemaRoot(DBMeta meta) throws SQLException {
        Map<String, List<DBSchema>> tree = meta.getSchemaTree();
        DefaultMutableTreeNode root = null;
        if (tree.size() > 1) {
            root = new DefaultMutableTreeNode(I18n.t("generatorMain.tree.database"));
            for (String catalog:tree.keySet()) {
                DefaultMutableTreeNode cat = new DefaultMutableTreeNode(catalog);
                for (DBSchema schema:tree.get(catalog)) {
                    cat.add(new DefaultMutableTreeNode(schema));
                }
                root.add(cat);
            }
        } else {
            for (String catalog:tree.keySet()) {
                root = new DefaultMutableTreeNode(catalog);
                for (DBSchema schema:tree.get(catalog)) {
                    root.add(new DefaultMutableTreeNode(schema));
                }
            }
        }
        return root;
    }

    /**
     * Attach the schema hierarchy built by {@link #buildSchemaRoot(DBMeta)}.
     * EDT only.
     *
     * @param root
     *            the root node built off the event dispatch thread.
     */
    private void showSchemaRoot(DefaultMutableTreeNode root) {
        treSchemas.setModel(new DefaultTreeModel(root));
        treSchemas.setCellRenderer(new SchemaCellRenderer());
    }

    /**
     * Drop everything that belongs to the previous connection. EDT only.
     */
    private void clearConnectionView() {
        tables = null;
        treSchemas.setModel(new DefaultTreeModel(null));
        lstTables.setModel(new DefaultListModel<>());
        lstTables.setToolTipText(null);
    }

    /**
     * Toggle the controls that must not be touched while a connection is being
     * opened on a background thread.
     *
     * @param flag
     *            <code>true</code> while connecting, which disables those
     *            controls and shows the wait cursor, <code>false</code> to
     *            restore them.
     */
    private void setConnecting(boolean flag) {
        connecting = flag;
        cboConnection.setEnabled(!flag);
        btnManageConn.setEnabled(!flag);
        btnGenerate.setEnabled(!flag);
        setCursor(Cursor.getPredefinedCursor(
                flag ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /**
     * Open the connection and read the schema tree on a background thread, then
     * apply the result to the UI on the EDT. Opening a JDBC connection can take
     * seconds, so it must never run on the EDT.
     *
     * @param jdr
     *            the driver definition to load the JDBC driver from.
     * @param jcc
     *            the connection to open.
     */
    private void connectAsync(final JDBDriver jdr, final JDBConnection jcc) {
        // EDT: tear down the previous connection first, so a failure while
        // creating the new one cannot leave a closed connection behind.
        if (dbmeta != null) {
            try { dbmeta.close(); } catch (Exception ignored) {}
            dbmeta = null;
        }
        clearConnectionView();
        lblConnectionInfo.setText(jcc.getConnectionUrl());
        setConnecting(true);

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                DBMeta meta = new DBMeta(jdr, jcc);
                try {
                    // force the (lazy) metadata lookup here, on this thread
                    return new Object[]{ meta, buildSchemaRoot(meta) };
                } catch (Exception e) {
                    try { meta.close(); } catch (Exception ignored) {}
                    throw e;
                }
            }

            @Override
            protected void done() {
                try {
                    Object[] res = get();
                    dbmeta = (DBMeta)res[0];
                    showSchemaRoot((DefaultMutableTreeNode)res[1]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    connectFailed(jcc, ie);
                } catch (ExecutionException ee) {
                    connectFailed(jcc, ee.getCause() == null ? ee : ee.getCause());
                } catch (Exception ex) {
                    connectFailed(jcc, ex);
                } finally {
                    setConnecting(false);
                }
            }
        }.execute();
    }

    /**
     * Restore a consistent "not connected" state after a failed connection.
     * EDT only.
     *
     * @param jcc
     *            the connection that could not be opened, named in the error
     *            message.
     * @param cause
     *            the failure, logged and shown to the user.
     */
    private void connectFailed(JDBConnection jcc, Throwable cause) {
        log.error(cause.getLocalizedMessage(), cause);
        dbmeta = null;
        clearConnectionView();
        lblConnectionInfo.setText("");
        // clear the selection so re-picking the same entry fires the event again
        boolean back = suppressCboConnEvent;
        suppressCboConnEvent = true;
        cboConnection.setSelectedIndex(-1);
        suppressCboConnEvent = back;
        UIUtils.error(this, I18n.t("generatorMain.msg.connectFailed",
                jcc.getName(), cause.getLocalizedMessage()));
    }

    // -----------------------------------------------------------------------
    // layout
    //
    // The window is laid out with MigLayout. Six pixels is the gap the look
    // and feel puts between neighbouring controls and at the edge of a
    // container, and the value the previous layout asked for by name - it is
    // spelled out in the constraints below so that the window keeps its
    // spacing whatever MigLayout's own platform defaults are.
    // -----------------------------------------------------------------------

    /**
     * Build the window: the connection bar on top, the three work panels
     * below it and the two action buttons at the bottom. Called from the
     * constructor, before anything that reads the configuration.
     */
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle(I18n.t("generatorMain.title"));

        JPanel content = new JPanel(new MigLayout(
                "insets 6, gap 6 6, fill",
                "[grow,fill]",
                // only the work area in the middle takes the height the
                // window has over the two bars
                "[][grow,fill][]"));
        content.add(buildConnectionBar(), "growx, wrap");
        content.add(buildWorkArea(), "grow, wrap");
        content.add(buildActionBar(), "growx");
        setContentPane(content);
    }

    /**
     * the top row: the connection to work with, the way to the connection
     * manager and the url of the open connection.
     *
     * @return the connection bar, not attached to the window yet.
     */
    private JPanel buildConnectionBar() {
        JLabel lblConnection = new JLabel(I18n.t("generatorMain.jLabel2.text"));

        cboConnection = new JComboBox<>();
        cboConnection.addActionListener(e -> connectionSelected());

        btnManageConn = new JButton(I18n.t("generatorMain.btnManageConn.text"));
        btnManageConn.addActionListener(e -> showConnectionManager());

        lblConnectionInfo = new JLabel("Connection Information Placeholder");
        lblConnectionInfo.setLabelFor(cboConnection);

        JPanel bar = new JPanel(new MigLayout("insets 0, gap 6, fillx",
                "[][][][grow,fill]", "[]"));
        bar.add(lblConnection);
        bar.add(cboConnection);
        bar.add(btnManageConn);
        // the url takes whatever is left of the row and is the only part of
        // it that yields when the window gets narrower
        bar.add(lblConnectionInfo, "growx, wmin 0");
        return bar;
    }

    /**
     * the middle of the window: the schema tree, the table list and the
     * generation options, side by side.
     *
     * @return the work area, not attached to the window yet.
     */
    private JComponent buildWorkArea() {
        // two nested split panes, so that the user can trade width between the
        // three panels: the divider between the schema tree and the table list
        // and the one between the table list and the generation options. Both
        // outer panels keep their width when the window is resized, the table
        // list in the middle is what grows and what yields.
        splOptions = workSplit(buildTableListPanel(), buildOptionsPanel(), 1.0);
        splSchema = workSplit(buildSchemaPanel(), splOptions, 0.0);
        return splSchema;
    }

    /**
     * a horizontal split of the work area: no border, a slim divider that
     * moves without repainting in between, and a minimum size of zero on
     * both sides so that the divider can be dragged all the way.
     *
     * @param left
     *            the left component.
     * @param right
     *            the right component.
     * @param resizeWeight
     *            the share of a size change the left side receives.
     * @return the split pane.
     */
    private static JSplitPane workSplit(JComponent left, JComponent right, double resizeWeight) {
        left.setMinimumSize(new Dimension(0, 0));
        right.setMinimumSize(new Dimension(0, 0));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, left, right);
        split.setBorder(null);
        split.setDividerSize(6);
        split.setResizeWeight(resizeWeight);
        return split;
    }

    /**
     * the catalog and schema tree of the open connection.
     *
     * @return the schema panel, not attached to the work area yet.
     */
    private JPanel buildSchemaPanel() {
        JLabel lblSchemas = new JLabel(I18n.t("generatorMain.jLabel1.text"));
        lblSchemas.setFont(headerFont(lblSchemas));

        treSchemas = new JTree(new DefaultTreeModel(
                new DefaultMutableTreeNode("root")));
        treSchemas.addTreeSelectionListener(e -> reloadTableList());

        JScrollPane scrSchemas = new JScrollPane(treSchemas);
        // freeze the preferred width at what the empty tree asks for: this
        // panel is laid out at its preferred size, so without this a long
        // schema name would widen it and push the panels on its right off
        // the window.
        scrSchemas.setPreferredSize(scrSchemas.getPreferredSize());

        JPanel panel = new JPanel(new MigLayout("insets 6 6 0 0, gap 6, fill, wrap 1",
                "[grow,fill]", "[][grow,fill]"));
        // the heading keeps a wide gap on its right, so that a narrow tree
        // still leaves room beside it
        panel.add(lblSchemas, "gapright 25");
        panel.add(scrSchemas);
        return panel;
    }

    /**
     * the tables of the selected schema, with the show-views tick and the
     * filter field above them.
     *
     * @return the table list panel, not attached to the work area yet.
     */
    private JPanel buildTableListPanel() {
        JLabel lblTables = new JLabel(I18n.t("generatorMain.jLabel5.text"));
        lblTables.setFont(headerFont(lblTables));

        chkShowView = new JCheckBox(I18n.t("generatorMain.chkShowView.text"));
        chkShowView.addActionListener(e -> reloadTableList());

        txtTableFilter = new JTextField();
        txtTableFilter.putClientProperty("JTextField.placeholderText",
                I18n.t("generatorMain.txtTableFilter.placeholder"));
        txtTableFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyTableFilter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyTableFilter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyTableFilter(); }
        });

        lstTables = new JList<>();
        lstTables.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tableListClicked(e);
            }
        });
        lstTables.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                showTableTooltip(e);
            }
        });
        JScrollPane scrTables = new JScrollPane(lstTables);

        JPanel panel = new JPanel(new MigLayout("insets 6 0 0 0, gap 6, fill, wrap 1",
                "[grow,fill]", "[][][][grow,fill]"));
        panel.add(lblTables);
        panel.add(chkShowView);
        panel.add(txtTableFilter, "growx, wmin 0");
        // the list is 224 wide by preference and may be squeezed down to
        // nothing: this is the panel that yields when the window shrinks
        panel.add(scrTables, "grow, w 0:224:");
        return panel;
    }

    /**
     * the right hand panel: the templates to apply and the values they are
     * applied with.
     *
     * @return the generation options panel, not attached to the work area yet.
     */
    private JPanel buildOptionsPanel() {
        JLabel lblOptions = new JLabel(I18n.t("generatorMain.jLabel4.text"));
        lblOptions.setFont(headerFont(lblOptions));
        JLabel lblTemplates = new JLabel(I18n.t("generatorMain.jLabel6.text"));

        tabTemplates = new JTable(new DefaultTableModel(
                new Object[]{ "Select", "Name", "Template File", "Out Template" }, 0) {
            private final Class<?>[] types = { Boolean.class, String.class,
                String.class, String.class };
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                // only the tick is edited in the table itself, the three texts
                // are edited in the template dialog
                return columnIndex == 0;
            }
        });
        tabTemplates.getTableHeader().setReorderingAllowed(false);
        tabTemplates.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                UIUtils.templateTooltip(tabTemplates, 1, e);
            }
        });
        scrTemplates = new JScrollPane(tabTemplates);

        JPanel panel = new JPanel(new MigLayout("insets 6 0 6 0, gap 6, fill, wrap 1",
                "[grow,fill]",
                // the template table takes the height the panel has over its
                // headings and the fields below it
                "[][][grow,fill][]"));
        panel.add(lblOptions);
        panel.add(lblTemplates);
        // a table asks its scroll pane for a viewport of a default 400 pixels,
        // which would make the window pack that much taller: the table gets
        // whatever height is left over instead, however little that is.
        panel.add(scrTemplates, "grow, h 0:0:");
        panel.add(buildOptionFields(), "growx");
        return panel;
    }

    /**
     * the labelled fields below the template table: the output directory, the
     * author, the abbreviation tick and the custom variables.
     *
     * @return the field block, not attached to the options panel yet.
     */
    private JPanel buildOptionFields() {
        JLabel lblOutputDir = new JLabel(
                I18n.t("generatorMain.jLabel11.text"), SwingConstants.TRAILING);
        JLabel lblAuthor = new JLabel(
                I18n.t("generatorMain.jLabel14.text"), SwingConstants.TRAILING);
        JLabel lblAbbr = new JLabel(
                I18n.t("generatorMain.jLabel15.text"), SwingConstants.TRAILING);
        JLabel lblVars = new JLabel(
                I18n.t("generatorMain.jLabel16.text"), SwingConstants.TRAILING);

        txtOutputDir = new JTextField("output");
        // the panel around this one is laid out at its preferred width, and a
        // text field grows its preferred width with the text it holds: a long
        // output path would widen the panel until it is pushed over the right
        // window edge. A fixed column count keeps the preferred width constant.
        txtOutputDir.setColumns(20);

        btnBrowseOutput = new JButton("...");
        btnBrowseOutput.addActionListener(e -> browseOutputDir());

        txtAuthor = new JTextField();
        txtAuthor.setColumns(20);

        chkApplyAbbr = new JCheckBox(I18n.t("generatorMain.chkApplyAbbr.text"));
        chkApplyAbbr.addActionListener(e -> storeApplyAbbr());

        tabVars = new JTable(new DefaultTableModel(
                new Object[]{ "Name", "Value" }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        });
        JScrollPane scrVars = new JScrollPane(tabVars);

        btnDelVar = new JButton("-");
        btnDelVar.addActionListener(e -> removeSelectedVar());

        JPanel panel = new JPanel(new MigLayout("insets 0 6 0 0, gap 6, fillx",
                // one label column that every label right-aligns in - 107 is
                // the width the window was designed with, which keeps the
                // fields lined up where a translation has shorter labels -
                // then the input column that takes the rest of the row, and
                // last the browse button
                "[107::,right][grow,fill][]",
                // the custom variables row is taller than its label and its
                // delete button, which stay at its top
                "[][][][top]"));
        panel.add(lblOutputDir);
        panel.add(txtOutputDir, "growx, wmin 0, sgy field");
        panel.add(btnBrowseOutput, "sgy field, wrap");
        panel.add(lblAuthor);
        panel.add(txtAuthor, "spanx 2, growx, wmin 0, wrap");
        panel.add(lblAbbr);
        panel.add(chkApplyAbbr, "spanx 2, growx, wrap");
        // the label and the delete button share the label column, stacked
        panel.add(lblVars, "split 2, flowy");
        panel.add(btnDelVar, "sgy field");
        // the same default viewport width of a table would widen the whole
        // panel: the variable table takes what the fields above it need, and
        // it keeps the height the window was designed with
        panel.add(scrVars, "spanx 2, growx, w 0:0:, h 103");
        return panel;
    }

    /**
     * the bottom row: generate and close, both at the right window edge.
     *
     * @return the action bar, not attached to the window yet.
     */
    private JPanel buildActionBar() {
        btnGenerate = new JButton(I18n.t("generatorMain.btnGenerate.text"));
        btnGenerate.addActionListener(e -> generate());

        btnClose = new JButton(I18n.t("generatorMain.btnClose.text"));
        btnClose.addActionListener(e -> closeApplication());

        // the space in front of the buttons is what grows, so both of them
        // stay at the right edge
        JPanel bar = new JPanel(new MigLayout("insets 0, gap 6, fillx",
                "push[][]", "[]"));
        bar.add(btnGenerate);
        bar.add(btnClose);
        return bar;
    }

    /**
     * the font of the panel headings: the label's own font, bold and four
     * points larger.
     *
     * @param label
     *            the heading label the font is derived for.
     * @return the heading font.
     */
    private static Font headerFont(JLabel label) {
        Font font = label.getFont();
        return font.deriveFont(font.getStyle() | Font.BOLD, font.getSize() + 4f);
    }

    /**
     * switch between the dark and the light look and feel and store the choice.
     *
     * @param dark
     *            <code>true</code> for the dark look and feel.
     */
    private void applyDarkUI(boolean dark) {
        if (dark) {
            UIUtils.setFlatDarkLaf();
        } else {
            UIUtils.setFlatLightLaf();
        }

        SwingUtilities.updateComponentTreeUI(this);
        applyConnectionInfoUI();
        conf.setDarkUI(dark);
        JDBGenConfig.saveInstance(this);
    }

    /**
     * the entry of the language menu a stored language setting selects.
     * Anything unknown falls back to the system default entry.
     *
     * @param language
     *            the stored language tag, or <code>null</code> for the
     *            operating system locale.
     * @return the index into <code>LANGUAGES</code>, <code>0</code> for the
     *         system default entry.
     */
    static int languageIndex(String language) {
        if (language != null) {
            String lang = language.trim();
            for (int i=1; i<LANGUAGES.length; i++) {
                if (LANGUAGES[i].equalsIgnoreCase(lang))
                    return i;
            }
        }
        return 0;
    }

    /**
     * the shown name of every entry of <code>LANGUAGES</code>, in the same
     * order. Only the system entry is translated - a language is named in
     * itself, so that it can be found whatever the user interface currently
     * speaks.
     *
     * @return the entry names of the language menu.
     */
    private static String[] languageNames() {
        return new String[] {
            I18n.t("common.language.system"), "English",
            "한국어", "Español", "日本語", "简体中文" };
    }

    /**
     * store the chosen interface language, which takes effect on the next start.
     *
     * @param idx
     *            the index into <code>LANGUAGES</code> of the chosen entry.
     */
    private void applyLanguage(int idx) {
        // re-selecting the entry that is already stored is not a change
        if (idx < 0 || idx == languageIndex(conf.getLanguage()))
            return;
        conf.setLanguage(LANGUAGES[idx]);
        JDBGenConfig.saveInstance(this);
        // switching the language live would have to rebuild every open window,
        // so it is left to the next start.
        UIUtils.info(this, I18n.t("common.language.restartRequired"));
    }

    /**
     * open the connection chosen in the combo box, after looking up the driver
     * it refers to.
     */
    private void connectionSelected() {
        // ignore while a connection is already being opened - the combo is
        // disabled meanwhile, this is just a second line of defense.
        if (suppressCboConnEvent || connecting)
            return;
        int idx = cboConnection.getSelectedIndex();
        if (idx < 0 || idx >= conf.getConnections().size())
            return;
        JDBConnection jcc = conf.getConnections().get(idx);
        if (jcc != currConn) {
            // the panel still shows the connection being left: store its edits
            // before the panel is refilled from the new one
            saveGenerationOptions();
            currConn = jcc;
            showGenerationOptions(jcc);
        }
        JDBDriver jdr = null;
        for (JDBDriver drv:conf.getDrivers()) {
            if (drv.getName().equals(jcc.getDriverType())) {
                jdr = drv;
                break;
            }
        }
        if (jdr == null) {
            UIUtils.error(this, I18n.t("generatorMain.msg.driverNotFound",
                    jcc.getDriverType(), jcc.getName()));
            return;
        }
        connectAsync(jdr, jcc);
    }

    /**
     * open the connection manager and apply the connection it returns.
     */
    private void showConnectionManager() {
        // the manager edits the same connection objects, so hand it whatever
        // is currently in the generation options panel
        saveGenerationOptions();
        JDBConnectionManager cm = JDBConnectionManager.getInstance();
        cm.setModal(true);
        cm.setLocationRelativeTo(this);
        cm.setSelection(currConn);
        cm.setVisible(true);
        if (cm.selectedConnection != null)
            applyConnection(cm.selectedConnection);
    }

    /**
     * reload the table list of the schema selected in the tree, with or
     * without views. Both the tree selection and the show-views tick end up
     * here.
     */
    private void reloadTableList() {
        // dbmeta is null while a connection is being opened (or after a failure)
        if (dbmeta == null)
            return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treSchemas.getLastSelectedPathComponent();
        lstTables.removeAll();
        if (node != null) {
            Object uobj = node.getUserObject();
            if (uobj instanceof DBSchema) {
                DBSchema schema = (DBSchema)uobj;
                try {
                    tables = dbmeta.getTables(schema, chkShowView.isSelected());
                    // the filter field is kept across a schema switch or a
                    // show-views toggle, so re-apply it to the freshly read list
                    applyTableFilter();
                } catch (Exception ex) {
                    log.error("cannot get tables", ex);
                    UIUtils.error(this, I18n.t("generatorMain.msg.getTablesFailed",
                            ex.getLocalizedMessage()));
                }
            }
        }
    }

    /**
     * close the database connection and terminate the application.
     */
    @SuppressWarnings("UseSpecificCatch")
    private void closeApplication() {
        saveOnExit();
        if (dbmeta != null)
            try { dbmeta.close(); } catch(Exception ignored) {}
        System.exit(0);
    }

    /**
     * Immutable snapshot of everything the generation worker needs. Taken on the
     * EDT so that the worker never has to read a Swing component.
     */
    private static final class GenerateRequest {
        /**
         * the metadata of the open connection.
         */
        final DBMeta meta;
        /**
         * the custom template variables, including the author.
         */
        final Map<String, String> customVars;
        /**
         * the tables selected for generation.
         */
        final List<DBTable> tables;
        /**
         * the templates ticked for generation.
         */
        final List<JDBTemplate> templates;
        /** already resolved, see {@link AppDirs#resolveOutputDir(String)}. */
        final String outputDir;

        /**
         * @param meta
         *            the metadata of the open connection.
         * @param customVars
         *            the custom template variables, including the author.
         * @param tables
         *            the tables selected for generation.
         * @param templates
         *            the templates ticked for generation.
         * @param outputDir
         *            the directory the generated files are written to.
         */
        GenerateRequest(DBMeta meta, Map<String, String> customVars,
                List<DBTable> tables, List<JDBTemplate> templates, String outputDir) {
            this.meta = meta;
            this.customVars = customVars;
            this.tables = tables;
            this.templates = templates;
            this.outputDir = outputDir;
        }
    }

    /**
     * build the worker that generates the code. It reads the columns of every
     * requested table, then applies each template to each table and writes the
     * result to the file named by the template's output expression, reporting
     * its progress to the progress dialog. A failure is logged and reported as
     * a failed result instead of being thrown.
     *
     * @param req
     *            the values snapshotted on the event dispatch thread; the
     *            worker must not read any Swing component itself.
     * @return the worker to hand to a <code>ProcessProgress</code> dialog.
     */
    private ProcessProgress.Worker getProgressWorker(final GenerateRequest req) {
        return new ProcessProgress.Worker() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // NOTE: this method must not touch any Swing component - every
                // value it needs was snapshotted into 'req' on the EDT.
                try {
                    JDBAbbr.buildMap();
                    publish(I18n.t("generatorMain.progress.readingColumns"));
                    for (DBTable t: req.tables)
                        req.meta.getTableColumns(t);
                    int totalProcs = req.tables.size() * req.templates.size();
                    int progress = 0;
                    for (JDBTemplate tpl:req.templates) {
                        publish(I18n.t("generatorMain.progress.templateProcessing", tpl.getName()));
                        String tplStr = ObjUtils.getFileContents(
                                AppDirs.resolvePath(tpl.getTemplateFile()));
                        TemplateManager tplCont = new TemplateManager(tplStr, req.customVars);
                        TemplateManager tplOut = new TemplateManager(tpl.getOutTemplate(), req.customVars);
                        for (DBTable t:req.tables) {
                            progress++;
                            if (totalProcs > 0)
                                setProgress(Math.min(100, progress * 100 / totalProcs));
                            publish(I18n.t("generatorMain.progress.applying",
                                    tpl.getName(), t.getTable()));
                            String result = tplCont.applyMapper(t);
                            String outFname = tplOut.applyMapper(t);
                            if (!StrUtils.isEmpty(req.outputDir))
                                outFname = req.outputDir + "/" + outFname;
                            ObjUtils.writeFile(outFname, result);
                        }
                    }
                    setProgress(100);
                    publish(I18n.t("generatorMain.progress.complete"));
                    return true;
                } catch(Exception e) {
                    log.error(e.getLocalizedMessage(), e);
                    publish(I18n.t("generatorMain.progress.failed", e.getLocalizedMessage()));
                    return false;
                }
            }
        };
    }

    /**
     * run the code generation for the selected tables and the ticked templates
     * in a modal progress dialog, and offer to open the output directory
     * afterwards.
     */
    private void generate() {
        if (dbmeta == null) {
            UIUtils.error(this, I18n.t("generatorMain.msg.connectFirst"));
            return;
        }
        // snapshot every UI value here, on the EDT - the worker below runs on a
        // background thread and must not read Swing components. The indices are
        // resolved against visibleTables, not tables: the list box only ever
        // shows the tables the filter field let through.
        int tidxs[] = lstTables.getSelectedIndices();
        List<DBTable> selTables = new ArrayList<>();
        if (visibleTables != null) {
            for (int idx: tidxs) {
                if (idx > -1 && idx < visibleTables.size())
                    selTables.add(visibleTables.get(idx));
            }
        }
        if (selTables.isEmpty()) {
            UIUtils.error(this, I18n.t("generatorMain.msg.selectTable"));
            return;
        }
        List<JDBTemplate> tpls = new ArrayList<>();
        for (JDBTemplate t: readTemplateTable(tabTemplates.getModel())) {
            if (t.isSelected())
                tpls.add(t);
        }
        if (tpls.isEmpty()) {
            UIUtils.error(this, I18n.t("generatorMain.msg.selectTemplate"));
            return;
        }
        // what is generated from is also what is kept: the options panel is the
        // one place these values are edited, so store them before the run
        saveGenerationOptions();
        Map<String, String> custVars = UIUtils.applyTableToMap(tabVars.getModel());
        custVars.put("author", txtAuthor.getText());
        // the configured value names a directory relative to the user data
        // directory or the installation, not to the working directory - the
        // browse button stores it that way, see AppDirs.relativize()
        String outputDir = AppDirs.resolveOutputDir(txtOutputDir.getText());

        GenerateRequest req = new GenerateRequest(
                dbmeta, custVars, selTables, tpls, outputDir);
        ProcessProgress pp = new ProcessProgress(this, true, getProgressWorker(req));
        pp.start();
        // modal - returns once the worker's done() hides the dialog
        pp.setVisible(true);
        if (pp.result) {
            if (UIUtils.confirm(this, I18n.t("generatorMain.msg.complete.title"),
                    I18n.t("generatorMain.msg.complete"))) {
                // must match the directory actually written to above
                PlatformUtils.openFile(StrUtils.isEmpty(outputDir) ? "." : outputDir);
            }
        } else {
            UIUtils.info(this, I18n.t("generatorMain.msg.failed"));
        }
    }

    /**
     * open the column view of a table on a double click.
     *
     * @param evt
     *            the mouse event on the table list.
     */
    private void tableListClicked(MouseEvent evt) {
        if (evt.getClickCount() == 2) {
            if (dbmeta == null || visibleTables == null)
                return;
            int idx = lstTables.getSelectedIndex();
            if (idx > -1 && idx < visibleTables.size()) {
                DBTable table = visibleTables.get(idx);
                try {
                    dbmeta.getTableColumns(table);
                    JDBTableView tview = new JDBTableView(this, table);
                    tview.setLocationRelativeTo(this);
                    tview.setVisible(true);
                } catch(Throwable t) {
                    log.error("cannot get columns", t);
                    UIUtils.error(this, I18n.t("generatorMain.msg.getColumnsFailed",
                            t.getLocalizedMessage()));
                }
            }
        }
    }

    /**
     * show the full name of the hovered table as a tooltip.
     *
     * @param evt
     *            the mouse event on the table list.
     */
    private void showTableTooltip(MouseEvent evt) {
        int idx = visibleTables == null ? -1 : lstTables.locationToIndex(evt.getPoint());
        if (idx > -1 && idx < visibleTables.size()) {
            DBTable table = visibleTables.get(idx);
            lstTables.setToolTipText(table.getName());
        } else {
            lstTables.setToolTipText(null);
        }
    }

    /**
     * store whether the abbreviation mapping is applied when generating.
     */
    private void storeApplyAbbr() {
        conf.setApplyAbbr(chkApplyAbbr.isSelected());
        JDBGenConfig.saveInstance(this);
    }

    /**
     * pick the directory the generated files are written to.
     */
    private void browseOutputDir() {
        String path = UIUtils.openDirDlg(this, "", true);
        if (!StrUtils.isEmpty(path))
            this.txtOutputDir.setText(path);
    }

    /** the connection to work with, and the way to the connection manager. */
    private JComboBox<String> cboConnection;
    private JButton btnManageConn;
    /** the url of the open connection, see {@link #applyConnectionInfoUI()}. */
    private JLabel lblConnectionInfo;

    /**
     * the two nested splits of the work area: <code>splSchema</code> divides
     * the schema tree from the rest, <code>splOptions</code> the table list
     * from the generation options. Their divider positions are stored in the
     * configuration, see {@link #storeWindowState()}.
     */
    private JSplitPane splSchema;
    private JSplitPane splOptions;

    /** the catalog and schema tree of the open connection. */
    private JTree treSchemas;

    /** the tables of the selected schema, and what narrows them down. */
    private JList<String> lstTables;
    private JCheckBox chkShowView;
    /**
     * the filter field above the table list. What is typed into it narrows
     * the list down to the matching tables, see {@link #applyTableFilter()}.
     */
    private JTextField txtTableFilter;

    /** the templates of the current connection, and their scroll pane. */
    private JTable tabTemplates;
    private JScrollPane scrTemplates;

    /** the values the templates are applied with. */
    private JTextField txtOutputDir;
    private JButton btnBrowseOutput;
    private JTextField txtAuthor;
    private JCheckBox chkApplyAbbr;
    private JTable tabVars;
    private JButton btnDelVar;

    /** the two actions of the window. */
    private JButton btnGenerate;
    private JButton btnClose;
}

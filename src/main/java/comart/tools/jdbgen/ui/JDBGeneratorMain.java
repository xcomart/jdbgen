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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.*;
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
     * the stored <code>language</code> value of every entry of
     * <code>cboLanguage</code>, in the order the entries appear.
     * <code>null</code> is the operating system locale.
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
     * the filter field placed above the table list at runtime, see
     * {@link #initTableFilterUI()}. Not part of the NetBeans generated code,
     * because the generator only adds new controls to the panel it owns
     * through the form editor, and this panel is instead rebuilt by hand so
     * that its content stays free-form.
     */
    private javax.swing.JTextField txtTableFilter;
    /**
     * guard against feedback while the variable table is being filled
     * programmatically. While <code>false</code>, the table model listener does
     * not append a trailing empty row.
     */
    private boolean autoReset = true;
    /** true while a connection is being opened on a background thread */
    private boolean connecting = false;
    
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
     * Creates new form JDBGeneratorMain. Restores the stored settings, asks for
     * a connection through the connection manager - the application exits when
     * that dialog is cancelled - and registers the platform handlers for the
     * about menu entry.
     */
    public JDBGeneratorMain() {
        initComponents();
        // freeze the schema pane's preferred width: the enclosing layout pins
        // this panel at preferred size, so without this a long tree item would
        // widen the panel and push the right-hand panels off screen.
        jScrollPane1.setPreferredSize(jScrollPane1.getPreferredSize());
        // the generation options panel is pinned the same way, and these two
        // fields grow their preferred width with the text they hold - a long
        // output path would widen the panel until it is pushed over the right
        // window edge. A column count keeps their preferred width constant.
        txtOutputDir.setColumns(20);
        txtAuthor.setColumns(20);
        // the custom variables label and its delete button form their own
        // trailing sub group, which is only as wide as its two members and
        // therefore hugs the left of the label column. Linking the label to
        // the other labels widens that group to the full column, so both of
        // them right-align with the labels above.
        ((javax.swing.GroupLayout)jPanel4.getLayout()).linkSize(
                javax.swing.SwingConstants.HORIZONTAL, jLabel11, jLabel14, jLabel16);
        // hiding the toolbar duplicates of the menu entries makes the layout
        // drop the container gap at the right window edge together with them,
        // so every row would end flush with the window border. The border
        // below puts that gap back for all of them at once.
        ((javax.swing.JComponent)getContentPane()).setBorder(
                javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 6));
        conf = JDBGenConfig.getInstance();
        chkDarkUI.setSelected(conf.isDarkUI());
        initLanguageCombo();
        chkApplyAbbr.setSelected(conf.isApplyAbbr());
        buildMenuBar();
        hideMenuDuplicates();
        initTableFilterUI();
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
        btnDelVar.addActionListener(e -> removeSelectedVar());
        initTemplateActions();
        // the window is EXIT_ON_CLOSE, and this runs before it exits: the
        // generation options are stored whichever way the window is left
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveGenerationOptions();
            }
        });

        PlatformUtils.registerHandlers(e -> showAbout(), null, null, null);
        log.info("before pack");
        this.pack();
        log.info("after pack");
        // below its minimum the layout does not shrink any further, it pushes
        // the right-hand panel over the window edge instead: do not let the
        // window be resized to where that happens. The computed minimum has
        // been measured to come out one layout gap short of what actually
        // fits, hence the extra slack on top of it.
        java.awt.Dimension minSize = getMinimumSize();
        setMinimumSize(new java.awt.Dimension(minSize.width + 12, minSize.height));
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
     * the dark user interface entry of the view menu, kept in step with
     * <code>chkDarkUI</code>. <code>null</code> until the menu bar is built.
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
     * Build the menu bar of the window. Nothing here duplicates the logic of a
     * button handler: an entry that mirrors a control clicks that control, so
     * that both ways in stay in step by construction. Call after
     * <code>initComponents()</code>, the controls it refers to have to exist.
     */
    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenu mnuFile = new JMenu(I18n.t("generatorMain.menu.file"));
        JMenuItem miGenerate = menuItem("generatorMain.menu.file.generate",
                e -> btnGenerate.doClick());
        miGenerate.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, shortcut));
        // the generate button is disabled while a connection is being opened,
        // mirror that instead of letting the accelerator through meanwhile
        miGenerate.setEnabled(btnGenerate.isEnabled());
        btnGenerate.addPropertyChangeListener("enabled",
                e -> miGenerate.setEnabled(btnGenerate.isEnabled()));
        mnuFile.add(miGenerate);
        mnuFile.addSeparator();
        mnuFile.add(menuItem("generatorMain.menu.file.close",
                e -> btnClose.doClick()));
        menuBar.add(mnuFile);

        JMenu mnuTools = new JMenu(I18n.t("generatorMain.menu.tools"));
        mnuTools.add(menuItem("generatorMain.menu.tools.connectionManager",
                e -> btnManageConn.doClick()));
        mnuTools.add(menuItem("generatorMain.menu.tools.driverManager",
                e -> showDriverManager()));
        mnuTools.addSeparator();
        mnuTools.add(menuItem("generatorMain.menu.tools.abbreviationMapper",
                e -> btnMapper.doClick()));
        menuBar.add(mnuTools);

        JMenu mnuView = new JMenu(I18n.t("generatorMain.menu.view"));
        miDarkUI = new JCheckBoxMenuItem(I18n.t("generatorMain.menu.view.darkUI"));
        miDarkUI.setSelected(chkDarkUI.isSelected());
        // both directions are guarded by a state comparison, so neither one can
        // trigger the other again
        miDarkUI.addActionListener(e -> {
            if (miDarkUI.isSelected() != chkDarkUI.isSelected())
                chkDarkUI.doClick();
        });
        chkDarkUI.addActionListener(e -> {
            if (miDarkUI.isSelected() != chkDarkUI.isSelected())
                miDarkUI.setSelected(chkDarkUI.isSelected());
        });
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
     * the language submenu, one radio entry per entry of
     * <code>cboLanguage</code>. Choosing one selects that entry in the combo
     * box, which is what stores the setting and tells the user that it is
     * applied after a restart.
     *
     * @return the language submenu, not attached to a menu yet.
     */
    private JMenu buildLanguageMenu() {
        JMenu mnuLanguage = new JMenu(I18n.t("generatorMain.menu.view.language"));
        ButtonGroup group = new ButtonGroup();
        String[] names = languageNames();
        int selected = languageIndex(conf.getLanguage());
        for (int i=0; i<names.length; i++) {
            final int idx = i;
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(names[i]);
            item.setSelected(i == selected);
            item.addActionListener(e -> cboLanguage.setSelectedIndex(idx));
            group.add(item);
            mnuLanguage.add(item);
        }
        return mnuLanguage;
    }

    /**
     * Hide the controls the menu bar took over. They are kept alive and
     * listened to, because the menu entries work by clicking them.
     */
    private void hideMenuDuplicates() {
        btnMapper.setVisible(false);
        btnAck.setVisible(false);
        chkDarkUI.setVisible(false);
        cboLanguage.setVisible(false);
    }

    /**
     * Add the filter field above the table list. This panel is small enough
     * that it is simpler to rebuild it by hand than to touch the form editor,
     * so it is re-laid out here instead of in <code>initComponents()</code>:
     * the label and the show-views tick keep their place, the filter field is
     * inserted between them and the scroll pane, and the scroll pane keeps
     * the rest of the panel. Call after <code>initComponents()</code>, the
     * components it rearranges have to exist already.
     */
    private void initTableFilterUI() {
        txtTableFilter = new javax.swing.JTextField();
        txtTableFilter.putClientProperty("JTextField.placeholderText",
                I18n.t("generatorMain.txtTableFilter.placeholder"));
        txtTableFilter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyTableFilter(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyTableFilter(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyTableFilter(); }
        });

        jPanel3.removeAll();
        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkShowView)
                    .addComponent(jLabel5))
                // a zero minimum filler, not a container gap with a minimum:
                // this panel is the one that yields when the window shrinks,
                // and a minimum here would push the generation options panel
                // over the right window edge instead.
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(txtTableFilter)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkShowView)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtTableFilter, javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2))
        );
        jPanel3.revalidate();
    }

    /**
     * Give the table list a "select all" / "clear selection" popup. Both act
     * on what is currently shown, i.e. on <code>lstTables</code>'s own model,
     * so they automatically respect whatever the filter field narrowed the
     * list down to. This listener is added on top of the one the form editor
     * already attached for the double click, Swing dispatches to every
     * listener of a component so the two do not interfere with each other.
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
        String filterText = txtTableFilter == null ? null : txtTableFilter.getText();
        visibleTables = filterTables(tables, filterText);
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
     * The column names of a generated table model are the untranslated
     * placeholders of the form editor, the shown ones are set here.
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
        UIUtils.addIcon(btnMapper, FontAwesome.BOOK);
        UIUtils.applyIcon(btnBrowseOutput, FontAwesome.FOLDER_O);
        UIUtils.applyIcon(btnAck, FontAwesome.INFO_CIRCLE);
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
        jScrollPane3.setViewportView(layer);
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
        if (currConn == null)
            return;
        currConn.setTemplates(readTemplateTable(tabTemplates.getModel()));
        currConn.setOutputDir(txtOutputDir.getText());
        currConn.setAuthor(txtAuthor.getText());
        currConn.setCustomVars(UIUtils.applyTableToMap(tabVars.getModel()));
        JDBGenConfig.saveInstance(this);
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnClose = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        treSchemas = new javax.swing.JTree();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstTables = new javax.swing.JList<>();
        chkShowView = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tabTemplates = new javax.swing.JTable();
        txtOutputDir = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        btnBrowseOutput = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        txtAuthor = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tabVars = new javax.swing.JTable();
        jLabel6 = new javax.swing.JLabel();
        btnDelVar = new javax.swing.JButton();
        chkApplyAbbr = new javax.swing.JCheckBox();
        jLabel15 = new javax.swing.JLabel();
        chkDarkUI = new javax.swing.JCheckBox();
        cboLanguage = new javax.swing.JComboBox<>();
        btnGenerate = new javax.swing.JButton();
        cboConnection = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        btnManageConn = new javax.swing.JButton();
        lblConnectionInfo = new javax.swing.JLabel();
        btnAck = new javax.swing.JButton();
        btnMapper = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(I18n.t("generatorMain.title"));

        btnClose.setText(I18n.t("generatorMain.btnClose.text"));
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD, jLabel1.getFont().getSize()+4));
        jLabel1.setText(I18n.t("generatorMain.jLabel1.text"));

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        treSchemas.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        treSchemas.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                treSchemasValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(treSchemas);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addContainerGap(25, Short.MAX_VALUE))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1))
        );

        lstTables.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                lstTablesMouseMoved(evt);
            }
        });
        lstTables.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lstTablesMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(lstTables);

        chkShowView.setText(I18n.t("generatorMain.chkShowView.text"));
        chkShowView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkShowViewActionPerformed(evt);
            }
        });

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getStyle() | java.awt.Font.BOLD, jLabel5.getFont().getSize()+4));
        jLabel5.setText(I18n.t("generatorMain.jLabel5.text"));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkShowView)
                    .addComponent(jLabel5))
                .addContainerGap(128, Short.MAX_VALUE))
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkShowView)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2))
        );

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() | java.awt.Font.BOLD, jLabel4.getFont().getSize()+4));
        jLabel4.setText(I18n.t("generatorMain.jLabel4.text"));

        tabTemplates.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Select", "Name", "Template File", "Out Template"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tabTemplates.getTableHeader().setReorderingAllowed(false);
        tabTemplates.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                tabTemplatesMouseMoved(evt);
            }
        });
        jScrollPane3.setViewportView(tabTemplates);

        txtOutputDir.setText("output");

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText(I18n.t("generatorMain.jLabel11.text"));

        btnBrowseOutput.setText("...");
        btnBrowseOutput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseOutputActionPerformed(evt);
            }
        });

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel14.setText(I18n.t("generatorMain.jLabel14.text"));

        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel16.setText(I18n.t("generatorMain.jLabel16.text"));

        tabVars.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null}
            },
            new String [] {
                "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane4.setViewportView(tabVars);

        jLabel6.setText(I18n.t("generatorMain.jLabel6.text"));

        btnDelVar.setText("-");

        chkApplyAbbr.setText(I18n.t("generatorMain.chkApplyAbbr.text"));
        chkApplyAbbr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkApplyAbbrActionPerformed(evt);
            }
        });

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel15.setText(I18n.t("generatorMain.jLabel15.text"));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel6))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtOutputDir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowseOutput))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                                .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel16)
                                .addComponent(btnDelVar)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(chkApplyAbbr, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtAuthor)))))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel11, jLabel14});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(btnBrowseOutput)
                    .addComponent(txtOutputDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtAuthor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkApplyAbbr)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDelVar))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {btnBrowseOutput, btnDelVar, txtOutputDir});

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        chkDarkUI.setText(I18n.t("generatorMain.chkDarkUI.text"));
        chkDarkUI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkDarkUIActionPerformed(evt);
            }
        });

        cboLanguage.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3" }));
        cboLanguage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboLanguageActionPerformed(evt);
            }
        });

        btnGenerate.setText(I18n.t("generatorMain.btnGenerate.text"));
        btnGenerate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGenerateActionPerformed(evt);
            }
        });

        cboConnection.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cboConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboConnectionActionPerformed(evt);
            }
        });

        jLabel2.setText(I18n.t("generatorMain.jLabel2.text"));

        btnManageConn.setText(I18n.t("generatorMain.btnManageConn.text"));
        btnManageConn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnManageConnActionPerformed(evt);
            }
        });

        lblConnectionInfo.setLabelFor(cboConnection);
        lblConnectionInfo.setText("Connection Information Placeholder");

        btnAck.setText("A");
        btnAck.setToolTipText(I18n.t("generatorMain.btnAck.toolTipText"));
        btnAck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAckActionPerformed(evt);
            }
        });

        btnMapper.setText(I18n.t("generatorMain.btnMapper.text"));
        btnMapper.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMapperActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(chkDarkUI)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cboLanguage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnGenerate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnClose))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cboConnection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnManageConn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblConnectionInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnMapper)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnAck)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cboConnection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(btnManageConn)
                    .addComponent(lblConnectionInfo)
                    .addComponent(btnAck)
                    .addComponent(btnMapper))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnClose)
                    .addComponent(chkDarkUI)
                    .addComponent(cboLanguage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGenerate))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * switch between the dark and the light look and feel and store the choice.
     */
    private void chkDarkUIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkDarkUIActionPerformed
        if (this.chkDarkUI.isSelected()) {
            UIUtils.setFlatDarkLaf();
        } else {
            UIUtils.setFlatLightLaf();
        }

        SwingUtilities.updateComponentTreeUI(this);
        applyConnectionInfoUI();
        conf.setDarkUI(this.chkDarkUI.isSelected());
        JDBGenConfig.saveInstance(this);
    }//GEN-LAST:event_chkDarkUIActionPerformed

    /**
     * the entry of <code>cboLanguage</code> a stored language setting selects.
     * Anything unknown falls back to the system default entry.
     *
     * @param language
     *            the stored language tag, or <code>null</code> for the
     *            operating system locale.
     * @return the index into <code>cboLanguage</code>, <code>0</code> for the
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
     * @return the entry names of the language combo and of the language menu.
     */
    private static String[] languageNames() {
        return new String[] {
            I18n.t("common.language.system"), "English",
            "한국어", "Español", "日本語", "简体中文" };
    }

    /**
     * Fill the language combo with {@link #languageNames()}.
     */
    private void initLanguageCombo() {
        cboLanguage.setModel(new DefaultComboBoxModel<>(languageNames()));
        cboLanguage.setToolTipText(I18n.t("common.language.tooltip"));
        cboLanguage.setSelectedIndex(languageIndex(conf.getLanguage()));
    }

    /**
     * store the chosen interface language, which takes effect on the next start.
     */
    private void cboLanguageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboLanguageActionPerformed
        int idx = cboLanguage.getSelectedIndex();
        // filling the combo in the constructor selects the stored entry, and
        // re-selecting the current one is not a change either
        if (idx < 0 || idx == languageIndex(conf.getLanguage()))
            return;
        conf.setLanguage(LANGUAGES[idx]);
        JDBGenConfig.saveInstance(this);
        // switching the language live would have to rebuild every open window,
        // so it is left to the next start.
        UIUtils.info(this, I18n.t("common.language.restartRequired"));
    }//GEN-LAST:event_cboLanguageActionPerformed

    /**
     * open the connection chosen in the combo box, after looking up the driver
     * it refers to.
     */
    private void cboConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboConnectionActionPerformed
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
    }//GEN-LAST:event_cboConnectionActionPerformed

    /**
     * open the connection manager and apply the connection it returns.
     */
    private void btnManageConnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnManageConnActionPerformed
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
    }//GEN-LAST:event_btnManageConnActionPerformed

    /**
     * reload the table list for the schema selected in the tree.
     */
    private void treSchemasValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_treSchemasValueChanged
        chkShowViewActionPerformed(null);
    }//GEN-LAST:event_treSchemasValueChanged

    /**
     * reload the table list of the selected schema, with or without views.
     */
    private void chkShowViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkShowViewActionPerformed
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
    }//GEN-LAST:event_chkShowViewActionPerformed

    /**
     * close the database connection and terminate the application.
     */
    @SuppressWarnings("UseSpecificCatch")
    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        saveGenerationOptions();
        if (dbmeta != null)
            try { dbmeta.close(); } catch(Exception ignored) {}
        System.exit(0);
    }//GEN-LAST:event_btnCloseActionPerformed

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
    private void btnGenerateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGenerateActionPerformed
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
    }//GEN-LAST:event_btnGenerateActionPerformed

    /**
     * open the column view of a table on a double click.
     */
    private void lstTablesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstTablesMouseClicked
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
    }//GEN-LAST:event_lstTablesMouseClicked

    /**
     * show the about dialog.
     */
    private void btnAckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAckActionPerformed
        showAbout();
    }//GEN-LAST:event_btnAckActionPerformed

    /**
     * show the template of the hovered row as a tooltip.
     */
    private void tabTemplatesMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tabTemplatesMouseMoved
        UIUtils.templateTooltip(tabTemplates, 1, evt);
    }//GEN-LAST:event_tabTemplatesMouseMoved

    /**
     * show the full name of the hovered table as a tooltip.
     */
    private void lstTablesMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstTablesMouseMoved
        int idx = visibleTables == null ? -1 : lstTables.locationToIndex(evt.getPoint());
        if (idx > -1 && idx < visibleTables.size()) {
            DBTable table = visibleTables.get(idx);
            lstTables.setToolTipText(table.getName());
        } else {
            lstTables.setToolTipText(null);
        }
    }//GEN-LAST:event_lstTablesMouseMoved

    /**
     * open the abbreviation mapper.
     */
    private void btnMapperActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMapperActionPerformed
        JDBAbbreviationMapper.getInstance(this).setVisible(true);
    }//GEN-LAST:event_btnMapperActionPerformed

    /**
     * store whether the abbreviation mapping is applied when generating.
     */
    private void chkApplyAbbrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkApplyAbbrActionPerformed
        conf.setApplyAbbr(chkApplyAbbr.isSelected());
        JDBGenConfig.saveInstance(this);
    }//GEN-LAST:event_chkApplyAbbrActionPerformed

    /**
     * pick the directory the generated files are written to.
     */
    private void btnBrowseOutputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseOutputActionPerformed
        String path = UIUtils.openDirDlg(this, "", true);
        if (!StrUtils.isEmpty(path))
            this.txtOutputDir.setText(path);
    }//GEN-LAST:event_btnBrowseOutputActionPerformed

    /**
     * show this window alone for development purposes. The regular entry point
     * of the application is <code>comart.tools.jdbgen.JDBGenerator</code>.
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        UIUtils.setFlatDarkLaf();
        EventQueue.invokeLater(() -> {
            JDBGeneratorMain gm = new JDBGeneratorMain();
            gm.setLocationRelativeTo(null);
            gm.setVisible(true);
            System.exit(0);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAck;
    private javax.swing.JButton btnBrowseOutput;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnDelVar;
    private javax.swing.JButton btnGenerate;
    private javax.swing.JButton btnManageConn;
    private javax.swing.JButton btnMapper;
    private javax.swing.JComboBox<String> cboConnection;
    private javax.swing.JComboBox<String> cboLanguage;
    private javax.swing.JCheckBox chkApplyAbbr;
    private javax.swing.JCheckBox chkDarkUI;
    private javax.swing.JCheckBox chkShowView;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JLabel lblConnectionInfo;
    private javax.swing.JList<String> lstTables;
    private javax.swing.JTable tabTemplates;
    private javax.swing.JTable tabVars;
    private javax.swing.JTree treSchemas;
    private javax.swing.JTextField txtAuthor;
    private javax.swing.JTextField txtOutputDir;
    // End of variables declaration//GEN-END:variables
}

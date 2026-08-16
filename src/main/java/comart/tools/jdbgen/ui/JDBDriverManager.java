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

import comart.tools.jdbgen.types.JDBDriver;
import comart.tools.jdbgen.types.JDBGenConfig;
import comart.tools.jdbgen.types.maven.SearchResponseItem;
import comart.utils.AppDirs;
import comart.utils.ClassUtils;
import comart.utils.I18n;
import comart.utils.PlatformUtils;
import comart.utils.StrUtils;
import comart.utils.UIUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import jiconfont.icons.font_awesome.FontAwesome;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * JDBC driver management dialog. It lists the configured drivers and edits the
 * selected one: the jar file and driver class to load, the url template and the
 * icon shown for it, the default connection properties, and the optional
 * vendor specific queries that replace the standard metadata lookups.
 * <p>
 * Drivers that ship with the application are marked as stock items; their name,
 * class and icon are read only and they cannot be deleted, but they can be
 * cloned into an editable copy. The dialog is a singleton, see
 * {@link #getInstance()}, and is shown modally from the connection manager.
 *
 * @author comart
 */
@Slf4j
public class JDBDriverManager extends JDialog {

    /**
     * color of the "driver required" hint of the driver list when the theme
     * defines none of its own.
     */
    private static final Color FALLBACK_MISSING_JAR_COLOR = new Color(0xD9534F);

    /**
     * the live driver list of the configuration. Every edit of this dialog
     * goes into this list, which is what gets saved.
     */
    private final List<JDBDriver> drivers;
    /**
     * the model behind the driver list, holding the driver names.
     */
    private final DefaultListModel<String> listModel;
    /**
     * the model of the connection property table of the selected driver.
     */
    private final DefaultTableModel tableModel;
    /**
     * <code>true</code> once a driver has been saved, so that the caller knows
     * it has to reload the driver list. Reset by {@link #getInstance()}.
     */
    public boolean changed = false;
    /**
     * guard against feedback while the property table is being filled
     * programmatically. While <code>false</code>, the table model listener
     * neither writes back into the selected driver nor appends a trailing
     * empty row.
     */
    private boolean autoreset = true;

    /**
     * the single instance of this dialog, see {@link #getInstance()}.
     */
    private static JDBDriverManager INSTANCE = null;
    /**
     * the single instance of this dialog, created on first use. The returned
     * instance is refreshed for the current look and feel and its
     * {@link #changed} flag is cleared, so that a caller can tell whether the
     * driver list was edited while the dialog was open.
     *
     * @return the shared driver manager dialog, never <code>null</code>.
     */
    public static synchronized JDBDriverManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JDBDriverManager();
            UIUtils.registerFrame(INSTANCE);
        }
        UIUtils.setApplicationIcon(INSTANCE);

        INSTANCE.updateComponents();
        INSTANCE.changed = false;
        return INSTANCE;
    }

    /**
     * Creates new form JDBDriverManager. Loads the drivers of the current
     * configuration into the list and registers the listener that writes the
     * property table back into the selected driver.
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    private JDBDriverManager() {
        initComponents();
        setModal(true);

        applyIcons();
        eventSetup();
        tableModel = (DefaultTableModel)tabProps.getModel();
        listModel = new DefaultListModel();
        lstDrivers.setModel(listModel);
        listModel.removeAllElements();
        JDBGenConfig conf = JDBGenConfig.getInstance();
        drivers = conf.getDrivers();
        drivers.forEach((d) -> listModel.addElement(d.getName()));

        UIUtils.iconHelpAction(btnIconHelp);

        tabProps.getModel().addTableModelListener((evt) -> {
            if (autoreset) {
                int idx = lstDrivers.getSelectedIndex();
                if (idx > -1) {
                    JDBDriver target = drivers.get(idx);
                    target.setProps(applyToPropsMap());
                }
                UIUtils.tableSetLastEmpty(tableModel);
            }
        });

        this.pack();
    }

    /**
     * convert the rows of the property table into a map, dropping the rows
     * with an empty key such as the trailing input row.
     *
     * @return the connection properties currently shown, in table order.
     */
    private Map<String, String> applyToPropsMap() {
        return UIUtils.applyTableToMap(tableModel);
    }

    /**
     * preselect a driver in the list before the dialog is shown. A negative
     * index leaves the current selection alone.
     *
     * @param index
     *            the index of the driver to select in the driver list.
     */
    public void setDriverIndex(int index) {
        if (index > -1) {
            lstDrivers.setSelectedIndex(index);
        }
    }

    /**
     * rebuild the user interface delegates of every component of this dialog
     * and restore what a look and feel change resets: the link style of the
     * download button and the icon renderer of the driver list. Needed because
     * the dialog outlives a look and feel change made in the main window.
     */
    public void updateComponents() {
        SwingUtilities.updateComponentTreeUI(this);
        btnDownJdbc.setBorder((Border)null);
        btnDownJdbc.setForeground(UIManager.getDefaults().getColor("Component.accentColor"));
        lstDrivers.setCellRenderer(driverListRenderer());
    }

    /**
     * the driver a name in the driver list belongs to.
     *
     * @param name the name shown in the list
     * @return the driver, or <code>null</code> when no driver carries the name.
     */
    private JDBDriver driverByName(Object name) {
        return drivers.stream()
                .filter(d -> d.getName() != null && d.getName().equals(name))
                .findFirst().orElse(null);
    }

    /**
     * whether the jar of a driver cannot be used: none is configured, or the
     * configured one is not there. Such a driver opens no connection, which is
     * what the driver list marks.
     *
     * @param driver the driver to check
     * @return <code>true</code> when the driver jar is missing.
     */
    private static boolean isJarMissing(JDBDriver driver) {
        if (StrUtils.isEmpty(driver.getJdbcJar()))
            return true;
        File jar = AppDirs.resolve(driver.getJdbcJar());
        return jar == null || !jar.exists();
    }

    /**
     * the renderer of the driver list: the icon and the name of the driver,
     * followed by a red hint for every driver whose jar is missing, so that a
     * fresh installation shows at a glance which drivers still need their jar
     * downloaded.
     *
     * @return the renderer, rebuilt on every look and feel change so that it
     *         picks up the colors of the current theme.
     */
    private ListCellRenderer<? super String> driverListRenderer() {
        ListCellRenderer<String> base = UIUtils.getListCellRenderer(this::driverByName);
        return (list, value, index, isSelected, cellHasFocus) -> {
            Component comp = base.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            JDBDriver driver = driverByName(value);
            if (driver != null && comp instanceof JLabel && isJarMissing(driver)) {
                // html, so that the hint keeps its own color whatever the
                // selection paints the rest of the cell in
                ((JLabel)comp).setText("<html>" + escapeHtml(driver.getName())
                        + " <span style='color:" + missingJarColor() + "'>("
                        + escapeHtml(I18n.t("driverManager.list.driverRequired"))
                        + ")</span></html>");
            }
            return comp;
        };
    }

    /**
     * @return the color of the "driver required" hint as a css value, taken
     *         from the theme where it defines one so that it stays readable in
     *         the light and in the dark theme.
     */
    private static String missingJarColor() {
        Color col = UIManager.getColor("Actions.Red");
        if (col == null)
            col = UIManager.getColor("Component.error.focusedBorderColor");
        return String.format("#%06x",
                (col == null ? FALLBACK_MISSING_JAR_COLOR : col).getRGB() & 0xFFFFFF);
    }

    /**
     * @param text the text to place into the html of a list cell
     * @return <code>text</code> with the characters html reads as markup escaped.
     */
    private static String escapeHtml(String text) {
        return text == null ? "" : text.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * apply the font icons of every button of this dialog.
     */
    private void applyIcons() {
        UIUtils.applyIcon(btnNewDriver, FontAwesome.PLUS);
        UIUtils.applyIcon(btnCloneDriver, FontAwesome.CLONE);
        UIUtils.applyIcon(btnDelDriver, FontAwesome.MINUS);
        UIUtils.applyIcon(btnDelProp, FontAwesome.MINUS);
        UIUtils.applyIcon(btnBrowseJar, FontAwesome.FOLDER_O);
        UIUtils.applyIcon(btnDelProp, FontAwesome.MINUS);
        UIUtils.applyIcon(btnBrowseIcon, FontAwesome.FOLDER_O);
        UIUtils.applyIcon(btnIconHelp, FontAwesome.QUESTION);

        UIUtils.applyIcon(btnTableComments, FontAwesome.QUESTION);
        UIUtils.applyIcon(btnColumnComments, FontAwesome.QUESTION);
        UIUtils.applyIcon(btnTables, FontAwesome.QUESTION);
        UIUtils.applyIcon(btnColumns, FontAwesome.QUESTION);

        UIUtils.addIcon(btnCancel, FontAwesome.TIMES);
        UIUtils.addIcon(btnSave, FontAwesome.CHECK);
    }

    /**
     * register the window listener that cancels the dialog when its window is
     * closed and raises it when it is activated.
     */
    private void eventSetup() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                btnCancelActionPerformed(null);
            }
            @Override
            public void windowActivated(WindowEvent e) {
                toFront();
            }
        });
        // NOTE: keeping one trailing empty row is handled by the (autoreset
        // guarded) table model listener registered in the constructor.
    }

    /**
     * clear the selection and every editor field, leaving the dialog in the
     * state of "no driver selected" with all read only fields editable again.
     */
    private void resetControls() {
        lstDrivers.clearSelection();
        txtDriverClass.setText("");
        txtDriverName.setText("");
        txtIcon.setText("");
        txtJarFile.setText("");
        txtUrlTemplate.setText("");

        chkTableComments.setSelected(false);
        txtTableComments.setText("");
        chkColumnComments.setSelected(false);
        txtColumnComments.setText("");
        chkTables.setSelected(false);
        txtTables.setText("");
        chkColumns.setSelected(false);
        txtColumns.setText("");
        txtDriverName.setEditable(true);
        txtDriverClass.setEditable(true);
        btnBrowseIcon.setEnabled(true);
    }

    /**
     * build the dialog: the driver list on the left, the editor of the selected
     * driver on the right, and the buttons that close the dialog below both of
     * them.
     */
    private void initComponents() {
        setTitle(I18n.t("driverManager.title"));

        btnSave = new JButton(I18n.t("driverManager.btnSave.text"));
        btnSave.addActionListener(this::btnSaveActionPerformed);

        btnCancel = new JButton(I18n.t("driverManager.btnCancel.text"));
        btnCancel.addActionListener(this::btnCancelActionPerformed);

        jTabbedPane1 = new JTabbedPane();
        jTabbedPane1.setFont(headingFont(jTabbedPane1.getFont()));
        jTabbedPane1.addTab(I18n.t("driverManager.tab.general"), createGeneralTab());
        jTabbedPane1.addTab(I18n.t("driverManager.tab.customQueries"), createCustomQueryTab());

        // the list keeps its preferred width, the editor takes the rest
        getContentPane().setLayout(new MigLayout("fill, insets dialog",
                "[][grow]", "[grow][]"));
        getContentPane().add(createDriverListPanel(), "grow, wmin 196");
        getContentPane().add(jTabbedPane1, "grow, wrap");
        getContentPane().add(btnSave, "spanx 2, split 2, align right");
        getContentPane().add(btnCancel);

        pack();
    }

    /**
     * build the left hand side of the dialog: the heading, the driver list and
     * the row of buttons that add, copy and remove a driver.
     *
     * @return the panel holding the driver list.
     */
    private JPanel createDriverListPanel() {
        jLabel1 = new JLabel(I18n.t("driverManager.jLabel1.text"));
        jLabel1.setFont(headingFont(jLabel1.getFont()));

        lstDrivers = new JList<>();
        lstDrivers.addListSelectionListener(this::lstDriversValueChanged);
        jScrollPane1 = new JScrollPane(lstDrivers);

        btnNewDriver = new JButton("+");
        btnNewDriver.addActionListener(this::btnNewDriverActionPerformed);
        btnCloneDriver = new JButton("c");
        btnCloneDriver.addActionListener(this::btnCloneDriverActionPerformed);
        btnDelDriver = new JButton("-");
        btnDelDriver.addActionListener(this::btnDelDriverActionPerformed);

        // the three buttons share the width of the list in equal parts
        jPanel3 = new JPanel(new MigLayout("insets 0, gap 0, fillx",
                "[sg listbtn, grow][sg listbtn, grow][sg listbtn, grow]", "[]"));
        jPanel3.add(btnNewDriver, "growx");
        jPanel3.add(btnCloneDriver, "growx");
        jPanel3.add(btnDelDriver, "growx");

        // the list takes the height that heading and button row leave over
        jPanel1 = new JPanel(new MigLayout("insets 0, fill, wrap 1",
                "[grow]", "[][grow][]"));
        jPanel1.add(jLabel1);
        jPanel1.add(jScrollPane1, "grow");
        jPanel1.add(jPanel3, "growx");
        return jPanel1;
    }

    /**
     * build the editor of the selected driver: what has to be loaded to open a
     * connection with it, how it is presented, and the connection properties it
     * hands to every connection using it.
     *
     * @return the panel shown on the general tab.
     */
    private JPanel createGeneralTab() {
        jLabel3 = trailingLabel("driverManager.jLabel3.text");
        txtDriverName = new JTextField();

        jLabel4 = trailingLabel("driverManager.jLabel4.text");
        txtJarFile = new JTextField();
        txtJarFile.setEditable(false);
        btnBrowseJar = new JButton("...");
        btnBrowseJar.addActionListener(this::btnBrowseJarActionPerformed);

        // a link rather than a button: it opens the maven explorer
        btnDownJdbc = new JButton(I18n.t("driverManager.btnDownJdbc.text"));
        btnDownJdbc.setFont(btnDownJdbc.getFont().deriveFont(btnDownJdbc.getFont().getSize()-1f));
        btnDownJdbc.setForeground(UIManager.getDefaults().getColor("Component.accentColor"));
        btnDownJdbc.setBorder(null);
        btnDownJdbc.setBorderPainted(false);
        btnDownJdbc.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDownJdbc.addActionListener(this::btnDownJdbcActionPerformed);

        jLabel8 = trailingLabel("driverManager.jLabel8.text");
        txtUrlTemplate = new JTextField();

        jLabel9 = trailingLabel("driverManager.jLabel9.text");
        txtDriverClass = new JTextField();
        txtDriverClass.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                txtDriverClassMouseClicked(evt);
            }
        });

        jLabel10 = trailingLabel("driverManager.jLabel10.text");
        txtIcon = new JTextField();
        txtIcon.setEditable(false);
        btnBrowseIcon = new JButton("...");
        btnBrowseIcon.addActionListener(this::btnBrowseIconActionPerformed);
        // what the button does is attached once the dialog is built, see the
        // constructor
        btnIconHelp = new JButton("?");

        chkNoAuth = new JCheckBox(I18n.t("driverManager.chkNoAuth.text"));

        jLabel11 = trailingLabel("driverManager.jLabel11.text");
        // the property table is edited in place, both of its columns hold text
        tabProps = new JTable(new DefaultTableModel(
                new Object[][] {{null, null}},
                new String[] {
                    I18n.t("driverManager.tabProps.key"),
                    I18n.t("driverManager.tabProps.value")
                }) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        });
        tabProps.getTableHeader().setReorderingAllowed(false);
        jScrollPane2 = new JScrollPane(tabProps);
        btnDelProp = new JButton("-");
        btnDelProp.addActionListener(this::btnDelPropActionPerformed);

        // a right aligned label column, an editor column that takes the width
        // left over, and two columns for the trailing buttons
        JPanel panel = new JPanel(new MigLayout("fillx, insets dialog",
                "[right][grow, fill][][]", "[]"));
        panel.add(jLabel3);
        panel.add(txtDriverName, "spanx 3, wrap");
        panel.add(jLabel4);
        panel.add(txtJarFile, "spanx 2");
        panel.add(btnBrowseJar, "wrap");
        panel.add(btnDownJdbc, "skip 1, spanx 3, growx, wrap");
        panel.add(jLabel8);
        panel.add(txtUrlTemplate, "spanx 3, wrap");
        panel.add(jLabel9);
        panel.add(txtDriverClass, "spanx 3, wrap");
        // the browse and help buttons keep the height of the field they follow
        panel.add(jLabel10);
        panel.add(txtIcon, "sgy iconrow");
        panel.add(btnBrowseIcon, "sgy iconrow");
        panel.add(btnIconHelp, "sgy iconrow, wrap");
        // the check box keeps its own width, the editor column would stretch it
        panel.add(chkNoAuth, "skip 1, spanx 3, alignx left, w pref!, wrap");
        // the property table takes the height left over, its heading and its
        // remove button stay beside it in the label column
        panel.add(jLabel11, "aligny top");
        // the table asks for a viewport of its own that would blow the dialog
        // up, it is given a share of the height and grows with the window
        panel.add(jScrollPane2, "spanx 3, spany 2, grow, h 240, wrap");
        panel.add(btnDelProp, "aligny top, sgy iconrow, pushy");
        return panel;
    }

    /**
     * build the editor of the queries that replace the standard metadata
     * lookups. Every section is a check box that turns the query on, a help
     * button pointing at the documentation, and the statement itself.
     *
     * @return the scroll pane shown on the custom queries tab; the sections do
     *         not fit into the dialog on smaller screens.
     */
    private JScrollPane createCustomQueryTab() {
        btnTableComments = new JButton("?");
        btnTableComments.addActionListener(this::btnTableCommentsActionPerformed);
        chkTableComments = new JCheckBox(I18n.t("driverManager.chkTableComments.text"));
        chkTableComments.addActionListener(this::chkTableCommentsActionPerformed);
        txtTableComments = queryArea();
        jScrollPane5 = new JScrollPane(txtTableComments);

        btnColumnComments = new JButton("?");
        btnColumnComments.addActionListener(this::btnColumnCommentsActionPerformed);
        chkColumnComments = new JCheckBox(I18n.t("driverManager.chkColumnComments.text"));
        chkColumnComments.addActionListener(this::chkColumnCommentsActionPerformed);
        txtColumnComments = queryArea();
        jScrollPane4 = new JScrollPane(txtColumnComments);

        btnTables = new JButton("?");
        btnTables.addActionListener(this::btnTablesActionPerformed);
        chkTables = new JCheckBox(I18n.t("driverManager.chkTables.text"));
        chkTables.addActionListener(this::chkTablesActionPerformed);
        txtTables = queryArea();
        jScrollPane6 = new JScrollPane(txtTables);

        btnColumns = new JButton("?");
        btnColumns.addActionListener(this::btnColumnsActionPerformed);
        chkColumns = new JCheckBox(I18n.t("driverManager.chkColumns.text"));
        chkColumns.addActionListener(this::chkColumnsActionPerformed);
        txtColumns = queryArea();
        jScrollPane7 = new JScrollPane(txtColumns);

        jPanel5 = new JPanel(new MigLayout("fillx, insets dialog", "[grow]", "[]"));
        addQuerySection(jPanel5, chkTableComments, btnTableComments, jScrollPane5, true);
        addQuerySection(jPanel5, chkColumnComments, btnColumnComments, jScrollPane4, false);
        addQuerySection(jPanel5, chkTables, btnTables, jScrollPane6, false);
        addQuerySection(jPanel5, chkColumns, btnColumns, jScrollPane7, false);

        jScrollPane3 = new JScrollPane(jPanel5);
        return jScrollPane3;
    }

    /**
     * add one query section to the custom query panel: the check box and its
     * help button on one row, the statement indented below them.
     *
     * @param panel
     *            the panel the section is added to.
     * @param chk
     *            the check box that turns the query on.
     * @param help
     *            the button opening the documentation of the query.
     * @param area
     *            the scroll pane holding the statement.
     * @param first
     *            whether this is the topmost section, which needs no extra gap
     *            above it.
     */
    private static void addQuerySection(JPanel panel, JCheckBox chk, JButton help,
            JScrollPane area, boolean first) {
        panel.add(chk, "split 2" + (first ? "" : ", gaptop unrelated"));
        panel.add(help, "wrap");
        panel.add(area, "growx, h 65!, gapleft 23, wrap");
    }

    /**
     * an editor for one of the custom queries.
     *
     * @return the text area, not added to a container yet.
     */
    private static JTextArea queryArea() {
        JTextArea area = new JTextArea();
        area.setColumns(20);
        area.setRows(5);
        return area;
    }

    /**
     * a label of the editor: right aligned, so that it reads towards the field
     * it names.
     *
     * @param key
     *            the resource key of the label text.
     * @return the label, not added to a container yet.
     */
    private static JLabel trailingLabel(String key) {
        JLabel label = new JLabel(I18n.t(key));
        label.setHorizontalAlignment(SwingConstants.TRAILING);
        return label;
    }

    /**
     * the heading font of this dialog: the given font in bold and four points
     * larger.
     *
     * @param base
     *            the font of the component the heading font is derived for.
     * @return the derived font.
     */
    private static Font headingFont(Font base) {
        return base.deriveFont(base.getStyle() | Font.BOLD, base.getSize() + 4f);
    }

    /**
     * close the dialog, discarding whatever has not been saved.
     */
    private void btnCancelActionPerformed(ActionEvent evt) {
        setVisible(false);
    }

    /**
     * validate the editor fields, store them into the driver, save the
     * configuration and close the dialog. The first failing check is reported
     * and focuses its field.
     */
    private void btnSaveActionPerformed(ActionEvent evt) {
        int idx = lstDrivers.getSelectedIndex();
        boolean isNameExists;
        JDBDriver target = null;
        if (idx == -1) {
            isNameExists = NamingUtils.nameExists(drivers, txtDriverName.getText());
        } else {
            target = drivers.get(idx);
            isNameExists = !target.getName().equals(txtDriverName.getText()) &&
                    NamingUtils.nameExists(drivers, txtDriverName.getText());
        }

        if (isNameExists) {
            UIUtils.error(this, I18n.t("driverManager.msg.nameExists", txtDriverName.getText()));
            txtDriverName.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtDriverName.getText())) {
            UIUtils.error(this, I18n.t("driverManager.msg.driverNameRequired"));
            txtDriverName.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtJarFile.getText())) {
            UIUtils.error(this, I18n.t("driverManager.msg.jarRequired"));
            txtJarFile.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtDriverClass.getText())) {
            UIUtils.error(this, I18n.t("driverManager.msg.driverClassRequired"));
            txtDriverClass.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtTableComments.getText()) && chkTableComments.isSelected()) {
            UIUtils.error(this, I18n.t("driverManager.msg.tableCommentsRequired"));
            txtTableComments.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtColumnComments.getText()) && chkColumnComments.isSelected()) {
            UIUtils.error(this, I18n.t("driverManager.msg.columnCommentsRequired"));
            txtColumnComments.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtTables.getText()) && chkTables.isSelected()) {
            UIUtils.error(this, I18n.t("driverManager.msg.tablesRequired"));
            txtTables.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtColumns.getText()) && chkColumns.isSelected()) {
            UIUtils.error(this, I18n.t("driverManager.msg.columnsRequired"));
            txtColumns.requestFocusInWindow();
        } else {
            boolean isNew = target == null;
            if (isNew)
                target = new JDBDriver();

            target.setDriverClass(txtDriverClass.getText());
            target.setIcon(txtIcon.getText());
            target.setJdbcJar(txtJarFile.getText());
            target.setName(txtDriverName.getText());
            target.setUrlTemplate(txtUrlTemplate.getText());
            target.setNoAuth(chkNoAuth.isSelected());

            // empty rows are filtered out by applyToPropsMap, so this also
            // reflects the removal of the very last property.
            target.setProps(applyToPropsMap());

            target.setUseTableComments(chkTableComments.isSelected());
            target.setTableCommentsSql(txtTableComments.getText());
            target.setUseColumnComments(chkColumnComments.isSelected());
            target.setColumnCommentsSql(txtColumnComments.getText());
            target.setUseTables(chkTables.isSelected());
            target.setTablesSql(txtTables.getText());
            target.setUseColumns(chkColumns.isSelected());
            target.setColumnsSql(txtColumns.getText());

            if (isNew) {
                drivers.add(target);
                listModel.addElement(target.getName());
            } else {
                // name may have been changed, keep the list model in sync
                listModel.set(idx, target.getName());
            }
            // the jar of the driver may have changed with it
            lstDrivers.repaint();

            JDBGenConfig.saveInstance(this);
            changed = true;
            setVisible(false);
        }
    }

    /**
     * apply a change to the driver that is currently selected in the list.
     * Does nothing when there is no selection.
     *
     * @param cons
     *            the change to perform on the selected driver.
     */
    private void updateDriver(Consumer<JDBDriver> cons) {
        int idx = lstDrivers.getSelectedIndex();
        if (idx < 0) return;
        JDBDriver driver = (JDBDriver)drivers.get(lstDrivers.getSelectedIndex());
        cons.accept(driver);
    }

    /**
     * load the selected driver into the editor fields. The identifying fields
     * of a driver shipped with the application stay read only.
     */
    private void lstDriversValueChanged(ListSelectionEvent evt) {
        int idx = lstDrivers.getSelectedIndex();
        if (idx < 0) return;
        autoreset = false;
        JDBDriver driver = (JDBDriver)drivers.get(lstDrivers.getSelectedIndex());
        txtDriverName.setText(driver.getName());
        txtDriverClass.setText(driver.getDriverClass());
        txtIcon.setText(driver.getIcon());
        txtJarFile.setText(driver.getJdbcJar());
        txtUrlTemplate.setText(driver.getUrlTemplate());

        chkNoAuth.setSelected(driver.isNoAuth());

        for(int i = tableModel.getRowCount() - 1; i >= 0; --i) {
            tableModel.removeRow(i);
        }

        if (driver.getProps() != null) {
            driver.getProps().forEach((k, v) -> {
                if (!"".equals(k))
                    tableModel.addRow(new String[]{k, v});
            });
        }

        chkTableComments.setSelected(driver.isUseTableComments());
        txtTableComments.setEnabled(chkTableComments.isSelected());
        txtTableComments.setText(driver.getTableCommentsSql());
        chkColumnComments.setSelected(driver.isUseColumnComments());
        txtColumnComments.setEnabled(chkColumnComments.isSelected());
        txtColumnComments.setText(driver.getColumnCommentsSql());
        chkTables.setSelected(driver.isUseTables());
        txtTables.setEnabled(chkTables.isSelected());
        txtTables.setText(driver.getTablesSql());
        chkColumns.setSelected(driver.isUseColumns());
        txtColumns.setEnabled(chkColumns.isSelected());
        txtColumns.setText(driver.getColumnsSql());

        boolean isStockItem = driver.isStockItem();
        txtDriverName.setEditable(!isStockItem);
        txtDriverClass.setEditable(!isStockItem);
        btnBrowseIcon.setEnabled(!isStockItem);
        btnDelDriver.setEnabled(!isStockItem);
        txtIcon.setEditable(!isStockItem);
        autoreset = true;
        // rows were filled while autoreset was off, so make sure there is a
        // trailing empty row to type a new property into.
        UIUtils.tableSetLastEmpty(tableModel);
    }

    /**
     * add a driver with a generated name and the generic icon, and select it.
     */
    private void btnNewDriverActionPerformed(ActionEvent evt) {
        JDBDriver driver = new JDBDriver();
        driver.setIcon("stock:generic.png");
        driver.setName(NamingUtils.nextNameOf(drivers, I18n.t("driverManager.msg.newDriverName")));
        driver.setProps(new LinkedHashMap<>());
        drivers.add(driver);
        listModel.addElement(driver.getName());
        lstDrivers.setSelectedIndex(drivers.size()-1);
    }

    /**
     * add an editable copy of the selected driver and select it.
     */
    private void btnCloneDriverActionPerformed(ActionEvent evt) {
        int idx = lstDrivers.getSelectedIndex();
        if (idx >= 0) {
            JDBDriver driver = drivers.get(idx);
            JDBDriver newOne = driver.toBuilder()
                    .name(NamingUtils.nextNameOf(drivers,
                            I18n.t("driverManager.msg.copyOfName", driver.getName())))
                    .stockItem(false)
                    .build();
            if (ObjectUtils.isNotEmpty(driver.getProps()))
                newOne.setProps(new LinkedHashMap(driver.getProps()));

            drivers.add(newOne);
            listModel.addElement(newOne.getName());
            lstDrivers.setSelectedIndex(drivers.size() - 1);
        }
    }

    /**
     * remove the selected driver unless it is shipped with the application.
     */
    private void btnDelDriverActionPerformed(ActionEvent evt) {
        int idx = lstDrivers.getSelectedIndex();
        if (idx >= 0) {
            JDBDriver driver = (JDBDriver)drivers.get(idx);
            if (!driver.isStockItem()) {
                drivers.remove(idx);
                listModel.remove(idx);
            }
            resetControls();
        }
    }

    /**
     * remove the selected connection property.
     */
    private void btnDelPropActionPerformed(ActionEvent evt) {
        int idx = tabProps.getSelectedRow();
        if (idx > -1) {
            tableModel.removeRow(idx);
        }
    }

    /**
     * pick the icon shown for this driver.
     */
    private void btnBrowseIconActionPerformed(ActionEvent evt) {
        String fpath = UIUtils.openIconDlg(this, "");
        if (!StrUtils.isEmpty(fpath)) {
            txtIcon.setText(fpath);
        }
    }

    /**
     * offer the driver classes found in the selected jar in a popup menu, and
     * store the picked one in the driver.
     */
    private void txtDriverClassMouseClicked(MouseEvent evt) {
        if (ObjectUtils.isNotEmpty(txtJarFile.getText())) {
            List<String> clazz = ClassUtils.getDrivers(txtJarFile.getText());
            if (ObjectUtils.isNotEmpty(clazz)) {
                JPopupMenu popup = new JPopupMenu();
                clazz.forEach((c) -> {
                    JMenuItem item = new JMenuItem(c);
                    item.addActionListener((e) -> {
                        txtDriverClass.setText(c);
                        updateDriver(d -> d.setDriverClass(c));
                    });
                    popup.add(item);
                });
                popup.show(txtDriverClass, evt.getX(), evt.getY());
            }
        }
    }

    /**
     * download the JDBC jar of the selected driver.
     *
     * <p>A driver that carries a Maven coordinate - every driver shipped with
     * the application whose driver is published on Maven Central - is fetched
     * straight from the repository, so that a fresh installation is one click
     * away from a working connection. Everything else opens the maven explorer
     * with the driver's default query as the initial search.</p>
     */
    private void btnDownJdbcActionPerformed(ActionEvent evt) {
        EventQueue.invokeLater(() -> updateDriver(d -> {
            SearchResponseItem item = SearchResponseItem.ofCoordinate(d.getMavenArtifact());
            if (item != null) {
                String stored = MavenExplorer.downloadJar(this, item);
                if (stored != null)
                    applyDownloadedJar(d, stored);
                return;
            }
            MavenExplorer me = MavenExplorer.getInstance();
            me.setModal(true);
            me.setLocationRelativeTo(this);
            String query = d.getDefaultQuery();
            if (!StrUtils.isEmpty(query))
                me.setQuery(query);
            me.setVisible(true);
            if (me.changed)
                applyDownloadedJar(d, me.saveLocation);
        }));
    }

    /**
     * store the jar a download produced in the driver and show it in the
     * editor. The driver list is repainted as well: the driver has stopped
     * missing its jar.
     *
     * @param driver the driver the jar belongs to
     * @param stored the location of the jar, relative to the user data directory
     */
    private void applyDownloadedJar(JDBDriver driver, String stored) {
        txtJarFile.setText(stored);
        driver.setJdbcJar(stored);
        lstDrivers.repaint();
    }

    /**
     * pick the JDBC jar, filtered to jar and zip files and starting in the
     * driver directory. A jar below the user data or the installation directory
     * is stored relative to it.
     */
    private void btnBrowseJarActionPerformed(ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(AppDirs.driversDir());
        fc.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    String fname = f.getName();
                    int idx = fname.lastIndexOf(46);
                    if (idx <= -1) {
                        return false;
                    } else {
                        String ext = fname.substring(idx + 1).toLowerCase();
                        return "jar".equals(ext) || "zip".equals(ext);
                    }
                }
            }

            @Override
            public String getDescription() {
                return I18n.t("driverManager.filechooser.jarFilter");
            }
        });
        if (fc.showOpenDialog(this) == 0) {
            // a jar below the user data or the installation directory is
            // stored relative to it, see AppDirs.resolve()
            String relative = AppDirs.relativize(fc.getSelectedFile().getAbsolutePath());
            this.txtJarFile.setText(relative);
            updateDriver(d -> d.setJdbcJar(relative));
            // the driver may just have stopped missing its jar
            lstDrivers.repaint();
        }
    }

    /**
     * enable the table comment query along with its check box.
     */
    private void chkTableCommentsActionPerformed(ActionEvent evt) {
        txtTableComments.setEnabled(chkTableComments.isSelected());
    }

    /**
     * enable the column comment query along with its check box.
     */
    private void chkColumnCommentsActionPerformed(ActionEvent evt) {
        txtColumnComments.setEnabled(chkColumnComments.isSelected());
    }

    /**
     * open the documentation of the table comment query.
     */
    private void btnTableCommentsActionPerformed(ActionEvent evt) {
        PlatformUtils.openDoc("custom-queries.md#get-table-comments-sql");
    }

    /**
     * open the documentation of the column comment query.
     */
    private void btnColumnCommentsActionPerformed(ActionEvent evt) {
        PlatformUtils.openDoc("custom-queries.md#get-column-comments-sql");
    }

    /**
     * enable the table list query along with its check box.
     */
    private void chkTablesActionPerformed(ActionEvent evt) {
        txtTables.setEnabled(chkTables.isSelected());
    }

    /**
     * open the documentation of the table list query.
     */
    private void btnTablesActionPerformed(ActionEvent evt) {
        PlatformUtils.openDoc("custom-queries.md#get-table-list-sql");
    }

    /**
     * enable the column list query along with its check box.
     */
    private void chkColumnsActionPerformed(ActionEvent evt) {
        txtColumns.setEnabled(chkColumns.isSelected());
    }

    /**
     * open the documentation of the column list query.
     */
    private void btnColumnsActionPerformed(ActionEvent evt) {
        PlatformUtils.openDoc("custom-queries.md#get-column-list-sql");
    }

    // the frame of the dialog
    private JTabbedPane jTabbedPane1;
    private JButton btnSave;
    private JButton btnCancel;

    // the driver list on the left
    private JPanel jPanel1;
    private JPanel jPanel3;
    private JLabel jLabel1;
    private JScrollPane jScrollPane1;
    private JList<String> lstDrivers;
    private JButton btnNewDriver;
    private JButton btnCloneDriver;
    private JButton btnDelDriver;

    // the editor of the selected driver
    private JLabel jLabel3;
    private JTextField txtDriverName;
    private JLabel jLabel4;
    private JTextField txtJarFile;
    private JButton btnBrowseJar;
    private JButton btnDownJdbc;
    private JLabel jLabel8;
    private JTextField txtUrlTemplate;
    private JLabel jLabel9;
    private JTextField txtDriverClass;
    private JLabel jLabel10;
    private JTextField txtIcon;
    private JButton btnBrowseIcon;
    private JButton btnIconHelp;
    private JCheckBox chkNoAuth;
    private JLabel jLabel11;
    private JScrollPane jScrollPane2;
    private JTable tabProps;
    private JButton btnDelProp;

    // the custom metadata queries
    private JScrollPane jScrollPane3;
    private JPanel jPanel5;
    private JCheckBox chkTableComments;
    private JButton btnTableComments;
    private JScrollPane jScrollPane5;
    private JTextArea txtTableComments;
    private JCheckBox chkColumnComments;
    private JButton btnColumnComments;
    private JScrollPane jScrollPane4;
    private JTextArea txtColumnComments;
    private JCheckBox chkTables;
    private JButton btnTables;
    private JScrollPane jScrollPane6;
    private JTextArea txtTables;
    private JCheckBox chkColumns;
    private JButton btnColumns;
    private JScrollPane jScrollPane7;
    private JTextArea txtColumns;
}

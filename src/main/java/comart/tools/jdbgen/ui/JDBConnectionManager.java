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

import comart.tools.jdbgen.types.JDBConnection;
import comart.tools.jdbgen.types.JDBDriver;
import comart.tools.jdbgen.types.JDBGenConfig;
import comart.utils.I18n;
import comart.utils.StrUtils;
import comart.utils.UIUtils;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import jiconfont.icons.font_awesome.FontAwesome;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.ObjectUtils;

/**
 * connection management dialog. It lists the configured database connections
 * and edits how the selected one is reached: driver, url, credentials,
 * keep-alive and JDBC connection properties. What a connection generates - its
 * templates, its output directory, its author and its custom variables - is
 * edited in the generation options panel of the main window instead, so this
 * dialog neither shows nor overwrites those settings.
 * <p>
 * The dialog is a singleton that is reused for the whole lifetime of the
 * application, see {@link #getInstance()}. It is shown modally both at start up
 * and from the main window; the connection the user finally picked is left in
 * {@link #selectedConnection}.
 *
 * @author comart
 */
@Slf4j
public class JDBConnectionManager extends JDialog {

    /**
     * the single instance of this dialog, see {@link #getInstance()}.
     */
    private static JDBConnectionManager INSTANCE = null;

    /**
     * where a connection writes its generated files when nothing else was
     * chosen. It names a directory below the user data directory, see
     * <code>AppDirs.resolveOutputDir</code>.
     */
    private static final String DEFAULT_OUTPUT_DIR = "output";

    /**
     * the single instance of this dialog, created on first use. The returned
     * instance is refreshed for the current look and feel and its
     * {@link #selectedConnection} is cleared, so that a caller can tell a
     * cancelled dialog from a confirmed one.
     *
     * @return the shared connection manager dialog, never <code>null</code>.
     */
    public static synchronized JDBConnectionManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JDBConnectionManager();
            UIUtils.registerFrame(INSTANCE);
        }
        UIUtils.setApplicationIcon(INSTANCE);

        INSTANCE.updateComponents();
        // the driver manager can be reached from the main window as well, so
        // the driver list may have changed since this dialog was last shown
        INSTANCE.refreshDrivers();
        INSTANCE.selectedConnection = null;
        return INSTANCE;
    }

    /**
     * the live connection list of the configuration. Every edit of this dialog
     * goes into this list, which is what gets saved.
     */
    private List<JDBConnection> connections = null;
    /**
     * the driver list of the configuration, reloaded whenever the driver
     * manager reports a change.
     */
    private List<JDBDriver> drivers = null;
    /**
     * the connections by name, used by the list cell renderer to find the icon
     * of an entry.
     */
    private Map<String, JDBConnection> connMap = new HashMap<>();
    /**
     * the drivers by name, used by the combo box renderer and to look up the
     * driver the edited connection refers to.
     */
    private Map<String, JDBDriver> driverMap = new HashMap<>();
    /**
     * the model of the JDBC connection property table.
     */
    private DefaultTableModel propsModel = null;
    /**
     * the model behind the connection list, holding the connection names.
     */
    private DefaultListModel listModel = null;
    /**
     * the configuration the connections and drivers are read from and written
     * back to.
     */
    private JDBGenConfig conf = null;
    /**
     * whether the last save attempt stored the connection. The connect button
     * only closes the dialog when it did.
     */
    private boolean saveSuccess = false;
    /**
     * guard against feedback while the property table is being filled
     * programmatically. While <code>false</code>, the table model listener
     * neither writes back into the selected connection nor appends a trailing
     * empty row.
     */
    private boolean autoReset = true;

    /**
     * the connection the user confirmed with "connect" or "save", or
     * <code>null</code> if the dialog was cancelled or nothing has been saved
     * yet. Reset by {@link #getInstance()} before the dialog is shown again.
     */
    public JDBConnection selectedConnection = null;

    /**
     * Creates new form JDBConnectionManager. Loads the drivers and connections
     * of the current configuration into the dialog, registers the listener that
     * keeps the property table in sync with the selected connection, and
     * selects the first connection if there is one.
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    private JDBConnectionManager() {
        initComponents();
        setModal(true);

        conf = JDBGenConfig.getInstance();
        applyIcons();
        applyTooltips();
        eventSetup();
        propsModel = (DefaultTableModel)tabProps.getModel();
        listModel = new DefaultListModel();
        lstConnections.setModel(listModel);
        listModel.removeAllElements();
        drivers = conf.getDrivers();
        drivers.forEach(d -> driverMap.put(d.getName(), d));
        connections = conf.getConnections();
        connections.forEach(c -> connMap.put(c.getName(), c));
        if (ObjectUtils.isNotEmpty(connections))
            connections.forEach(c -> listModel.addElement(c.getName()));
        cboDriver.setRenderer(UIUtils.getListCellRenderer(s -> driverMap.get(s)));
        lstConnections.setCellRenderer(UIUtils.getListCellRenderer(s -> connMap.get(s)));

        refreshDrivers();
        resetControls();

        UIUtils.iconHelpAction(btnIconHelp);

        propsModel.addTableModelListener((evt) -> {
            if (autoReset) {
                int idx = lstConnections.getSelectedIndex();
                if (idx > -1) {
                    JDBConnection target = connections.get(idx);
                    target.setConnectionProps(applyToPropsMap());
                }
                autoReset = false;
                UIUtils.tableSetLastEmpty(propsModel);
                autoReset = true;
            }
        });

        if (!connections.isEmpty()) {
            lstConnections.setSelectedIndex(0);
        }

        UIUtils.setCommitOnLostFocus(tabProps);

        this.pack();
    }

    /**
     * rebuild the user interface delegates of every component of this dialog.
     * Needed because the dialog outlives a look and feel change made in the
     * main window.
     */
    public void updateComponents() {
        SwingUtilities.updateComponentTreeUI(this);
    }

    /**
     * apply the font icons of every button of this dialog.
     */
    private void applyIcons() {
        UIUtils.applyIcon(btnNewConn, FontAwesome.PLUS);
        UIUtils.applyIcon(btnCloneConn, FontAwesome.CLONE);
        UIUtils.applyIcon(btnDelConn, FontAwesome.MINUS);
        UIUtils.applyIcon(btnDelProp, FontAwesome.MINUS);
        UIUtils.applyIcon(btnBrowseIcon, FontAwesome.FOLDER_O);
        UIUtils.applyIcon(btnIconHelp, FontAwesome.QUESTION);

        UIUtils.addIcon(btnManage, FontAwesome.COG);
        UIUtils.addIcon(btnSave, FontAwesome.FLOPPY_O);
        UIUtils.addIcon(btnConnect, FontAwesome.PLUG);
        UIUtils.addIcon(btnCancel, FontAwesome.TIMES);
    }

    /**
     * describe the inputs whose accepted values are not obvious.
     */
    private void applyTooltips() {
        txtIcon.setToolTipText(I18n.t("connectionManager.txtIcon.toolTipText"));
        tabProps.setToolTipText(I18n.t("connectionManager.tabProps.toolTipText"));
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
    }

    /**
     * rebuild the driver combo box from the configuration, keeping the driver
     * that was selected before if it still exists. Rebuilding the combo box
     * changes its selection on the way, which must not be taken for the user
     * picking a driver - that would overwrite the url and the properties of the
     * connection being edited with the defaults of whatever driver happens to
     * come first.
     */
    private void refreshDrivers() {
        boolean back = autoReset;
        autoReset = false;
        try {
            String dname = (String)cboDriver.getSelectedItem();
            int idx = -1;
            cboDriver.removeAllItems();
            // the driver list may have been added to/renamed/removed meanwhile,
            // so rebuild the lookup map together with the combo box.
            driverMap.clear();
            drivers = conf.getDrivers();
            for (int i=0; i<drivers.size(); i++) {
                JDBDriver d = drivers.get(i);
                driverMap.put(d.getName(), d);
                cboDriver.addItem(d.getName());
                if (dname != null && dname.equals(d.getName()))
                    idx = i;
            }
            cboDriver.setSelectedIndex(idx);
        } finally {
            autoReset = back;
        }
    }

    /**
     * empty the JDBC connection property table.
     */
    private void removeProps() {
        propsModel.setRowCount(0);
    }

    /**
     * clear the selection and every editor field, leaving the dialog in the
     * state of "no connection selected".
     */
    private void resetControls() {
        lstConnections.clearSelection();
        txtConnUrl.setText("");
        txtIcon.setText("");
        txtKeepAliveQuery.setText("");
        txtName.setText("");
        txtPassword.setText("");
        txtUser.setText("");
        txtKeepAliveSec.setText("");
        chkKeepAlive.setSelected(false);
        cboDriver.setSelectedIndex(-1);
        removeProps();
    }

    /**
     * build the dialog: the connection list on the left, the editor of the
     * selected connection on the right, and the buttons that close the dialog
     * below both of them.
     */
    private void initComponents() {
        setTitle(I18n.t("connectionManager.title"));

        btnSave = new JButton(I18n.t("connectionManager.btnSave.text"));
        btnSave.addActionListener(this::btnSaveActionPerformed);

        btnConnect = new JButton(I18n.t("connectionManager.btnConnect.text"));
        btnConnect.addActionListener(this::btnConnectActionPerformed);

        btnCancel = new JButton(I18n.t("connectionManager.btnCancel.text"));
        btnCancel.addActionListener(this::btnCancelActionPerformed);

        jTabbedPane1 = new JTabbedPane();
        jTabbedPane1.setFont(headingFont(jTabbedPane1.getFont()));
        jTabbedPane1.addTab(I18n.t("connectionManager.tab.general"), createGeneralTab());

        jSplitPane1 = new JSplitPane();
        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setLeftComponent(createConnectionListPanel());
        jSplitPane1.setRightComponent(jTabbedPane1);

        // the editor takes all the room, the buttons sit right aligned below
        // it. The width the dialog opens with is the one the form was drawn
        // for, the editor alone would ask for a wider window.
        getContentPane().setLayout(new MigLayout("fill, insets dialog",
                "[grow]", "[grow][]"));
        getContentPane().add(jSplitPane1, "grow, w 827, wrap");
        getContentPane().add(btnSave, "split 3, align right");
        getContentPane().add(btnConnect);
        getContentPane().add(btnCancel);

        pack();
    }

    /**
     * build the left hand side of the split pane: the heading, the connection
     * list and the row of buttons that add, copy and remove a connection.
     *
     * @return the panel holding the connection list.
     */
    private JPanel createConnectionListPanel() {
        jLabel1 = new JLabel(I18n.t("connectionManager.jLabel1.text"));
        jLabel1.setFont(headingFont(jLabel1.getFont()));

        lstConnections = new JList<>();
        lstConnections.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                lstConnectionsMouseClicked(evt);
            }
        });
        lstConnections.addListSelectionListener(this::lstConnectionsValueChanged);
        jscrollpane1 = new JScrollPane(lstConnections);

        btnNewConn = new JButton("+");
        btnNewConn.setToolTipText(I18n.t("connectionManager.btnNewConn.toolTipText"));
        btnNewConn.addActionListener(this::btnNewConnActionPerformed);

        btnCloneConn = new JButton("c");
        btnCloneConn.setToolTipText(I18n.t("connectionManager.btnCloneConn.toolTipText"));
        btnCloneConn.addActionListener(this::btnCloneConnActionPerformed);

        btnDelConn = new JButton("-");
        btnDelConn.setToolTipText(I18n.t("connectionManager.btnDelConn.toolTipText"));
        btnDelConn.addActionListener(this::btnDelConnActionPerformed);

        // the three buttons share the width of the list in equal parts
        jPanel3 = new JPanel(new MigLayout("insets 0, gap 0, fillx",
                "[sg listbtn, grow][sg listbtn, grow][sg listbtn, grow]", "[]"));
        jPanel3.add(btnNewConn, "growx");
        jPanel3.add(btnCloneConn, "growx");
        jPanel3.add(btnDelConn, "growx");

        // the list takes the height that heading and button row leave over
        jPanel1 = new JPanel(new MigLayout("insets 0, fill, wrap 1",
                "[grow]", "[][grow][]"));
        jPanel1.add(jLabel1);
        jPanel1.add(jscrollpane1, "grow, h 493");
        jPanel1.add(jPanel3, "growx");
        return jPanel1;
    }

    /**
     * build the editor of the selected connection: the fields that tell how the
     * database is reached, the JDBC connection properties and the keep alive
     * settings.
     *
     * @return the panel shown on the general tab.
     */
    private JPanel createGeneralTab() {
        jLabel2 = trailingLabel("connectionManager.jLabel2.text");
        txtName = new JTextField();

        jLabel3 = trailingLabel("connectionManager.jLabel3.text");
        cboDriver = new JComboBox<>();
        cboDriver.addItemListener(this::cboDriverItemStateChanged);

        btnManage = new JButton(I18n.t("connectionManager.btnManage.text"));
        btnManage.setToolTipText(I18n.t("connectionManager.btnManage.toolTipText"));
        btnManage.addActionListener(this::btnManageActionPerformed);

        jLabel4 = trailingLabel("connectionManager.jLabel4.text");
        txtConnUrl = new JTextField();

        jLabel5 = trailingLabel("connectionManager.jLabel5.text");
        txtUser = new JTextField();

        jLabel6 = trailingLabel("connectionManager.jLabel6.text");
        txtPassword = new JPasswordField();

        jLabel12 = trailingLabel("connectionManager.jLabel12.text");
        txtIcon = new JTextField();
        btnBrowseIcon = new JButton("...");
        btnBrowseIcon.setToolTipText(I18n.t("connectionManager.btnBrowseIcon.toolTipText"));
        btnBrowseIcon.addActionListener(this::btnBrowseIconActionPerformed);
        // what the button does is attached once the dialog is built, see the
        // constructor
        btnIconHelp = new JButton("?");

        jLabel7 = trailingLabel("connectionManager.jLabel7.text");
        // the property table is edited in place, both of its columns hold text
        tabProps = new JTable(new DefaultTableModel(
                new Object[][] {{null, null}},
                new String[] {
                    I18n.t("connectionManager.tabProps.col.key"),
                    I18n.t("connectionManager.tabProps.col.value")
                }) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        });
        tabProps.getTableHeader().setReorderingAllowed(false);
        jScrollPane1 = new JScrollPane(tabProps);
        btnDelProp = new JButton("-");
        btnDelProp.addActionListener(this::btnDelPropActionPerformed);

        chkKeepAlive = new JCheckBox(I18n.t("connectionManager.chkKeepAlive.text"));
        chkKeepAlive.addActionListener(this::chkKeepAliveActionPerformed);
        txtKeepAliveSec = new JTextField();
        txtKeepAliveSec.setHorizontalAlignment(SwingConstants.TRAILING);
        jLabel15 = new JLabel(I18n.t("connectionManager.jLabel15.text"));
        txtKeepAliveQuery = new JTextArea();
        txtKeepAliveQuery.setColumns(20);
        txtKeepAliveQuery.setRows(5);
        jScrollPane3 = new JScrollPane(txtKeepAliveQuery);

        // a right aligned label column, an editor column that takes the width
        // left over, and two columns for the trailing buttons
        JPanel panel = new JPanel(new MigLayout("fillx, insets dialog",
                "[right][grow, fill][][]", "[]"));
        panel.add(jLabel2);
        panel.add(txtName, "spanx 3, wrap");
        panel.add(jLabel3);
        panel.add(cboDriver);
        panel.add(btnManage, "spanx 2, wrap");
        panel.add(jLabel4);
        panel.add(txtConnUrl, "spanx 3, wrap");
        panel.add(jLabel5);
        panel.add(txtUser, "spanx 3, wrap");
        panel.add(jLabel6);
        panel.add(txtPassword, "spanx 3, wrap");
        // the browse and help buttons keep the height of the field they follow
        panel.add(jLabel12);
        panel.add(txtIcon, "sgy iconrow");
        panel.add(btnBrowseIcon, "sgy iconrow");
        panel.add(btnIconHelp, "sgy iconrow, wrap");
        // the property table takes the height left over, its heading and its
        // remove button stay beside it in the label column
        panel.add(jLabel7, "aligny top");
        // the table asks for a viewport of its own that would blow the dialog
        // up, it is given a share of the height and grows with the window
        panel.add(jScrollPane1, "spanx 3, spany 2, grow, h 240, wrap");
        panel.add(btnDelProp, "aligny top, sgy iconrow, pushy, wrap");
        panel.add(chkKeepAlive, "spanx 4, split 3, align left");
        panel.add(txtKeepAliveSec, "w 35!");
        panel.add(jLabel15, "wrap");
        // the keep alive query is indented below the check box that turns it on
        panel.add(jScrollPane3, "spanx 4, growx, h 64!, gapleft 21");
        return panel;
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
     * close the dialog without a selected connection.
     */
    private void btnCancelActionPerformed(ActionEvent evt) {
        selectedConnection = null;
        setVisible(false);
    }

    /**
     * show the driver manager modally on top of this dialog.
     *
     * @param driverIndex
     *            the index of the driver to preselect there, or a negative
     *            value to leave its selection alone.
     * @return <code>true</code> if the driver list was changed there, so that
     *         the caller knows it has to reload the driver combo box.
     */
    private boolean manageDrivers(int driverIndex) {
        JDBDriverManager dm = JDBDriverManager.getInstance();
        dm.setModal(true);
        dm.setLocationRelativeTo(this);
        if (driverIndex > -1)
            dm.setDriverIndex(driverIndex);
        dm.setVisible(true);
        return dm.changed;
    }

    /**
     * open the driver manager and reload the driver combo box if the driver
     * list was changed there.
     */
    private void btnManageActionPerformed(ActionEvent evt) {
        if (manageDrivers(cboDriver.getSelectedIndex()))
            refreshDrivers();
    }

    /**
     * add a copy of the selected connection and select it. The properties, the
     * variables and the templates are copied into fresh collections, so that
     * the copy can be edited independently - the latter two are not shown here,
     * but they belong to the connection and have to be carried over.
     */
    private void btnCloneConnActionPerformed(ActionEvent evt) {
        int idx = lstConnections.getSelectedIndex();
        if (idx > -1) {
            JDBConnection conn = connections.get(idx);
            JDBConnection newOne = conn.toBuilder()
                    .name(NamingUtils.nextNameOf(connections,
                            I18n.t("connectionManager.msg.copyOf", conn.getName())))
                    .build();
            if (ObjectUtils.isNotEmpty(conn.getConnectionProps()))
                newOne.setConnectionProps(new LinkedHashMap(conn.getConnectionProps()));

            if (ObjectUtils.isNotEmpty(conn.getCustomVars()))
                newOne.setCustomVars(new LinkedHashMap(conn.getCustomVars()));

            if (ObjectUtils.isNotEmpty(conn.getTemplates()))
                newOne.setTemplates(new ArrayList(conn.getTemplates()));

            connections.add(newOne);
            connMap.put(newOne.getName(), newOne);
            listModel.addElement(newOne.getName());
            lstConnections.setSelectedIndex(listModel.size() - 1);
        }
    }

    /**
     * select the given connection in the list and load it into the editor.
     * The connection is looked up by identity, an object that is not part of
     * the configured list leaves the selection unchanged.
     *
     * @param conn
     *            the connection to select, may be <code>null</code>.
     */
    public void setSelection(JDBConnection conn) {
        for (int i=0; i<connections.size(); i++) {
            if (connections.get(i) == conn) {
                lstConnections.setSelectedIndex(i);
                lstConnectionsValueChanged(null);
                break;
            }
        }
    }

    /**
     * load the selected connection into the editor fields and the property
     * table.
     */
    private void lstConnectionsValueChanged(ListSelectionEvent evt) {
        int idx = lstConnections.getSelectedIndex();
        if (idx > -1) {
            autoReset = false;
            JDBConnection conn = connections.get(idx);
            txtConnUrl.setText(conn.getConnectionUrl());
            txtIcon.setText(conn.getIcon());
            txtKeepAliveQuery.setText(conn.getKeepAliveQuery());
            txtName.setText(conn.getName());
            txtPassword.setText(conn.getUserPassword());
            txtUser.setText(conn.getUserName());
            chkKeepAlive.setSelected(conn.isUseKeepAlive());
            txtKeepAliveSec.setText(conn.getKeepAliveSec());
            chkKeepAliveActionPerformed(null);
            cboDriver.getModel().setSelectedItem(conn.getDriverType());
            removeProps();

            conn.getConnectionProps().forEach((k, v) -> {if (!"".equals(k)) propsModel.addRow(new String[]{k, v});});
            // add last empty row
            propsModel.addRow(new String[]{"", ""});
            autoReset = true;
            cboDriverItemStateChanged(null);
        }
    }

    /**
     * add an empty connection with a generated name and select it.
     */
    private void btnNewConnActionPerformed(ActionEvent evt) {
        resetControls();
        JDBConnection newConn = new JDBConnection();
        newConn.setName(NamingUtils.nextNameOf(connections,
                I18n.t("connectionManager.msg.newConnection")));
        newConn.setConnectionProps(new HashMap<>());
        applyGenerationDefaults(newConn);
        connections.add(newConn);
        connMap.put(newConn.getName(), newConn);
        listModel.addElement(newConn.getName());
        lstConnections.setSelectedIndex(connections.size() - 1);
    }

    /**
     * fill in what a connection needs to be generated from but this dialog does
     * not edit. Only a setting that is still missing is given a default, an
     * existing value is never touched: the generation options panel of the main
     * window owns them.
     *
     * @param conn
     *            the connection that was just created or is about to be stored.
     */
    private static void applyGenerationDefaults(JDBConnection conn) {
        if (StrUtils.isEmpty(conn.getOutputDir()))
            conn.setOutputDir(DEFAULT_OUTPUT_DIR);
        if (conn.getAuthor() == null)
            conn.setAuthor("");
        if (conn.getCustomVars() == null)
            conn.setCustomVars(new HashMap<>());
        if (conn.getTemplates() == null)
            conn.setTemplates(new ArrayList<>());
    }

    /**
     * convert the rows of the property table into a map, dropping the rows
     * with an empty key such as the trailing input row.
     *
     * @return the JDBC connection properties currently shown.
     */
    private Map<String, String> applyToPropsMap() {
        return UIUtils.applyTableToMap(propsModel);
    }

    /**
     * validate the editor fields, store them into the connection and save the
     * configuration. The first failing check is reported and focuses its field.
     */
    private void btnSaveActionPerformed(ActionEvent evt) {
        int idx = lstConnections.getSelectedIndex();
        boolean isNameExists;
        JDBConnection target;
        saveSuccess = false;
        if (idx == -1) {
            target = new JDBConnection();
            isNameExists = NamingUtils.nameExists(connections, txtName.getText());
        } else {
            target = connections.get(idx);
            isNameExists = !target.getName().equals(txtName.getText()) &&
                    NamingUtils.nameExists(connections, txtName.getText());
        }

        JDBDriver driver = driverMap.get((String)cboDriver.getSelectedItem());

        if (isNameExists) {
            UIUtils.error(this, I18n.t("connectionManager.msg.nameExists", txtName.getText()));
            txtName.requestFocusInWindow();
        } else if (StrUtils.isEmpty(txtName.getText())) {
            UIUtils.error(this, I18n.t("connectionManager.msg.nameRequired"));
            txtName.requestFocusInWindow();
        } else if (StrUtils.isEmpty(txtConnUrl.getText())) {
            UIUtils.error(this, I18n.t("connectionManager.msg.urlRequired"));
            txtConnUrl.requestFocusInWindow();
        } else if (driver == null) {
            // no driver selected: every branch below dereferences it
            UIUtils.error(this, I18n.t("connectionManager.msg.driverRequired"));
            cboDriver.requestFocusInWindow();
        } else if (StrUtils.isEmpty(txtUser.getText()) && !driver.isNoAuth()) {
            UIUtils.error(this, I18n.t("connectionManager.msg.userRequired"));
            txtUser.requestFocusInWindow();
        } else if (StrUtils.isEmpty(new String(txtPassword.getPassword())) && !driver.isNoAuth()) {
            UIUtils.error(this, I18n.t("connectionManager.msg.passwordRequired"));
            txtPassword.requestFocusInWindow();
        } else if (StrUtils.isEmpty(txtKeepAliveQuery.getText()) && chkKeepAlive.isSelected()) {
            UIUtils.error(this, I18n.t("connectionManager.msg.keepAliveQueryRequired"));
            txtKeepAliveQuery.requestFocusInWindow();
        } else if (StrUtils.isEmpty(txtKeepAliveSec.getText()) && chkKeepAlive.isSelected()) {
            UIUtils.error(this, I18n.t("connectionManager.msg.keepAliveSecRequired"));
            txtKeepAliveSec.requestFocusInWindow();
        } else if (!driver.validate()) {
            UIUtils.error(this, I18n.t("connectionManager.msg.driverIncomplete"));
            manageDrivers(cboDriver.getSelectedIndex());
        } else {
            connMap.remove(target.getName());

            target.setConnectionUrl(txtConnUrl.getText());
            target.setIcon(txtIcon.getText());
            target.setKeepAliveQuery(txtKeepAliveQuery.getText());
            target.setKeepAliveSec(txtKeepAliveSec.getText());
            target.setName(txtName.getText());
            target.setUserPassword(new String(txtPassword.getPassword()));
            target.setUserName(txtUser.getText());
            target.setUseKeepAlive(chkKeepAlive.isSelected());
            target.setDriverType((String)cboDriver.getSelectedItem());
            target.setConnectionProps(applyToPropsMap());
            // the templates, the output directory, the author and the custom
            // variables are edited in the main window: what the connection
            // already has is kept, a connection that has none yet - a new one -
            // is given the defaults instead of being rejected for a setting
            // this dialog does not show.
            applyGenerationDefaults(target);

            connMap.put(target.getName(), target);

            if (idx == -1) {
                connections.add(target);
                listModel.addElement(target.getName());
                lstConnections.setSelectedIndex(connections.size() - 1);
            } else {
                // name may have been changed, keep the list model in sync
                listModel.set(idx, target.getName());
            }

            JDBGenConfig.saveInstance(this);
            saveSuccess = true;
            selectedConnection = target;
            lstConnections.updateUI();
        }
    }

    /**
     * save the connection and close the dialog if it was stored.
     */
    private void btnConnectActionPerformed(ActionEvent evt) {
        btnSaveActionPerformed(evt);
        if (saveSuccess)
            setVisible(false);
    }

    /**
     * connect on a double click on a connection.
     */
    private void lstConnectionsMouseClicked(MouseEvent evt) {
        if (evt.getClickCount() == 2) {
            btnConnectActionPerformed(null);
        }
    }

    /**
     * remove the selected connection property, or clear it when it is the only
     * row left, and write the result back into the selected connection.
     */
    private void btnDelPropActionPerformed(ActionEvent evt) {
        int row = tabProps.getSelectedRow();
        if (row > -1) {
            if (tabProps.getRowCount() > 1) {
                propsModel.removeRow(row);
            } else if (row == 0) {
                for (int i=0; i<propsModel.getColumnCount(); i++)
                    propsModel.setValueAt("", row, i);
            }
            int idx = lstConnections.getSelectedIndex();
            if (idx > -1) {
                JDBConnection target = connections.get(idx);
                target.setConnectionProps(applyToPropsMap());
            }
        }
    }

    /**
     * pick the icon shown for this connection.
     */
    private void btnBrowseIconActionPerformed(ActionEvent evt) {
        String fpath = UIUtils.openIconDlg(this, "");
        if (!StrUtils.isEmpty(fpath))
            this.txtIcon.setText(fpath);
    }

    /**
     * apply the defaults of the newly selected driver: the url template and the
     * icon replace a still unedited value, the connection properties are
     * replaced, and the credential fields follow the driver's no-auth flag.
     */
    private void cboDriverItemStateChanged(ItemEvent evt) {
        if (autoReset) {
            String dname = (String)cboDriver.getSelectedItem();
            JDBDriver driver = driverMap.get(dname);
            if (driver != null) {
                if (txtConnUrl.getText().contains("<") || StrUtils.isEmpty(txtConnUrl.getText()))
                    txtConnUrl.setText(driver.getUrlTemplate());
                if (txtIcon.getText().startsWith("stock:"))
                    txtIcon.setText(driver.getIcon());
                for(int i = propsModel.getRowCount() - 1; i >= 0; --i) {
                    propsModel.removeRow(i);
                }
                if (driver.getProps() != null)
                    driver.getProps().forEach((key, value) ->
                        propsModel.addRow(new String[]{key, value}));
                txtUser.setEnabled(!driver.isNoAuth());
                txtPassword.setEnabled(!driver.isNoAuth());
            }
        }
    }

    /**
     * remove the selected connection after asking the user, then save the
     * configuration.
     */
    private void btnDelConnActionPerformed(ActionEvent evt) {
        int idx = lstConnections.getSelectedIndex();
        if (idx > -1) {
            // use the stored name: the name field may hold unsaved edits
            JDBConnection obj = connections.get(idx);
            if (UIUtils.confirm(this, I18n.t("connectionManager.msg.removeConfirm.title"),
                    I18n.t("connectionManager.msg.removeConfirm.message", obj.getName()))) {
                listModel.remove(idx);
                connections.remove(idx);
                connMap.remove(obj.getName());
                lstConnections.setSelectedIndex(-1);
                resetControls();
                JDBGenConfig.saveInstance(this);
            }
        }
    }

    /**
     * enable the keep alive query and interval fields along with the check box.
     */
    private void chkKeepAliveActionPerformed(ActionEvent evt) {
        txtKeepAliveQuery.setEnabled(chkKeepAlive.isSelected());
        txtKeepAliveSec.setEnabled(chkKeepAlive.isSelected());
    }

    // the frame of the dialog
    private JSplitPane jSplitPane1;
    private JTabbedPane jTabbedPane1;
    private JButton btnSave;
    private JButton btnConnect;
    private JButton btnCancel;

    // the connection list on the left
    private JPanel jPanel1;
    private JPanel jPanel3;
    private JLabel jLabel1;
    private JScrollPane jscrollpane1;
    private JList<String> lstConnections;
    private JButton btnNewConn;
    private JButton btnCloneConn;
    private JButton btnDelConn;

    // the editor of the selected connection
    private JLabel jLabel2;
    private JTextField txtName;
    private JLabel jLabel3;
    private JComboBox<String> cboDriver;
    private JButton btnManage;
    private JLabel jLabel4;
    private JTextField txtConnUrl;
    private JLabel jLabel5;
    private JTextField txtUser;
    private JLabel jLabel6;
    private JPasswordField txtPassword;
    private JLabel jLabel12;
    private JTextField txtIcon;
    private JButton btnBrowseIcon;
    private JButton btnIconHelp;
    private JLabel jLabel7;
    private JScrollPane jScrollPane1;
    private JTable tabProps;
    private JButton btnDelProp;
    private JCheckBox chkKeepAlive;
    private JTextField txtKeepAliveSec;
    private JLabel jLabel15;
    private JScrollPane jScrollPane3;
    private JTextArea txtKeepAliveQuery;
}

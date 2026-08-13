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
import comart.tools.jdbgen.types.JDBTemplate;
import comart.utils.AppDirs;
import comart.utils.I18n;
import comart.utils.StrUtils;
import comart.utils.UIUtils;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import jiconfont.icons.font_awesome.FontAwesome;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

/**
 * connection management dialog. It lists the configured database connections
 * and edits the selected one: driver, url, credentials, keep-alive, output
 * directory, JDBC connection properties, code templates and custom template
 * variables.
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
     * the model of the code template table.
     */
    private DefaultTableModel tplModel = null;
    /**
     * the model of the custom template variable table.
     */
    private DefaultTableModel varsModel = null;
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
     * guard against feedback while a table is being filled programmatically.
     * While <code>false</code>, the table model listeners neither write back
     * into the selected connection nor append a trailing empty row.
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
     * of the current configuration into the dialog, registers the listeners
     * that keep the property and variable tables in sync with the selected
     * connection, and selects the first connection if there is one.
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    private JDBConnectionManager() {
        initComponents();
        setModal(true);
        
        conf = JDBGenConfig.getInstance();
        applyIcons();
        applyColumnHeaders();
        eventSetup();
        propsModel = (DefaultTableModel)tabProps.getModel();
        tplModel = (DefaultTableModel)tabTemplates.getModel();
        varsModel = (DefaultTableModel)tabVars.getModel();
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
        
        tabTemplates.getSelectionModel().addListSelectionListener(this::tabTemplateSelectionChanged);
        tabTemplates.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refreshDrivers();
        resetControls();
        
        UIUtils.iconHelpAction(btnIconHelp);
        UIUtils.templateHelpAction(btnTemplateHelp);
        
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
        varsModel.addTableModelListener((evt) -> {
            if (autoReset) {
                int idx = lstConnections.getSelectedIndex();
                if (idx > -1) {
                    JDBConnection target = connections.get(idx);
                    target.setCustomVars(applyToVarsMap());
                }
                autoReset = false;
                UIUtils.tableSetLastEmpty(varsModel);
                autoReset = true;
            }
        });
        
        if (!connections.isEmpty()) {
            lstConnections.setSelectedIndex(0);
        }

        UIUtils.setCommitOnLostFocus(tabProps);
        UIUtils.setCommitOnLostFocus(tabVars);

        this.pack();
    }
    
    /**
     * show the selected template of the template table in the editor fields
     * below it. Registered as the selection listener of that table; the
     * intermediate events of an ongoing selection change are ignored.
     *
     * @param e
     *            the selection event of the template table.
     */
    public void tabTemplateSelectionChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            int idx = tabTemplates.getSelectedRow();
            if (idx > -1) {
                this.txtTemplateName.setText(tplModel.getValueAt(idx, 0).toString());
                this.txtTemplateFile.setText(tplModel.getValueAt(idx, 1).toString());
                this.txtOutTemplate.setText(tplModel.getValueAt(idx, 2).toString());
            }
        }
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
        UIUtils.applyIcon(btnDelVar, FontAwesome.MINUS);
        UIUtils.applyIcon(btnBrowseIcon, FontAwesome.FOLDER_O);
        UIUtils.applyIcon(btnIconHelp, FontAwesome.QUESTION);
        
        UIUtils.addIcon(btnManage, FontAwesome.COG);
        UIUtils.addIcon(btnSave, FontAwesome.FLOPPY_O);
        UIUtils.addIcon(btnConnect, FontAwesome.PLUG);
        UIUtils.addIcon(btnCancel, FontAwesome.TIMES);
        
        UIUtils.applyIcon(btnTemplateHelp, FontAwesome.QUESTION);
        UIUtils.addIcon(btnPresets, FontAwesome.PAW);
        UIUtils.applyIcon(btnBrowseTemplate, FontAwesome.FOLDER_O);
        UIUtils.addIcon(btnNewTemplate, FontAwesome.FILE);
        UIUtils.addIcon(btnDelTemplate, FontAwesome.MINUS);
        UIUtils.addIcon(btnSaveTemplate, FontAwesome.ARROW_UP);
        UIUtils.applyIcon(btnBrowseOutput, FontAwesome.FOLDER_O);
    }

    /**
     * The column titles come from the form designer, which cannot hold a
     * translated string: name them here instead.
     */
    private void applyColumnHeaders() {
        setColumnHeaders(tabProps,
                "connectionManager.tabProps.col.key",
                "connectionManager.tabProps.col.value");
        setColumnHeaders(tabTemplates,
                "connectionManager.tabTemplates.col.name",
                "connectionManager.tabTemplates.col.templateFile",
                "connectionManager.tabTemplates.col.outTemplate");
        setColumnHeaders(tabVars,
                "connectionManager.tabVars.col.name",
                "connectionManager.tabVars.col.value");
    }

    /**
     * name the leading columns of a table with translated texts.
     *
     * @param table
     *            the table whose column headers are replaced.
     * @param keys
     *            the resource keys of the header texts, one per column,
     *            starting at the first column.
     */
    private static void setColumnHeaders(javax.swing.JTable table, String... keys) {
        for (int i = 0; i < keys.length; i++)
            table.getColumnModel().getColumn(i).setHeaderValue(I18n.t(keys[i]));
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
     * that was selected before if it still exists.
     */
    private void refreshDrivers() {
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
    }
    
    /**
     * empty the JDBC connection property table.
     */
    private void removeProps() {
        propsModel.setRowCount(0);
    }
    
    /**
     * empty the code template table.
     */
    private void removeTemplates() {
        tplModel.setRowCount(0);
    }
    
    /**
     * empty the custom template variable table.
     */
    private void removeVars() {
        varsModel.setRowCount(0);
    }
    
    /**
     * clear the selection and every editor field, leaving the dialog in the
     * state of "no connection selected".
     */
    private void resetControls() {
        lstConnections.clearSelection();
        txtAuthor.setText("");
        txtConnUrl.setText("");
        txtIcon.setText("");
        txtKeepAliveQuery.setText("");
        txtName.setText("");
        txtOutTemplate.setText("");
        txtOutputDir.setText("output");
        txtPassword.setText("");
        txtTemplateFile.setText("");
        txtTemplateName.setText("");
        txtUser.setText("");
        txtKeepAliveSec.setText("");
        chkKeepAlive.setSelected(false);
        cboDriver.setSelectedIndex(-1);
        removeProps();
        removeTemplates();
        removeVars();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnCancel = new javax.swing.JButton();
        btnConnect = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jscrollpane1 = new javax.swing.JScrollPane();
        lstConnections = new javax.swing.JList<>();
        jPanel3 = new javax.swing.JPanel();
        btnNewConn = new javax.swing.JButton();
        btnCloneConn = new javax.swing.JButton();
        btnDelConn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tabProps = new javax.swing.JTable();
        txtName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        cboDriver = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        btnManage = new javax.swing.JButton();
        txtConnUrl = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtUser = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        btnDelProp = new javax.swing.JButton();
        chkKeepAlive = new javax.swing.JCheckBox();
        txtIcon = new javax.swing.JTextField();
        btnBrowseIcon = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtKeepAliveQuery = new javax.swing.JTextArea();
        txtKeepAliveSec = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        btnIconHelp = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tabTemplates = new javax.swing.JTable();
        btnTemplateHelp = new javax.swing.JButton();
        btnSaveTemplate = new javax.swing.JButton();
        btnDelTemplate = new javax.swing.JButton();
        btnNewTemplate = new javax.swing.JButton();
        txtTemplateName = new javax.swing.JTextField();
        btnBrowseTemplate = new javax.swing.JButton();
        txtTemplateFile = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        txtOutTemplate = new javax.swing.JTextField();
        btnPresets = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        txtOutputDir = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        btnBrowseOutput = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        txtAuthor = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tabVars = new javax.swing.JTable();
        btnDelVar = new javax.swing.JButton();

        setTitle(I18n.t("connectionManager.title"));

        btnCancel.setText(I18n.t("connectionManager.btnCancel.text"));
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        btnConnect.setText(I18n.t("connectionManager.btnConnect.text"));
        btnConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectActionPerformed(evt);
            }
        });

        btnSave.setText(I18n.t("connectionManager.btnSave.text"));
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        jSplitPane1.setDividerLocation(200);

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD, jLabel1.getFont().getSize()+4));
        jLabel1.setText(I18n.t("connectionManager.jLabel1.text"));

        lstConnections.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        lstConnections.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lstConnectionsMouseClicked(evt);
            }
        });
        lstConnections.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstConnectionsValueChanged(evt);
            }
        });
        jscrollpane1.setViewportView(lstConnections);

        jPanel3.setLayout(new java.awt.GridLayout(1, 0));

        btnNewConn.setText("+");
        btnNewConn.setToolTipText(I18n.t("connectionManager.btnNewConn.toolTipText"));
        btnNewConn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewConnActionPerformed(evt);
            }
        });
        jPanel3.add(btnNewConn);

        btnCloneConn.setText("c");
        btnCloneConn.setToolTipText(I18n.t("connectionManager.btnCloneConn.toolTipText"));
        btnCloneConn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloneConnActionPerformed(evt);
            }
        });
        jPanel3.add(btnCloneConn);

        btnDelConn.setText("-");
        btnDelConn.setToolTipText(I18n.t("connectionManager.btnDelConn.toolTipText"));
        btnDelConn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelConnActionPerformed(evt);
            }
        });
        jPanel3.add(btnDelConn);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jscrollpane1)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jscrollpane1, javax.swing.GroupLayout.DEFAULT_SIZE, 493, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jTabbedPane1.setFont(jTabbedPane1.getFont().deriveFont(jTabbedPane1.getFont().getStyle() | java.awt.Font.BOLD, jTabbedPane1.getFont().getSize()+4));

        tabProps.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null}
            },
            new String [] {
                "Key", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tabProps.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tabProps);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText(I18n.t("connectionManager.jLabel2.text"));

        cboDriver.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cboDriverItemStateChanged(evt);
            }
        });
        cboDriver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboDriverActionPerformed(evt);
            }
        });

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText(I18n.t("connectionManager.jLabel3.text"));

        btnManage.setText(I18n.t("connectionManager.btnManage.text"));
        btnManage.setToolTipText(I18n.t("connectionManager.btnManage.toolTipText"));
        btnManage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnManageActionPerformed(evt);
            }
        });

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText(I18n.t("connectionManager.jLabel4.text"));

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText(I18n.t("connectionManager.jLabel5.text"));

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText(I18n.t("connectionManager.jLabel6.text"));

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText(I18n.t("connectionManager.jLabel7.text"));

        btnDelProp.setText("-");
        btnDelProp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelPropActionPerformed(evt);
            }
        });

        chkKeepAlive.setText(I18n.t("connectionManager.chkKeepAlive.text"));
        chkKeepAlive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkKeepAliveActionPerformed(evt);
            }
        });

        btnBrowseIcon.setText("...");
        btnBrowseIcon.setToolTipText(I18n.t("connectionManager.btnBrowseIcon.toolTipText"));
        btnBrowseIcon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseIconActionPerformed(evt);
            }
        });

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel12.setText(I18n.t("connectionManager.jLabel12.text"));

        txtKeepAliveQuery.setColumns(20);
        txtKeepAliveQuery.setRows(5);
        jScrollPane3.setViewportView(txtKeepAliveQuery);

        txtKeepAliveSec.setHorizontalAlignment(javax.swing.JTextField.TRAILING);

        jLabel15.setText(I18n.t("connectionManager.jLabel15.text"));

        btnIconHelp.setText("?");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtName))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cboDriver, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnManage))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtConnUrl))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtUser))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(jLabel7)
                            .addComponent(btnDelProp, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(txtPassword)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtIcon)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowseIcon)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnIconHelp))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(jScrollPane3))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(chkKeepAlive)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtKeepAliveSec, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel15)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel12, jLabel2, jLabel3, jLabel4, jLabel5, jLabel6, jLabel7});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cboDriver, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(btnManage))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtConnUrl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtIcon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowseIcon)
                    .addComponent(jLabel12)
                    .addComponent(btnIconHelp))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDelProp)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkKeepAlive)
                    .addComponent(txtKeepAliveSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {btnBrowseIcon, btnDelProp, btnIconHelp, txtIcon});

        jTabbedPane1.addTab(I18n.t("connectionManager.tab.general"), jPanel4);

        tabTemplates.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Template FIle", "Out Template"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tabTemplates.getTableHeader().setReorderingAllowed(false);
        tabTemplates.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                tabTemplatesMouseMoved(evt);
            }
        });
        jScrollPane2.setViewportView(tabTemplates);

        btnTemplateHelp.setText("?");
        btnTemplateHelp.setToolTipText(I18n.t("connectionManager.btnTemplateHelp.toolTipText"));

        btnSaveTemplate.setText(I18n.t("connectionManager.btnSaveTemplate.text"));
        btnSaveTemplate.setToolTipText(I18n.t("connectionManager.btnSaveTemplate.toolTipText"));
        btnSaveTemplate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveTemplateActionPerformed(evt);
            }
        });

        btnDelTemplate.setText(I18n.t("connectionManager.btnDelTemplate.text"));
        btnDelTemplate.setToolTipText(I18n.t("connectionManager.btnDelTemplate.toolTipText"));
        btnDelTemplate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelTemplateActionPerformed(evt);
            }
        });

        btnNewTemplate.setText(I18n.t("connectionManager.btnNewTemplate.text"));
        btnNewTemplate.setToolTipText(I18n.t("connectionManager.btnNewTemplate.toolTipText"));
        btnNewTemplate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewTemplateActionPerformed(evt);
            }
        });

        txtTemplateName.setToolTipText(I18n.t("connectionManager.txtTemplateName.toolTipText"));

        btnBrowseTemplate.setText("...");
        btnBrowseTemplate.setToolTipText(I18n.t("connectionManager.btnBrowseTemplate.toolTipText"));
        btnBrowseTemplate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseTemplateActionPerformed(evt);
            }
        });

        txtTemplateFile.setToolTipText(I18n.t("connectionManager.txtTemplateFile.toolTipText"));

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText(I18n.t("connectionManager.jLabel8.text"));

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText(I18n.t("connectionManager.jLabel9.text"));

        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel13.setText(I18n.t("connectionManager.jLabel13.text"));

        txtOutTemplate.setToolTipText(I18n.t("connectionManager.txtOutTemplate.toolTipText"));

        btnPresets.setText(I18n.t("connectionManager.btnPresets.text"));
        btnPresets.setToolTipText(I18n.t("connectionManager.btnPresets.toolTipText"));
        btnPresets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPresetsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtTemplateName))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtTemplateFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowseTemplate))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtOutTemplate))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(btnTemplateHelp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnPresets)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnNewTemplate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDelTemplate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSaveTemplate))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 621, Short.MAX_VALUE)))
        );

        jPanel5Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel13, jLabel8, jLabel9});

        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 339, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnTemplateHelp)
                    .addComponent(btnPresets)
                    .addComponent(btnSaveTemplate)
                    .addComponent(btnDelTemplate)
                    .addComponent(btnNewTemplate))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtTemplateName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnBrowseTemplate)
                    .addComponent(txtTemplateFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(txtOutTemplate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        jTabbedPane1.addTab(I18n.t("connectionManager.tab.templates"), jPanel5);

        txtOutputDir.setText("output");

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText(I18n.t("connectionManager.jLabel11.text"));

        btnBrowseOutput.setText("...");
        btnBrowseOutput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseOutputActionPerformed(evt);
            }
        });

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel14.setText(I18n.t("connectionManager.jLabel14.text"));

        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel16.setText(I18n.t("connectionManager.jLabel16.text"));

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
        tabVars.getTableHeader().setReorderingAllowed(false);
        jScrollPane4.setViewportView(tabVars);

        btnDelVar.setText("-");
        btnDelVar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelVarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtAuthor))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtOutputDir, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowseOutput))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel16)
                            .addComponent(btnDelVar))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))))
        );

        jPanel6Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel11, jLabel14, jLabel16});

        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtOutputDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)
                    .addComponent(btnBrowseOutput))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtAuthor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 432, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDelVar)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );

        jTabbedPane1.addTab(I18n.t("connectionManager.tab.options"), jPanel6);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );

        jSplitPane1.setRightComponent(jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnConnect)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancel))
                    .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 827, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancel)
                    .addComponent(btnConnect)
                    .addComponent(btnSave))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * close the dialog without a selected connection.
     */
    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        selectedConnection = null;
        setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed

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
    private void btnManageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnManageActionPerformed
        if (manageDrivers(cboDriver.getSelectedIndex()))
            refreshDrivers();
    }//GEN-LAST:event_btnManageActionPerformed

    /**
     * nothing to do here: a driver change is handled by
     * {@link #cboDriverItemStateChanged(java.awt.event.ItemEvent)} instead.
     */
    private void cboDriverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboDriverActionPerformed

    }//GEN-LAST:event_cboDriverActionPerformed

    /**
     * add a copy of the selected connection and select it. The properties,
     * variables and templates are copied into fresh collections, so that the
     * copy can be edited independently.
     */
    private void btnCloneConnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloneConnActionPerformed
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
    }//GEN-LAST:event_btnCloneConnActionPerformed

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
     * load the selected connection into the editor fields and tables.
     */
    private void lstConnectionsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstConnectionsValueChanged
        int idx = lstConnections.getSelectedIndex();
        if (idx > -1) {
            autoReset = false;
            JDBConnection conn = connections.get(idx);
            txtAuthor.setText(conn.getAuthor());
            txtConnUrl.setText(conn.getConnectionUrl());
            txtIcon.setText(conn.getIcon());
            txtKeepAliveQuery.setText(conn.getKeepAliveQuery());
            txtName.setText(conn.getName());
            txtOutTemplate.setText("");
            txtOutputDir.setText(conn.getOutputDir());
            txtPassword.setText(conn.getUserPassword());
            txtTemplateFile.setText("");
            txtTemplateName.setText("");
            txtUser.setText(conn.getUserName());
            chkKeepAlive.setSelected(conn.isUseKeepAlive());
            txtKeepAliveSec.setText(conn.getKeepAliveSec());
            chkKeepAliveActionPerformed(null);
            cboDriver.getModel().setSelectedItem(conn.getDriverType());
            removeProps();
            removeTemplates();
            removeVars();
            
            propsModel.setRowCount(0);
            conn.getConnectionProps().forEach((k, v) -> {if (!"".equals(k)) propsModel.addRow(new String[]{k, v});});
            // add last empty row
            propsModel.addRow(new String[]{"", ""});

            varsModel.setRowCount(0);
            conn.getTemplates().forEach(t -> tplModel.addRow(
                    new String[]{
                        t.getName(),
                        t.getTemplateFile(),
                        t.getOutTemplate()}));
            if (conn.getCustomVars() != null)
                conn.getCustomVars().forEach((k, v) -> {if (!"".equals(k)) varsModel.addRow(new String[]{k, v});});
            // add last empty row
            varsModel.addRow(new String[]{"", ""});
            autoReset = true;
            cboDriverItemStateChanged(null);
        }
    }//GEN-LAST:event_lstConnectionsValueChanged

    /**
     * add an empty connection with a generated name and select it.
     */
    private void btnNewConnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewConnActionPerformed
        resetControls();
        JDBConnection newConn = new JDBConnection();
        newConn.setName(NamingUtils.nextNameOf(connections,
                I18n.t("connectionManager.msg.newConnection")));
        newConn.setConnectionProps(new HashMap<>());
        newConn.setCustomVars(new HashMap<>());
        newConn.setTemplates(new ArrayList<>());
        connections.add(newConn);
        connMap.put(newConn.getName(), newConn);
        listModel.addElement(newConn.getName());
        lstConnections.setSelectedIndex(connections.size() - 1);
    }//GEN-LAST:event_btnNewConnActionPerformed

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
     * convert the rows of the variable table into a map, dropping the rows
     * with an empty key such as the trailing input row.
     *
     * @return the custom template variables currently shown.
     */
    private Map<String, String> applyToVarsMap() {
        return UIUtils.applyTableToMap(varsModel);
    }
    
    /**
     * convert the rows of the template table into template objects.
     *
     * @return the templates currently shown, in table order.
     */
    private List<JDBTemplate> applyToTplList() {
        List<JDBTemplate> tpls = new ArrayList<>();
        for (int i=0; i<tplModel.getRowCount(); i++) {
            String name = (String)tplModel.getValueAt(i, 0);
            String tplf = (String)tplModel.getValueAt(i, 1);
            String otpl = (String)tplModel.getValueAt(i, 2);
            tpls.add(new JDBTemplate(name, tplf, otpl));
        }
        return tpls;
    }
    
    /**
     * validate the editor fields, store them into the connection and save the
     * configuration. The first failing check is reported and focuses its field.
     */
    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
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
        } else if (StrUtils.isEmpty(txtOutputDir.getText())) {
            UIUtils.error(this, I18n.t("connectionManager.msg.outputDirRequired"));
            txtOutputDir.requestFocusInWindow();
        } else if (!driver.validate()) {
            UIUtils.error(this, I18n.t("connectionManager.msg.driverIncomplete"));
            manageDrivers(cboDriver.getSelectedIndex());
        } else {
            connMap.remove(target.getName());
            
            target.setAuthor(txtAuthor.getText());
            target.setConnectionUrl(txtConnUrl.getText());
            target.setIcon(txtIcon.getText());
            target.setKeepAliveQuery(txtKeepAliveQuery.getText());
            target.setKeepAliveSec(txtKeepAliveSec.getText());
            target.setName(txtName.getText());
            target.setOutputDir(txtOutputDir.getText());
            target.setUserPassword(new String(txtPassword.getPassword()));
            target.setUserName(txtUser.getText());
            target.setUseKeepAlive(chkKeepAlive.isSelected());
            target.setDriverType((String)cboDriver.getSelectedItem());
            target.setConnectionProps(applyToPropsMap());
            target.setCustomVars(applyToVarsMap());
            target.setTemplates(applyToTplList());

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
    }//GEN-LAST:event_btnSaveActionPerformed

    /**
     * save the connection and close the dialog if it was stored.
     */
    private void btnConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConnectActionPerformed
        btnSaveActionPerformed(evt);
        if (saveSuccess)
            setVisible(false);
    }//GEN-LAST:event_btnConnectActionPerformed

    /**
     * open the preset dialog on the template table of this dialog.
     */
    private void btnPresetsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPresetsActionPerformed
        JDBPresets preset = new JDBPresets(tabTemplates);
        preset.setModal(true);
        preset.setLocationRelativeTo(this);
        preset.setVisible(true);
    }//GEN-LAST:event_btnPresetsActionPerformed

    /**
     * connect on a double click on a connection.
     */
    private void lstConnectionsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstConnectionsMouseClicked
        // TODO add your handling code here:
        if (evt.getClickCount() == 2) {
            btnConnectActionPerformed(null);
        }
    }//GEN-LAST:event_lstConnectionsMouseClicked

    /**
     * remove the selected connection property, or clear it when it is the only
     * row left, and write the result back into the selected connection.
     */
    private void btnDelPropActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelPropActionPerformed
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
    }//GEN-LAST:event_btnDelPropActionPerformed

    /**
     * pick a template file, starting in the installed template directory.
     */
    private void btnBrowseTemplateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseTemplateActionPerformed
        String path = UIUtils.openFileDlg(this,
                AppDirs.installResourceFile("templates").getAbsolutePath(), true);
        if (!StrUtils.isEmpty(path))
            this.txtTemplateFile.setText(path);
    }//GEN-LAST:event_btnBrowseTemplateActionPerformed

    /**
     * pick the icon shown for this connection.
     */
    private void btnBrowseIconActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseIconActionPerformed
        String fpath = UIUtils.openIconDlg(this, "");
        if (!StrUtils.isEmpty(fpath))
            this.txtIcon.setText(fpath);
    }//GEN-LAST:event_btnBrowseIconActionPerformed

    /**
     * pick the directory the generated files are written to.
     */
    private void btnBrowseOutputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseOutputActionPerformed
        String path = UIUtils.openDirDlg(this, "", true);
        if (!StrUtils.isEmpty(path))
            this.txtOutputDir.setText(path);
    }//GEN-LAST:event_btnBrowseOutputActionPerformed

    /**
     * clear the template selection and the editor fields, so they describe a
     * new template.
     */
    private void btnNewTemplateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewTemplateActionPerformed
        tabTemplates.clearSelection();
        this.txtTemplateName.setText("");
        this.txtTemplateFile.setText("");
        this.txtOutTemplate.setText("");
    }//GEN-LAST:event_btnNewTemplateActionPerformed

    /**
     * remove the selected template and clear the editor fields.
     */
    private void btnDelTemplateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelTemplateActionPerformed
        int idx = tabTemplates.getSelectedRow();
        if (idx > -1) {
            tplModel.removeRow(idx);
            tabTemplates.clearSelection();
            this.txtTemplateName.setText("");
            this.txtTemplateFile.setText("");
            this.txtOutTemplate.setText("");
        }
    }//GEN-LAST:event_btnDelTemplateActionPerformed

    /**
     * store the template editor fields into the template table, appending a
     * row when nothing is selected.
     */
    private void btnSaveTemplateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveTemplateActionPerformed
        int idx = tabTemplates.getSelectedRow();
        if (idx > -1) {
            tplModel.setValueAt(txtTemplateName.getText(), idx, 0);
            tplModel.setValueAt(txtTemplateFile.getText(), idx, 1);
            tplModel.setValueAt(txtOutTemplate.getText(), idx, 2);
        } else {
            tplModel.addRow(new String[]{
                        txtTemplateName.getText(),
                        txtTemplateFile.getText(),
                        txtOutTemplate.getText()
            });
            idx = tplModel.getRowCount() - 1;
            tabTemplates.setRowSelectionInterval(idx, idx);
        }
    }//GEN-LAST:event_btnSaveTemplateActionPerformed

    /**
     * remove the selected custom variable, or clear it when it is the only row
     * left, and write the result back into the selected connection.
     */
    private void btnDelVarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelVarActionPerformed
        int row = tabVars.getSelectedRow();
        if (row > -1) {
            if (tabVars.getRowCount() > 1) {
                varsModel.removeRow(row);
            } else if (row == 0) {
                for (int i=0; i<varsModel.getColumnCount(); i++)
                    varsModel.setValueAt("", row, i);
            }
            int idx = lstConnections.getSelectedIndex();
            if (idx > -1) {
                JDBConnection target = connections.get(idx);
                target.setCustomVars(applyToVarsMap());
            }
        }
    }//GEN-LAST:event_btnDelVarActionPerformed

    /**
     * apply the defaults of the newly selected driver: the url template and the
     * icon replace a still unedited value, the connection properties are
     * replaced, and the credential fields follow the driver's no-auth flag.
     */
    private void cboDriverItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cboDriverItemStateChanged
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
    }//GEN-LAST:event_cboDriverItemStateChanged

    /**
     * show the template of the hovered row as a tooltip.
     */
    private void tabTemplatesMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tabTemplatesMouseMoved
        UIUtils.templateTooltip(tabTemplates, 0, evt);
    }//GEN-LAST:event_tabTemplatesMouseMoved

    /**
     * remove the selected connection after asking the user, then save the
     * configuration.
     */
    private void btnDelConnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelConnActionPerformed
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
    }//GEN-LAST:event_btnDelConnActionPerformed

    /**
     * enable the keep alive query and interval fields along with the check box.
     */
    private void chkKeepAliveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkKeepAliveActionPerformed
        txtKeepAliveQuery.setEnabled(chkKeepAlive.isSelected());
        txtKeepAliveSec.setEnabled(chkKeepAlive.isSelected());
    }//GEN-LAST:event_chkKeepAliveActionPerformed

    /**
     * show this dialog alone for development purposes. The virtual machine is
     * terminated as soon as the dialog is closed.
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        UIUtils.setFlatDarkLaf();
        EventQueue.invokeLater(() -> {
            JDBConnectionManager cm = getInstance();
            cm.setLocationRelativeTo(null);
            cm.setVisible(true);
            System.exit(0);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBrowseIcon;
    private javax.swing.JButton btnBrowseOutput;
    private javax.swing.JButton btnBrowseTemplate;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnCloneConn;
    private javax.swing.JButton btnConnect;
    private javax.swing.JButton btnDelConn;
    private javax.swing.JButton btnDelProp;
    private javax.swing.JButton btnDelTemplate;
    private javax.swing.JButton btnDelVar;
    private javax.swing.JButton btnIconHelp;
    private javax.swing.JButton btnManage;
    private javax.swing.JButton btnNewConn;
    private javax.swing.JButton btnNewTemplate;
    private javax.swing.JButton btnPresets;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnSaveTemplate;
    private javax.swing.JButton btnTemplateHelp;
    private javax.swing.JComboBox<String> cboDriver;
    private javax.swing.JCheckBox chkKeepAlive;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JScrollPane jscrollpane1;
    private javax.swing.JList<String> lstConnections;
    private javax.swing.JTable tabProps;
    private javax.swing.JTable tabTemplates;
    private javax.swing.JTable tabVars;
    private javax.swing.JTextField txtAuthor;
    private javax.swing.JTextField txtConnUrl;
    private javax.swing.JTextField txtIcon;
    private javax.swing.JTextArea txtKeepAliveQuery;
    private javax.swing.JTextField txtKeepAliveSec;
    private javax.swing.JTextField txtName;
    private javax.swing.JTextField txtOutTemplate;
    private javax.swing.JTextField txtOutputDir;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtTemplateFile;
    private javax.swing.JTextField txtTemplateName;
    private javax.swing.JTextField txtUser;
    // End of variables declaration//GEN-END:variables
}

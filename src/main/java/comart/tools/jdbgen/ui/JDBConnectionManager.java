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
import comart.tools.jdbgen.types.TemplateType;
import comart.utils.StrUtils;
import comart.utils.UIUtils;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import jiconfont.icons.font_awesome.FontAwesome;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author comart
 */
public class JDBConnectionManager extends JDialog {
    private static final Logger logger = Logger.getLogger(JDBConnectionManager.class.getName());
    
    private static JDBConnectionManager INSTANCE = null;

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

    private List<JDBConnection> connections = null;
    private List<JDBDriver> drivers = null;
    private Map<String, JDBConnection> connMap = new HashMap<>();
    private Map<String, JDBDriver> driverMap = new HashMap<>();
    private DefaultTableModel propsModel = null;
    private DefaultTableModel tplModel = null;
    private DefaultTableModel varsModel = null;
    private DefaultListModel listModel = null;
    private JDBGenConfig conf = null;
    private boolean saveSuccess = false;
    private boolean autoReset = true;
    
    public JDBConnection selectedConnection = null;
    
    /**
     * Creates new form JDBConnectionManager
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    private JDBConnectionManager() {
        initComponents();
        setModal(true);
        
        conf = JDBGenConfig.getInstance();
        applyIcons();
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
    
    public void updateComponents() {
        SwingUtilities.updateComponentTreeUI(this);
    }
    
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
    
    private void refreshDrivers() {
        String dname = (String)cboDriver.getSelectedItem();
        int idx = -1;
        cboDriver.removeAllItems();
        for (int i=0; i<conf.getDrivers().size(); i++) {
            JDBDriver d = conf.getDrivers().get(i);
            cboDriver.addItem(d.getName());
            if (dname != null && dname.equals(d.getName()))
                idx = i;
        }
        cboDriver.setSelectedIndex(idx);
    }
    
    private void removeProps() {
        propsModel.setRowCount(0);
    }
    
    private void removeTemplates() {
        tplModel.setRowCount(0);
    }
    
    private void removeVars() {
        varsModel.setRowCount(0);
    }
    
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
    
    private void setIfEmpty(TextField field, String text) {
        if (StrUtils.isEmpty(field.getText()))
            field.setText(text);
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

        setTitle("Connection Manager");

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        btnConnect.setText("Connect");
        btnConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectActionPerformed(evt);
            }
        });

        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        jSplitPane1.setDividerLocation(200);

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD, jLabel1.getFont().getSize()+4));
        jLabel1.setText("Connections");

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
        btnNewConn.setToolTipText("Create New Connection");
        btnNewConn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewConnActionPerformed(evt);
            }
        });
        jPanel3.add(btnNewConn);

        btnCloneConn.setText("c");
        btnCloneConn.setToolTipText("Clone Current Connection");
        btnCloneConn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloneConnActionPerformed(evt);
            }
        });
        jPanel3.add(btnCloneConn);

        btnDelConn.setText("-");
        btnDelConn.setToolTipText("Remove Current Connection");
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
        jLabel2.setText("Connection Name:");

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
        jLabel3.setText("Driver:");

        btnManage.setText("Manage");
        btnManage.setToolTipText("Manage Drivers");
        btnManage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnManageActionPerformed(evt);
            }
        });

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Connection URL:");

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("User Name:");

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText("User Password:");

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Connection Props:");

        btnDelProp.setText("-");
        btnDelProp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelPropActionPerformed(evt);
            }
        });

        chkKeepAlive.setText("Keep connection alive using below statement every");
        chkKeepAlive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkKeepAliveActionPerformed(evt);
            }
        });

        btnBrowseIcon.setText("...");
        btnBrowseIcon.setToolTipText("Browse Icon File");

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel12.setText("Icon:");

        txtKeepAliveQuery.setColumns(20);
        txtKeepAliveQuery.setRows(5);
        jScrollPane3.setViewportView(txtKeepAliveQuery);

        txtKeepAliveSec.setHorizontalAlignment(javax.swing.JTextField.TRAILING);

        jLabel15.setText("seconds.");

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

        jTabbedPane1.addTab("General", jPanel4);

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
        btnTemplateHelp.setToolTipText("Template Help");

        btnSaveTemplate.setText("Apply");
        btnSaveTemplate.setToolTipText("Apply Selected Template Modification");
        btnSaveTemplate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveTemplateActionPerformed(evt);
            }
        });

        btnDelTemplate.setText("Delete");
        btnDelTemplate.setToolTipText("Remove Current Template");
        btnDelTemplate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelTemplateActionPerformed(evt);
            }
        });

        btnNewTemplate.setText("New");
        btnNewTemplate.setToolTipText("Create New Template");
        btnNewTemplate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewTemplateActionPerformed(evt);
            }
        });

        txtTemplateName.setToolTipText("Name of Template");

        btnBrowseTemplate.setText("...");
        btnBrowseTemplate.setToolTipText("Browse Template File");
        btnBrowseTemplate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseTemplateActionPerformed(evt);
            }
        });

        txtTemplateFile.setToolTipText("Template File Location");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("Template File:");

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("Template Name:");

        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel13.setText("Output Name Template:");

        txtOutTemplate.setToolTipText("Output File Name Template");

        btnPresets.setText("Presets");
        btnPresets.setToolTipText("Manage Template Presets");
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

        jTabbedPane1.addTab("Templates", jPanel5);

        txtOutputDir.setText("output");

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText("Output Directory:");

        btnBrowseOutput.setText("...");
        btnBrowseOutput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseOutputActionPerformed(evt);
            }
        });

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel14.setText("Author Name:");

        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel16.setText("Custom Variables:");

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

        jTabbedPane1.addTab("Options", jPanel6);

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

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        selectedConnection = null;
        setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed

    private boolean manageDrivers(int driverIndex) {
        JDBDriverManager dm = JDBDriverManager.getInstance();
        dm.setModal(true);
        dm.setLocationRelativeTo(this);
        if (driverIndex > -1)
            dm.setDriverIndex(driverIndex);
        dm.setVisible(true);
        return dm.changed;
    }
    
    private void btnManageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnManageActionPerformed
        if (manageDrivers(cboDriver.getSelectedIndex()))
            refreshDrivers();
    }//GEN-LAST:event_btnManageActionPerformed

    private void cboDriverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboDriverActionPerformed

    }//GEN-LAST:event_cboDriverActionPerformed

    private void btnCloneConnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloneConnActionPerformed
        int idx = lstConnections.getSelectedIndex();
        if (idx > -1) {
            JDBConnection conn = connections.get(idx);
            JDBConnection newOne = conn.toBuilder()
                    .name(NamingUtils.nextNameOf(connections, "Copy of " + conn.getName()))
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

    public void setSelection(JDBConnection conn) {
        for (int i=0; i<connections.size(); i++) {
            if (connections.get(i) == conn) {
                lstConnections.setSelectedIndex(i);
                lstConnectionsValueChanged(null);
                break;
            }
        }
    }
    
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
//            for (int i=0; i<cboDriver.getItemCount(); i++)
//                if (conn.getDriverType().equals(cboDriver.getItemAt(i))) {
//                    cboDriver.setSelectedIndex(i);
//                    break;
//                }
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

    private void btnNewConnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewConnActionPerformed
        resetControls();
        JDBConnection newConn = new JDBConnection();
        newConn.setName(NamingUtils.nextNameOf(connections, "New Connection"));
        newConn.setConnectionProps(new HashMap<>());
        newConn.setCustomVars(new HashMap<>());
        newConn.setTemplates(new ArrayList<>());
        connections.add(newConn);
        connMap.put(newConn.getName(), newConn);
        listModel.addElement(newConn.getName());
        lstConnections.setSelectedIndex(connections.size() - 1);
    }//GEN-LAST:event_btnNewConnActionPerformed

    private Map<String, String> applyToPropsMap() {
        return UIUtils.applyTableToMap(propsModel);
    }
    
    private Map<String, String> applyToVarsMap() {
        return UIUtils.applyTableToMap(varsModel);
    }
    
    private List<JDBTemplate> applyToTplList(List<JDBTemplate> tpls) {
        tpls = new ArrayList<>();
        for (int i=0; i<tplModel.getRowCount(); i++) {
            String name = (String)tplModel.getValueAt(i, 0);
            String tplf = (String)tplModel.getValueAt(i, 1);
            String otpl = (String)tplModel.getValueAt(i, 2);
            tpls.add(new JDBTemplate(name, tplf, otpl));
        }
        return tpls;
    }
    
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
            UIUtils.error(this, "Name " + txtName.getText() + " exists already.");
            txtName.requestFocusInWindow();
        } else if (StrUtils.isEmpty(txtName.getText())) {
            UIUtils.error(this, "Connection name required.");
            txtName.requestFocusInWindow();
        } else if (StrUtils.isEmpty(txtConnUrl.getText())) {
            UIUtils.error(this, "Connection url required.");
            txtConnUrl.requestFocusInWindow();
        } else if (StrUtils.isEmpty(txtUser.getText()) && !driver.isNoAuth()) {
            UIUtils.error(this, "Database user name required.");
            txtUser.requestFocusInWindow();
        } else if (StrUtils.isEmpty(new String(txtPassword.getPassword())) && !driver.isNoAuth()) {
            UIUtils.error(this, "Database user password required.");
            txtPassword.requestFocusInWindow();
        } else if (StrUtils.isEmpty(txtKeepAliveQuery.getText()) && chkKeepAlive.isSelected()) {
            UIUtils.error(this, "Keep alive SQL statement required.");
            txtKeepAliveQuery.requestFocusInWindow();
        } else if (StrUtils.isEmpty(txtKeepAliveSec.getText()) && chkKeepAlive.isSelected()) {
            UIUtils.error(this, "Keep alive statement execution interval required.");
            txtKeepAliveSec.requestFocusInWindow();
        } else if (StrUtils.isEmpty(txtOutputDir.getText())) {
            UIUtils.error(this, "Output directory required.");
            txtOutputDir.requestFocusInWindow();
        } else if (!driver.validate()) {
            UIUtils.error(this, "Driver information is not fulfilled.");
            manageDrivers(cboDriver.getSelectedIndex());
        } else {
            connMap.remove(target.getName());
            
            target.setAuthor(txtAuthor.getText());
            target.setConnectionUrl(txtConnUrl.getText());
            target.setIcon(txtIcon.getText());
            target.setKeepAliveQuery(txtKeepAliveQuery.getText());
            target.setName(txtName.getText());
            target.setOutputDir(txtOutputDir.getText());
            target.setUserPassword(new String(txtPassword.getPassword()));
            target.setUserName(txtUser.getText());
            target.setUseKeepAlive(chkKeepAlive.isSelected());
            target.setDriverType((String)cboDriver.getSelectedItem());
            target.setConnectionProps(applyToPropsMap());
            target.setCustomVars(applyToVarsMap());
            target.setTemplates(applyToTplList(target.getTemplates()));

            connMap.put(target.getName(), target);
            
            if (idx == -1) {
                connections.add(target);
                listModel.addElement(target.getName());
                lstConnections.setSelectedIndex(connections.size() - 1);
            }
            
            JDBGenConfig.saveInstance(this);
            saveSuccess = true;
            selectedConnection = target;
            lstConnections.updateUI();
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConnectActionPerformed
        btnSaveActionPerformed(evt);
        if (saveSuccess)
            setVisible(false);
    }//GEN-LAST:event_btnConnectActionPerformed

    private void btnPresetsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPresetsActionPerformed
        JDBPresets preset = new JDBPresets(tabTemplates);
        preset.setModal(true);
        preset.setLocationRelativeTo(this);
        preset.setVisible(true);
    }//GEN-LAST:event_btnPresetsActionPerformed

    private void lstConnectionsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstConnectionsMouseClicked
        // TODO add your handling code here:
        if (evt.getClickCount() == 2) {
            btnConnectActionPerformed(null);
        }
    }//GEN-LAST:event_lstConnectionsMouseClicked

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

    private void btnBrowseTemplateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseTemplateActionPerformed
        String path = UIUtils.openFileDlg(this, "templates", true);
        if (!StrUtils.isEmpty(path))
            this.txtTemplateFile.setText(path);
    }//GEN-LAST:event_btnBrowseTemplateActionPerformed

    private void btnBrowseOutputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseOutputActionPerformed
        String path = UIUtils.openDirDlg(this, "", true);
        if (!StrUtils.isEmpty(path))
            this.txtOutputDir.setText(path);
    }//GEN-LAST:event_btnBrowseOutputActionPerformed

    private void btnNewTemplateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewTemplateActionPerformed
        tabTemplates.clearSelection();
        this.txtTemplateName.setText("");
        this.txtTemplateFile.setText("");
        this.txtOutTemplate.setText("");
    }//GEN-LAST:event_btnNewTemplateActionPerformed

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

    private void btnDelVarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelVarActionPerformed
        int row = tabVars.getSelectedRow();
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
    }//GEN-LAST:event_btnDelVarActionPerformed

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
                driver.getProps().forEach((key, value) -> 
                    propsModel.addRow(new String[]{key, value}));
                txtUser.setEnabled(!driver.isNoAuth());
                txtPassword.setEnabled(!driver.isNoAuth());
            }
        }
    }//GEN-LAST:event_cboDriverItemStateChanged

    private void tabTemplatesMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tabTemplatesMouseMoved
        UIUtils.templateTooltip(tabTemplates, 0, evt);
    }//GEN-LAST:event_tabTemplatesMouseMoved

    private void btnDelConnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelConnActionPerformed
        // TODO add your handling code here:
        int idx = lstConnections.getSelectedIndex();
        if (idx > -1) {
            if (UIUtils.confirm(this, "Remove Connection Confirm",
                    "You realy want to delete '" + txtName.getText() + "' connection?")) {
                listModel.remove(idx);
                connections.remove(idx);
                connMap.remove(txtName.getText());
                lstConnections.setSelectedIndex(-1);
                resetControls();
                JDBGenConfig.saveInstance(this);
            }
        }
    }//GEN-LAST:event_btnDelConnActionPerformed

    private void chkKeepAliveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkKeepAliveActionPerformed
        txtKeepAliveQuery.setEnabled(chkKeepAlive.isSelected());
        txtKeepAliveSec.setEnabled(chkKeepAlive.isSelected());
    }//GEN-LAST:event_chkKeepAliveActionPerformed

    /**
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

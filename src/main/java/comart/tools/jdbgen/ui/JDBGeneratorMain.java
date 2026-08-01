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
import comart.utils.ObjUtils;
import comart.utils.PlatformUtils;
import comart.utils.StrUtils;
import comart.utils.UIUtils;

import java.awt.*;
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
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import jiconfont.icons.font_awesome.FontAwesome;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author comart
 */
@Slf4j
public class JDBGeneratorMain extends javax.swing.JFrame {

    private static class MyLabelUI extends BasicLabelUI {
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

    private final JDBGenConfig conf;
    private final Map<String, JDBConnection> connMap = new HashMap<>();
    private JDBConnection currConn = null;
    private DBMeta dbmeta = null;
    private List<DBTable> tables;
    private boolean autoReset = true;
    /** true while a connection is being opened on a background thread */
    private boolean connecting = false;
    
    public static JDBGeneratorMain INSTANCE = null;
    
    public List<DBTable> getTables() {
        return tables;
    }
    
    /**
     * Creates new form JDBGeneratorMain
     */
    public JDBGeneratorMain() {
        initComponents();
        // freeze the schema pane's preferred width: the enclosing layout pins
        // this panel at preferred size, so without this a long tree item would
        // widen the panel and push the right-hand panels off screen.
        jScrollPane1.setPreferredSize(jScrollPane1.getPreferredSize());
        conf = JDBGenConfig.getInstance();
        chkDarkUI.setSelected(conf.isDarkUI());
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
        clearContents();
        
        tabVars.getModel().addTableModelListener((evt) -> {
            if (autoReset) {
                UIUtils.tableSetLastEmpty(tabVars.getModel());
            }
        });
        UIUtils.setCommitOnLostFocus(tabVars);
        
        PlatformUtils.registerHandlers(e -> showAbout(), null, null, null);
        log.info("before pack");
        this.pack();
        log.info("after pack");
        INSTANCE = this;
    }
    
    private void showAbout() {
        JDBAbout.getInstance(this).setVisible(true);
    }
    
    private void clearContents() {
        ((DefaultTableModel)this.tabTemplates.getModel()).setRowCount(0);
        ((DefaultTableModel)this.tabVars.getModel()).setRowCount(0);
        this.lstTables.removeAll();
        this.txtAuthor.setText("");
        this.txtOutputDir.setText("");
    }

    private void initTemplates() {
        tabTemplates.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColumnModel colModel = tabTemplates.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(50);
        colModel.getColumn(1).setPreferredWidth(130);
        colModel.getColumn(2).setPreferredWidth(130);
        colModel.getColumn(3).setPreferredWidth(130);

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
    
    private void applyIcons() {
        UIUtils.addIcon(btnManageConn, FontAwesome.COG);
        UIUtils.applyIcon(btnDelVar, FontAwesome.MINUS);
        UIUtils.addIcon(btnGenerate, FontAwesome.COGS);
        UIUtils.addIcon(btnClose, FontAwesome.TIMES);
        UIUtils.addIcon(btnMapper, FontAwesome.BOOK);
        UIUtils.applyIcon(btnBrowseOutput, FontAwesome.FOLDER_O);
    }    
    
    private boolean suppressCboConnEvent = false;
    private void applyConnection(JDBConnection conn) {
        suppressCboConnEvent = conn == null;
        currConn = conn;
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
        DefaultTableModel tplModel = (DefaultTableModel)tabTemplates.getModel();
        tplModel.setRowCount(0);
        DefaultTableModel cstModel = (DefaultTableModel)tabVars.getModel();
        cstModel.setRowCount(0);
        if (conn != null) {
            List<JDBTemplate> tpls = conn.getTemplates();
            tplModel.setRowCount(tpls.size());
            for (int row=0; row<tpls.size(); row++) {
                JDBTemplate t = tpls.get(row);
                tplModel.setValueAt(t.getName(), row, 1);
                tplModel.setValueAt(t.getTemplateFile(), row, 2);
                tplModel.setValueAt(t.getOutTemplate(), row, 3);
            }
            txtAuthor.setText(conn.getAuthor());
            txtOutputDir.setText(conn.getOutputDir());
            autoReset = false;
            conn.getCustomVars().forEach((k, v) -> {if (!StrUtils.isEmpty(k)) cstModel.addRow(new String[]{k, v});});
            cstModel.addRow(new String[]{"", ""});
            autoReset = true;
        }
    }
    
    /**
     * Build the catalog/schema node hierarchy. This queries the database, so it
     * must NOT be called on the EDT. The returned nodes are not attached to any
     * component yet, so building them off the EDT is safe.
     */
    private static DefaultMutableTreeNode buildSchemaRoot(DBMeta meta) throws SQLException {
        Map<String, List<DBSchema>> tree = meta.getSchemaTree();
        DefaultMutableTreeNode root = null;
        if (tree.size() > 1) {
            root = new DefaultMutableTreeNode("Database");
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
        UIUtils.error(this, "Cannot connect to '" + jcc.getName()
                + "': " + cause.getLocalizedMessage());
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
        btnGenerate = new javax.swing.JButton();
        cboConnection = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        btnManageConn = new javax.swing.JButton();
        lblConnectionInfo = new javax.swing.JLabel();
        btnAck = new javax.swing.JButton();
        btnMapper = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("JDBGen Main");

        btnClose.setText("Close");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD, jLabel1.getFont().getSize()+4));
        jLabel1.setText("Catalogs/Schemas");

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

        chkShowView.setText("Show Views");
        chkShowView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkShowViewActionPerformed(evt);
            }
        });

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getStyle() | java.awt.Font.BOLD, jLabel5.getFont().getSize()+4));
        jLabel5.setText("Tables");

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
        jLabel4.setText("Generation Options");

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
        jLabel11.setText("Output Directory:");

        btnBrowseOutput.setText("...");

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
        jScrollPane4.setViewportView(tabVars);

        jLabel6.setText("Templates");

        btnDelVar.setText("-");

        chkApplyAbbr.setText("Apply abbreviation rule to all name fields.");
        chkApplyAbbr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkApplyAbbrActionPerformed(evt);
            }
        });

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel15.setText("Abbreviation:");

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

        chkDarkUI.setText("Dark UI");
        chkDarkUI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkDarkUIActionPerformed(evt);
            }
        });

        btnGenerate.setText("Generate");
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

        jLabel2.setText("Connection");

        btnManageConn.setText("Manage");
        btnManageConn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnManageConnActionPerformed(evt);
            }
        });

        lblConnectionInfo.setLabelFor(cboConnection);
        lblConnectionInfo.setText("Connection Information Placeholder");

        btnAck.setText("A");
        btnAck.setToolTipText("About of this program");
        btnAck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAckActionPerformed(evt);
            }
        });

        btnMapper.setText("Abbreviation Mapper");
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
                    .addComponent(btnGenerate))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

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

    private void cboConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboConnectionActionPerformed
        // ignore while a connection is already being opened - the combo is
        // disabled meanwhile, this is just a second line of defense.
        if (suppressCboConnEvent || connecting)
            return;
        int idx = cboConnection.getSelectedIndex();
        if (idx < 0 || idx >= conf.getConnections().size())
            return;
        JDBConnection jcc = conf.getConnections().get(idx);
        JDBDriver jdr = null;
        for (JDBDriver drv:conf.getDrivers()) {
            if (drv.getName().equals(jcc.getDriverType())) {
                jdr = drv;
                break;
            }
        }
        if (jdr == null) {
            UIUtils.error(this, "Driver '" + jcc.getDriverType()
                    + "' of connection '" + jcc.getName() + "' not found.");
            return;
        }
        connectAsync(jdr, jcc);
    }//GEN-LAST:event_cboConnectionActionPerformed

    private void btnManageConnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnManageConnActionPerformed
        JDBConnectionManager cm = JDBConnectionManager.getInstance();
        cm.setModal(true);
        cm.setLocationRelativeTo(this);
        cm.setSelection(currConn);
        cm.setVisible(true);
        if (cm.selectedConnection != null)
            applyConnection(cm.selectedConnection);
    }//GEN-LAST:event_btnManageConnActionPerformed

    private void treSchemasValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_treSchemasValueChanged
        chkShowViewActionPerformed(null);
    }//GEN-LAST:event_treSchemasValueChanged

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
                    DefaultListModel listModel = new DefaultListModel();
                    tables.forEach(t -> listModel.addElement(t.getTable()));
                    this.lstTables.setModel(listModel);
                    this.lstTables.setCellRenderer(UIUtils.getListCellRenderer(
                            s -> tables.stream()
                                .filter(t -> s.equals(t.getName()))
                                .findFirst().orElse(null)));
                } catch (Exception ex) {
                    log.error("cannot get tables", ex);
                    UIUtils.error(this, "Cannot get tables: "+ ex.getLocalizedMessage());
                }
            }
        }
    }//GEN-LAST:event_chkShowViewActionPerformed

    @SuppressWarnings("UseSpecificCatch")
    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        if (dbmeta != null)
            try { dbmeta.close(); } catch(Exception ignored) {}
        System.exit(0);
    }//GEN-LAST:event_btnCloseActionPerformed

    /**
     * Immutable snapshot of everything the generation worker needs. Taken on the
     * EDT so that the worker never has to read a Swing component.
     */
    private static final class GenerateRequest {
        final DBMeta meta;
        final Map<String, String> customVars;
        final List<DBTable> tables;
        final List<JDBTemplate> templates;
        final String outputDir;

        GenerateRequest(DBMeta meta, Map<String, String> customVars,
                List<DBTable> tables, List<JDBTemplate> templates, String outputDir) {
            this.meta = meta;
            this.customVars = customVars;
            this.tables = tables;
            this.templates = templates;
            this.outputDir = outputDir;
        }
    }

    private ProcessProgress.Worker getProgressWorker(final GenerateRequest req) {
        return new ProcessProgress.Worker() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // NOTE: this method must not touch any Swing component - every
                // value it needs was snapshotted into 'req' on the EDT.
                try {
                    JDBAbbr.buildMap();
                    publish("reading table columns...");
                    for (DBTable t: req.tables)
                        req.meta.getTableColumns(t);
                    int totalProcs = req.tables.size() * req.templates.size();
                    int progress = 0;
                    for (JDBTemplate tpl:req.templates) {
                        publish(tpl.getName() + " template processing...");
                        String tplStr = ObjUtils.getFileContents(tpl.getTemplateFile());
                        TemplateManager tplCont = new TemplateManager(tplStr, req.customVars);
                        TemplateManager tplOut = new TemplateManager(tpl.getOutTemplate(), req.customVars);
                        for (DBTable t:req.tables) {
                            progress++;
                            if (totalProcs > 0)
                                setProgress(Math.min(100, progress * 100 / totalProcs));
                            publish(tpl.getName() + " applyng to " + t.getTable() + "...");
                            String result = tplCont.applyMapper(t);
                            String outFname = tplOut.applyMapper(t);
                            if (!StrUtils.isEmpty(req.outputDir))
                                outFname = req.outputDir + "/" + outFname;
                            ObjUtils.writeFile(outFname, result);
                        }
                    }
                    setProgress(100);
                    publish("all process complete!");
                    return true;
                } catch(Exception e) {
                    log.error(e.getLocalizedMessage(), e);
                    publish("process failed! : " + e.getLocalizedMessage());
                    return false;
                }
            }
        };
    }

    private void btnGenerateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGenerateActionPerformed
        if (dbmeta == null) {
            UIUtils.error(this, "Please connect to a database first.");
            return;
        }
        // snapshot every UI value here, on the EDT - the worker below runs on a
        // background thread and must not read Swing components.
        int tidxs[] = lstTables.getSelectedIndices();
        List<DBTable> selTables = new ArrayList<>();
        if (tables != null) {
            for (int idx: tidxs) {
                if (idx > -1 && idx < tables.size())
                    selTables.add(tables.get(idx));
            }
        }
        if (selTables.isEmpty()) {
            UIUtils.error(this, "Please select at least one table to generate.");
            return;
        }
        DefaultTableModel tplModel = (DefaultTableModel)tabTemplates.getModel();
        List<JDBTemplate> tpls = new ArrayList<>();
        for (int i=0; i<tplModel.getRowCount(); i++) {
            Object val = tplModel.getValueAt(i, 0);
            if (val != null && (boolean)val) {
                tpls.add(new JDBTemplate(
                        String.valueOf(tplModel.getValueAt(i, 1)),
                        String.valueOf(tplModel.getValueAt(i, 2)),
                        String.valueOf(tplModel.getValueAt(i, 3))
                ));
            }
        }
        if (tpls.isEmpty()) {
            UIUtils.error(this, "Please select at least one template to generate.");
            return;
        }
        Map<String, String> custVars = UIUtils.applyTableToMap(tabVars.getModel());
        custVars.put("author", txtAuthor.getText());
        String outputDir = txtOutputDir.getText();

        GenerateRequest req = new GenerateRequest(
                dbmeta, custVars, selTables, tpls, outputDir);
        ProcessProgress pp = new ProcessProgress(this, true, getProgressWorker(req));
        pp.start();
        // modal - returns once the worker's done() hides the dialog
        pp.setVisible(true);
        if (pp.result) {
            if (UIUtils.confirm(this, "Process Complete",
                    "Process complete successfully!\nDo you want open output directory?")) {
                // must match the directory actually written to above
                PlatformUtils.openFile(StrUtils.isEmpty(outputDir) ? "." : outputDir);
            }
        } else {
            UIUtils.info(this, "Process failed!");
        }
    }//GEN-LAST:event_btnGenerateActionPerformed

    private void lstTablesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstTablesMouseClicked
        if (evt.getClickCount() == 2) {
            if (dbmeta == null || tables == null)
                return;
            int idx = lstTables.getSelectedIndex();
            if (idx > -1 && idx < tables.size()) {
                DBTable table = tables.get(idx);
                try {
                    dbmeta.getTableColumns(table);
                    JDBTableView tview = new JDBTableView(this, table);
                    tview.setLocationRelativeTo(this);
                    tview.setVisible(true);
                } catch(Throwable t) {
                    log.error("cannot get columns", t);
                    UIUtils.error(this, "Cannot get columns: "+ t.getLocalizedMessage());
                }
            }
        }
    }//GEN-LAST:event_lstTablesMouseClicked

    private void btnAckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAckActionPerformed
        showAbout();
    }//GEN-LAST:event_btnAckActionPerformed

    private void tabTemplatesMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tabTemplatesMouseMoved
        UIUtils.templateTooltip(tabTemplates, 1, evt);
    }//GEN-LAST:event_tabTemplatesMouseMoved

    private void lstTablesMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstTablesMouseMoved
        int idx = tables == null ? -1 : lstTables.locationToIndex(evt.getPoint());
        if (idx > -1 && idx < tables.size()) {
            DBTable table = tables.get(idx);
            lstTables.setToolTipText(table.getName());
        } else {
            lstTables.setToolTipText(null);
        }
    }//GEN-LAST:event_lstTablesMouseMoved

    private void btnMapperActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMapperActionPerformed
        JDBAbbreviationMapper.getInstance(this).setVisible(true);
    }//GEN-LAST:event_btnMapperActionPerformed

    private void chkApplyAbbrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkApplyAbbrActionPerformed
        conf.setApplyAbbr(chkApplyAbbr.isSelected());
        JDBGenConfig.saveInstance(this);
    }//GEN-LAST:event_chkApplyAbbrActionPerformed

    /**
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

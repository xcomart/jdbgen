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
import comart.utils.ClassUtils;
import comart.utils.PlatformUtils;
import comart.utils.StrUtils;
import comart.utils.UIUtils;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import jiconfont.icons.font_awesome.FontAwesome;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author comart
 */
public class JDBDriverManager extends JDialog {

    private static final Logger logger = Logger.getLogger(JDBDriverManager.class.getName());
    private final List<JDBDriver> drivers;
    private final DefaultListModel<String> listModel;
    private final DefaultTableModel tableModel;
    public boolean changed = false;

    private static JDBDriverManager INSTANCE = null;
    public static synchronized JDBDriverManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JDBDriverManager();
            UIUtils.registerFrame(INSTANCE);
        }

        INSTANCE.updateComponents();
        INSTANCE.changed = false;
        return INSTANCE;
    }

    /**
     * Creates new form JDBDriverManager
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public JDBDriverManager() {
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
        this.pack();
    }
    
    public void setDriverIndex(int index) {
        if (index > -1) {
            lstDrivers.setSelectedIndex(index);
        }
    }

    public void updateComponents() {
        SwingUtilities.updateComponentTreeUI(this);
        btnDownJdbc.setBorder((Border)null);
        btnDownJdbc.setForeground(UIManager.getDefaults().getColor("Actions.Blue"));
        lstDrivers.setCellRenderer(UIUtils.getListCellRenderer(
                s -> drivers.stream()
                        .filter(d -> s.equals(d.getName()))
                        .findFirst().orElse(null)));
    }

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
        DefaultTableModel tmodel = (DefaultTableModel)tabProps.getModel();
        tmodel.addTableModelListener((e) -> {
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
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnCancel = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        btnNewDriver = new javax.swing.JButton();
        btnCloneDriver = new javax.swing.JButton();
        btnDelDriver = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstDrivers = new javax.swing.JList<>();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        txtDriverName = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        btnBrowseJar = new javax.swing.JButton();
        txtJarFile = new javax.swing.JTextField();
        btnDownJdbc = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        txtUrlTemplate = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        txtDriverClass = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        btnBrowseIcon = new javax.swing.JButton();
        txtIcon = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tabProps = new javax.swing.JTable();
        btnDelProp = new javax.swing.JButton();
        chkNoAuth = new javax.swing.JCheckBox();
        btnIconHelp = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jPanel5 = new javax.swing.JPanel();
        btnTableComments = new javax.swing.JButton();
        chkTableComments = new javax.swing.JCheckBox();
        jScrollPane4 = new javax.swing.JScrollPane();
        txtColumnComments = new javax.swing.JTextArea();
        btnColumnComments = new javax.swing.JButton();
        chkColumnComments = new javax.swing.JCheckBox();
        jScrollPane5 = new javax.swing.JScrollPane();
        txtTableComments = new javax.swing.JTextArea();
        chkTables = new javax.swing.JCheckBox();
        jScrollPane6 = new javax.swing.JScrollPane();
        txtTables = new javax.swing.JTextArea();
        btnTables = new javax.swing.JButton();
        jScrollPane7 = new javax.swing.JScrollPane();
        txtColumns = new javax.swing.JTextArea();
        chkColumns = new javax.swing.JCheckBox();
        btnColumns = new javax.swing.JButton();

        setTitle("Driver Manager");

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        btnSave.setText("Ok");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        jPanel3.setLayout(new java.awt.GridLayout(1, 0));

        btnNewDriver.setText("+");
        btnNewDriver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewDriverActionPerformed(evt);
            }
        });
        jPanel3.add(btnNewDriver);

        btnCloneDriver.setText("c");
        btnCloneDriver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloneDriverActionPerformed(evt);
            }
        });
        jPanel3.add(btnCloneDriver);

        btnDelDriver.setText("-");
        btnDelDriver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelDriverActionPerformed(evt);
            }
        });
        jPanel3.add(btnDelDriver);

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD, jLabel1.getFont().getSize()+4));
        jLabel1.setText("Drivers");

        lstDrivers.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        lstDrivers.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstDriversValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(lstDrivers);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jTabbedPane1.setFont(jTabbedPane1.getFont().deriveFont(jTabbedPane1.getFont().getStyle() | java.awt.Font.BOLD, jTabbedPane1.getFont().getSize()+4));

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Driver Name:");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("JDBC Jar:");

        btnBrowseJar.setText("...");
        btnBrowseJar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseJarActionPerformed(evt);
            }
        });

        txtJarFile.setEditable(false);

        btnDownJdbc.setFont(btnDownJdbc.getFont().deriveFont(btnDownJdbc.getFont().getSize()-1f));
        btnDownJdbc.setForeground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
        btnDownJdbc.setText("Download jdbc driver from Maven Repository");
        btnDownJdbc.setBorder(null);
        btnDownJdbc.setBorderPainted(false);
        btnDownJdbc.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        btnDownJdbc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDownJdbcActionPerformed(evt);
            }
        });

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("URL Template:");

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("Driver Class:");

        txtDriverClass.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                txtDriverClassMouseClicked(evt);
            }
        });

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel10.setText("Icon:");

        btnBrowseIcon.setText("...");
        btnBrowseIcon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseIconActionPerformed(evt);
            }
        });

        txtIcon.setEditable(false);

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText("Connection Props:");

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
        jScrollPane2.setViewportView(tabProps);

        btnDelProp.setText("-");
        btnDelProp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelPropActionPerformed(evt);
            }
        });

        chkNoAuth.setText("Authentication is not required for this driver.");

        btnIconHelp.setText("?");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnDelProp, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtDriverName)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(txtJarFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowseJar))
                    .addComponent(btnDownJdbc, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtUrlTemplate)
                    .addComponent(txtDriverClass)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 487, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(chkNoAuth)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(txtIcon)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowseIcon)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnIconHelp)))
                .addContainerGap())
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel10, jLabel11, jLabel3, jLabel4, jLabel8, jLabel9});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtDriverName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(btnBrowseJar)
                    .addComponent(txtJarFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnDownJdbc)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(txtUrlTemplate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(txtDriverClass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(txtIcon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowseIcon)
                    .addComponent(btnIconHelp))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkNoAuth)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDelProp)
                        .addGap(172, 216, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addContainerGap())))
        );

        jTabbedPane1.addTab("General", jPanel2);

        btnTableComments.setText("?");
        btnTableComments.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTableCommentsActionPerformed(evt);
            }
        });

        chkTableComments.setText("Get table comments");
        chkTableComments.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkTableCommentsActionPerformed(evt);
            }
        });

        txtColumnComments.setColumns(20);
        txtColumnComments.setRows(5);
        jScrollPane4.setViewportView(txtColumnComments);

        btnColumnComments.setText("?");
        btnColumnComments.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnColumnCommentsActionPerformed(evt);
            }
        });

        chkColumnComments.setText("Get table column comments");
        chkColumnComments.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkColumnCommentsActionPerformed(evt);
            }
        });

        txtTableComments.setColumns(20);
        txtTableComments.setRows(5);
        jScrollPane5.setViewportView(txtTableComments);

        chkTables.setText("Get table list");
        chkTables.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkTablesActionPerformed(evt);
            }
        });

        txtTables.setColumns(20);
        txtTables.setRows(5);
        jScrollPane6.setViewportView(txtTables);

        btnTables.setText("?");
        btnTables.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTablesActionPerformed(evt);
            }
        });

        txtColumns.setColumns(20);
        txtColumns.setRows(5);
        jScrollPane7.setViewportView(txtColumns);

        chkColumns.setText("Get table column list");
        chkColumns.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkColumnsActionPerformed(evt);
            }
        });

        btnColumns.setText("?");
        btnColumns.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnColumnsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(chkColumnComments)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnColumnComments)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(chkTableComments)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnTableComments))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(chkTables)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnTables))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(chkColumns)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnColumns)))
                        .addContainerGap(391, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane6)
                            .addComponent(jScrollPane5)
                            .addComponent(jScrollPane4)
                            .addComponent(jScrollPane7)))))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkTableComments)
                    .addComponent(btnTableComments))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkColumnComments)
                    .addComponent(btnColumnComments))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkTables)
                    .addComponent(btnTables))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkColumns)
                    .addComponent(btnColumns))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(93, Short.MAX_VALUE))
        );

        jScrollPane3.setViewportView(jPanel5);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jScrollPane3)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3)
        );

        jTabbedPane1.addTab("Custom Queries", jPanel4);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnSave)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCancel)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jTabbedPane1)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancel)
                    .addComponent(btnSave))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        int idx = lstDrivers.getSelectedIndex();
        boolean isNameExists;
        JDBDriver target;
        if (idx == -1) {
            isNameExists = NamingUtils.nameExists(drivers, txtDriverName.getText());
        } else {
            target = (JDBDriver)drivers.get(idx);
            isNameExists = !target.getName().equals(txtDriverName.getText()) &&
                    NamingUtils.nameExists(drivers, txtDriverName.getText());
        }

        if (isNameExists) {
            UIUtils.error(this, "Name " + txtDriverName.getText() + " exists already.");
            txtDriverName.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtDriverName.getText())) {
            UIUtils.error(this, "Driver name required.");
            txtDriverName.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtJarFile.getText())) {
            UIUtils.error(this, "JDBC jar file required.");
            txtJarFile.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtDriverClass.getText())) {
            UIUtils.error(this, "Driver class required.");
            txtDriverClass.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtTableComments.getText()) && chkTableComments.isSelected()) {
            UIUtils.error(this, "Table comments query required.");
            txtTableComments.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtColumnComments.getText()) && chkColumnComments.isSelected()) {
            UIUtils.error(this, "Column comments query required.");
            txtColumnComments.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtTables.getText()) && chkTables.isSelected()) {
            UIUtils.error(this, "Table list query required.");
            txtTables.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtColumns.getText()) && chkColumns.isSelected()) {
            UIUtils.error(this, "Column list query required.");
            txtColumns.requestFocusInWindow();
        } else {
            if (idx == -1) {
                target = new JDBDriver();
                drivers.add(target);
                listModel.addElement(target.getName());
            } else {
                target = (JDBDriver)drivers.get(idx);
            }

            target.setDriverClass(txtDriverClass.getText());
            target.setIcon(txtIcon.getText());
            target.setJdbcJar(txtJarFile.getText());
            target.setName(txtDriverName.getText());
            target.setUrlTemplate(txtUrlTemplate.getText());
            target.setNoAuth(chkNoAuth.isSelected());

            if (tableModel.getRowCount() > 1) {
                Map<String, String> props = new LinkedHashMap();
                int rcnt = tableModel.getRowCount() - 1;

                for(int i = 0; i < rcnt; ++i) {
                    Object k = tableModel.getValueAt(i, 0);
                    Object v = tableModel.getValueAt(i, 1);
                    if (ObjectUtils.isNotEmpty(k) && ObjectUtils.isNotEmpty(v)) {
                        props.put(k.toString(), v.toString());
                    }
                }

                target.setProps(props);
            }

            target.setUseTableComments(chkTableComments.isSelected());
            target.setTableCommentsSql(txtTableComments.getText());
            target.setUseColumnComments(chkColumnComments.isSelected());
            target.setColumnCommentsSql(txtColumnComments.getText());
            target.setUseTables(chkTables.isSelected());
            target.setTablesSql(txtTables.getText());
            target.setUseColumns(chkColumns.isSelected());
            target.setColumnsSql(txtColumns.getText());
            
            JDBGenConfig.saveInstance(this);
            changed = true;
        }
        setVisible(false);
    }//GEN-LAST:event_btnSaveActionPerformed

    private void updateDriver(Consumer<JDBDriver> cons) {
        int idx = lstDrivers.getSelectedIndex();
        if (idx < 0) return;
        JDBDriver driver = (JDBDriver)drivers.get(lstDrivers.getSelectedIndex());
        cons.accept(driver);
    }
    
    private void lstDriversValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstDriversValueChanged
        int idx = lstDrivers.getSelectedIndex();
        if (idx < 0) return;
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

        driver.getProps().forEach((k, v) -> {
            tableModel.addRow(new String[]{k, v});
        });

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
        

    }//GEN-LAST:event_lstDriversValueChanged

    private void btnNewDriverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewDriverActionPerformed
//        resetControls();
        JDBDriver driver = new JDBDriver();
        driver.setIcon("stock:generic.png");
        driver.setName(NamingUtils.nextNameOf(drivers, "New Driver"));
        driver.setProps(new LinkedHashMap<>());
        drivers.add(driver);
        listModel.addElement(driver.getName());
        lstDrivers.setSelectedIndex(drivers.size()-1);
    }//GEN-LAST:event_btnNewDriverActionPerformed

    private void btnCloneDriverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloneDriverActionPerformed
        int idx = lstDrivers.getSelectedIndex();
        if (idx >= 0) {
            JDBDriver driver = drivers.get(idx);
            JDBDriver newOne = driver.toBuilder()
                    .name(NamingUtils.nextNameOf(drivers, "Copy of " + driver.getName()))
                    .stockItem(false)
                    .build();
            if (ObjectUtils.isNotEmpty(driver.getProps()))
                newOne.setProps(new LinkedHashMap(driver.getProps()));

            drivers.add(newOne);
            listModel.addElement(newOne.getName());
            lstDrivers.setSelectedIndex(drivers.size() - 1);
        }

    }//GEN-LAST:event_btnCloneDriverActionPerformed

    private void btnDelDriverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelDriverActionPerformed
        int idx = lstDrivers.getSelectedIndex();
        if (idx >= 0) {
            JDBDriver driver = (JDBDriver)drivers.get(idx);
            if (!driver.isStockItem()) {
                drivers.remove(idx);
                listModel.remove(idx);
            }
            resetControls();
        }
    }//GEN-LAST:event_btnDelDriverActionPerformed

    private void btnDelPropActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelPropActionPerformed
        int idx = tabProps.getSelectedRow();
        if (idx > -1) {
            tableModel.removeRow(idx);
        }
    }//GEN-LAST:event_btnDelPropActionPerformed

    private void btnBrowseIconActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseIconActionPerformed
        String fpath = UIUtils.openIconDlg(this, "");
        if (!StrUtils.isEmpty(fpath)) {
            txtIcon.setText(fpath);
        }
    }//GEN-LAST:event_btnBrowseIconActionPerformed

    private void txtDriverClassMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtDriverClassMouseClicked
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
    }//GEN-LAST:event_txtDriverClassMouseClicked

    private void btnDownJdbcActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDownJdbcActionPerformed
        MavenExplorer me = MavenExplorer.getInstance();
        EventQueue.invokeLater(() -> {
            me.setModal(true);
            me.setLocationRelativeTo(this);
            updateDriver(d -> {
                String query = d.getDefaultQuery();
                if (!StrUtils.isEmpty(query))
                    me.setQuery(d.getDefaultQuery());
                me.setVisible(true);
                if (me.changed) {
                    txtJarFile.setText(me.saveLocation);
                    d.setJdbcJar(me.saveLocation);
                }
            });
        });
//        String groupId = txtGroupId.getText();
//        String artifactId = txtArtifactId.getText();
//        String vInclude = txtVersionInclude.getText();
//        if (ObjectUtils.isNotEmpty(groupId) && ObjectUtils.isNotEmpty(artifactId)) {
////            btnDownJdbc.setEnabled(false);
////            EventQueue.invokeLater(() -> {
////                Pair<String, String> res = MavenUtils.downloadMaven(groupId, artifactId, vInclude);
////                if (res != null) {
////                    txtVersion.setText((String)res.getFirst());
////                    txtJarFile.setText((String)res.getSecond());
////                }
////                btnDownJdbc.setEnabled(true);
////            });
//        }
    }//GEN-LAST:event_btnDownJdbcActionPerformed

    private void btnBrowseJarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseJarActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("drivers"));
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
                return "Java library files";
            }
        });
        if (fc.showOpenDialog(this) == 0) {
            String cpath = new File("").getAbsolutePath();
            String fpath = fc.getSelectedFile().getAbsolutePath();
            String relative = fpath.startsWith(cpath) ? fpath.substring(cpath.length()+1) : fpath;
            this.txtJarFile.setText(relative);
            updateDriver(d -> d.setJdbcJar(relative));
        }
    }//GEN-LAST:event_btnBrowseJarActionPerformed

    private void chkTableCommentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkTableCommentsActionPerformed
        txtTableComments.setEnabled(chkTableComments.isSelected());
    }//GEN-LAST:event_chkTableCommentsActionPerformed

    private void chkColumnCommentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkColumnCommentsActionPerformed
        txtColumnComments.setEnabled(chkColumnComments.isSelected());
    }//GEN-LAST:event_chkColumnCommentsActionPerformed

    private void btnTableCommentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTableCommentsActionPerformed
        // TODO add your handling code here:
        PlatformUtils.openURL("https://github.com/xcomart/jdbgen/docs/README.md#get-table-comments-sql");
    }//GEN-LAST:event_btnTableCommentsActionPerformed

    private void btnColumnCommentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnColumnCommentsActionPerformed
        // TODO add your handling code here:
        PlatformUtils.openURL("https://github.com/xcomart/jdbgen/docs/README.md#get-column-comments-sql");
    }//GEN-LAST:event_btnColumnCommentsActionPerformed

    private void chkTablesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkTablesActionPerformed
        txtTables.setEnabled(chkTables.isSelected());
    }//GEN-LAST:event_chkTablesActionPerformed

    private void btnTablesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTablesActionPerformed
        // TODO add your handling code here:
        PlatformUtils.openURL("https://github.com/xcomart/jdbgen/docs/README.md#get-table-list-sql");
    }//GEN-LAST:event_btnTablesActionPerformed

    private void chkColumnsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkColumnsActionPerformed
        txtColumns.setEnabled(chkColumns.isSelected());
    }//GEN-LAST:event_chkColumnsActionPerformed

    private void btnColumnsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnColumnsActionPerformed
        // TODO add your handling code here:
        PlatformUtils.openURL("https://github.com/xcomart/jdbgen/docs/README.md#get-column-list-sql");
    }//GEN-LAST:event_btnColumnsActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        UIUtils.setFlatDarkLaf();
        EventQueue.invokeLater(() -> {
            JDBDriverManager instance = getInstance();
            instance.setLocationRelativeTo(null);
            instance.setVisible(true);
            System.exit(0);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBrowseIcon;
    private javax.swing.JButton btnBrowseJar;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnCloneDriver;
    private javax.swing.JButton btnColumnComments;
    private javax.swing.JButton btnColumns;
    private javax.swing.JButton btnDelDriver;
    private javax.swing.JButton btnDelProp;
    private javax.swing.JButton btnDownJdbc;
    private javax.swing.JButton btnIconHelp;
    private javax.swing.JButton btnNewDriver;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnTableComments;
    private javax.swing.JButton btnTables;
    private javax.swing.JCheckBox chkColumnComments;
    private javax.swing.JCheckBox chkColumns;
    private javax.swing.JCheckBox chkNoAuth;
    private javax.swing.JCheckBox chkTableComments;
    private javax.swing.JCheckBox chkTables;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JList<String> lstDrivers;
    private javax.swing.JTable tabProps;
    private javax.swing.JTextArea txtColumnComments;
    private javax.swing.JTextArea txtColumns;
    private javax.swing.JTextField txtDriverClass;
    private javax.swing.JTextField txtDriverName;
    private javax.swing.JTextField txtIcon;
    private javax.swing.JTextField txtJarFile;
    private javax.swing.JTextArea txtTableComments;
    private javax.swing.JTextArea txtTables;
    private javax.swing.JTextField txtUrlTemplate;
    // End of variables declaration//GEN-END:variables
}

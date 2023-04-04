/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.ui;

import comart.tools.jdbgen.types.JDBDriver;
import comart.tools.jdbgen.types.JDBGenConfig;
import comart.utils.ClassUtils;
import comart.utils.PlatformUtils;
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
        
        UIUtils.applyIcon(btnCatalog, FontAwesome.QUESTION);
        UIUtils.applyIcon(btnSchema, FontAwesome.QUESTION);
        UIUtils.applyIcon(btnTable, FontAwesome.QUESTION);
        UIUtils.applyIcon(btnColumn, FontAwesome.QUESTION);
        
        UIUtils.addIcon(btnCancel, FontAwesome.TIMES);
        UIUtils.addIcon(btnSave, FontAwesome.FLOPPY_O);
    }

    private void eventSetup() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                btnCancelActionPerformed(null);
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
        txtDriverName.setText(NamingUtils.nextNameOf(drivers, "New Driver"));
        txtIcon.setText("stock:generic.png");
        txtJarFile.setText("");
        txtUrlTemplate.setText("");
        
        chkCatalog.setSelected(false);
        txtCatalog.setText("");
        chkSchema.setSelected(false);
        txtSchema.setText("");
        chkTable.setSelected(false);
        txtTable.setText("");
        chkColumn.setSelected(false);
        txtColumn.setText("");
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
        jPanel4 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jPanel5 = new javax.swing.JPanel();
        btnCatalog = new javax.swing.JButton();
        chkCatalog = new javax.swing.JCheckBox();
        jScrollPane4 = new javax.swing.JScrollPane();
        txtSchema = new javax.swing.JTextArea();
        btnSchema = new javax.swing.JButton();
        chkSchema = new javax.swing.JCheckBox();
        jScrollPane5 = new javax.swing.JScrollPane();
        txtCatalog = new javax.swing.JTextArea();
        chkTable = new javax.swing.JCheckBox();
        btnTable = new javax.swing.JButton();
        jScrollPane6 = new javax.swing.JScrollPane();
        txtTable = new javax.swing.JTextArea();
        chkColumn = new javax.swing.JCheckBox();
        btnColumn = new javax.swing.JButton();
        jScrollPane7 = new javax.swing.JScrollPane();
        txtColumn = new javax.swing.JTextArea();

        setTitle("Driver Manager");

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        jPanel3.setLayout(new java.awt.GridLayout(1, 0));

        btnNewDriver.setText("+");
        btnNewDriver.setPreferredSize(new java.awt.Dimension(30, 26));
        btnNewDriver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewDriverActionPerformed(evt);
            }
        });
        jPanel3.add(btnNewDriver);

        btnCloneDriver.setText("c");
        btnCloneDriver.setPreferredSize(new java.awt.Dimension(30, 26));
        btnCloneDriver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloneDriverActionPerformed(evt);
            }
        });
        jPanel3.add(btnCloneDriver);

        btnDelDriver.setText("-");
        btnDelDriver.setPreferredSize(new java.awt.Dimension(30, 26));
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
        jLabel3.setText("Name:");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("JDBC Jar:");

        btnBrowseJar.setText("...");
        btnBrowseJar.setPreferredSize(new java.awt.Dimension(30, 26));
        btnBrowseJar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseJarActionPerformed(evt);
            }
        });

        txtJarFile.setEditable(false);

        btnDownJdbc.setFont(btnDownJdbc.getFont().deriveFont(btnDownJdbc.getFont().getSize()-1f));
        btnDownJdbc.setForeground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
        btnDownJdbc.setText("Download jdbc driver from mvnrepository.com");
        btnDownJdbc.setBorder(null);
        btnDownJdbc.setBorderPainted(false);
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
        jLabel10.setText("Icon File:");

        btnBrowseIcon.setText("...");
        btnBrowseIcon.setPreferredSize(new java.awt.Dimension(30, 26));
        btnBrowseIcon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseIconActionPerformed(evt);
            }
        });

        txtIcon.setEditable(false);

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText("Connection Properties:");

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
        jScrollPane2.setViewportView(tabProps);

        btnDelProp.setText("-");
        btnDelProp.setPreferredSize(new java.awt.Dimension(30, 26));
        btnDelProp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelPropActionPerformed(evt);
            }
        });

        chkNoAuth.setText("Authentication is not required for this driver.");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel8)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtDriverName)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(txtJarFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowseJar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnDownJdbc, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtUrlTemplate)
                    .addComponent(txtDriverClass)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(txtIcon)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowseIcon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(chkNoAuth)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(134, 134, 134)
                .addComponent(btnDelProp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(396, 396, 396))
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
                    .addComponent(btnBrowseJar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(btnBrowseIcon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtIcon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkNoAuth)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDelProp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addContainerGap())))
        );

        jTabbedPane1.addTab("General", jPanel2);

        btnCatalog.setText("?");
        btnCatalog.setPreferredSize(new java.awt.Dimension(30, 26));
        btnCatalog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCatalogActionPerformed(evt);
            }
        });

        chkCatalog.setText("Get catalogs");
        chkCatalog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkCatalogActionPerformed(evt);
            }
        });

        txtSchema.setColumns(20);
        txtSchema.setRows(5);
        jScrollPane4.setViewportView(txtSchema);

        btnSchema.setText("?");
        btnSchema.setPreferredSize(new java.awt.Dimension(30, 26));
        btnSchema.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSchemaActionPerformed(evt);
            }
        });

        chkSchema.setText("Get schemas of catalog");
        chkSchema.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkSchemaActionPerformed(evt);
            }
        });

        txtCatalog.setColumns(20);
        txtCatalog.setRows(5);
        jScrollPane5.setViewportView(txtCatalog);

        chkTable.setText("Get tables of schema");
        chkTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkTableActionPerformed(evt);
            }
        });

        btnTable.setText("?");
        btnTable.setPreferredSize(new java.awt.Dimension(30, 26));
        btnTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTableActionPerformed(evt);
            }
        });

        txtTable.setColumns(20);
        txtTable.setRows(5);
        jScrollPane6.setViewportView(txtTable);

        chkColumn.setText("Get columns of table");
        chkColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkColumnActionPerformed(evt);
            }
        });

        btnColumn.setText("?");
        btnColumn.setPreferredSize(new java.awt.Dimension(30, 26));
        btnColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnColumnActionPerformed(evt);
            }
        });

        txtColumn.setColumns(20);
        txtColumn.setRows(5);
        jScrollPane7.setViewportView(txtColumn);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(chkSchema)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSchema, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(chkColumn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnColumn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel5Layout.createSequentialGroup()
                                        .addComponent(chkTable)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnTable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel5Layout.createSequentialGroup()
                                        .addComponent(chkCatalog)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnCatalog, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jScrollPane7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 503, Short.MAX_VALUE)
                                    .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jScrollPane5)
                                    .addComponent(jScrollPane6))))
                        .addContainerGap())))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkCatalog)
                    .addComponent(btnCatalog, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkSchema)
                    .addComponent(btnSchema, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkTable)
                    .addComponent(btnTable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkColumn)
                    .addComponent(btnColumn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 444, Short.MAX_VALUE)
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
        } else if (StringUtils.isBlank(txtCatalog.getText()) && chkCatalog.isSelected()) {
            UIUtils.error(this, "Catalog query required.");
            txtCatalog.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtSchema.getText()) && chkSchema.isSelected()) {
            UIUtils.error(this, "Schema query required.");
            txtSchema.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtTable.getText()) && chkTable.isSelected()) {
            UIUtils.error(this, "Table query required.");
            txtTable.requestFocusInWindow();
        } else if (StringUtils.isBlank(txtColumn.getText()) && chkColumn.isSelected()) {
            UIUtils.error(this, "Column query required.");
            txtColumn.requestFocusInWindow();
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

            target.setUseCatalogSql(chkCatalog.isSelected());
            target.setCatalogSql(txtCatalog.getText());
            target.setUseSchemaSql(chkSchema.isSelected());
            target.setSchemaSql(txtSchema.getText());
            target.setUseTableSql(chkTable.isSelected());
            target.setTableSql(txtTable.getText());
            target.setUseColumnSql(chkColumn.isSelected());
            target.setColumnSql(txtColumn.getText());
            
            JDBGenConfig.saveInstace(this);
            changed = true;
        }
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

        for(int i = tableModel.getRowCount() - 1; i >= 0; --i) {
            tableModel.removeRow(i);
        }

        driver.getProps().forEach((k, v) -> {
            tableModel.addRow(new String[]{k, v});
        });

        chkCatalog.setSelected(driver.isUseCatalogSql());
        txtCatalog.setEnabled(chkCatalog.isSelected());
        txtCatalog.setText(driver.getCatalogSql());
        chkSchema.setSelected(driver.isUseSchemaSql());
        txtSchema.setEnabled(chkSchema.isSelected());
        txtSchema.setText(driver.getSchemaSql());
        chkTable.setSelected(driver.isUseTableSql());
        txtTable.setEnabled(chkTable.isSelected());
        txtTable.setText(driver.getTableSql());
        chkColumn.setSelected(driver.isUseColumnSql());
        txtColumn.setEnabled(chkColumn.isSelected());
        txtColumn.setText(driver.getColumnSql());
        
        boolean isStockItem = driver.isStockItem();
        txtDriverName.setEditable(!isStockItem);
        txtDriverClass.setEditable(!isStockItem);
        btnBrowseIcon.setEnabled(!isStockItem);
        btnDelDriver.setEnabled(!isStockItem);
        

    }//GEN-LAST:event_lstDriversValueChanged

    private void btnNewDriverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewDriverActionPerformed
        resetControls();
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
        }

    }//GEN-LAST:event_btnDelDriverActionPerformed

    private void btnDelPropActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelPropActionPerformed
        int idx = tabProps.getSelectedRow();
        if (idx > -1) {
            tableModel.removeRow(idx);
        }
    }//GEN-LAST:event_btnDelPropActionPerformed

    private void btnBrowseIconActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseIconActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    String fname = f.getName();
                    int idx = fname.lastIndexOf(46);
                    if (idx > -1) {
                        String ext = fname.substring(idx + 1).toLowerCase();
                        String[] extList = new String[]{"jpg", "jpeg", "tiff", "tif", "gif", "png", "ico"};
                        return ArrayUtils.contains(extList, ext);
                    } else {
                        return false;
                    }
                }
            }

            @Override
            public String getDescription() {
                return "Image/Icon files";
            }
        });
        if (fc.showOpenDialog(this) == 0) {
            txtIcon.setText(fc.getSelectedFile().getPath());
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
            me.setVisible(true);
            if (me.changed) {
                txtJarFile.setText(me.saveLocation);
                updateDriver(d -> d.setJdbcJar(me.saveLocation));
            }
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

    private void chkCatalogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkCatalogActionPerformed
        txtCatalog.setEnabled(chkCatalog.isSelected());
    }//GEN-LAST:event_chkCatalogActionPerformed

    private void chkSchemaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkSchemaActionPerformed
        txtSchema.setEnabled(chkSchema.isSelected());
    }//GEN-LAST:event_chkSchemaActionPerformed

    private void chkTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkTableActionPerformed
        txtTable.setEnabled(chkTable.isSelected());
    }//GEN-LAST:event_chkTableActionPerformed

    private void chkColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkColumnActionPerformed
        txtColumn.setEnabled(chkColumn.isSelected());
    }//GEN-LAST:event_chkColumnActionPerformed

    private void btnCatalogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCatalogActionPerformed
        // TODO add your handling code here:
        PlatformUtils.openURL("https://github.com/xcomart/jdbgen");
    }//GEN-LAST:event_btnCatalogActionPerformed

    private void btnSchemaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSchemaActionPerformed
        // TODO add your handling code here:
        PlatformUtils.openURL("https://github.com/xcomart/jdbgen");
    }//GEN-LAST:event_btnSchemaActionPerformed

    private void btnTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTableActionPerformed
        // TODO add your handling code here:
        PlatformUtils.openURL("https://github.com/xcomart/jdbgen");
    }//GEN-LAST:event_btnTableActionPerformed

    private void btnColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnColumnActionPerformed
        // TODO add your handling code here:
        PlatformUtils.openURL("https://github.com/xcomart/jdbgen");
    }//GEN-LAST:event_btnColumnActionPerformed

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
    private javax.swing.JButton btnCatalog;
    private javax.swing.JButton btnCloneDriver;
    private javax.swing.JButton btnColumn;
    private javax.swing.JButton btnDelDriver;
    private javax.swing.JButton btnDelProp;
    private javax.swing.JButton btnDownJdbc;
    private javax.swing.JButton btnNewDriver;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnSchema;
    private javax.swing.JButton btnTable;
    private javax.swing.JCheckBox chkCatalog;
    private javax.swing.JCheckBox chkColumn;
    private javax.swing.JCheckBox chkNoAuth;
    private javax.swing.JCheckBox chkSchema;
    private javax.swing.JCheckBox chkTable;
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
    private javax.swing.JTextArea txtCatalog;
    private javax.swing.JTextArea txtColumn;
    private javax.swing.JTextField txtDriverClass;
    private javax.swing.JTextField txtDriverName;
    private javax.swing.JTextField txtIcon;
    private javax.swing.JTextField txtJarFile;
    private javax.swing.JTextArea txtSchema;
    private javax.swing.JTextArea txtTable;
    private javax.swing.JTextField txtUrlTemplate;
    // End of variables declaration//GEN-END:variables
}

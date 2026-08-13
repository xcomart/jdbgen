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
import comart.utils.AppDirs;
import comart.utils.ClassUtils;
import comart.utils.I18n;
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
import lombok.extern.slf4j.Slf4j;
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
        // the column titles come from the form's design time table model, which
        // cannot hold custom code, so they are translated here.
        tabProps.getColumnModel().getColumn(0).setHeaderValue(I18n.t("driverManager.tabProps.key"));
        tabProps.getColumnModel().getColumn(1).setHeaderValue(I18n.t("driverManager.tabProps.value"));
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
        lstDrivers.setCellRenderer(UIUtils.getListCellRenderer(
                s -> drivers.stream()
                        .filter(d -> s.equals(d.getName()))
                        .findFirst().orElse(null)));
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

        setTitle(I18n.t("driverManager.title"));

        btnCancel.setText(I18n.t("driverManager.btnCancel.text"));
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        btnSave.setText(I18n.t("driverManager.btnSave.text"));
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
        jLabel1.setText(I18n.t("driverManager.jLabel1.text"));

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
        jLabel3.setText(I18n.t("driverManager.jLabel3.text"));

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText(I18n.t("driverManager.jLabel4.text"));

        btnBrowseJar.setText("...");
        btnBrowseJar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseJarActionPerformed(evt);
            }
        });

        txtJarFile.setEditable(false);

        btnDownJdbc.setFont(btnDownJdbc.getFont().deriveFont(btnDownJdbc.getFont().getSize()-1f));
        btnDownJdbc.setForeground(javax.swing.UIManager.getDefaults().getColor("Component.accentColor"));
        btnDownJdbc.setText(I18n.t("driverManager.btnDownJdbc.text"));
        btnDownJdbc.setBorder(null);
        btnDownJdbc.setBorderPainted(false);
        btnDownJdbc.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnDownJdbc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDownJdbcActionPerformed(evt);
            }
        });

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText(I18n.t("driverManager.jLabel8.text"));

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText(I18n.t("driverManager.jLabel9.text"));

        txtDriverClass.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                txtDriverClassMouseClicked(evt);
            }
        });

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel10.setText(I18n.t("driverManager.jLabel10.text"));

        btnBrowseIcon.setText("...");
        btnBrowseIcon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseIconActionPerformed(evt);
            }
        });

        txtIcon.setEditable(false);

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText(I18n.t("driverManager.jLabel11.text"));

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

        chkNoAuth.setText(I18n.t("driverManager.chkNoAuth.text"));

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

        jTabbedPane1.addTab(I18n.t("driverManager.tab.general"), jPanel2);

        btnTableComments.setText("?");
        btnTableComments.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTableCommentsActionPerformed(evt);
            }
        });

        chkTableComments.setText(I18n.t("driverManager.chkTableComments.text"));
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

        chkColumnComments.setText(I18n.t("driverManager.chkColumnComments.text"));
        chkColumnComments.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkColumnCommentsActionPerformed(evt);
            }
        });

        txtTableComments.setColumns(20);
        txtTableComments.setRows(5);
        jScrollPane5.setViewportView(txtTableComments);

        chkTables.setText(I18n.t("driverManager.chkTables.text"));
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

        chkColumns.setText(I18n.t("driverManager.chkColumns.text"));
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

        jTabbedPane1.addTab(I18n.t("driverManager.tab.customQueries"), jPanel4);

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

    /**
     * close the dialog, discarding whatever has not been saved.
     */
    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed

    /**
     * validate the editor fields, store them into the driver, save the
     * configuration and close the dialog. The first failing check is reported
     * and focuses its field.
     */
    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
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

            JDBGenConfig.saveInstance(this);
            changed = true;
            setVisible(false);
        }
    }//GEN-LAST:event_btnSaveActionPerformed

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
    private void lstDriversValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstDriversValueChanged
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
    }//GEN-LAST:event_lstDriversValueChanged

    /**
     * add a driver with a generated name and the generic icon, and select it.
     */
    private void btnNewDriverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewDriverActionPerformed
//        resetControls();
        JDBDriver driver = new JDBDriver();
        driver.setIcon("stock:generic.png");
        driver.setName(NamingUtils.nextNameOf(drivers, I18n.t("driverManager.msg.newDriverName")));
        driver.setProps(new LinkedHashMap<>());
        drivers.add(driver);
        listModel.addElement(driver.getName());
        lstDrivers.setSelectedIndex(drivers.size()-1);
    }//GEN-LAST:event_btnNewDriverActionPerformed

    /**
     * add an editable copy of the selected driver and select it.
     */
    private void btnCloneDriverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloneDriverActionPerformed
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

    }//GEN-LAST:event_btnCloneDriverActionPerformed

    /**
     * remove the selected driver unless it is shipped with the application.
     */
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

    /**
     * remove the selected connection property.
     */
    private void btnDelPropActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelPropActionPerformed
        int idx = tabProps.getSelectedRow();
        if (idx > -1) {
            tableModel.removeRow(idx);
        }
    }//GEN-LAST:event_btnDelPropActionPerformed

    /**
     * pick the icon shown for this driver.
     */
    private void btnBrowseIconActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseIconActionPerformed
        String fpath = UIUtils.openIconDlg(this, "");
        if (!StrUtils.isEmpty(fpath)) {
            txtIcon.setText(fpath);
        }
    }//GEN-LAST:event_btnBrowseIconActionPerformed

    /**
     * offer the driver classes found in the selected jar in a popup menu, and
     * store the picked one in the driver.
     */
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

    /**
     * download the JDBC jar of this driver through the maven explorer, using
     * the driver's default query as the initial search.
     */
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

    /**
     * pick the JDBC jar, filtered to jar and zip files and starting in the
     * driver directory. A jar below the user data or the installation directory
     * is stored relative to it.
     */
    private void btnBrowseJarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseJarActionPerformed
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
        }
    }//GEN-LAST:event_btnBrowseJarActionPerformed

    /**
     * enable the table comment query along with its check box.
     */
    private void chkTableCommentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkTableCommentsActionPerformed
        txtTableComments.setEnabled(chkTableComments.isSelected());
    }//GEN-LAST:event_chkTableCommentsActionPerformed

    /**
     * enable the column comment query along with its check box.
     */
    private void chkColumnCommentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkColumnCommentsActionPerformed
        txtColumnComments.setEnabled(chkColumnComments.isSelected());
    }//GEN-LAST:event_chkColumnCommentsActionPerformed

    /**
     * open the documentation of the table comment query.
     */
    private void btnTableCommentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTableCommentsActionPerformed
        // TODO add your handling code here:
        PlatformUtils.openDoc("custom-queries.md#get-table-comments-sql");
    }//GEN-LAST:event_btnTableCommentsActionPerformed

    /**
     * open the documentation of the column comment query.
     */
    private void btnColumnCommentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnColumnCommentsActionPerformed
        // TODO add your handling code here:
        PlatformUtils.openDoc("custom-queries.md#get-column-comments-sql");
    }//GEN-LAST:event_btnColumnCommentsActionPerformed

    /**
     * enable the table list query along with its check box.
     */
    private void chkTablesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkTablesActionPerformed
        txtTables.setEnabled(chkTables.isSelected());
    }//GEN-LAST:event_chkTablesActionPerformed

    /**
     * open the documentation of the table list query.
     */
    private void btnTablesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTablesActionPerformed
        // TODO add your handling code here:
        PlatformUtils.openDoc("custom-queries.md#get-table-list-sql");
    }//GEN-LAST:event_btnTablesActionPerformed

    /**
     * enable the column list query along with its check box.
     */
    private void chkColumnsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkColumnsActionPerformed
        txtColumns.setEnabled(chkColumns.isSelected());
    }//GEN-LAST:event_chkColumnsActionPerformed

    /**
     * open the documentation of the column list query.
     */
    private void btnColumnsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnColumnsActionPerformed
        // TODO add your handling code here:
        PlatformUtils.openDoc("custom-queries.md#get-column-list-sql");
    }//GEN-LAST:event_btnColumnsActionPerformed

    /**
     * show this dialog alone for development purposes. The virtual machine is
     * terminated as soon as the dialog is closed.
     *
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

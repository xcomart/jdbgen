//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package comart.tools.jdbgen.ui;

import comart.tools.jdbgen.types.JDBGenConfig;
import comart.utils.UIUtils;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.table.DefaultTableModel;
import jiconfont.icons.font_awesome.FontAwesome;
import org.apache.commons.lang3.ObjectUtils;

public class JDBConnectionManagerOld extends JFrame {
    private static final Logger logger = Logger.getLogger(JDBConnectionManagerOld.class.getName());
    private DefaultListModel<String> listModel = new DefaultListModel();
    private JButton btnApply;
    private JButton btnCloneConn;
    private JButton btnClose;
    private JButton btnConnect;
    private JButton btnDelConn;
    private JButton btnDelProp;
    private JButton btnDelTemplate;
    private JButton btnManage;
    private JButton btnNewConn;
    private JButton btnNewTemplate;
    private JButton btnPreset;
    private JButton btnSave;
    private JButton btnTemplateBrowse;
    private JButton btnTemplateHelp;
    private JComboBox<String> cboDriverType;
    private JComboBox<String> cboTemplateType;
    private JCheckBox chkCustomAuthor;
    private JCheckBox chkDarkUI;
    private JCheckBox chkIgnoreFirst;
    private JCheckBox chkJavaPackage;
    private JCheckBox chkSqlNamespace;
    private JLabel jLabel1;
    private JLabel jLabel10;
    private JLabel jLabel11;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JLabel jLabel4;
    private JLabel jLabel5;
    private JLabel jLabel6;
    private JLabel jLabel7;
    private JLabel jLabel9;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JPanel jPanel3;
    private JPanel jPanel4;
    private JPanel jPanel5;
    private JPanel jPanel6;
    private JScrollPane jScrollPane1;
    private JScrollPane jScrollPane2;
    private JScrollPane jScrollPane3;
    private JSplitPane jSplitPane1;
    private JTabbedPane jTabbedPane1;
    private JList<String> lstConnections;
    private JTable tabConnProps;
    private JTable tabTemplates;
    private JTextField txtCustomAuthor;
    private JTextField txtDispName;
    private JTextField txtTemplateFile;
    private JTextField txtTemplateName;
    private JTextField txtUserName;
    private JPasswordField txtUserPass;

    public JDBConnectionManagerOld() {
        this.initComponents();
        this.applyIcons();
        this.eventSetup();
        JDBGenConfig conf = JDBGenConfig.getInstance();
        this.chkDarkUI.setSelected(conf.isDarkUI());
        this.chkDarkUIActionPerformed((ActionEvent)null);
        ((List)Optional.ofNullable(conf.getConnections()).orElse(new ArrayList())).forEach((c) -> {
            this.listModel.addElement(c.toString());
        });
        this.lstConnections.setModel(this.listModel);
    }

    private void applyIcons() {
        UIUtils.applyIcon(this.btnNewConn, FontAwesome.PLUS);
        UIUtils.applyIcon(this.btnCloneConn, FontAwesome.CLONE);
        UIUtils.applyIcon(this.btnDelConn, FontAwesome.MINUS);
        UIUtils.applyIcon(this.btnTemplateHelp, FontAwesome.QUESTION);
        UIUtils.applyIcon(this.btnNewTemplate, FontAwesome.PLUS);
        UIUtils.applyIcon(this.btnDelTemplate, FontAwesome.MINUS);
        UIUtils.applyIcon(this.btnTemplateBrowse, FontAwesome.FOLDER_O);
        UIUtils.applyIcon(this.btnDelProp, FontAwesome.MINUS);
        UIUtils.addIcon(this.btnClose, FontAwesome.TIMES);
        UIUtils.addIcon(this.btnSave, FontAwesome.FLOPPY_O);
        UIUtils.addIcon(this.btnConnect, FontAwesome.PLUG);
        UIUtils.addIcon(this.btnPreset, FontAwesome.CUBES);
        UIUtils.addIcon(this.btnApply, FontAwesome.ARROW_UP);
        UIUtils.addIcon(this.btnManage, FontAwesome.COG);
    }

    private void eventSetup() {
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                JDBConnectionManagerOld.this.btnCloseActionPerformed((ActionEvent)null);
            }
        });
        DefaultTableModel tmodel = (DefaultTableModel)this.tabConnProps.getModel();
        tmodel.addTableModelListener((e) -> {
            int ridx = tmodel.getRowCount() - 1;
            boolean needToAdd = false;

            for(int i = 0; i < tmodel.getColumnCount(); ++i) {
                if (ObjectUtils.isNotEmpty(tmodel.getValueAt(ridx, i))) {
                    needToAdd = true;
                    break;
                }
            }

            if (needToAdd) {
                EventQueue.invokeLater(() -> {
                    tmodel.addRow(new String[tmodel.getColumnCount()]);
                });
            }

        });
    }

    private void initComponents() {
        this.jSplitPane1 = new JSplitPane();
        this.jPanel1 = new JPanel();
        this.jLabel1 = new JLabel();
        this.jPanel2 = new JPanel();
        this.btnNewConn = new JButton();
        this.btnCloneConn = new JButton();
        this.btnDelConn = new JButton();
        this.jScrollPane1 = new JScrollPane();
        this.lstConnections = new JList();
        this.jPanel3 = new JPanel();
        this.jLabel2 = new JLabel();
        this.jPanel4 = new JPanel();
        this.jLabel3 = new JLabel();
        this.cboDriverType = new JComboBox();
        this.jLabel4 = new JLabel();
        this.jLabel5 = new JLabel();
        this.txtDispName = new JTextField();
        this.txtUserName = new JTextField();
        this.jLabel6 = new JLabel();
        this.txtUserPass = new JPasswordField();
        this.jLabel7 = new JLabel();
        this.jScrollPane2 = new JScrollPane();
        this.tabConnProps = new JTable();
        this.btnManage = new JButton();
        this.btnDelProp = new JButton();
        this.jTabbedPane1 = new JTabbedPane();
        this.jPanel5 = new JPanel();
        this.btnTemplateHelp = new JButton();
        this.btnPreset = new JButton();
        this.jScrollPane3 = new JScrollPane();
        this.tabTemplates = new JTable();
        this.btnNewTemplate = new JButton();
        this.btnDelTemplate = new JButton();
        this.btnApply = new JButton();
        this.jLabel11 = new JLabel();
        this.jLabel10 = new JLabel();
        this.jLabel9 = new JLabel();
        this.txtTemplateFile = new JTextField();
        this.txtTemplateName = new JTextField();
        this.cboTemplateType = new JComboBox();
        this.btnTemplateBrowse = new JButton();
        this.jPanel6 = new JPanel();
        this.chkIgnoreFirst = new JCheckBox();
        this.chkCustomAuthor = new JCheckBox();
        this.txtCustomAuthor = new JTextField();
        this.chkJavaPackage = new JCheckBox();
        this.chkSqlNamespace = new JCheckBox();
        this.chkDarkUI = new JCheckBox();
        this.btnClose = new JButton();
        this.btnConnect = new JButton();
        this.btnSave = new JButton();
        this.setDefaultCloseOperation(3);
        this.setTitle("Connection Manager");
        this.setMinimumSize(new Dimension(554, 554));
        this.jSplitPane1.setDividerLocation(150);
        this.jSplitPane1.setDividerSize(2);
        this.jSplitPane1.setResizeWeight(1.0D);
        this.jLabel1.setFont(this.jLabel1.getFont().deriveFont(this.jLabel1.getFont().getStyle() | 1, (float)(this.jLabel1.getFont().getSize() + 4)));
        this.jLabel1.setText("Connections");
        this.jPanel2.setLayout(new GridLayout(1, 0));
        this.btnNewConn.setText("+");
        this.btnNewConn.setPreferredSize(new Dimension(30, 26));
        this.jPanel2.add(this.btnNewConn);
        this.btnCloneConn.setText("c");
        this.btnCloneConn.setPreferredSize(new Dimension(30, 26));
        this.jPanel2.add(this.btnCloneConn);
        this.btnDelConn.setText("-");
        this.btnDelConn.setPreferredSize(new Dimension(30, 26));
        this.jPanel2.add(this.btnDelConn);
        this.jScrollPane1.setViewportView(this.lstConnections);
        GroupLayout jPanel1Layout = new GroupLayout(this.jPanel1);
        this.jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING).addComponent(this.jLabel1, -1, -1, 32767).addComponent(this.jPanel2, -1, -1, 32767).addComponent(this.jScrollPane1, -2, 0, 32767));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(this.jLabel1).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.jScrollPane1, -1, 511, 32767).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.jPanel2, -2, -1, -2)));
        this.jSplitPane1.setLeftComponent(this.jPanel1);
        this.jLabel2.setFont(this.jLabel2.getFont().deriveFont(this.jLabel2.getFont().getStyle() | 1, (float)(this.jLabel2.getFont().getSize() + 4)));
        this.jLabel2.setText("Connection Informations");
        this.jLabel3.setHorizontalAlignment(11);
        this.jLabel3.setText("Driver Type:");
        this.jLabel4.setHorizontalAlignment(11);
        this.jLabel4.setText("Display Name:");
        this.jLabel5.setHorizontalAlignment(11);
        this.jLabel5.setText("User Name:");
        this.jLabel6.setHorizontalAlignment(11);
        this.jLabel6.setText("User Password:");
        this.jLabel7.setHorizontalAlignment(11);
        this.jLabel7.setText("Additional Properties:");
        this.tabConnProps.setModel(new DefaultTableModel(new Object[][]{{"", ""}}, new String[]{"Key", "Value"}) {
            Class[] types = new Class[]{String.class, String.class};

            public Class getColumnClass(int columnIndex) {
                return this.types[columnIndex];
            }
        });
        this.jScrollPane2.setViewportView(this.tabConnProps);
        this.btnManage.setText("Manage...");
        this.btnManage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                JDBConnectionManagerOld.this.btnManageActionPerformed(evt);
            }
        });
        this.btnDelProp.setText("-");
        this.btnDelProp.setMaximumSize(new Dimension(30, 26));
        this.btnDelProp.setMinimumSize(new Dimension(30, 26));
        this.btnDelProp.setPreferredSize(new Dimension(30, 26));
        GroupLayout jPanel4Layout = new GroupLayout(this.jPanel4);
        this.jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(jPanel4Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel4Layout.createSequentialGroup().addContainerGap().addGroup(jPanel4Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel4Layout.createSequentialGroup().addComponent(this.jLabel3).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.cboDriverType, 0, -1, 32767).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.btnManage)).addGroup(jPanel4Layout.createSequentialGroup().addComponent(this.jLabel5).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.txtUserName)).addGroup(jPanel4Layout.createSequentialGroup().addComponent(this.jLabel4).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.txtDispName)).addGroup(jPanel4Layout.createSequentialGroup().addComponent(this.jLabel6).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.txtUserPass)).addGroup(jPanel4Layout.createSequentialGroup().addGroup(jPanel4Layout.createParallelGroup(Alignment.TRAILING).addComponent(this.jLabel7).addComponent(this.btnDelProp, -2, -1, -2)).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.jScrollPane2, -2, 0, 32767)))));
        jPanel4Layout.linkSize(0, new Component[]{this.jLabel3, this.jLabel4, this.jLabel5, this.jLabel6, this.jLabel7});
        jPanel4Layout.setVerticalGroup(jPanel4Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel4Layout.createSequentialGroup().addContainerGap().addGroup(jPanel4Layout.createParallelGroup(Alignment.CENTER).addComponent(this.jLabel3).addComponent(this.cboDriverType, -2, -1, -2).addComponent(this.btnManage)).addPreferredGap(ComponentPlacement.RELATED).addGroup(jPanel4Layout.createParallelGroup(Alignment.BASELINE).addComponent(this.jLabel4).addComponent(this.txtDispName, -2, -1, -2)).addGap(6, 6, 6).addGroup(jPanel4Layout.createParallelGroup(Alignment.BASELINE).addComponent(this.jLabel5).addComponent(this.txtUserName, -2, -1, -2)).addPreferredGap(ComponentPlacement.RELATED).addGroup(jPanel4Layout.createParallelGroup(Alignment.BASELINE).addComponent(this.jLabel6).addComponent(this.txtUserPass, -2, -1, -2)).addPreferredGap(ComponentPlacement.RELATED).addGroup(jPanel4Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel4Layout.createSequentialGroup().addComponent(this.jLabel7).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.btnDelProp, -2, -1, -2).addGap(0, 9, 32767)).addComponent(this.jScrollPane2, -2, 0, 32767)).addContainerGap()));
        this.jTabbedPane1.setFont(this.jTabbedPane1.getFont().deriveFont(this.jTabbedPane1.getFont().getStyle() | 1, (float)(this.jTabbedPane1.getFont().getSize() + 4)));
        this.jTabbedPane1.setName("");
        this.btnTemplateHelp.setText("?");
        this.btnTemplateHelp.setMaximumSize(new Dimension(30, 26));
        this.btnTemplateHelp.setMinimumSize(new Dimension(30, 26));
        this.btnTemplateHelp.setPreferredSize(new Dimension(30, 26));
        this.btnPreset.setText("Presets...");
        this.tabTemplates.setModel(new DefaultTableModel(new Object[0][], new String[]{"Type", "Name", "Template"}) {
            Class[] types = new Class[]{String.class, String.class, String.class};
            boolean[] canEdit = new boolean[]{false, false, false};

            public Class getColumnClass(int columnIndex) {
                return this.types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return this.canEdit[columnIndex];
            }
        });
        this.jScrollPane3.setViewportView(this.tabTemplates);
        this.btnNewTemplate.setText("+");
        this.btnNewTemplate.setMaximumSize(new Dimension(30, 26));
        this.btnNewTemplate.setMinimumSize(new Dimension(30, 26));
        this.btnNewTemplate.setPreferredSize(new Dimension(30, 26));
        this.btnDelTemplate.setText("-");
        this.btnDelTemplate.setMaximumSize(new Dimension(30, 26));
        this.btnDelTemplate.setMinimumSize(new Dimension(30, 26));
        this.btnDelTemplate.setPreferredSize(new Dimension(30, 26));
        this.btnApply.setText("Apply");
        this.jLabel11.setHorizontalAlignment(11);
        this.jLabel11.setText("Template Type:");
        this.jLabel10.setHorizontalAlignment(11);
        this.jLabel10.setText("Template Name:");
        this.jLabel9.setHorizontalAlignment(11);
        this.jLabel9.setText("Template File:");
        this.cboTemplateType.setModel(new DefaultComboBoxModel(new String[]{"Java", "SqlMapperXML", "Generic"}));
        this.btnTemplateBrowse.setText("...");
        this.btnTemplateBrowse.setMaximumSize(new Dimension(30, 26));
        this.btnTemplateBrowse.setMinimumSize(new Dimension(30, 26));
        this.btnTemplateBrowse.setPreferredSize(new Dimension(30, 26));
        GroupLayout jPanel5Layout = new GroupLayout(this.jPanel5);
        this.jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(jPanel5Layout.createParallelGroup(Alignment.LEADING).addGroup(Alignment.TRAILING, jPanel5Layout.createSequentialGroup().addGap(12, 12, 12).addGroup(jPanel5Layout.createParallelGroup(Alignment.TRAILING).addGroup(jPanel5Layout.createSequentialGroup().addComponent(this.btnTemplateHelp, -2, -1, -2).addPreferredGap(ComponentPlacement.RELATED, -1, 32767).addComponent(this.btnPreset)).addComponent(this.jScrollPane3, -1, 506, 32767).addGroup(Alignment.LEADING, jPanel5Layout.createSequentialGroup().addComponent(this.btnNewTemplate, -2, -1, -2).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.btnDelTemplate, -2, -1, -2).addPreferredGap(ComponentPlacement.RELATED, -1, 32767).addComponent(this.btnApply)).addGroup(Alignment.LEADING, jPanel5Layout.createSequentialGroup().addComponent(this.jLabel11).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.cboTemplateType, 0, -1, 32767)).addGroup(Alignment.LEADING, jPanel5Layout.createSequentialGroup().addComponent(this.jLabel10).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.txtTemplateName)).addGroup(Alignment.LEADING, jPanel5Layout.createSequentialGroup().addComponent(this.jLabel9).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.txtTemplateFile).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.btnTemplateBrowse, -2, -1, -2))).addContainerGap()));
        jPanel5Layout.linkSize(0, new Component[]{this.jLabel10, this.jLabel11, this.jLabel9});
        jPanel5Layout.setVerticalGroup(jPanel5Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel5Layout.createSequentialGroup().addContainerGap().addGroup(jPanel5Layout.createParallelGroup(Alignment.BASELINE).addComponent(this.btnTemplateHelp, -2, -1, -2).addComponent(this.btnPreset)).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.jScrollPane3, -1, 130, 32767).addPreferredGap(ComponentPlacement.RELATED).addGroup(jPanel5Layout.createParallelGroup(Alignment.BASELINE).addComponent(this.btnNewTemplate, -2, -1, -2).addComponent(this.btnDelTemplate, -2, -1, -2).addComponent(this.btnApply)).addPreferredGap(ComponentPlacement.RELATED).addGroup(jPanel5Layout.createParallelGroup(Alignment.BASELINE).addComponent(this.jLabel11).addComponent(this.cboTemplateType, -2, -1, -2)).addPreferredGap(ComponentPlacement.RELATED).addGroup(jPanel5Layout.createParallelGroup(Alignment.BASELINE).addComponent(this.jLabel10).addComponent(this.txtTemplateName, -2, -1, -2)).addPreferredGap(ComponentPlacement.RELATED).addGroup(jPanel5Layout.createParallelGroup(Alignment.CENTER).addComponent(this.btnTemplateBrowse, -2, -1, -2).addComponent(this.txtTemplateFile, -2, -1, -2).addComponent(this.jLabel9)).addContainerGap()));
        this.jTabbedPane1.addTab("Template Settings", this.jPanel5);
        this.chkIgnoreFirst.setText("Ignore first word of table name('firstWord' variable will be set)");
        this.chkIgnoreFirst.setActionCommand("ignore first word");
        this.chkCustomAuthor.setText("Use custom 'author' value");
        this.chkCustomAuthor.setActionCommand("custom author");
        this.chkJavaPackage.setText("Create directory structure with java package information");
        this.chkJavaPackage.setActionCommand("java package");
        this.chkSqlNamespace.setText("Create directory structure with SQL mapper namespace");
        this.chkSqlNamespace.setActionCommand("mapper namespace");
        GroupLayout jPanel6Layout = new GroupLayout(this.jPanel6);
        this.jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(jPanel6Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel6Layout.createSequentialGroup().addContainerGap().addGroup(jPanel6Layout.createParallelGroup(Alignment.LEADING).addComponent(this.chkSqlNamespace, -1, -1, 32767).addComponent(this.chkJavaPackage, -1, -1, 32767).addComponent(this.chkIgnoreFirst, -1, 512, 32767).addGroup(jPanel6Layout.createSequentialGroup().addComponent(this.chkCustomAuthor).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.txtCustomAuthor))).addContainerGap()));
        jPanel6Layout.setVerticalGroup(jPanel6Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel6Layout.createSequentialGroup().addContainerGap().addComponent(this.chkIgnoreFirst).addPreferredGap(ComponentPlacement.RELATED).addGroup(jPanel6Layout.createParallelGroup(Alignment.CENTER).addComponent(this.chkCustomAuthor).addComponent(this.txtCustomAuthor, -2, -1, -2)).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.chkJavaPackage).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.chkSqlNamespace).addContainerGap(180, 32767)));
        this.jTabbedPane1.addTab("Default Options", this.jPanel6);
        this.chkDarkUI.setText("Dark UI");
        this.chkDarkUI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                JDBConnectionManagerOld.this.chkDarkUIActionPerformed(evt);
            }
        });
        GroupLayout jPanel3Layout = new GroupLayout(this.jPanel3);
        this.jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addContainerGap().addGroup(jPanel3Layout.createParallelGroup(Alignment.LEADING).addComponent(this.jTabbedPane1).addComponent(this.jPanel4, -1, -1, 32767).addGroup(jPanel3Layout.createSequentialGroup().addComponent(this.jLabel2).addPreferredGap(ComponentPlacement.RELATED, -1, 32767).addComponent(this.chkDarkUI)))));
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addGroup(jPanel3Layout.createParallelGroup(Alignment.BASELINE).addComponent(this.jLabel2).addComponent(this.chkDarkUI)).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.jPanel4, -2, -1, -2).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.jTabbedPane1)));
        this.jSplitPane1.setRightComponent(this.jPanel3);
        this.btnClose.setText("Close");
        this.btnClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                JDBConnectionManagerOld.this.btnCloseActionPerformed(evt);
            }
        });
        this.btnConnect.setText("Connect");
        this.btnSave.setText("Save");
        GroupLayout layout = new GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(this.jSplitPane1, -1, 682, 32767).addGroup(Alignment.TRAILING, layout.createSequentialGroup().addGap(0, 0, 32767).addComponent(this.btnSave).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.btnConnect).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.btnClose))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(this.jSplitPane1).addPreferredGap(ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(this.btnClose).addComponent(this.btnConnect).addComponent(this.btnSave)).addContainerGap()));
        this.pack();
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        System.exit(0);
    }

    private void btnManageActionPerformed(ActionEvent evt) {
        JDBDriverManager.getInstance().setVisible(true);
    }

    private void chkDarkUIActionPerformed(ActionEvent evt) {
        if (this.chkDarkUI.isSelected()) {
            UIUtils.setFlatDarkLaf();
        } else {
            UIUtils.setFlatLightLaf();
        }

        SwingUtilities.updateComponentTreeUI(this);
    }

    public static void main(String[] args) {
        UIUtils.setFlatDarkLaf();
        EventQueue.invokeLater(() -> {
            (new JDBConnectionManagerOld()).setVisible(true);
        });
    }
}

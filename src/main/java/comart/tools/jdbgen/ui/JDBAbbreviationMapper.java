/*
 * The MIT License
 *
 * Copyright 2024 comart.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package comart.tools.jdbgen.ui;

import comart.tools.jdbgen.types.JDBAbbr;
import comart.tools.jdbgen.types.JDBGenConfig;
import comart.tools.jdbgen.types.db.DBTable;
import comart.utils.I18n;
import comart.utils.StrUtils;
import comart.utils.UIUtils;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import jiconfont.icons.font_awesome.FontAwesome;
import lombok.extern.slf4j.Slf4j;

/**
 * editor of the abbreviation mappings kept in the application configuration.
 * Every row of the table maps an abbreviation to the word it is replaced with
 * while names are generated, and tells whether the mapping is active and
 * whether it applies to a whole name instead of a single name part. Edits are
 * written back to <code>JDBGenConfig</code> as they happen, the table always
 * keeps one trailing empty row for new entries, and duplicated abbreviations
 * are rejected. On a right click in the abbreviation cell of a whole name
 * mapping, a popup offers the table names of the current connection.
 *
 * @author comart
 */
@Slf4j
public class JDBAbbreviationMapper extends javax.swing.JDialog {


    /** the shared dialog instance, created on the first call of
     * <code>getInstance(Frame)</code>. */
    private static JDBAbbreviationMapper INSTANCE = null;
    /**
     * return the shared abbreviation mapper dialog. The dialog is created as a
     * modal dialog of <code>parent</code> and registered for look and feel
     * updates on the first call, later calls reuse that instance. The
     * application icon and the component tree are refreshed and the dialog is
     * centered on <code>parent</code> on every call.
     *
     * @param parent
     *            frame the dialog is centered on, used as owner on the first
     *            call.
     * @return the shared <code>JDBAbbreviationMapper</code> instance.
     */
    public static synchronized JDBAbbreviationMapper getInstance(Frame parent) {
        if (INSTANCE == null) {
            INSTANCE = new JDBAbbreviationMapper(parent, true);
            UIUtils.registerFrame(INSTANCE);
        }
        UIUtils.setApplicationIcon(INSTANCE);

        INSTANCE.updateComponents();
        INSTANCE.setLocationRelativeTo(parent);
        return INSTANCE;
    }
    
    /**
     * reapply the current look and feel to the whole dialog. Called after a
     * theme or font change so that the already created dialog is redrawn with
     * the new settings.
     */
    public void updateComponents() {
        SwingUtilities.updateComponentTreeUI(this);
    }
    
    /** application configuration the edited abbreviations are written to. */
    private JDBGenConfig conf;
    /** model of the mapping table, kept for the delete button and the
     * duplication check. */
    private DefaultTableModel mdl;

    /**
     * Creates new form JDBAbbreviationMapper
     * <p>
     * The column headers are translated, the preferred column widths are
     * applied and the table is filled with the abbreviations of the current
     * configuration followed by one empty row. A model listener is installed
     * which rejects duplicated abbreviations and otherwise writes the table
     * back to the configuration and makes sure an empty trailing row remains.
     *
     * @param parent
     *            frame the dialog belongs to.
     * @param modal
     *            <code>true</code> to create a modal dialog.
     */
    public JDBAbbreviationMapper(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        
        TableColumnModel colModel = tblMapping.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(50);
        colModel.getColumn(1).setPreferredWidth(50);
        colModel.getColumn(2).setPreferredWidth(110);
        colModel.getColumn(3).setPreferredWidth(110);
        // the column names of the generated model are the untranslated
        // placeholders of the form editor, the shown ones are set here.
        String[] headerKeys = {
            "abbreviationMapper.tblMapping.column.apply",
            "abbreviationMapper.tblMapping.column.totalName",
            "abbreviationMapper.tblMapping.column.abbreviation",
            "abbreviationMapper.tblMapping.column.replaceTo"
        };
        for (int i=0; i<headerKeys.length; i++)
            colModel.getColumn(i).setHeaderValue(I18n.t(headerKeys[i]));

        applyIcons();
        
        pack();
        
        conf = JDBGenConfig.getInstance();
        
        mdl = (DefaultTableModel)tblMapping.getModel();
        
        mdl.setRowCount(0);
        
        conf.getAbbrs().forEach(a -> mdl.addRow(a.getRowArray()));
        mdl.addRow(new Object[]{Boolean.FALSE, Boolean.FALSE, "", ""});
        
        tblMapping.getModel().addTableModelListener((evt) -> {
            if (checkDuplication(mdl)) {
                conf.setAbbrs(applyTableToList(mdl));
                UIUtils.tableSetLastEmpty(tblMapping.getModel(), 2);
            }
        });
        UIUtils.setCommitOnLostFocus(tblMapping);
        
    }
    
    /**
     * check that no abbreviation is mapped twice. Only rows which are applied
     * and which have both an abbreviation and a replacement take part, and an
     * error message naming the offending abbreviation is shown for the first
     * collision found.
     *
     * @param model
     *            model of the mapping table to be checked.
     * @return <code>true</code> when all abbreviations are unique,
     *         <code>false</code> when a duplicate was found and reported.
     */
    private boolean checkDuplication(DefaultTableModel model) {
        HashMap<String,String> map = new HashMap<>();
        for (int i=0; i<model.getRowCount(); i++) {
            Boolean check = (Boolean)model.getValueAt(i, 0);
            String k = (String)model.getValueAt(i, 2);
            String v = (String)model.getValueAt(i, 3);
            if (check && !StrUtils.isEmpty(k) && !StrUtils.isEmpty(v)) {
                if (map.containsKey(k)) {
                    UIUtils.error(this, I18n.t("abbreviationMapper.msg.duplicated", k));
                    return false;
                } else {
                    map.put(k, v);
                }
            }
        }
        return true;
    }
    
    /** apply the font icons of the ok and of the delete button. */
    private void applyIcons() {
        UIUtils.addIcon(btnOk, FontAwesome.CHECK);
        UIUtils.applyIcon(btnDel, FontAwesome.MINUS);
    }    

    /**
     * convert the rows of the mapping table into abbreviation objects. Rows
     * whose abbreviation or replacement is empty are skipped, so the trailing
     * empty row of the editor is dropped, and the remaining rows are mapped to
     * <code>JDBAbbr</code> instances keeping their apply and whole name flags.
     *
     * @param model
     *            table model of the mapping table, holding the apply flag, the
     *            whole name flag, the abbreviation and the replacement in
     *            columns <code>0</code> to <code>3</code>.
     * @return the abbreviations of all filled rows, in table order.
     */
    public static List<JDBAbbr> applyTableToList(TableModel model) {
        List<JDBAbbr> abbrs = new ArrayList<>();
        for (int i=0; i<model.getRowCount(); i++) {
            Boolean check = (Boolean)model.getValueAt(i, 0);
            Boolean tname = (Boolean)model.getValueAt(i, 1);
            String k = (String)model.getValueAt(i, 2);
            String v = (String)model.getValueAt(i, 3);
            if (!StrUtils.isEmpty(k) && !StrUtils.isEmpty(v))
                abbrs.add(new JDBAbbr(check, tname, k, v));
        }
        return abbrs;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblMapping = new javax.swing.JTable();
        btnOk = new javax.swing.JButton();
        btnDel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD, jLabel1.getFont().getSize()+4));
        jLabel1.setText(I18n.t("abbreviationMapper.jLabel1.text"));

        tblMapping.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null}
            },
            new String [] {
                "Apply", "Total Name", "Abbreviation", "Replace To"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tblMapping.setToolTipText(I18n.t("abbreviationMapper.tblMapping.toolTipText"));
        tblMapping.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblMappingMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tblMapping);

        btnOk.setText(I18n.t("abbreviationMapper.btnOk.text"));
        btnOk.setToolTipText("");
        btnOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkActionPerformed(evt);
            }
        });

        btnDel.setText("-");
        btnDel.setToolTipText(I18n.t("abbreviationMapper.btnDel.toolTipText"));
        btnDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 513, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(btnDel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnOk)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 310, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnOk)
                    .addComponent(btnDel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /** save the configuration and hide the dialog. */
    private void btnOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOkActionPerformed
        JDBGenConfig.saveInstance(this);
        setVisible(false);
    }//GEN-LAST:event_btnOkActionPerformed

    /** ask for confirmation and remove the selected mapping row. */
    private void btnDelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelActionPerformed
        int idx = tblMapping.getSelectedRow();
        if (idx > -1 && idx < mdl.getRowCount()) {
            // the model always keeps a trailing empty row and empty rows are
            // filtered out on save, so model row index cannot be used to index
            // the config list. Remove the model row and rebuild the list.
            String k = (String)mdl.getValueAt(idx, 2);
            String v = (String)mdl.getValueAt(idx, 3);
            String desc = StrUtils.isEmpty(k) && StrUtils.isEmpty(v) ?
                    I18n.t("abbreviationMapper.msg.delete.row") :
                    I18n.t("abbreviationMapper.msg.delete.mapping", k, v);
            if (UIUtils.confirm(this, I18n.t("abbreviationMapper.msg.delete.title"),
                    I18n.t("abbreviationMapper.msg.delete", desc))) {
                mdl.removeRow(idx);
                conf.setAbbrs(applyTableToList(mdl));
                UIUtils.tableSetLastEmpty(mdl, 2);
            }
        }
    }//GEN-LAST:event_btnDelActionPerformed

    /**
     * show the table name popup for a right click on the mapping table. The
     * popup only appears on the abbreviation cell of a row which is marked as
     * a whole name mapping and only while the main window holds tables, and
     * the chosen table name is written into the clicked cell.
     *
     * @param evt
     *            mouse event of the click, its button and point decide whether
     *            and where the popup is shown.
     */
    private void showPopupTrigger(java.awt.event.MouseEvent evt) {
        if (evt.getButton() == MouseEvent.BUTTON3) {
            Point p = evt.getPoint();
            int row = tblMapping.rowAtPoint(p);
            int col = tblMapping.columnAtPoint(p);
            if (row < 0 || col < 0)
                return;
            Boolean tmap = (Boolean)mdl.getValueAt(row, 1);
            if (col == 2 && tmap != null && tmap) {
                List<DBTable> tables = JDBGeneratorMain.INSTANCE == null ?
                        null : JDBGeneratorMain.INSTANCE.getTables();
                if (tables == null || tables.isEmpty())
                    return;
                JPopupMenu menu = new JPopupMenu();
                for (DBTable t: tables) {
                    JMenuItem item = new JMenuItem(t.getName());
                    item.addActionListener(e -> mdl.setValueAt(t.getName(), row, col));
                    menu.add(item);
                }
                menu.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }
    }
    
    /** hand a click on the mapping table to <code>showPopupTrigger()</code>. */
    private void tblMappingMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblMappingMouseClicked
        showPopupTrigger(evt);
    }//GEN-LAST:event_tblMappingMouseClicked

    /**
     * stand alone entry point which shows this dialog on its own, used to
     * preview the form during development. The Nimbus look and feel is
     * selected when available and the virtual machine is terminated once the
     * dialog is closed.
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(JDBAbbreviationMapper.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(JDBAbbreviationMapper.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(JDBAbbreviationMapper.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JDBAbbreviationMapper.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                JDBAbbreviationMapper dialog = new JDBAbbreviationMapper(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDel;
    private javax.swing.JButton btnOk;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblMapping;
    // End of variables declaration//GEN-END:variables
}

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
import comart.utils.StrUtils;
import comart.utils.UIUtils;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import jiconfont.icons.font_awesome.FontAwesome;

/**
 *
 * @author comart
 */
public class JDBAbbreviationMapper extends javax.swing.JDialog {
    private static final Logger logger = Logger.getLogger(JDBAbbreviationMapper.class.getName());


    private static JDBAbbreviationMapper INSTANCE = null;
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
    
    public void updateComponents() {
        SwingUtilities.updateComponentTreeUI(this);
    }
    
    private JDBGenConfig conf;
    private DefaultTableModel mdl;

    /**
     * Creates new form JDBAbbreviationMapper
     */
    public JDBAbbreviationMapper(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        
        TableColumnModel colModel = tblMapping.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(50);
        colModel.getColumn(1).setPreferredWidth(130);
        colModel.getColumn(2).setPreferredWidth(130);
        
        applyIcons();
        
        pack();
        
        conf = JDBGenConfig.getInstance();
        
        mdl = (DefaultTableModel)tblMapping.getModel();
        
        mdl.setRowCount(0);
        
        conf.getAbbrs().forEach(a -> mdl.addRow(a.getRowArray()));
        mdl.addRow(new Object[]{Boolean.FALSE, "", ""});
        
        tblMapping.getModel().addTableModelListener((evt) -> {
            if (checkDuplication(mdl)) {
                conf.setAbbrs(applyTableToList(mdl));
                UIUtils.tableSetLastEmpty(tblMapping.getModel(), 1);
            }
        });
        UIUtils.setCommitOnLostFocus(tblMapping);
        
    }
    
    private boolean checkDuplication(DefaultTableModel model) {
        HashMap<String,String> map = new HashMap<>();
        for (int i=0; i<model.getRowCount(); i++) {
            Boolean check = (Boolean)model.getValueAt(i, 0);
            String k = (String)model.getValueAt(i, 1);
            String v = (String)model.getValueAt(i, 2);
            if (check && !StrUtils.isEmpty(k) && !StrUtils.isEmpty(v)) {
                if (map.containsKey(k)) {
                    UIUtils.error(this, "'"+k+"' duplicated.");
                    return false;
                } else {
                    map.put(k, v);
                }
            }
        }
        return true;
    }
    
    private void applyIcons() {
        UIUtils.addIcon(btnOk, FontAwesome.CHECK);
        UIUtils.applyIcon(btnDel, FontAwesome.MINUS);
    }    

    public static List<JDBAbbr> applyTableToList(TableModel model) {
        List<JDBAbbr> abbrs = new ArrayList<>();
        for (int i=0; i<model.getRowCount(); i++) {
            Boolean check = (Boolean)model.getValueAt(i, 0);
            String k = (String)model.getValueAt(i, 1);
            String v = (String)model.getValueAt(i, 2);
            if (!StrUtils.isEmpty(k) && !StrUtils.isEmpty(v))
                abbrs.add(new JDBAbbr(check, k, v));
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
        jLabel1.setText("Abbreviation Mapping");

        tblMapping.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null}
            },
            new String [] {
                "Apply", "Abbreviation", "Replace To"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tblMapping.setToolTipText("Abbreviation Mapping Rules applied to object(table, column) name.");
        jScrollPane1.setViewportView(tblMapping);

        btnOk.setText("Ok");
        btnOk.setToolTipText("");
        btnOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkActionPerformed(evt);
            }
        });

        btnDel.setText("-");
        btnDel.setToolTipText("Remove selected mapping");
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

    private void btnOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOkActionPerformed
        JDBGenConfig.saveInstance(this);
        setVisible(false);
    }//GEN-LAST:event_btnOkActionPerformed

    private void btnDelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelActionPerformed
        int idx = tblMapping.getSelectedRow();
        if (idx > -1) {
            JDBAbbr abbr = conf.getAbbrs().get(idx);
            if (UIUtils.confirm(this, "Confirm", "Do you want to delete "+abbr+"?")) {
                mdl.removeRow(idx);
                conf.getAbbrs().remove(idx);
            }
        }
    }//GEN-LAST:event_btnDelActionPerformed

    /**
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

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
import comart.tools.jdbgen.types.JDBGenConfig;
import comart.tools.jdbgen.types.JDBPreset;
import comart.tools.jdbgen.types.JDBTemplate;
import comart.utils.PlatformUtils;
import comart.utils.UIUtils;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import jiconfont.icons.font_awesome.FontAwesome;

/**
 *
 * @author comart
 */
public class JDBPresets extends JDialog {

    private final DefaultTableModel templateModel;
    private final DefaultListModel presetModel;
    
    private final JDBGenConfig conf = JDBGenConfig.getInstance();
    
    private final List<JDBPreset> presets = conf.getPresets();
    
    private final JTable connTpls;
    /**
     * Creates new form JDBPresets
     * @param connTpls
     */
    public JDBPresets(JTable connTpls) {
        initComponents();
        applyIcons();
        
        this.connTpls = connTpls;
        
        templateModel = (DefaultTableModel)tabTemplates.getModel();
        presetModel = new DefaultListModel();
        //presetModel = (DefaultListModel)lstPresets.getModel();
        lstPresets.setModel(presetModel);
        
        presets.forEach(p -> presetModel.addElement(p.getName()));
        
        tabTemplates.getSelectionModel().addListSelectionListener(e -> {
            int row = tabTemplates.getSelectedRow();
            setTemplate(row);
        });

        this.addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowClosing(WindowEvent e) {
//                btnCancelActionPerformed(null);
//            }
            @Override
            public void windowActivated(WindowEvent e) {
                toFront();
            }
        });
    }
    
    private void applyIcons() {
        UIUtils.applyIcon(btnNew, FontAwesome.PLUS);
        UIUtils.applyIcon(btnClone, FontAwesome.CLONE);
        UIUtils.applyIcon(btnDel, FontAwesome.MINUS);

        UIUtils.applyIcon(btnTemplateHelp, FontAwesome.QUESTION);
        UIUtils.applyIcon(btnBrowseTemplate, FontAwesome.FOLDER_O);
        UIUtils.addIcon(btnNewTemplate, FontAwesome.FILE);
        UIUtils.addIcon(btnDelTemplate, FontAwesome.MINUS);
        UIUtils.addIcon(btnSaveTemplate, FontAwesome.ARROW_UP);
        
        UIUtils.addIcon(btnApply, FontAwesome.ANGLE_DOUBLE_DOWN);
        UIUtils.addIcon(btnNewFromConn, FontAwesome.ANGLE_DOUBLE_UP);
        UIUtils.addIcon(btnOk, FontAwesome.CHECK);
    }
    
    private void setTemplate(int row) {
        if (row > -1) {
            txtTemplateName.setText((String)tabTemplates.getValueAt(row, 0));
            txtTemplateFile.setText((String)tabTemplates.getValueAt(row, 1));
            txtOutTemplate.setText((String)tabTemplates.getValueAt(row, 2));
        } else {
            txtTemplateName.setText("");
            txtTemplateFile.setText("");
            txtOutTemplate.setText("");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnOk = new javax.swing.JButton();
        splPreset = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstPresets = new javax.swing.JList<>();
        jPanel3 = new javax.swing.JPanel();
        btnNew = new javax.swing.JButton();
        btnClone = new javax.swing.JButton();
        btnDel = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        txtPresetName = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        tabTemplates = new javax.swing.JTable();
        btnTemplateHelp = new javax.swing.JButton();
        btnNewTemplate = new javax.swing.JButton();
        btnDelTemplate = new javax.swing.JButton();
        btnSaveTemplate = new javax.swing.JButton();
        txtTemplateName = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        txtTemplateFile = new javax.swing.JTextField();
        btnBrowseTemplate = new javax.swing.JButton();
        txtOutTemplate = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        btnApply = new javax.swing.JButton();
        btnNewFromConn = new javax.swing.JButton();

        btnOk.setText("Ok");
        btnOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkActionPerformed(evt);
            }
        });

        splPreset.setDividerLocation(200);
        splPreset.setDividerSize(6);
        splPreset.setResizeWeight(1.0);

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD, jLabel1.getFont().getSize()+4));
        jLabel1.setText("Presets");

        lstPresets.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstPresets.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstPresetsValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(lstPresets);

        jPanel3.setLayout(new java.awt.GridLayout(1, 0));

        btnNew.setText("+");
        btnNew.setPreferredSize(new java.awt.Dimension(23, 26));
        btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewActionPerformed(evt);
            }
        });
        jPanel3.add(btnNew);

        btnClone.setText("C");
        btnClone.setPreferredSize(new java.awt.Dimension(23, 26));
        btnClone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloneActionPerformed(evt);
            }
        });
        jPanel3.add(btnClone);

        btnDel.setText("-");
        btnDel.setPreferredSize(new java.awt.Dimension(23, 26));
        btnDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelActionPerformed(evt);
            }
        });
        jPanel3.add(btnDel);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        splPreset.setLeftComponent(jPanel1);

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getStyle() | java.awt.Font.BOLD, jLabel2.getFont().getSize()+4));
        jLabel2.setText("Preset Detail");

        jLabel3.setText("Preset Name:");

        tabTemplates.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Template Name", "Template File", "Out Template"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tabTemplates.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(tabTemplates);

        btnTemplateHelp.setText("?");
        btnTemplateHelp.setPreferredSize(new java.awt.Dimension(30, 26));
        btnTemplateHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTemplateHelpActionPerformed(evt);
            }
        });

        btnNewTemplate.setText("New");
        btnNewTemplate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewTemplateActionPerformed(evt);
            }
        });

        btnDelTemplate.setText("Delete");
        btnDelTemplate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelTemplateActionPerformed(evt);
            }
        });

        btnSaveTemplate.setText("Apply");
        btnSaveTemplate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveTemplateActionPerformed(evt);
            }
        });

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("Template Name:");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("Template File:");

        btnBrowseTemplate.setText("...");
        btnBrowseTemplate.setPreferredSize(new java.awt.Dimension(30, 26));

        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel13.setText("Output Name Template:");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPresetName))
            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addComponent(txtTemplateFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowseTemplate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(txtOutTemplate)
                    .addComponent(txtTemplateName)))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addComponent(btnTemplateHelp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 125, Short.MAX_VALUE)
                .addComponent(btnNewTemplate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnDelTemplate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSaveTemplate))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel13, jLabel8, jLabel9});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtPresetName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 225, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSaveTemplate)
                    .addComponent(btnDelTemplate)
                    .addComponent(btnNewTemplate)
                    .addComponent(btnTemplateHelp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel9)
                    .addComponent(txtTemplateName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnBrowseTemplate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTemplateFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtOutTemplate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        splPreset.setRightComponent(jPanel2);

        btnApply.setText("Apply to Current Connection");
        btnApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnApplyActionPerformed(evt);
            }
        });

        btnNewFromConn.setText("New Preset from Current Connection");
        btnNewFromConn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewFromConnActionPerformed(evt);
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
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnNewFromConn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnApply)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnOk))
                    .addComponent(splPreset))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(splPreset)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnOk)
                    .addComponent(btnApply)
                    .addComponent(btnNewFromConn))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnNewFromConnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewFromConnActionPerformed
        // clear selection
        btnNewActionPerformed(evt);
        for (int i=0; i<connTpls.getRowCount(); i++) {
            String name = (String)connTpls.getValueAt(i, 0);
            String tplf = (String)connTpls.getValueAt(i, 1);
            String otpl = (String)connTpls.getValueAt(i, 2);
            templateModel.addRow(new Object[]{ name, tplf, otpl });
        }
    }//GEN-LAST:event_btnNewFromConnActionPerformed

    private void btnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewActionPerformed
        lstPresets.setSelectedIndex(-1);
        while (templateModel.getRowCount() > 0) templateModel.removeRow(0);
        setTemplate(-1);
        txtPresetName.setText(NamingUtils.nextNameOf(presets, "New Preset"));
    }//GEN-LAST:event_btnNewActionPerformed

    private void btnCloneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloneActionPerformed
        int idx = lstPresets.getSelectedIndex();
        if (idx > -1) {
            try {
                JDBPreset npreset = (JDBPreset)presets.get(idx).clone();
                npreset.setName(NamingUtils.nextNameOf(
                        presets, "Copy of "+npreset.getName()));
                presets.add(npreset);
                presetModel.addElement(npreset.getName());
                lstPresets.setSelectedIndex(presets.size()-1);
            } catch(Exception ignored) {}
        }
    }//GEN-LAST:event_btnCloneActionPerformed

    private void btnDelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelActionPerformed
        int idx = lstPresets.getSelectedIndex();
        if (idx > -1) {
            boolean isDel = UIUtils.confirm(this, "Delete Preset",
                    "Do you want to delete '"+this.txtPresetName+"'?");
            if (isDel) {
                presets.remove(idx);
                presetModel.remove(idx);
            }
        }
    }//GEN-LAST:event_btnDelActionPerformed

    private void btnTemplateHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTemplateHelpActionPerformed
        PlatformUtils.openURL("");
    }//GEN-LAST:event_btnTemplateHelpActionPerformed

    private void btnNewTemplateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewTemplateActionPerformed
        tabTemplates.clearSelection();
    }//GEN-LAST:event_btnNewTemplateActionPerformed

    private void btnDelTemplateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelTemplateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnDelTemplateActionPerformed

    private void btnSaveTemplateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveTemplateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSaveTemplateActionPerformed

    private void btnApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnApplyActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnApplyActionPerformed

    private void btnOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOkActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnOkActionPerformed

    private void lstPresetsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstPresetsValueChanged
        // TODO add your handling code here:
        int idx = lstPresets.getSelectedIndex();
        if (idx > -1) {
            JDBPreset preset = presets.get(idx);
            this.txtPresetName.setText(preset.getName());
            while (templateModel.getRowCount() > 0) templateModel.removeRow(0);
            preset.getTemplates().forEach(t -> templateModel.addRow(t.getRowArray()));
        }
    }//GEN-LAST:event_lstPresetsValueChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        UIUtils.setFlatDarkLaf();
        EventQueue.invokeLater(() -> {
            JDBPresets gm = new JDBPresets(null);
            gm.setLocationRelativeTo(null);
            gm.setVisible(true);
            System.exit(0);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnApply;
    private javax.swing.JButton btnBrowseTemplate;
    private javax.swing.JButton btnClone;
    private javax.swing.JButton btnDel;
    private javax.swing.JButton btnDelTemplate;
    private javax.swing.JButton btnNew;
    private javax.swing.JButton btnNewFromConn;
    private javax.swing.JButton btnNewTemplate;
    private javax.swing.JButton btnOk;
    private javax.swing.JButton btnSaveTemplate;
    private javax.swing.JButton btnTemplateHelp;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JList<String> lstPresets;
    private javax.swing.JSplitPane splPreset;
    private javax.swing.JTable tabTemplates;
    private javax.swing.JTextField txtOutTemplate;
    private javax.swing.JTextField txtPresetName;
    private javax.swing.JTextField txtTemplateFile;
    private javax.swing.JTextField txtTemplateName;
    // End of variables declaration//GEN-END:variables
}

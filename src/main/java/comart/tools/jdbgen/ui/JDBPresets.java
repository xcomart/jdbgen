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

import comart.tools.jdbgen.types.JDBGenConfig;
import comart.tools.jdbgen.types.JDBPreset;
import comart.tools.jdbgen.types.JDBTemplate;
import comart.utils.AppDirs;
import comart.utils.I18n;
import comart.utils.StrUtils;
import comart.utils.UIUtils;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import jiconfont.icons.font_awesome.FontAwesome;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;

/**
 * preset management dialog. A preset is a named list of
 * <code>JDBTemplate</code> entries that is stored in the application
 * configuration and can be reused by any connection. The left side lists the
 * presets, the right side edits the templates of the selected one.
 * <p>
 * The dialog is opened on top of the connection editor and keeps a reference to
 * that editor's template table, so the selected preset can be copied down into
 * the connection and the connection's templates can be lifted up into a new
 * preset.
 *
 * @author comart
 */
@Slf4j
public class JDBPresets extends JDialog {

    /**
     * the model of the template table of the selected preset.
     */
    private final DefaultTableModel templateModel;
    /**
     * the model behind the preset list, holding the preset names.
     */
    private final DefaultListModel presetModel;
    /**
     * the presets by name, used by the list cell renderer to find the icon
     * of an entry.
     */
    private final HashMap<String, JDBPreset> presetMap;
    
    /**
     * the configuration the presets are read from and written back to.
     */
    private final JDBGenConfig conf = JDBGenConfig.getInstance();
    
    /**
     * the live preset list of the configuration. Every edit of this dialog
     * goes into this list, which is what gets saved.
     */
    private final List<JDBPreset> presets = conf.getPresets();
    
    /**
     * the template table of the connection editor that opened this dialog,
     * or <code>null</code> when the dialog runs stand-alone.
     */
    private final JTable connTpls;
    /**
     * index of the template name column of {@link #connTpls}. The template
     * file and the output name follow it. The columns before it - the tick
     * column of the main window - are set to <code>TRUE</code> when a preset is
     * applied, so that an applied preset is generated right away.
     */
    private final int connBaseIdx;

    /**
     * Creates new form JDBPresets. The dialog is filled with the presets of the
     * current configuration and stays connected to the given table for the
     * apply/import buttons.
     *
     * @param connTpls
     *            the template table of the window that opened this dialog. Its
     *            rows are the source of "new preset from the connection" and
     *            the destination of "apply preset". May be <code>null</code>
     *            when the dialog is opened stand-alone, in which case those two
     *            buttons must not be used.
     * @param connBaseIdx
     *            index of the template name column of that table; the template
     *            file and the output name are the two columns after it.
     */
    public JDBPresets(JTable connTpls, int connBaseIdx) {
        initComponents();
        this.connBaseIdx = connBaseIdx;
        applyIcons();
        applyTooltips();

        this.connTpls = connTpls;
        applyTemplateHeaders();

        templateModel = (DefaultTableModel)tabTemplates.getModel();
        presetModel = new DefaultListModel();
        presetMap = new HashMap<>();
        //presetModel = (DefaultListModel)lstPresets.getModel();
        lstPresets.setModel(presetModel);
        presets.forEach(p -> presetMap.put(p.getName(), p));
        presets.forEach(p -> presetModel.addElement(p.getName()));
        lstPresets.setCellRenderer(UIUtils.getListCellRenderer(s -> presetMap.get(s)));
        
        tabTemplates.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabTemplates.getSelectionModel().addListSelectionListener(e -> {
            int row = tabTemplates.getSelectedRow();
            setTemplate(row);
        });
        
        UIUtils.templateHelpAction(btnTemplateHelp);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                toFront();
            }
        });
        UIUtils.setApplicationIcon(this);
        this.pack();
    }
    
    /**
     * apply the font icons of every button of this dialog.
     */
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
        UIUtils.addIcon(btnSave, FontAwesome.FLOPPY_O);
        UIUtils.addIcon(btnCancel, FontAwesome.TIMES);
    }

    /**
     * warn about the one button of this dialog that throws work away: applying
     * a preset replaces the template list of the connection instead of adding
     * to it.
     */
    private void applyTooltips() {
        btnApply.setToolTipText(I18n.t("presets.btnApply.toolTipText"));
    }

    /**
     * The column names of the generated table model are the untranslated
     * placeholders of the form editor, the shown ones are set here.
     */
    private void applyTemplateHeaders() {
        String[] keys = {
            "presets.tabTemplates.column.templateName",
            "presets.tabTemplates.column.templateFile",
            "presets.tabTemplates.column.outTemplate"
        };
        TableColumnModel colModel = tabTemplates.getColumnModel();
        for (int i=0; i<keys.length; i++)
            colModel.getColumn(i).setHeaderValue(I18n.t(keys[i]));
    }

    /**
     * show the template of the given table row in the editor fields below the
     * table, or clear them when nothing is selected.
     *
     * @param row
     *            the selected row of <code>tabTemplates</code>, or a negative
     *            value for "no selection".
     */
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
     * create the components of the dialog and lay them out. The dialog is a
     * split pane with the preset list on the left and the editor of the
     * selected preset on the right, above the row of dialog buttons.
     */
    private void initComponents() {
        btnCancel = new JButton();
        splPreset = new JSplitPane();
        btnApply = new JButton();
        btnNewFromConn = new JButton();
        btnSave = new JButton();

        btnCancel.setText(I18n.t("presets.btnCancel.text"));
        btnCancel.addActionListener(this::btnCancelActionPerformed);

        splPreset.setDividerLocation(200);
        splPreset.setDividerSize(6);
        splPreset.setResizeWeight(1.0);
        splPreset.setLeftComponent(createPresetList());
        splPreset.setRightComponent(createPresetEditor());

        btnApply.setText(I18n.t("presets.btnApply.text"));
        btnApply.addActionListener(this::btnApplyActionPerformed);

        btnNewFromConn.setText(I18n.t("presets.btnNewFromConn.text"));
        btnNewFromConn.addActionListener(this::btnNewFromConnActionPerformed);

        btnSave.setText(I18n.t("presets.btnSave.text"));
        btnSave.addActionListener(this::btnSaveActionPerformed);

        // the split pane fills the dialog and sets its width, the buttons are
        // pushed to the right of the row below it.
        getContentPane().setLayout(new MigLayout(
                "insets dialog, fill", "[grow]", "[grow][]"));
        getContentPane().add(splPreset, "grow, push, wrap");
        getContentPane().add(btnNewFromConn, "split 4, gapbefore push");
        getContentPane().add(btnApply);
        getContentPane().add(btnSave);
        getContentPane().add(btnCancel);

        pack();
    }

    /**
     * build the left half of the split pane: the heading, the list of the
     * stored presets and the strip of buttons which add, copy and remove a
     * preset.
     *
     * @return the panel to be shown left of the divider.
     */
    private JPanel createPresetList() {
        jPanel1 = new JPanel();
        jLabel1 = new JLabel();
        jScrollPane1 = new JScrollPane();
        lstPresets = new JList<>();
        jPanel3 = new JPanel();
        btnNew = new JButton();
        btnClone = new JButton();
        btnDel = new JButton();

        jLabel1.setFont(jLabel1.getFont().deriveFont(
                jLabel1.getFont().getStyle() | Font.BOLD,
                jLabel1.getFont().getSize()+4));
        jLabel1.setText(I18n.t("presets.jLabel1.text"));

        lstPresets.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstPresets.addListSelectionListener(this::lstPresetsValueChanged);
        jScrollPane1.setViewportView(lstPresets);

        btnNew.setText("+");
        btnNew.addActionListener(this::btnNewActionPerformed);

        btnClone.setText("C");
        btnClone.addActionListener(this::btnCloneActionPerformed);

        btnDel.setText("-");
        btnDel.addActionListener(this::btnDelActionPerformed);

        // the three buttons share the width of the list in equal parts
        jPanel3.setLayout(new MigLayout("insets 0, gap 0, fillx",
                "[grow,fill][grow,fill][grow,fill]", "[]"));
        jPanel3.add(btnNew, "sg presetBtn");
        jPanel3.add(btnClone, "sg presetBtn");
        jPanel3.add(btnDel, "sg presetBtn");

        // heading, list and button strip; only the list grows. The size of the
        // list is the one the form was designed with.
        jPanel1.setLayout(new MigLayout("insets 0, fill, wrap 1", "[grow]", "[][grow][]"));
        jPanel1.add(jLabel1);
        jPanel1.add(jScrollPane1, "grow, push, w :208:, h :415:");
        jPanel1.add(jPanel3, "growx");
        return jPanel1;
    }

    /**
     * build the right half of the split pane: the heading above the editor of
     * the selected preset.
     *
     * @return the panel to be shown right of the divider.
     */
    private JPanel createPresetEditor() {
        jPanel2 = new JPanel();
        jLabel2 = new JLabel();

        jLabel2.setFont(jLabel2.getFont().deriveFont(
                jLabel2.getFont().getStyle() | Font.BOLD,
                jLabel2.getFont().getSize()+4));
        jLabel2.setText(I18n.t("presets.jLabel2.text"));

        jPanel2.setLayout(new MigLayout("insets 0, fill, wrap 1", "[grow]", "[][grow]"));
        jPanel2.add(jLabel2);
        jPanel2.add(createTemplateEditor(), "grow, push");
        return jPanel2;
    }

    /**
     * build the editor of the selected preset: its name, the table of its
     * templates, the buttons acting on that table and the fields describing
     * the template selected in it.
     *
     * @return the panel holding the preset editor.
     */
    private JPanel createTemplateEditor() {
        jPanel4 = new JPanel();
        jLabel3 = new JLabel();
        txtPresetName = new JTextField();
        jScrollPane3 = new JScrollPane();
        tabTemplates = new JTable();
        btnTemplateHelp = new JButton();
        btnNewTemplate = new JButton();
        btnDelTemplate = new JButton();
        btnSaveTemplate = new JButton();
        txtTemplateName = new JTextField();
        jLabel9 = new JLabel();
        jLabel8 = new JLabel();
        txtTemplateFile = new JTextField();
        btnBrowseTemplate = new JButton();
        txtOutTemplate = new JTextField();
        jLabel13 = new JLabel();

        jLabel3.setText(I18n.t("presets.jLabel3.text"));

        // the rows are filled from the selected preset, the column names are
        // translated by applyTemplateHeaders()
        tabTemplates.setModel(new DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Template Name", "Template File", "Out Template"
            }
        ) {
            final Class[] types = new Class [] {
                String.class, String.class, String.class
            };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        });
        tabTemplates.getTableHeader().setReorderingAllowed(false);
        tabTemplates.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent evt) {
                tabTemplatesMouseMoved(evt);
            }
        });
        jScrollPane3.setViewportView(tabTemplates);

        // the help action is attached by UIUtils.templateHelpAction()
        btnTemplateHelp.setText("?");

        btnNewTemplate.setText(I18n.t("presets.btnNewTemplate.text"));
        btnNewTemplate.addActionListener(this::btnNewTemplateActionPerformed);

        btnDelTemplate.setText(I18n.t("presets.btnDelTemplate.text"));
        btnDelTemplate.addActionListener(this::btnDelTemplateActionPerformed);

        btnSaveTemplate.setText(I18n.t("presets.btnSaveTemplate.text"));
        btnSaveTemplate.addActionListener(this::btnSaveTemplateActionPerformed);

        jLabel9.setHorizontalAlignment(SwingConstants.TRAILING);
        jLabel9.setText(I18n.t("presets.jLabel9.text"));

        jLabel8.setHorizontalAlignment(SwingConstants.TRAILING);
        jLabel8.setText(I18n.t("presets.jLabel8.text"));

        btnBrowseTemplate.setText("...");
        btnBrowseTemplate.addActionListener(this::browseTemplateFile);

        jLabel13.setHorizontalAlignment(SwingConstants.TRAILING);
        jLabel13.setText(I18n.t("presets.jLabel13.text"));

        // a label column, a growing field column and the column of the browse
        // button. The name of the preset spans all three, so its label keeps
        // its own width, while the three labels of the template fields share
        // the label column and are right aligned in it. Only the template
        // table grows; it asks for a modest width instead of the viewport a
        // table wants by default.
        jPanel4.setLayout(new MigLayout("insets 0, fill", "[][grow][]",
                "[baseline][grow][][baseline][baseline][baseline]"));
        jPanel4.add(jLabel3, "span 3, split 2");
        jPanel4.add(txtPresetName, "growx, wrap");
        jPanel4.add(jScrollPane3, "span 3, grow, push, w 0:300:, h :235:, wrap");
        jPanel4.add(btnTemplateHelp, "span 3, split 4");
        jPanel4.add(btnNewTemplate, "gapbefore push");
        jPanel4.add(btnDelTemplate);
        jPanel4.add(btnSaveTemplate, "wrap");
        jPanel4.add(jLabel9, "growx");
        jPanel4.add(txtTemplateName, "span 2, growx, wrap");
        jPanel4.add(jLabel8, "growx");
        jPanel4.add(txtTemplateFile, "growx");
        jPanel4.add(btnBrowseTemplate, "wrap");
        jPanel4.add(jLabel13, "growx");
        jPanel4.add(txtOutTemplate, "span 2, growx");
        return jPanel4;
    }

    /**
     * create a new preset from the templates of the connection editor.
     */
    private void btnNewFromConnActionPerformed(ActionEvent evt) {
        // clear selection
        btnNewActionPerformed(evt);
        for (int i=0; i<connTpls.getRowCount(); i++) {
            String name = (String)connTpls.getValueAt(i, connBaseIdx);
            String tplf = (String)connTpls.getValueAt(i, connBaseIdx+1);
            String otpl = (String)connTpls.getValueAt(i, connBaseIdx+2);
            templateModel.addRow(new Object[]{ name, tplf, otpl });
        }
    }

    /**
     * add an empty preset with a generated name and select it.
     */
    private void btnNewActionPerformed(ActionEvent evt) {
        JDBPreset preset = new JDBPreset();
        preset.setName(NamingUtils.nextNameOf(presets, I18n.t("presets.msg.newName")));
        preset.setTemplates(new ArrayList<>());
        presets.add(preset);
        presetMap.put(preset.getName(), preset);
        presetModel.addElement(preset.getName());
        lstPresets.setSelectedIndex(presets.size()-1);
    }

    /**
     * add a copy of the selected preset and select it.
     */
    private void btnCloneActionPerformed(ActionEvent evt) {
        int idx = lstPresets.getSelectedIndex();
        if (idx > -1) {
            try {
                JDBPreset npreset = (JDBPreset)presets.get(idx).clone();
                npreset.setName(NamingUtils.nextNameOf(
                        presets, I18n.t("presets.msg.copyName", npreset.getName())));
                presets.add(npreset);
                presetMap.put(npreset.getName(), npreset);
                presetModel.addElement(npreset.getName());
                lstPresets.setSelectedIndex(presets.size()-1);
            } catch(Exception e) {
                log.error("cannot clone preset: " + e.getLocalizedMessage(), e);
                UIUtils.error(this, I18n.t("presets.msg.cloneFailed", e.getLocalizedMessage()));
            }
        }
    }

    /**
     * remove the selected preset after asking the user.
     */
    private void btnDelActionPerformed(ActionEvent evt) {
        int idx = lstPresets.getSelectedIndex();
        if (idx > -1) {
            JDBPreset target = presets.get(idx);
            boolean isDel = UIUtils.confirm(this, I18n.t("presets.msg.delete.title"),
                    I18n.t("presets.msg.delete", target.getName()));
            if (isDel) {
                presetMap.remove(target.getName());
                presets.remove(idx);
                presetModel.remove(idx);
                txtPresetName.setText("");
                templateModel.setRowCount(0);
                
                txtTemplateName.setText("");
                txtTemplateFile.setText("");
                txtOutTemplate.setText("");
            }
        }
    }

    /**
     * clear the template selection, so the editor fields describe a new one.
     */
    private void btnNewTemplateActionPerformed(ActionEvent evt) {
        tabTemplates.clearSelection();
    }

    /**
     * remove the selected template from the table.
     */
    private void btnDelTemplateActionPerformed(ActionEvent evt) {
        int idx = tabTemplates.getSelectedRow();
        if (idx > -1) {
            templateModel.removeRow(idx);
            tabTemplates.clearSelection();
        }
    }

    /**
     * write the template editor fields back into the template table. An
     * existing selection is updated in place, otherwise a row is appended and
     * selected. An empty editor without a selection is not an error, it simply
     * means there is nothing to store.
     *
     * @return <code>true</code> if the table now reflects the editor,
     *         <code>false</code> if a required field was left empty, in which
     *         case the user has already been told about it.
     */
    private boolean saveTemplate() {
        int idx = tabTemplates.getSelectedRow();
        String tname = txtTemplateName.getText();
        String tfile = txtTemplateFile.getText();
        String outpl = txtOutTemplate.getText();
        if (idx < 0 && StrUtils.isEmpty(tname))
            return true;
        
        JComponent[] targets = new JComponent[] {
            txtTemplateName, txtTemplateFile, txtOutTemplate
        };
        if (!UIUtils.checkNotEmpty(this, targets))
            return false;
            
        if (idx > -1) {
            templateModel.setValueAt(tname, idx, 0);
            templateModel.setValueAt(tfile, idx, 1);
            templateModel.setValueAt(outpl, idx, 2);
        } else {
            templateModel.addRow(new String[]{tname, tfile, outpl});
            idx = templateModel.getRowCount() - 1;
            tabTemplates.setRowSelectionInterval(idx, idx);
        }
        return true;
    }
    
    /**
     * store the template editor fields into the template table.
     */
    private void btnSaveTemplateActionPerformed(ActionEvent evt) {
        saveTemplate();
    }

    /**
     * replace the templates of the connection editor with the ones shown here.
     */
    private void btnApplyActionPerformed(ActionEvent evt) {
        DefaultTableModel connTplModel = (DefaultTableModel)connTpls.getModel();
        connTplModel.setRowCount(0);
        for (int i=0; i<tabTemplates.getRowCount(); i++) {
            Object[] row = new Object[connBaseIdx + 3];
            // the columns before the name are the tick of the main window: a
            // preset is applied to be generated, so it comes in ticked
            for (int c=0; c<connBaseIdx; c++)
                row[c] = Boolean.TRUE;
            row[connBaseIdx] = tabTemplates.getValueAt(i, 0);
            row[connBaseIdx+1] = tabTemplates.getValueAt(i, 1);
            row[connBaseIdx+2] = tabTemplates.getValueAt(i, 2);
            connTplModel.addRow(row);
        }
    }

    /**
     * close the dialog, discarding whatever has not been saved.
     */
    private void btnCancelActionPerformed(ActionEvent evt) {
        setVisible(false);
    }

    /**
     * load the selected preset into the name field and the template table.
     */
    private void lstPresetsValueChanged(ListSelectionEvent evt) {
        int idx = lstPresets.getSelectedIndex();
        if (idx > -1) {
            JDBPreset preset = presets.get(idx);
            this.txtPresetName.setText(preset.getName());
            while (templateModel.getRowCount() > 0) templateModel.removeRow(0);
            preset.getTemplates().forEach(t -> templateModel.addRow(t.getRowArray()));
        }
    }

    /**
     * store the edited preset and persist the configuration. The pending
     * template edit is saved first, then the name is checked for emptiness and
     * for collisions with the other presets. On success the preset receives the
     * rows of the template table and the configuration is written to disk.
     *
     * @return <code>true</code> if the preset was stored,
     *         <code>false</code> if a validation error was reported to the
     *         user instead.
     */
    private boolean savePreset() {
        if (!saveTemplate())
            return false;
        int idx = lstPresets.getSelectedIndex();
        boolean isNameExists;
        JDBPreset target = null;
        if (idx > -1) {
            target = presets.get(idx);
            isNameExists = !target.getName().equals(txtPresetName.getText()) &&
                    NamingUtils.nameExists(presets, txtPresetName.getText());
        } else {
            target = new JDBPreset();
            isNameExists = NamingUtils.nameExists(presets, txtPresetName.getText());
        }
        
        if (isNameExists) {
            UIUtils.error(this, I18n.t("presets.msg.nameExists", txtPresetName.getText()));
            txtPresetName.requestFocusInWindow();
        } else if (StrUtils.isEmpty(txtPresetName.getText())) {
            UIUtils.error(this, I18n.t("presets.msg.nameRequired"));
            txtPresetName.requestFocusInWindow();
        } else {
            presetMap.remove(target.getName());
            target.setName(txtPresetName.getText());
            target.setIcon("FA:paw");
            ArrayList<JDBTemplate> tpls = new ArrayList<>();
            for (int i=0; i<tabTemplates.getRowCount(); i++) {
                String name = (String)tabTemplates.getValueAt(i, 0);
                String tplf = (String)tabTemplates.getValueAt(i, 1);
                String otpl = (String)tabTemplates.getValueAt(i, 2);
                tpls.add(new JDBTemplate(name, tplf, otpl));
            }
            target.setTemplates(tpls);
            presetMap.put(target.getName(), target);
            if (idx < 0) {
                presets.add(target);
                presetModel.addElement(target.getName());
                lstPresets.setSelectedIndex(presets.size() - 1);
            } else {
                // name may have been changed, keep the list model in sync
                presetModel.set(idx, target.getName());
            }
            JDBGenConfig.saveInstance(this);
            return true;
        }
        return false;
    }
    
    /**
     * store the edited preset and save the configuration.
     */
    private void btnSaveActionPerformed(ActionEvent evt) {
        savePreset();
    }

    /**
     * show the template of the hovered row as a tooltip.
     */
    private void tabTemplatesMouseMoved(MouseEvent evt) {
        UIUtils.templateTooltip(tabTemplates, 0, evt);
    }

    /**
     * pick the template file of the edited template with a file dialog, which
     * opens in the bundled templates directory.
     *
     * @param evt
     *            ignored.
     */
    private void browseTemplateFile(ActionEvent evt) {
        String path = UIUtils.openFileDlg(this,
                AppDirs.installResourceFile("templates").getAbsolutePath(), true);
        if (!StrUtils.isEmpty(path))
            txtTemplateFile.setText(path);
    }

    /** left half of the dialog, holding the list of the stored presets. */
    private JPanel jPanel1;
    /** heading of the preset list. */
    private JLabel jLabel1;
    /** scroll pane around the preset list. */
    private JScrollPane jScrollPane1;
    /** the names of the stored presets. */
    private JList<String> lstPresets;
    /** strip of buttons acting on the preset list. */
    private JPanel jPanel3;
    /** button adding an empty preset. */
    private JButton btnNew;
    /** button copying the selected preset. */
    private JButton btnClone;
    /** button removing the selected preset. */
    private JButton btnDel;
    /** right half of the dialog, holding the editor of the selected preset. */
    private JPanel jPanel2;
    /** heading of the preset editor. */
    private JLabel jLabel2;
    /** the preset editor below its heading. */
    private JPanel jPanel4;
    /** caption of the preset name. */
    private JLabel jLabel3;
    /** name of the edited preset. */
    private JTextField txtPresetName;
    /** scroll pane around the template table. */
    private JScrollPane jScrollPane3;
    /** the templates of the edited preset, one per row. */
    private JTable tabTemplates;
    /** button opening the help on the template variables. */
    private JButton btnTemplateHelp;
    /** button clearing the template fields for a new template. */
    private JButton btnNewTemplate;
    /** button removing the selected template. */
    private JButton btnDelTemplate;
    /** button storing the template fields into the table. */
    private JButton btnSaveTemplate;
    /** caption of the template name. */
    private JLabel jLabel9;
    /** name of the selected template. */
    private JTextField txtTemplateName;
    /** caption of the template file. */
    private JLabel jLabel8;
    /** file of the selected template. */
    private JTextField txtTemplateFile;
    /** button choosing the template file. */
    private JButton btnBrowseTemplate;
    /** caption of the output name template. */
    private JLabel jLabel13;
    /** output name template of the selected template. */
    private JTextField txtOutTemplate;
    /** the split pane dividing the preset list from the preset editor. */
    private JSplitPane splPreset;
    /** button copying the shown templates into the connection editor. */
    private JButton btnApply;
    /** button creating a preset from the templates of the connection editor. */
    private JButton btnNewFromConn;
    /** button saving the edited preset. */
    private JButton btnSave;
    /** button closing the dialog. */
    private JButton btnCancel;
}

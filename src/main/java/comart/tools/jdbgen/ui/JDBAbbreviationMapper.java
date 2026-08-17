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
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import jiconfont.icons.font_awesome.FontAwesome;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;

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
public class JDBAbbreviationMapper extends JDialog {


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
    public JDBAbbreviationMapper(Frame parent, boolean modal) {
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
     * create the components of the dialog and lay them out. The heading sits
     * above the mapping table, which fills the dialog, and the delete and ok
     * buttons share the bottom row at its two ends.
     */
    private void initComponents() {
        jLabel1 = new JLabel();
        jScrollPane1 = new JScrollPane();
        tblMapping = new JTable();
        btnOk = new JButton();
        btnDel = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setFont(jLabel1.getFont().deriveFont(
                jLabel1.getFont().getStyle() | Font.BOLD,
                jLabel1.getFont().getSize()+4));
        jLabel1.setText(I18n.t("abbreviationMapper.jLabel1.text"));

        // the rows are filled from the configuration by the constructor, the
        // single design time row only gives the table its size in the layout.
        tblMapping.setModel(new DefaultTableModel(
            new Object [][] {
                {null, null, null, null}
            },
            new String [] {
                "Apply", "Total Name", "Abbreviation", "Replace To"
            }
        ) {
            final Class[] types = new Class [] {
                Boolean.class, Boolean.class, String.class, String.class
            };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tblMapping.setToolTipText(I18n.t("abbreviationMapper.tblMapping.toolTipText"));
        tblMapping.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                showPopupTrigger(evt);
            }
        });
        jScrollPane1.setViewportView(tblMapping);

        btnOk.setText(I18n.t("abbreviationMapper.btnOk.text"));
        btnOk.setToolTipText("");
        btnOk.addActionListener(this::btnOkActionPerformed);

        btnDel.setText("-");
        btnDel.setToolTipText(I18n.t("abbreviationMapper.btnDel.toolTipText"));
        btnDel.addActionListener(this::btnDelActionPerformed);

        // heading and table span both columns, the button row uses them to
        // keep the delete button left and the ok button right. The size of the
        // table is the one the form was designed with.
        getContentPane().setLayout(new MigLayout(
                "insets dialog, fill", "[grow][]", "[][grow][]"));
        getContentPane().add(jLabel1, "span 2, wrap");
        getContentPane().add(jScrollPane1, "span 2, grow, push, w :513:, h :310:, wrap");
        getContentPane().add(btnDel, "align left");
        getContentPane().add(btnOk, "align right");

        pack();
    }

    /** save the configuration and hide the dialog. */
    private void btnOkActionPerformed(ActionEvent evt) {
        JDBGenConfig.saveInstance(this);
        setVisible(false);
    }

    /** ask for confirmation and remove the selected mapping row. */
    private void btnDelActionPerformed(ActionEvent evt) {
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
    }

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
    private void showPopupTrigger(MouseEvent evt) {
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
    
    /** heading of the dialog. */
    private JLabel jLabel1;
    /** scroll pane around the mapping table. */
    private JScrollPane jScrollPane1;
    /** table holding one abbreviation mapping per row. */
    private JTable tblMapping;
    /** button removing the selected mapping. */
    private JButton btnDel;
    /** button saving the configuration and closing the dialog. */
    private JButton btnOk;
}

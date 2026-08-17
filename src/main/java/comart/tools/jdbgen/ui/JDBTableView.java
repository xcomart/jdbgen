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

import comart.tools.jdbgen.types.db.DBTable;
import comart.utils.I18n;
import comart.utils.UIUtils;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * modal dialog showing the structure of a single database table. The table
 * name and its remarks are shown as header, the columns of the table are
 * listed with their ordinal number, name, type and primary key flag, and the
 * remarks of a column are shown as tool tip while the mouse hovers its row.
 *
 * @author comart
 */
public class JDBTableView extends JDialog {
    
    /** table shown by this dialog, kept to look up the column remarks. */
    private final DBTable table;
    
    /**
     * Creates new form JDBTableView
     * <p>
     * The dialog is always modal. The design time rows of the column table are
     * dropped and replaced by one row per column of <code>table</code>, the
     * column headers are translated and the preferred column widths are
     * applied.
     *
     * @param parent
     *            frame the dialog belongs to.
     * @param table
     *            table whose name, remarks and columns are shown, also used
     *            to look up the column remarks shown as tool tip.
     */
    public JDBTableView(Frame parent, DBTable table) {
        super(parent, true);
        initComponents();
        this.table = table;
        lblTableName.setText("<html>"+table.getTable()+"</html>");
        lblRemark.setText("<html>"+table.getRemarks()+"</html>");

        tabColumns.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabColumns.getColumnModel().getColumn(0).setPreferredWidth(50);
        tabColumns.getColumnModel().getColumn(1).setPreferredWidth(200);
        tabColumns.getColumnModel().getColumn(2).setPreferredWidth(150);
        tabColumns.getColumnModel().getColumn(3).setPreferredWidth(50);
        // the column titles come from the form's design time table model, which
        // cannot hold custom code, so they are translated here.
        tabColumns.getColumnModel().getColumn(1).setHeaderValue(I18n.t("tableView.column.name"));
        tabColumns.getColumnModel().getColumn(2).setHeaderValue(I18n.t("tableView.column.type"));
        tabColumns.getColumnModel().getColumn(3).setHeaderValue(I18n.t("tableView.column.key"));
        
        DefaultTableModel model = (DefaultTableModel)tabColumns.getModel();
        while (model.getRowCount() > 0)
            model.removeRow(0);
        
        //this.tabColumns.setModel(model);
        table.getColumns().forEach(c -> {
            List<Object> row = new ArrayList<>();
            row.add(c.getNo());
            row.add(c.getColumn());
            row.add(c.getTypeString());
            row.add(c.isKey());
            model.addRow(row.toArray());
        });
        UIUtils.setApplicationIcon(this);
        this.pack();
    }

    /**
     * create the components of the dialog and lay them out. The table name and
     * its remarks share the first row, the column table fills the middle and
     * the close button sits in the lower right corner.
     */
    private void initComponents() {
        jScrollPane1 = new JScrollPane();
        tabColumns = new JTable();
        lblTableName = new JLabel();
        btnClose = new JButton();
        lblRemark = new JLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // the rows are replaced by the columns of the shown table, the design
        // time rows only give the table its size in the layout.
        tabColumns.setModel(new DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "#", "Name", "Type", "Key"
            }
        ) {
            final Class[] types = new Class [] {
                Integer.class, String.class, String.class, Boolean.class
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
        tabColumns.getTableHeader().setReorderingAllowed(false);
        tabColumns.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent evt) {
                tabColumnsMouseMoved(evt);
            }
        });
        jScrollPane1.setViewportView(tabColumns);

        lblTableName.setFont(lblTableName.getFont().deriveFont(
                lblTableName.getFont().getStyle() | Font.BOLD,
                lblTableName.getFont().getSize()+7));
        lblTableName.setText(I18n.t("tableView.lblTableName.text"));

        btnClose.setText(I18n.t("tableView.btnClose.text"));
        btnClose.addActionListener(this::btnCloseActionPerformed);

        lblRemark.setText(I18n.t("tableView.lblRemark.text"));
        lblRemark.setVerticalAlignment(SwingConstants.TOP);

        // name and remarks share the head row on a common baseline, the column
        // table below takes every additional pixel and the close button is
        // pushed to the right edge of the dialog.
        getContentPane().setLayout(new MigLayout(
                "insets dialog, fill", "[][grow]", "[baseline][grow][]"));
        getContentPane().add(lblTableName, "gapright 18");
        getContentPane().add(lblRemark, "growx, wrap");
        getContentPane().add(jScrollPane1, "span 2, grow, push, w :475:, h :263:, wrap");
        getContentPane().add(btnClose, "span 2, align right");

        pack();
    }

    /** hide the dialog when the close button is pressed. */
    private void btnCloseActionPerformed(ActionEvent evt) {
        this.setVisible(false);
    }

    /** show the remarks of the hovered column as tool tip of the column table. */
    private void tabColumnsMouseMoved(MouseEvent evt) {
        Point p = evt.getPoint();
        int row = tabColumns.rowAtPoint(p);
        if (row < 0 || row >= table.getColumns().size()) {
            tabColumns.setToolTipText(null);
            return;
        }
        tabColumns.setToolTipText(table.getColumns().get(row).getRemarks());
    }

    /** name of the shown table. */
    private JLabel lblTableName;
    /** remarks of the shown table, next to its name. */
    private JLabel lblRemark;
    /** scroll pane around the column table. */
    private JScrollPane jScrollPane1;
    /** table listing the columns of the shown table. */
    private JTable tabColumns;
    /** button closing the dialog. */
    private JButton btnClose;
}

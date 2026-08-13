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

import comart.utils.ObjUtils;
import comart.utils.UIUtils;
import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * tree cell renderer for the database schema tree of the main window. Nodes
 * whose user object is a plain <code>String</code>(the grouping nodes) are
 * rendered with their text only, every other node is assumed to be a schema
 * object and is rendered with the schema icon and with the value of its
 * <code>name</code> property as label.
 *
 * @author comart
 */
public class SchemaCellRenderer extends DefaultTreeCellRenderer {

    // same icon as FontAwesome.WINDOW_RESTORE, but resolved through the shared
    // icon cache instead of being rebuilt(and registered) on every paint.
    /**
     * icon key of the schema nodes, resolved through
     * <code>UIUtils.getIcon(String)</code>.
     */
    private static final String SCHEMA_ICON = "fa:window_restore";

    /**
     * configure and return the component used to draw the given tree node.
     * The node text is taken from the user object when it is a
     * <code>String</code>, otherwise the schema icon is applied and the text
     * is read from the <code>name</code> property of the user object, leaving
     * the inherited text untouched when that property cannot be read.
     *
     * @param tree
     *            tree the cell belongs to.
     * @param value
     *            node to be rendered, a <code>DefaultMutableTreeNode</code>.
     * @param sel
     *            <code>true</code> if the node is selected.
     * @param expanded
     *            <code>true</code> if the node is expanded.
     * @param leaf
     *            <code>true</code> if the node has no children.
     * @param row
     *            row index of the node.
     * @param hasFocus
     *            <code>true</code> if the node currently has the focus.
     * @return this renderer, configured for the given node.
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object uobj = node.getUserObject();
        if (uobj instanceof String) {
            setText((String) uobj);
        } else {
            setIcon(UIUtils.getIcon(SCHEMA_ICON));
            try {
                setText(ObjUtils.getValue(uobj, "name").toString());
            } catch(Throwable ignored) {}
        }
        return this;
    }
}

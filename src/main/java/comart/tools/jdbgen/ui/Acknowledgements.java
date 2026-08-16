/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package comart.tools.jdbgen.ui;

import comart.utils.I18n;
import comart.utils.UIUtils;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;

/**
 * acknowledgements dialog, reachable from the about dialog. The dialog shows
 * the read only content of the bundled <code>/acknowledgements.txt</code>
 * resource, which lists the third party works used by the application. A
 * single shared instance is kept and reused.
 *
 * @author comart
 */
@Slf4j
public class Acknowledgements extends JDialog {

    /** the shared dialog instance, created on the first call of
     * <code>getInstance(Frame)</code>. */
    private static Acknowledgements INSTANCE = null;
    /**
     * return the shared acknowledgements dialog. The dialog is created as a
     * modal dialog of <code>parent</code> and registered for look and feel
     * updates on the first call, later calls reuse that instance regardless of
     * <code>parent</code>. The application icon and the component tree are
     * refreshed on every call.
     *
     * @param parent
     *            frame the dialog is created for, used on the first call only.
     * @return the shared <code>Acknowledgements</code> instance.
     */
    public static synchronized Acknowledgements getInstance(Frame parent) {
        if (INSTANCE == null) {
            INSTANCE = new Acknowledgements(parent, true);
            UIUtils.registerFrame(INSTANCE);
        }
        UIUtils.setApplicationIcon(INSTANCE);

        INSTANCE.updateComponents();
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
    
    /**
     * Creates new form Acknowledgements
     * <p>
     * The dialog is centered on <code>parent</code> and the bundled
     * <code>/acknowledgements.txt</code> resource is read as UTF-8 into the
     * read only text area, an unreadable resource is logged and leaves the
     * text area empty.
     *
     * @param parent
     *            frame the dialog belongs to and is centered on.
     * @param modal
     *            <code>true</code> to create a modal dialog.
     */
    private Acknowledgements(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        
        setLocationRelativeTo(parent);
        
        StringBuilder sb = new StringBuilder();
        
        try (InputStream is = Acknowledgements.class.getResourceAsStream("/acknowledgements.txt");
            InputStreamReader rd = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            
            int c;
            while ((c = rd.read()) >= 0) {
                sb.append((char)c);
            }
        } catch (Exception e) {
            log.error("cannot read acknowledges.", e);
        }
        txtContents.setText(sb.toString());
        this.pack();
    }

    /**
     * create the components of the dialog and lay them out. The heading, the
     * read only contents and the ok button sit in one column, the contents
     * take every pixel the dialog gains and the button stays in the lower
     * right corner.
     */
    private void initComponents() {
        jLabel1 = new JLabel();
        jScrollPane1 = new JScrollPane();
        txtContents = new JTextArea();
        btnOk = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        jLabel1.setText(I18n.t("acknowledgements.jLabel1.text"));

        txtContents.setEditable(false);
        txtContents.setColumns(20);
        txtContents.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtContents.setRows(5);
        jScrollPane1.setViewportView(txtContents);

        btnOk.setText(I18n.t("acknowledgements.btnOk.text"));
        btnOk.addActionListener(this::btnOkActionPerformed);

        // one component per row; only the contents row grows. The size of the
        // text area is the one the form was designed with.
        getContentPane().setLayout(new MigLayout(
                "insets dialog, fill, wrap 1", "[grow]", "[][grow][]"));
        getContentPane().add(jLabel1);
        getContentPane().add(jScrollPane1, "grow, push, w :698:, h :498:");
        getContentPane().add(btnOk, "align right");

        pack();
    }

    /** hide the dialog when the ok button is pressed. */
    private void btnOkActionPerformed(ActionEvent evt) {
        this.setVisible(false);
    }

    /** heading of the dialog. */
    private JLabel jLabel1;
    /** scroll pane around the acknowledgement contents. */
    private JScrollPane jScrollPane1;
    /** read only text area showing the bundled acknowledgements. */
    private JTextArea txtContents;
    /** button closing the dialog. */
    private JButton btnOk;
}

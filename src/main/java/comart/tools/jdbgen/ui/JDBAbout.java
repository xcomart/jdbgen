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

import comart.utils.I18n;
import comart.utils.PlatformUtils;
import comart.utils.UIUtils;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/**
 * about dialog of the application. The dialog shows the application icon, the
 * name and the running version, and offers clickable links to the author mail
 * address and to the project page as well as a button which opens the
 * acknowledgements dialog. A single shared instance is kept and reused.
 *
 * @author comart
 */
public class JDBAbout extends JDialog {
    
    /** logger of this dialog, used when the application icon cannot be read. */
    private static final Logger logger = Logger.getLogger(JDBAbout.class.getName());


    /** the shared dialog instance, created on the first call of
     * <code>getInstance(Frame)</code>. */
    private static JDBAbout INSTANCE = null;
    /**
     * return the shared about dialog. The dialog is created as a modal dialog
     * of <code>parent</code> and registered for look and feel updates on the
     * first call, later calls reuse that instance. The application icon and
     * the component tree are refreshed and the dialog is centered on
     * <code>parent</code> on every call.
     *
     * @param parent
     *            frame the dialog is centered on, used as owner on the first
     *            call.
     * @return the shared <code>JDBAbout</code> instance.
     */
    public static synchronized JDBAbout getInstance(Frame parent) {
        if (INSTANCE == null) {
            INSTANCE = new JDBAbout(parent, true);
            UIUtils.registerFrame(INSTANCE);
        }
        UIUtils.setApplicationIcon(INSTANCE);

        INSTANCE.updateComponents();
        INSTANCE.setLocationRelativeTo(parent);
        return INSTANCE;
    }
    
    /**
     * reapply the current look and feel to the whole dialog. Called after a
     * theme or font change, the accent color of the mail and project links is
     * taken from the new look and feel afterwards.
     */
    public void updateComponents() {
        SwingUtilities.updateComponentTreeUI(this);
        lblEmail.setForeground(UIManager.getDefaults().getColor("Component.accentColor"));
        lblGithub.setForeground(UIManager.getDefaults().getColor("Component.accentColor"));
    }
    
    /**
     * Creates new form JDBAbout
     * <p>
     * The version label is filled with the running application version and the
     * application icon resource is loaded and scaled into the image label.
     *
     * @param parent
     *            frame the dialog belongs to.
     * @param modal
     *            <code>true</code> to create a modal dialog.
     */
    public JDBAbout(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        
        lblVersion.setText(I18n.t("about.lblVersion.text", PlatformUtils.getVersion()));
        loadAppIcon();
        pack();
    }
    
    /**
     * read the bundled application icon and show it in the image label. The
     * image is scaled smoothly to the size of the label, a resource which
     * cannot be read is logged and leaves the label empty.
     */
    private void loadAppIcon() {
        try (InputStream is = getClass().getResourceAsStream("/icons/generic.png") ){
            BufferedImage img = ImageIO.read(is);
            Image dimg = img.getScaledInstance(lblImage.getWidth(), lblImage.getHeight(),
                Image.SCALE_SMOOTH);
            lblImage.setIcon(new ImageIcon(dimg));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "", e);
        }
    }

    /**
     * create the components of the dialog and lay them out. The upper half
     * shows the application icon next to the name, the version and the two
     * links, the lower half holds the separated button bar.
     */
    private void initComponents() {
        jPanel3 = new JPanel();
        lblImage = new JLabel();
        jPanel1 = new JPanel();
        jLabel1 = new JLabel();
        lblVersion = new JLabel();
        jLabel2 = new JLabel();
        lblEmail = new JLabel();
        jLabel3 = new JLabel();
        lblGithub = new JLabel();
        jPanel4 = new JPanel();
        btnOk = new JButton();
        jSeparator1 = new JSeparator();
        btnAck = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        jLabel1.setFont(jLabel1.getFont().deriveFont(
                jLabel1.getFont().getStyle() | Font.BOLD,
                jLabel1.getFont().getSize()+35));
        jLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabel1.setText("JDBGen");

        lblVersion.setFont(lblVersion.getFont().deriveFont(lblVersion.getFont().getSize()+7f));
        lblVersion.setHorizontalAlignment(SwingConstants.RIGHT);
        lblVersion.setText("Version v0.1.4");

        jLabel2.setText(I18n.t("about.jLabel2.text"));

        lblEmail.setForeground(UIManager.getDefaults().getColor("Component.accentColor"));
        lblEmail.setText("<xcomart@gmail.com>");
        lblEmail.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblEmail.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                lblEmailMouseClicked(evt);
            }
        });

        jLabel3.setText("github:");

        lblGithub.setForeground(UIManager.getDefaults().getColor("Component.accentColor"));
        lblGithub.setText("https://github.com/xcomart/jdbgen");
        lblGithub.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblGithub.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                lblGithubMouseClicked(evt);
            }
        });

        // the text block: name and version on their own rows, the mail address
        // and the project link as a caption and value pair each. Everything is
        // aligned to the right edge and stays at the top of the block.
        jPanel1.setLayout(new MigLayout(
                "insets dialog, fillx, aligny top", "[grow]", "[]18[]18[][]"));
        jPanel1.add(jLabel1, "growx, wrap");
        jPanel1.add(lblVersion, "growx, wrap");
        jPanel1.add(jLabel2, "split 2, gapbefore push");
        jPanel1.add(lblEmail, "wrap");
        jPanel1.add(jLabel3, "split 2, gapbefore push");
        jPanel1.add(lblGithub);

        // the icon keeps its designed size at the top left - it is scaled to
        // that size by loadAppIcon() once this method has packed the dialog -
        // and the text block takes the rest of the upper half.
        jPanel3.setLayout(new MigLayout("insets 31 30 11 11, fill", "[]18[grow]", "[grow]"));
        jPanel3.add(lblImage, "w 183!, h 172!, aligny top, gapbottom 0:18:push");
        jPanel3.add(jPanel1, "grow, push");

        btnOk.setText(I18n.t("about.btnOk.text"));
        btnOk.addActionListener(this::btnOkActionPerformed);

        btnAck.setText(I18n.t("about.btnAck.text"));
        btnAck.addActionListener(this::btnAckActionPerformed);

        // the separator runs across the whole dialog, so the button bar has no
        // side insets of its own and the buttons carry their gap themselves.
        // The gap between them is the one of the form and keeps the dialog
        // from becoming narrower than it was designed.
        jPanel4.setLayout(new MigLayout("insets 11 0 11 0, fillx", "[grow]", "[][]"));
        jPanel4.add(jSeparator1, "growx, wrap");
        jPanel4.add(btnAck, "split 2, gapleft 11");
        jPanel4.add(btnOk, "gapbefore rel:288:push, gapright 11");

        getContentPane().setLayout(new MigLayout(
                "insets dialog, fill, wrap 1", "[grow]", "[grow][]"));
        getContentPane().add(jPanel3, "grow, push");
        getContentPane().add(jPanel4, "growx");

        pack();
    }

    /** open the author mail address in the default mail client. */
    private void lblEmailMouseClicked(MouseEvent evt) {
        PlatformUtils.openURL("mailto:xcomart@gmail.com");
    }

    /** show the acknowledgements dialog, modal and centered on this dialog. */
    private void btnAckActionPerformed(ActionEvent evt) {
        JFrame dummy = new JFrame();
        Acknowledgements ack = Acknowledgements.getInstance(dummy);
        ack.setLocationRelativeTo(this);
        ack.setModal(true);
        ack.setVisible(true);
    }

    /** hide the dialog when the ok button is pressed. */
    private void btnOkActionPerformed(ActionEvent evt) {
        setVisible(false);
    }

    /** open the project page shown by the label in the default browser. */
    private void lblGithubMouseClicked(MouseEvent evt) {
        PlatformUtils.openURL(lblGithub.getText());
    }

    /** upper half of the dialog, holding the icon and the text block. */
    private JPanel jPanel3;
    /** label showing the scaled application icon. */
    private JLabel lblImage;
    /** text block with the application name, the version and the links. */
    private JPanel jPanel1;
    /** the application name. */
    private JLabel jLabel1;
    /** the running application version. */
    private JLabel lblVersion;
    /** caption of the mail address. */
    private JLabel jLabel2;
    /** clickable mail address of the author. */
    private JLabel lblEmail;
    /** caption of the project page. */
    private JLabel jLabel3;
    /** clickable address of the project page. */
    private JLabel lblGithub;
    /** button bar at the bottom of the dialog. */
    private JPanel jPanel4;
    /** line separating the button bar from the rest of the dialog. */
    private JSeparator jSeparator1;
    /** button opening the acknowledgements dialog. */
    private JButton btnAck;
    /** button closing the dialog. */
    private JButton btnOk;
}

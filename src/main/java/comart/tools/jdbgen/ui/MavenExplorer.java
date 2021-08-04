/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.ui;

import comart.tools.jdbgen.types.maven.MavenSearchItem;
import comart.utils.MavenUtils;
import comart.utils.PlatformUtils;
import comart.utils.UIUtils;
import java.awt.EventQueue;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import jiconfont.icons.font_awesome.FontAwesome;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author comart
 */
public class MavenExplorer extends JDialog {
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private static MavenExplorer INSTANCE = null;
    public static synchronized MavenExplorer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MavenExplorer();
            UIUtils.registerFrame(INSTANCE);
        }

        INSTANCE.updateComponents();
        INSTANCE.changed = false;
        return INSTANCE;
    }
    public boolean changed = false;
    public String saveLocation = "";
    
    private String searchText = "";
    private int searchPageNo = 1;
    
    private final DefaultListModel<String> searchModel;
    private final DefaultListModel<String> versionModel;
    
    private final List<MavenSearchItem> searchItems = new ArrayList<>();
    
    public void updateComponents() {
        SwingUtilities.updateComponentTreeUI(this);
        lblMvnLink.setForeground(UIManager.getDefaults().getColor("Actions.Blue"));
        lstSearchResult.setCellRenderer(UIUtils.getListCellRenderer(
                s -> searchItems.stream()
                        .filter(d -> s.equals(d.getName()))
                        .findFirst().orElse(null)));
        clearSearch();
    }
    
    
    /**
     * Creates new form MavenExplorer
     */
    public MavenExplorer() {
        initComponents();
        setModal(true);
        
        searchModel = new DefaultListModel<>();
        versionModel = new DefaultListModel<>();
        lstSearchResult.setModel(searchModel);
        lstVersion.setModel(versionModel);
        
        applyIcons();
        eventSetup();
    }

    private void applyIcons() {
        UIUtils.applyIcon(btnSearch, FontAwesome.SEARCH);
        UIUtils.addIcon(btnCancel, FontAwesome.TIMES);
        UIUtils.addIcon(btnDownload, FontAwesome.DOWNLOAD);
        UIUtils.addIcon(btnMore, FontAwesome.PLUS);
    }

    private void clearSearch() {
        searchItems.clear();
        searchModel.removeAllElements();
        versionModel.removeAllElements();
        cboRepository.removeAllItems();
        searchPageNo = 1;
    }

    private void eventSetup() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                btnCancelActionPerformed(null);
            }
        });
        
        cboRepository.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String repo = (String)e.getItem();
                int idx = lstSearchResult.getSelectedIndex();
                if (idx > -1 && StringUtils.isNotBlank(repo)) {
                    EventQueue.invokeLater(() -> {
                        MavenSearchItem sitem = searchItems.get(idx);
                        versionModel.removeAllElements();
                        try {
                            List<String> versions = MavenUtils.getVersions(sitem, repo);
                            versions.forEach(v -> versionModel.addElement(v));
                        } catch (ParseException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                    });
                }
            }
        });
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
        txtSearch = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        btnDownload = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        lstSearchResult = new javax.swing.JList<>();
        btnMore = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        cboRepository = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        lstVersion = new javax.swing.JList<>();
        jLabel2 = new javax.swing.JLabel();
        lblMvnLink = new javax.swing.JLabel();

        setTitle("Maven Repository Explorer");

        jLabel1.setText("Search in Maven Repositories:");

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtSearchKeyPressed(evt);
            }
        });

        btnSearch.setText("O");
        btnSearch.setPreferredSize(new java.awt.Dimension(30, 26));
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        btnDownload.setText("Download & Use");
        btnDownload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDownloadActionPerformed(evt);
            }
        });

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getStyle() | java.awt.Font.BOLD, jLabel3.getFont().getSize()+3));
        jLabel3.setText("Search Results");

        lstSearchResult.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                lstSearchResultMouseMoved(evt);
            }
        });
        lstSearchResult.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstSearchResultValueChanged(evt);
            }
        });
        jScrollPane5.setViewportView(lstSearchResult);

        btnMore.setText("More");
        btnMore.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMoreActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 333, Short.MAX_VALUE)
            .addComponent(btnMore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnMore))
        );

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() | java.awt.Font.BOLD, jLabel4.getFont().getSize()+3));
        jLabel4.setText("Repositories");

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getStyle() | java.awt.Font.BOLD, jLabel5.getFont().getSize()+3));
        jLabel5.setText("Versions");

        jScrollPane6.setViewportView(lstVersion);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(cboRepository, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cboRepository, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 393, Short.MAX_VALUE))
        );

        jLabel2.setText("Powered by");

        lblMvnLink.setForeground(javax.swing.UIManager.getDefaults().getColor("Objects.Blue"));
        lblMvnLink.setText("mvnrepository.com");
        lblMvnLink.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblMvnLink.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblMvnLinkMouseClicked(evt);
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSearch)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblMvnLink)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnDownload)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancel))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancel)
                    .addComponent(btnDownload)
                    .addComponent(jLabel2)
                    .addComponent(lblMvnLink))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void lstSearchResultMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstSearchResultMouseMoved
        int idx = lstSearchResult.locationToIndex(evt.getPoint());
        if (idx > -1)
            lstSearchResult.setToolTipText(searchItems.get(idx).getToolTip());
        else
            lstSearchResult.setToolTipText(null);
    }//GEN-LAST:event_lstSearchResultMouseMoved

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed

    private void txtSearchKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER)
            btnSearchActionPerformed(null);
    }//GEN-LAST:event_txtSearchKeyPressed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        if (StringUtils.isBlank(txtSearch.getText()))
            return;
        searchText = txtSearch.getText();
        clearSearch();
        searchMaven();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnMoreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMoreActionPerformed
        if (StringUtils.isBlank(searchText))
            return;
        searchPageNo++;
        searchMaven();
    }//GEN-LAST:event_btnMoreActionPerformed

    private void lstSearchResultValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstSearchResultValueChanged
        int idx = lstSearchResult.getSelectedIndex();
        if (idx > -1 && !evt.getValueIsAdjusting()) {
            EventQueue.invokeLater(() -> {
                MavenSearchItem sitem = searchItems.get(idx);
                cboRepository.removeAllItems();
                try {
                    List<String> repos = MavenUtils.getRepositories(sitem);
                    repos.forEach(r -> cboRepository.addItem(r));
                } catch (ParseException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    UIUtils.error(this, ex.getLocalizedMessage());
                }
            });
        }
    }//GEN-LAST:event_lstSearchResultValueChanged

    private void lblMvnLinkMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblMvnLinkMouseClicked
        PlatformUtils.openURL("https://mvnrepository.com");
    }//GEN-LAST:event_lblMvnLinkMouseClicked

    private void btnDownloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDownloadActionPerformed
        int sidx = lstSearchResult.getSelectedIndex();
        int vidx = lstVersion.getSelectedIndex();
        if (sidx > -1 && vidx > -1) {
            MavenSearchItem sitem = searchItems.get(sidx);
            String version = lstVersion.getSelectedValue();
            EventQueue.invokeLater(() -> {
                try {
                    String url = MavenUtils.getDownLink(sitem, version);
                    String fname = "drivers/" + url.substring(url.lastIndexOf('/') + 1);
                    OkHttpClient client = MavenUtils.getHttpClient();
                    Request req = new Request.Builder().url(url).build();
                    try (Response response = client.newCall(req).execute()) {
                        byte[] data = response.body().bytes();
                        File f = new File(fname);
                        FileUtils.forceMkdirParent(f);
                        FileUtils.writeByteArrayToFile(f, data);
                        saveLocation = fname;
                        changed = true;
                        setVisible(false);
                    }
                } catch(Exception e) {
                    logger.log(Level.SEVERE, null, e);
                    UIUtils.error(this, e.getLocalizedMessage());
                }
            });
        } else {
            UIUtils.error(this, "Select version to download");
        }
    }//GEN-LAST:event_btnDownloadActionPerformed

    private void searchMaven() {
        EventQueue.invokeLater(() -> {
            try {
                if (StringUtils.isBlank(searchText))
                    return;
                List<MavenSearchItem> items = MavenUtils.searchMaven(searchText, searchPageNo);
                searchItems.addAll(items);
                items.forEach(i -> searchModel.addElement(i.getName()));
            } catch (ParseException ex) {
                logger.log(Level.SEVERE, null, ex);
                UIUtils.error(this, ex.getLocalizedMessage());
            }
        });
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        UIUtils.setFlatDarkLaf();
        EventQueue.invokeLater(() -> {
            MavenExplorer instance = getInstance();
            instance.setLocationRelativeTo(null);
            instance.setVisible(true);
            System.exit(0);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnDownload;
    private javax.swing.JButton btnMore;
    private javax.swing.JButton btnSearch;
    private javax.swing.JComboBox<String> cboRepository;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JLabel lblMvnLink;
    private javax.swing.JList<String> lstSearchResult;
    private javax.swing.JList<String> lstVersion;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}

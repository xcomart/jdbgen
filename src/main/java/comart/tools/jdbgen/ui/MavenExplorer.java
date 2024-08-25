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

import comart.tools.jdbgen.types.maven.SearchResponseItem;
import comart.tools.jdbgen.types.maven.SearchResult;
import comart.utils.HttpUtils;
import comart.utils.MavenREST;
import comart.utils.PlatformUtils;
import comart.utils.StrUtils;
import comart.utils.UIUtils;
import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
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
    private static final Logger logger = Logger.getLogger(MavenExplorer.class.getName());

    private static MavenExplorer INSTANCE = null;
    public static synchronized MavenExplorer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MavenExplorer();
            UIUtils.registerFrame(INSTANCE);
        }
        UIUtils.setApplicationIcon(INSTANCE);

        INSTANCE.updateComponents();
        INSTANCE.changed = false;
        return INSTANCE;
    }
    public boolean changed = false;
    public String saveLocation = "";
    
    private String searchText = "";
    private int searchPageNo = 0;
    private int searchTotal = 0;
    private int versionPageNo = 0;
    private int versionTotal = 0;
    
    private final DefaultListModel<String> searchModel;
    private final DefaultListModel<String> versionModel;
    
    private final List<SearchResponseItem> searchItems = new ArrayList<>();
    private final List<SearchResponseItem> versionItems = new ArrayList<>();
    
    public void updateComponents() {
        SwingUtilities.updateComponentTreeUI(this);
        lblMvnLink.setForeground(UIManager.getDefaults().getColor("Actions.Blue"));
        lstSearchResult.setCellRenderer(UIUtils.getListCellRenderer(
                s -> searchItems.stream()
                        .filter(d -> s.equals(d.getTitle()))
                        .findFirst().orElse(null)));
        clearSearch();
    }
    
    
    /**
     * Creates new form MavenExplorer
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public MavenExplorer() {
        initComponents();
        setModal(true);
        
        searchModel = new DefaultListModel<>();
        versionModel = new DefaultListModel<>();
        lstSearchResult.setModel(searchModel);
        lstVersion.setModel(versionModel);
        
        applyIcons();
        eventSetup();
        this.pack();
    }

    private void applyIcons() {
        UIUtils.applyIcon(btnSearch, FontAwesome.SEARCH);
        UIUtils.addIcon(btnCancel, FontAwesome.TIMES);
        UIUtils.addIcon(btnDownload, FontAwesome.DOWNLOAD);
        UIUtils.addIcon(btnMore, FontAwesome.PLUS);
        UIUtils.addIcon(btnMore1, FontAwesome.PLUS);
    }
    
    private void clearVersions() {
        versionItems.clear();
        versionModel.removeAllElements();
        lstVersion.removeAll();
        versionPageNo = 0;
        versionTotal = 0;
    }

    private void clearSearch() {
        searchItems.clear();
        searchModel.removeAllElements();
        lstSearchResult.removeAll();
        searchPageNo = 0;
        searchTotal = 0;
        clearVersions();
    }
    
    public void setQuery(String query) {
        txtSearch.setText(query);
        EventQueue.invokeLater(() -> btnSearchActionPerformed(null));
    }

    private void eventSetup() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                btnCancelActionPerformed(null);
            }
            @Override
            public void windowActivated(WindowEvent e) {
                toFront();
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
        jScrollPane6 = new javax.swing.JScrollPane();
        lstVersion = new javax.swing.JList<>();
        btnMore1 = new javax.swing.JButton();
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
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnMore))
        );

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() | java.awt.Font.BOLD, jLabel4.getFont().getSize()+3));
        jLabel4.setText("Versions");

        jScrollPane6.setViewportView(lstVersion);

        btnMore1.setText("More");
        btnMore1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMore1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel4)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(btnMore1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnMore1))
        );

        jLabel2.setText("Powered by");

        lblMvnLink.setForeground(javax.swing.UIManager.getDefaults().getColor("Objects.Blue"));
        lblMvnLink.setText("Apache Maven");
        lblMvnLink.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
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
        if (searchTotal > searchItems.size()) {
            searchPageNo++;
            searchMaven();
        } else {
            UIUtils.info(this, "No more results");
        }
    }//GEN-LAST:event_btnMoreActionPerformed

    private void lstSearchResultValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstSearchResultValueChanged
        int idx = lstSearchResult.getSelectedIndex();
        if (idx > -1 && !evt.getValueIsAdjusting()) {
            EventQueue.invokeLater(() -> {
                clearVersions();
                searchVersion();
            });
        }
    }//GEN-LAST:event_lstSearchResultValueChanged

    private void lblMvnLinkMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblMvnLinkMouseClicked
        PlatformUtils.openURL("https://maven.org");
    }//GEN-LAST:event_lblMvnLinkMouseClicked

    private ProcessProgress.Worker getProgressWorker() {
        return new ProcessProgress.Worker() {
            @Override
            protected Boolean doInBackground() throws Exception {
                int vidx = lstVersion.getSelectedIndex();
                SearchResponseItem sitem = versionItems.get(vidx);
                try {
                    String url = MavenREST.downloadLink(sitem);
                    String fname = "drivers/" + url.substring(url.lastIndexOf('/') + 1);
                    File f = new File(fname);
                    publish("driver saving to " + f.getAbsolutePath() + "...");
                    FileUtils.forceMkdirParent(f);
                    OkHttpClient client = HttpUtils.getClient();
                    Request req = new Request.Builder().url(url).build();
                    byte[] buffer = new byte[1024];
                    try (Response response = client.newCall(req).execute();
                            InputStream is = response.body().byteStream();
                            FileOutputStream fos = new FileOutputStream(fname)) {
                        long totallen = response.body().contentLength();
                        long curlen = 0;
                        int cnt;
                        while ((cnt = is.read(buffer)) > -1) {
                            if (cnt > 0) {
                                fos.write(buffer, 0, cnt);
                                curlen += cnt;
                                setProgress((int)(curlen * 100 / totallen));
                                publish("" + curlen + "/" + totallen + " bytes received.");
                            }
                        }
                        saveLocation = fname;
                        changed = true;
                        publish("Download complete.");
                        UIUtils.info(parent, "Download complete!");
                        return true;
                    }
                } catch(Exception e) {
                    logger.log(Level.SEVERE, null, e);
                    UIUtils.error(parent, e.getLocalizedMessage());
                }
                return false;
            }
        };
    }

    @SuppressWarnings("UseSpecificCatch")
    private void btnDownloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDownloadActionPerformed
        // We need to show modal dialog in front of another modal dialog.
        JFrame dummy = new JFrame();
        ProcessProgress pp = new ProcessProgress(dummy, true, getProgressWorker());
        pp.setModal(true);
        pp.setLocationRelativeTo(this);
        pp.start();
        pp.setVisible(true);
        if (pp.result) {
            setVisible(false);
        }
    }//GEN-LAST:event_btnDownloadActionPerformed

    private void btnMore1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMore1ActionPerformed
        if (versionTotal > versionItems.size()) {
            versionPageNo++;
            searchVersion();
        } else {
            UIUtils.info(this, "No more versions");
        }
    }//GEN-LAST:event_btnMore1ActionPerformed

    private void searchMaven() {
        EventQueue.invokeLater(() -> {
            try {
                if (StrUtils.isEmpty(searchText))
                    return;
                SearchResult sr = MavenREST.search(searchText, searchPageNo);
                searchTotal = sr.getResponse().getNumFound();
                List<SearchResponseItem> items = Arrays.asList(sr.getResponse().getDocs());
                searchItems.addAll(items);
                items.forEach(i -> searchModel.addElement(i.getTitle()));
                btnMore.setEnabled(searchTotal > searchItems.size());
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
                UIUtils.error(this, ex.getLocalizedMessage());
            }
        });
    }

    private void searchVersion() {
        int idx = lstSearchResult.getSelectedIndex();
        if (idx < 0)
            return;
        SearchResponseItem sitem = searchItems.get(idx);
        EventQueue.invokeLater(() -> {
            try {
                SearchResult sr = MavenREST.version(sitem, versionPageNo);
                versionTotal = sr.getResponse().getNumFound();
                List<SearchResponseItem> items = Arrays.asList(sr.getResponse().getDocs());
                versionItems.addAll(items);
                items.forEach(i -> versionModel.addElement(i.getTitle()));
                btnMore1.setEnabled(versionTotal > versionItems.size());
            } catch (Exception ex) {
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
    private javax.swing.JButton btnMore1;
    private javax.swing.JButton btnSearch;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
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

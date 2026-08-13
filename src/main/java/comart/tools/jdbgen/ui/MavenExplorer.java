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
import comart.utils.AppDirs;
import comart.utils.HttpUtils;
import comart.utils.I18n;
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
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import jiconfont.icons.font_awesome.FontAwesome;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * modal dialog which searches the maven central repository for a JDBC driver
 * and downloads its jar. The left list shows the artifacts matching the search
 * text, the right list the versions of the selected artifact, both are paged
 * and extended by their more button. The selected version is downloaded into
 * the drivers directory below the user data directory while a
 * <code>ProcessProgress</code> dialog reports the progress. A single shared
 * instance is kept and reused.
 *
 * @author comart
 */
@Slf4j
public class MavenExplorer extends JDialog {

    /** the shared dialog instance, created on the first call of
     * <code>getInstance()</code>. */
    private static MavenExplorer INSTANCE = null;
    /**
     * return the shared maven explorer dialog. The dialog is created and
     * registered for look and feel updates on the first call, later calls
     * reuse that instance. The application icon and the component tree are
     * refreshed, the search is cleared and <code>changed</code> is reset on
     * every call.
     *
     * @return the shared <code>MavenExplorer</code> instance.
     */
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
    /**
     * whether a driver jar has been downloaded since the dialog was obtained
     * from <code>getInstance()</code>. Read by the caller to find out whether
     * <code>saveLocation</code> holds a new download.
     */
    public boolean changed = false;
    /**
     * location of the downloaded jar, relative to the user data directory, in
     * the form <code>drivers/&lt;jar name&gt;</code>. Only meaningful when
     * <code>changed</code> is <code>true</code>.
     */
    public String saveLocation = "";
    
    /** text of the running artifact search, empty while nothing was searched. */
    private String searchText = "";
    /** zero based page of the artifact search, raised by the more button. */
    private int searchPageNo = 0;
    /** number of artifacts the repository reports for <code>searchText</code>. */
    private int searchTotal = 0;
    /** zero based page of the version search, raised by the more button. */
    private int versionPageNo = 0;
    /** number of versions the repository reports for the selected artifact. */
    private int versionTotal = 0;
    
    /** titles of the artifacts shown in the result list. */
    private final DefaultListModel<String> searchModel;
    /** titles of the versions shown in the version list. */
    private final DefaultListModel<String> versionModel;
    
    /** artifacts found so far, in the order of the result list. */
    private final List<SearchResponseItem> searchItems = new ArrayList<>();
    /** versions found so far, in the order of the version list. */
    private final List<SearchResponseItem> versionItems = new ArrayList<>();
    
    /**
     * reapply the current look and feel to the whole dialog. Called after a
     * theme or font change, the accent color of the maven link and the cell
     * renderer of the result list are installed again afterwards and the
     * search results are cleared.
     */
    public void updateComponents() {
        SwingUtilities.updateComponentTreeUI(this);
        lblMvnLink.setForeground(UIManager.getDefaults().getColor("Component.accentColor"));
        lstSearchResult.setCellRenderer(UIUtils.getListCellRenderer(
                s -> searchItems.stream()
                        .filter(d -> s.equals(d.getTitle()))
                        .findFirst().orElse(null)));
        clearSearch();
    }
    
    
    /**
     * Creates new form MavenExplorer
     * <p>
     * The dialog is made modal, the empty list models of the result and of the
     * version list are installed, the button icons are applied and a window
     * listener is registered which cancels the dialog when it is closed and
     * keeps it in front while it is active.
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

    /** apply the font icons of the search, cancel, download and more buttons. */
    private void applyIcons() {
        UIUtils.applyIcon(btnSearch, FontAwesome.SEARCH);
        UIUtils.addIcon(btnCancel, FontAwesome.TIMES);
        UIUtils.addIcon(btnDownload, FontAwesome.DOWNLOAD);
        UIUtils.addIcon(btnMore, FontAwesome.PLUS);
        UIUtils.addIcon(btnMore1, FontAwesome.PLUS);
    }
    
    /**
     * drop the version list and its paging state, so that the next version
     * search starts at the first page of the selected artifact.
     */
    private void clearVersions() {
        versionItems.clear();
        versionModel.removeAllElements();
        lstVersion.removeAll();
        versionPageNo = 0;
        versionTotal = 0;
    }

    /**
     * drop the artifact list and its paging state, together with the version
     * list, so that the next search starts at the first page.
     */
    private void clearSearch() {
        searchItems.clear();
        searchModel.removeAllElements();
        lstSearchResult.removeAll();
        searchPageNo = 0;
        searchTotal = 0;
        clearVersions();
    }
    
    /**
     * fill the search field and run that search. The search itself is
     * scheduled on the event dispatch thread, so it starts once the caller
     * returns, which lets the dialog be prepared before it is made visible.
     *
     * @param query
     *            text to be searched on maven central, a blank text runs no
     *            search at all.
     */
    public void setQuery(String query) {
        txtSearch.setText(query);
        EventQueue.invokeLater(() -> {
            btnSearchActionPerformed(null);
        });
    }

    /**
     * register the window listener of the dialog. Closing the window is
     * handled like the cancel button and the dialog is raised to the front
     * whenever it becomes active, as it may be shown above another modal
     * dialog.
     */
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

        setTitle(I18n.t("mavenExplorer.title"));

        jLabel1.setText(I18n.t("mavenExplorer.jLabel1.text"));

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

        btnCancel.setText(I18n.t("mavenExplorer.btnCancel.text"));
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        btnDownload.setText(I18n.t("mavenExplorer.btnDownload.text"));
        btnDownload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDownloadActionPerformed(evt);
            }
        });

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getStyle() | java.awt.Font.BOLD, jLabel3.getFont().getSize()+3));
        jLabel3.setText(I18n.t("mavenExplorer.jLabel3.text"));

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

        btnMore.setText(I18n.t("mavenExplorer.btnMore.text"));
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
        jLabel4.setText(I18n.t("mavenExplorer.jLabel4.text"));

        jScrollPane6.setViewportView(lstVersion);

        btnMore1.setText(I18n.t("mavenExplorer.btnMore1.text"));
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

        jLabel2.setText(I18n.t("mavenExplorer.jLabel2.text"));

        lblMvnLink.setForeground(javax.swing.UIManager.getDefaults().getColor("Component.accentColor"));
        lblMvnLink.setText("Apache Maven");
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

    /** show the details of the hovered artifact as tool tip of the result list. */
    private void lstSearchResultMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstSearchResultMouseMoved
        int idx = lstSearchResult.locationToIndex(evt.getPoint());
        if (idx > -1)
            lstSearchResult.setToolTipText(searchItems.get(idx).getToolTip());
        else
            lstSearchResult.setToolTipText(null);
    }//GEN-LAST:event_lstSearchResultMouseMoved

    /** hide the dialog without downloading anything. */
    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed

    /** run the search when enter is pressed in the search field. */
    private void txtSearchKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER)
            btnSearchActionPerformed(null);
    }//GEN-LAST:event_txtSearchKeyPressed

    /** search the first page of artifacts for the text of the search field. */
    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        if (StringUtils.isBlank(txtSearch.getText()))
            return;
        searchText = txtSearch.getText();
        clearSearch();
        searchMaven();
    }//GEN-LAST:event_btnSearchActionPerformed

    /** append the next page of artifacts, or report that there are no more. */
    private void btnMoreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMoreActionPerformed
        if (StringUtils.isBlank(searchText))
            return;
        if (searchTotal > searchItems.size()) {
            searchPageNo++;
            searchMaven();
        } else {
            UIUtils.info(this, I18n.t("mavenExplorer.msg.noMoreResults"));
        }
    }//GEN-LAST:event_btnMoreActionPerformed

    /** load the versions of the artifact selected in the result list. */
    private void lstSearchResultValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstSearchResultValueChanged
        int idx = lstSearchResult.getSelectedIndex();
        if (idx > -1 && !evt.getValueIsAdjusting()) {
            EventQueue.invokeLater(() -> {
                clearVersions();
                searchVersion();
            });
        }
    }//GEN-LAST:event_lstSearchResultValueChanged

    /** open the maven repository page in the default browser. */
    private void lblMvnLinkMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblMvnLinkMouseClicked
        PlatformUtils.openURL("https://maven.org");
    }//GEN-LAST:event_lblMvnLinkMouseClicked

    /**
     * build the background task which downloads the jar of the given version.
     * The task resolves the download link, streams the jar into the drivers
     * directory below the user data directory, reports its progress and stores
     * the relative location in <code>saveLocation</code> while setting
     * <code>changed</code> on success. Failures are logged, published to the
     * progress log and reported through <code>errHolder</code>.
     *
     * @param sitem the version selected on the EDT by the caller
     * @param errHolder receives the failure message, read by the caller on the EDT
     * @return a worker returning <code>true</code> when the jar was stored,
     *         <code>false</code> otherwise.
     */
    private ProcessProgress.Worker getProgressWorker(
            final SearchResponseItem sitem, final String[] errHolder) {
        return new ProcessProgress.Worker() {
            /**
             * stream the jar of the snapshotted version into the drivers
             * directory, publishing the received amount as it goes.
             *
             * @return <code>true</code> when the jar was written completely,
             *         <code>false</code> when the download failed.
             * @throws Exception
             *             never thrown, every failure is caught and reported
             *             through <code>errHolder</code>.
             */
            @Override
            protected Boolean doInBackground() throws Exception {
                // NOTE: this method must not touch any Swing component - the
                // selected item was snapshotted by the caller on the EDT.
                try {
                    String url = MavenREST.downloadLink(sitem);
                    // the jar goes below the user data directory - the
                    // installation may well be read only - and is stored
                    // relative to it, see AppDirs.resolve()
                    String jarName = url.substring(url.lastIndexOf('/') + 1);
                    String stored = AppDirs.DRIVERS_DIR + "/" + jarName;
                    File f = new File(AppDirs.driversDir(), jarName);
                    String fname = f.getAbsolutePath();
                    publish(I18n.t("mavenExplorer.progress.saving", fname));
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
                                // contentLength() is -1 for chunked responses
                                if (totallen > 0)
                                    setProgress((int)Math.min(100, curlen * 100 / totallen));
                                publish(I18n.t("mavenExplorer.progress.received", curlen, totallen));
                            }
                        }
                        saveLocation = stored;
                        changed = true;
                        publish(I18n.t("mavenExplorer.progress.complete"));
                        return true;
                    }
                } catch(Exception e) {
                    log.error(e.getLocalizedMessage(), e);
                    errHolder[0] = e.getLocalizedMessage();
                    publish(I18n.t("mavenExplorer.progress.failed", e.getLocalizedMessage()));
                }
                return false;
            }
        };
    }

    /** download the selected version while a modal progress dialog is shown. */
    @SuppressWarnings("UseSpecificCatch")
    private void btnDownloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDownloadActionPerformed
        int vidx = lstVersion.getSelectedIndex();
        if (vidx < 0 || vidx >= versionItems.size()) {
            UIUtils.error(this, I18n.t("mavenExplorer.msg.selectVersion"));
            return;
        }
        SearchResponseItem sitem = versionItems.get(vidx);
        final String[] err = new String[1];
        // We need to show modal dialog in front of another modal dialog.
        JFrame dummy = new JFrame();
        ProcessProgress pp = new ProcessProgress(dummy, true, getProgressWorker(sitem, err));
        pp.setModal(true);
        pp.setLocationRelativeTo(this);
        pp.start();
        // modal - returns once the worker's done() hides the dialog
        pp.setVisible(true);
        if (pp.result) {
            UIUtils.info(this, I18n.t("mavenExplorer.msg.downloadComplete"));
            setVisible(false);
        } else {
            UIUtils.error(this, err[0] == null
                    ? I18n.t("mavenExplorer.msg.downloadFailed") : err[0]);
        }
    }//GEN-LAST:event_btnDownloadActionPerformed

    /** append the next page of versions, or report that there are no more. */
    private void btnMore1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMore1ActionPerformed
        if (versionTotal > versionItems.size()) {
            versionPageNo++;
            searchVersion();
        } else {
            UIUtils.info(this, I18n.t("mavenExplorer.msg.noMoreVersions"));
        }
    }//GEN-LAST:event_btnMore1ActionPerformed

    /**
     * search the current page of artifacts on maven central. The request runs
     * behind the loading overlay, the found artifacts are appended to the
     * result list and the more button is enabled while further pages are
     * available. A failing request is logged and reported to the user.
     */
    private void searchMaven() {
        UIUtils.loading(this, () -> {
            try {
                if (StrUtils.isEmpty(searchText))
                    return;
                SearchResult sr = MavenREST.search(searchText, searchPageNo);
                searchTotal = sr.getResponse().getNumFound();
                List<SearchResponseItem> items = Arrays.asList(sr.getResponse().getDocs());
                searchItems.addAll(items);
                EventQueue.invokeLater(() -> {
                    items.forEach(i -> searchModel.addElement(i.getTitle()));
                    btnMore.setEnabled(searchTotal > searchItems.size());
                });
            } catch (Exception ex) {
                log.error(ex.getLocalizedMessage(), ex);
                UIUtils.error(this, ex.getLocalizedMessage());
            }
        });
    }

    /**
     * search the current page of versions of the selected artifact. Nothing
     * happens while no artifact is selected, otherwise the request runs behind
     * the loading overlay, the found versions are appended to the version list
     * and the more button is enabled while further pages are available. A
     * failing request is logged and reported to the user.
     */
    private void searchVersion() {
        int idx = lstSearchResult.getSelectedIndex();
        if (idx < 0)
            return;
        SearchResponseItem sitem = searchItems.get(idx);
        UIUtils.loading(this, () -> {
            try {
                SearchResult sr = MavenREST.version(sitem, versionPageNo);
                versionTotal = sr.getResponse().getNumFound();
                List<SearchResponseItem> items = Arrays.asList(sr.getResponse().getDocs());
                versionItems.addAll(items);
                EventQueue.invokeLater(() -> {
                    items.forEach(i -> versionModel.addElement(i.getTitle()));
                    btnMore1.setEnabled(versionTotal > versionItems.size());
                });
            } catch (Exception ex) {
                log.error(ex.getLocalizedMessage(), ex);
                UIUtils.error(this, ex.getLocalizedMessage());
            }
        });
    }

    /**
     * stand alone entry point which shows this dialog on its own, used to
     * preview the form during development. The dark flat look and feel is
     * installed and the virtual machine is terminated once the modal dialog is
     * closed.
     *
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

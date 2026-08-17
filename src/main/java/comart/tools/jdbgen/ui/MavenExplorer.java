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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import jiconfont.icons.font_awesome.FontAwesome;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;
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
     * create the components of the dialog and lay them out. The search field
     * sits on top, the artifact list and the version list of the selected
     * artifact fill the middle, and the link to the repository and the two
     * dialog buttons close the bottom row.
     */
    private void initComponents() {
        jLabel1 = new JLabel();
        txtSearch = new JTextField();
        btnSearch = new JButton();
        btnCancel = new JButton();
        btnDownload = new JButton();
        jPanel1 = new JPanel();
        jLabel3 = new JLabel();
        jScrollPane5 = new JScrollPane();
        lstSearchResult = new JList<>();
        btnMore = new JButton();
        jPanel3 = new JPanel();
        jLabel4 = new JLabel();
        jScrollPane6 = new JScrollPane();
        lstVersion = new JList<>();
        btnMore1 = new JButton();
        jLabel2 = new JLabel();
        lblMvnLink = new JLabel();

        setTitle(I18n.t("mavenExplorer.title"));

        jLabel1.setText(I18n.t("mavenExplorer.jLabel1.text"));

        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                txtSearchKeyPressed(evt);
            }
        });

        btnSearch.setText("O");
        btnSearch.setPreferredSize(new Dimension(30, 26));
        btnSearch.addActionListener(this::btnSearchActionPerformed);

        btnCancel.setText(I18n.t("mavenExplorer.btnCancel.text"));
        btnCancel.addActionListener(this::btnCancelActionPerformed);

        btnDownload.setText(I18n.t("mavenExplorer.btnDownload.text"));
        btnDownload.addActionListener(this::btnDownloadActionPerformed);

        jLabel3.setFont(jLabel3.getFont().deriveFont(
                jLabel3.getFont().getStyle() | Font.BOLD,
                jLabel3.getFont().getSize()+3));
        jLabel3.setText(I18n.t("mavenExplorer.jLabel3.text"));

        lstSearchResult.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent evt) {
                lstSearchResultMouseMoved(evt);
            }
        });
        lstSearchResult.addListSelectionListener(this::lstSearchResultValueChanged);
        jScrollPane5.setViewportView(lstSearchResult);

        btnMore.setText(I18n.t("mavenExplorer.btnMore.text"));
        btnMore.addActionListener(this::btnMoreActionPerformed);

        // the found artifacts: heading, list and the button loading the next
        // page, which runs across the whole width of the list.
        jPanel1.setLayout(new MigLayout(
                "insets 11 0 0 0, fill, wrap 1", "[grow]", "[][grow][]"));
        jPanel1.add(jLabel3);
        jPanel1.add(jScrollPane5, "grow, push, w :333:, h :425:");
        jPanel1.add(btnMore, "growx");

        jLabel4.setFont(jLabel4.getFont().deriveFont(
                jLabel4.getFont().getStyle() | Font.BOLD,
                jLabel4.getFont().getSize()+3));
        jLabel4.setText(I18n.t("mavenExplorer.jLabel4.text"));

        jScrollPane6.setViewportView(lstVersion);

        btnMore1.setText(I18n.t("mavenExplorer.btnMore1.text"));
        btnMore1.addActionListener(this::btnMore1ActionPerformed);

        // the versions of the selected artifact, built like the artifact list
        // but keeping its designed width when the dialog is resized.
        jPanel3.setLayout(new MigLayout(
                "insets 11 0 0 0, fill, wrap 1", "[grow]", "[][grow][]"));
        jPanel3.add(jLabel4);
        jPanel3.add(jScrollPane6, "grow, push, w :267:");
        jPanel3.add(btnMore1, "growx");

        jLabel2.setText(I18n.t("mavenExplorer.jLabel2.text"));

        lblMvnLink.setForeground(UIManager.getDefaults().getColor("Component.accentColor"));
        lblMvnLink.setText("Apache Maven");
        lblMvnLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblMvnLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                lblMvnLinkMouseClicked(evt);
            }
        });

        // search row, the two lists and the bottom row. Only the middle row
        // grows, the artifact list takes the width the version list leaves.
        getContentPane().setLayout(new MigLayout(
                "insets dialog, fill", "[][grow][]", "[baseline][grow][baseline]"));
        getContentPane().add(jLabel1);
        getContentPane().add(txtSearch, "growx");
        getContentPane().add(btnSearch, "wrap");
        getContentPane().add(jPanel1, "span 3, split 2, grow, push");
        getContentPane().add(jPanel3, "growy, wrap");
        getContentPane().add(jLabel2, "span 3, split 4");
        getContentPane().add(lblMvnLink);
        getContentPane().add(btnDownload, "gapbefore push");
        getContentPane().add(btnCancel);

        pack();
    }

    /** show the details of the hovered artifact as tool tip of the result list. */
    private void lstSearchResultMouseMoved(MouseEvent evt) {
        int idx = lstSearchResult.locationToIndex(evt.getPoint());
        if (idx > -1)
            lstSearchResult.setToolTipText(searchItems.get(idx).getToolTip());
        else
            lstSearchResult.setToolTipText(null);
    }

    /** hide the dialog without downloading anything. */
    private void btnCancelActionPerformed(ActionEvent evt) {
        setVisible(false);
    }

    /** run the search when enter is pressed in the search field. */
    private void txtSearchKeyPressed(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER)
            btnSearchActionPerformed(null);
    }

    /** search the first page of artifacts for the text of the search field. */
    private void btnSearchActionPerformed(ActionEvent evt) {
        if (StringUtils.isBlank(txtSearch.getText()))
            return;
        searchText = txtSearch.getText();
        clearSearch();
        searchMaven();
    }

    /** append the next page of artifacts, or report that there are no more. */
    private void btnMoreActionPerformed(ActionEvent evt) {
        if (StringUtils.isBlank(searchText))
            return;
        if (searchTotal > searchItems.size()) {
            searchPageNo++;
            searchMaven();
        } else {
            UIUtils.info(this, I18n.t("mavenExplorer.msg.noMoreResults"));
        }
    }

    /** load the versions of the artifact selected in the result list. */
    private void lstSearchResultValueChanged(ListSelectionEvent evt) {
        int idx = lstSearchResult.getSelectedIndex();
        if (idx > -1 && !evt.getValueIsAdjusting()) {
            EventQueue.invokeLater(() -> {
                clearVersions();
                searchVersion();
            });
        }
    }

    /** open the maven repository page in the default browser. */
    private void lblMvnLinkMouseClicked(MouseEvent evt) {
        PlatformUtils.openURL("https://maven.org");
    }

    /**
     * build the background task which downloads the jar of the given version.
     * The task resolves the download link, streams the jar into the drivers
     * directory below the user data directory, reports its progress and stores
     * the location it was saved at - relative to the user data directory - in
     * <code>locHolder</code>. Failures are logged, published to the progress
     * log and reported through <code>errHolder</code>.
     *
     * @param sitem the version selected on the EDT by the caller
     * @param errHolder receives the failure message, read by the caller on the EDT
     * @param locHolder receives the stored location, read by the caller on the EDT
     * @return a worker returning <code>true</code> when the jar was stored,
     *         <code>false</code> otherwise.
     */
    private static ProcessProgress.Worker getProgressWorker(
            final SearchResponseItem sitem, final String[] errHolder,
            final String[] locHolder) {
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
                        locHolder[0] = stored;
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

    /**
     * download the jar of one artifact version into the drivers directory,
     * showing the progress in a modal dialog and reporting the outcome the way
     * the download button of this dialog does. Usable without this dialog: the
     * driver manager downloads the jar of a shipped driver straight from its
     * configured Maven coordinate.
     *
     * <p>Has to be called on the event dispatch thread; it returns once the
     * download has finished, failed or was cancelled.</p>
     *
     * @param owner the window the progress and message dialogs are centered on
     * @param sitem the artifact version to download, its group, artifact and
     *              version fields are the ones that matter
     * @return the location the jar was stored at, relative to the user data
     *         directory, or <code>null</code> when it was not downloaded.
     */
    @SuppressWarnings("UseSpecificCatch")
    public static String downloadJar(Component owner, SearchResponseItem sitem) {
        final String[] err = new String[1];
        final String[] loc = new String[1];
        // We need to show modal dialog in front of another modal dialog.
        JFrame dummy = new JFrame();
        ProcessProgress pp = new ProcessProgress(dummy, true, getProgressWorker(sitem, err, loc));
        pp.setModal(true);
        pp.setLocationRelativeTo(owner);
        pp.start();
        // modal - returns once the worker's done() hides the dialog
        pp.setVisible(true);
        if (pp.result && loc[0] != null) {
            UIUtils.info(owner, I18n.t("mavenExplorer.msg.downloadComplete"));
            return loc[0];
        }
        UIUtils.error(owner, err[0] == null
                ? I18n.t("mavenExplorer.msg.downloadFailed") : err[0]);
        return null;
    }

    /** download the selected version while a modal progress dialog is shown. */
    private void btnDownloadActionPerformed(ActionEvent evt) {
        int vidx = lstVersion.getSelectedIndex();
        if (vidx < 0 || vidx >= versionItems.size()) {
            UIUtils.error(this, I18n.t("mavenExplorer.msg.selectVersion"));
            return;
        }
        String stored = downloadJar(this, versionItems.get(vidx));
        if (stored != null) {
            saveLocation = stored;
            changed = true;
            setVisible(false);
        }
    }

    /** append the next page of versions, or report that there are no more. */
    private void btnMore1ActionPerformed(ActionEvent evt) {
        if (versionTotal > versionItems.size()) {
            versionPageNo++;
            searchVersion();
        } else {
            UIUtils.info(this, I18n.t("mavenExplorer.msg.noMoreVersions"));
        }
    }

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

    /** caption of the search field. */
    private JLabel jLabel1;
    /** the artifact the repository is searched for. */
    private JTextField txtSearch;
    /** button running the search. */
    private JButton btnSearch;
    /** left half of the dialog, holding the artifacts found. */
    private JPanel jPanel1;
    /** heading of the artifact list. */
    private JLabel jLabel3;
    /** scroll pane around the artifact list. */
    private JScrollPane jScrollPane5;
    /** the artifacts found for the search text. */
    private JList<String> lstSearchResult;
    /** button loading the next page of artifacts. */
    private JButton btnMore;
    /** right half of the dialog, holding the versions. */
    private JPanel jPanel3;
    /** heading of the version list. */
    private JLabel jLabel4;
    /** scroll pane around the version list. */
    private JScrollPane jScrollPane6;
    /** the versions of the selected artifact. */
    private JList<String> lstVersion;
    /** button loading the next page of versions. */
    private JButton btnMore1;
    /** caption of the repository link. */
    private JLabel jLabel2;
    /** clickable link to the maven repository. */
    private JLabel lblMvnLink;
    /** button downloading the selected version. */
    private JButton btnDownload;
    /** button closing the dialog. */
    private JButton btnCancel;
}

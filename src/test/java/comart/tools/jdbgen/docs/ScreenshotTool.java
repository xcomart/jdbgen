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
package comart.tools.jdbgen.docs;

import comart.tools.jdbgen.types.JDBAbbr;
import comart.tools.jdbgen.types.JDBConnection;
import comart.tools.jdbgen.types.JDBDriver;
import comart.tools.jdbgen.types.JDBGenConfig;
import comart.tools.jdbgen.types.JDBPreset;
import comart.tools.jdbgen.types.JDBTemplate;
import comart.tools.jdbgen.types.db.DBSchema;
import comart.tools.jdbgen.ui.JDBAbbreviationMapper;
import comart.tools.jdbgen.ui.JDBConnectionManager;
import comart.tools.jdbgen.ui.JDBDriverManager;
import comart.tools.jdbgen.ui.JDBGeneratorMain;
import comart.tools.jdbgen.ui.JDBPresets;
import comart.tools.jdbgen.ui.MavenExplorer;
import comart.tools.jdbgen.ui.ProcessProgress;
import comart.utils.AppDirs;
import comart.utils.I18n;
import comart.utils.StrUtils;
import comart.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Retakes the screenshots below <code>docs/images</code> by opening the real
 * application windows, filling them with a fixed sample configuration and
 * photographing them with {@link Robot}. It is not a test - it needs a desktop
 * session and it writes into the working copy - which is why it lives here as a
 * plain <code>main</code> and is run by the <code>docShots</code> Gradle task:
 *
 * <pre>gradlew docShots</pre>
 *
 * <p>Everything the application writes goes into the sandbox named by
 * <code>-Djdbgen.dataDir</code>; the tool refuses to start when that points at
 * the real user data directory, so a run can never touch the configuration,
 * the driver jars or the master password of the user running it. The read only
 * files - <code>templates/</code>, <code>resource/</code> - are taken from
 * <code>-Djdbgen.resourceBase</code>, which the task points at the working
 * copy.</p>
 *
 * <p>The master password prompt is avoided altogether: the configuration is
 * built with {@link JDBGenConfig#getInstance(boolean)} from the bundled
 * defaults, which neither reads nor writes a configuration file, and is then
 * filled in by {@link #buildConfig()}.</p>
 *
 * @author comart
 */
public final class ScreenshotTool {

    /** the master password of the sandbox; it is never prompted for. */
    private static final String MASTER = "jdbgen-doc-shots";
    /** where the sample H2 database and the sandbox configuration live. */
    private static File dataDir;
    /** the directory the PNG files are written to, i.e. docs/images. */
    private static File outDir;
    /** screen grabber. */
    private static Robot robot;
    /** what was shot, reported in one block at the end of the run. */
    private static final List<String> REPORT = new ArrayList<>();

    /** upper left corner every window is moved to before it is photographed. */
    private static final int WIN_X = 40;
    /** @see #WIN_X */
    private static final int WIN_Y = 40;

    /** this class only holds <code>static</code> methods. */
    private ScreenshotTool() {
    }

    /**
     * take every screenshot of the documentation.
     *
     * @param args
     *            command line arguments; they are not evaluated.
     * @throws Exception
     *             when the sandbox is not set up, the sample database cannot be
     *             created or a screenshot cannot be written.
     */
    public static void main(String[] args) throws Exception {
        dataDir = sandbox();
        outDir = new File(require("jdbgen.shots.out")).getAbsoluteFile();
        if (!outDir.isDirectory())
            throw new IllegalStateException("'" + outDir + "' is no directory.");
        robot = new Robot();

        // the documentation is English, whatever the machine's locale is
        Locale.setDefault(Locale.ENGLISH);
        I18n.applyLanguage("en");
        StrUtils.setMaster(MASTER);
        onEdt(() -> {
            UIUtils.setFlatLightLaf();
            UIManager.put("ToolTip.font", new Font("Monospaced", Font.PLAIN, 13));
        });

        JDBConnection conn = buildConfig();
        createSampleDatabase();

        shootPasswordDialogs();
        shootProgress();
        shootAbbreviations();
        shootDriverManager();
        shootMavenExplorer();
        shootPresets();
        shootMainWindow(conn);

        System.out.println("--- screenshots -------------------------------------------");
        REPORT.forEach(System.out::println);
        System.out.flush();
        System.exit(0);
    }

    // -----------------------------------------------------------------------
    // sandbox
    // -----------------------------------------------------------------------

    /**
     * the throwaway data directory of this run, refusing anything that could be
     * the real one. This is the whole safety net of the tool: every file the
     * application writes ends up below the returned directory.
     *
     * @return the sandbox directory, created when it is missing.
     */
    private static File sandbox() {
        File dir = new File(require(AppDirs.DATA_DIR_PROPERTY).trim()).getAbsoluteFile();
        String home = System.getProperty("user.home", "");
        List<File> forbidden = new ArrayList<>();
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isEmpty())
            forbidden.add(new File(appData, "jdbgen"));
        if (!home.isEmpty()) {
            forbidden.add(new File(home, "AppData/Roaming/jdbgen"));
            forbidden.add(new File(home, ".config/jdbgen"));
            forbidden.add(new File(home, "Library/Application Support/jdbgen"));
        }
        for (File f: forbidden) {
            String real = f.getAbsolutePath();
            if (dir.getAbsolutePath().equalsIgnoreCase(real))
                throw new IllegalStateException("refusing to run in the real user data " +
                        "directory '" + real + "'. Point -D" + AppDirs.DATA_DIR_PROPERTY +
                        " at a throwaway directory.");
        }
        if (!dir.isDirectory() && !dir.mkdirs())
            throw new IllegalStateException("cannot create the sandbox '" + dir + "'.");
        System.out.println("sandbox      : " + dir);
        System.out.println("resource base: " + AppDirs.installResourceBase());
        return dir;
    }

    /**
     * a system property that has to be there.
     *
     * @param name
     *            name of the property.
     * @return its value.
     */
    private static String require(String name) {
        String v = System.getProperty(name);
        if (v == null || v.trim().isEmpty())
            throw new IllegalStateException("-D" + name + " is not set.");
        return v;
    }

    // -----------------------------------------------------------------------
    // the sample configuration
    // -----------------------------------------------------------------------

    /**
     * Build the configuration the screenshots show: the stock drivers with the
     * H2 jar in place, one sample connection with its three templates, one
     * preset and two abbreviation rules.
     *
     * <p>{@link JDBGenConfig#getInstance(boolean)} is called with
     * <code>true</code>, so the bundled defaults are used and neither a
     * configuration file nor the master password prompt is involved.</p>
     *
     * @return the sample connection, the one the main window opens.
     * @throws Exception
     *             when the H2 jar cannot be copied into the sandbox.
     */
    private static JDBConnection buildConfig() throws Exception {
        JDBGenConfig conf = JDBGenConfig.getInstance(true);
        conf.setLanguage("en");
        conf.setDarkUI(false);
        conf.setApplyAbbr(false);

        // the driver jar: copied into the sandbox and named relative to it, the
        // way the maven download stores it - see AppDirs.resolve()
        File h2Src = new File(require("jdbgen.shots.h2jar"));
        File h2Dst = new File(new File(dataDir, AppDirs.DRIVERS_DIR), h2Src.getName());
        Files.createDirectories(h2Dst.toPath().getParent());
        Files.copy(h2Src.toPath(), h2Dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        String jarPath = AppDirs.DRIVERS_DIR + "/" + h2Src.getName();
        for (JDBDriver d: conf.getDrivers()) {
            if ("H2 Embedded".equals(d.getName()))
                d.setJdbcJar(jarPath);
        }

        List<JDBTemplate> templates = new ArrayList<>(Arrays.asList(
                new JDBTemplate("Java Model", "templates/java_model.java",
                        "${name.suffix.pascal}.java", true),
                new JDBTemplate("MyBatis mapper", "templates/mybatis_mapper.xml",
                        "${name.suffix.camel}-mapper.xml", true),
                new JDBTemplate("PHP CI Model", "templates/php_ci.php",
                        "${name.suffix.lower}_model.php", false)));

        JDBConnection conn = conf.getConnections().get(0);
        conn.setName("Sample H2 Embedded");
        conn.setDriverType("H2 Embedded");
        conn.setIcon("stock:h2.png");
        // relative to the working directory, which the docShots task sets to
        // the sandbox - so this is the database created by createSampleDatabase()
        conn.setConnectionUrl("jdbc:h2:./" + SAMPLE_DB);
        conn.setUserName("");
        conn.setUserPassword("");
        conn.setOutputDir("output");
        conn.setAuthor("Dennis Park<xcomart@gmail.com>");
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("packageKey", "music");
        conn.setCustomVars(vars);
        conn.setConnectionProps(new LinkedHashMap<>());
        conn.setTemplates(templates);

        JDBPreset preset = new JDBPreset();
        preset.setName("Java + MyBatis");
        preset.setTemplates(new ArrayList<>(Arrays.asList(
                (JDBTemplate) templates.get(0).clone(),
                (JDBTemplate) templates.get(1).clone())));
        conf.getPresets().clear();
        conf.getPresets().add(preset);

        conf.setAbbrs(new ArrayList<>(Arrays.asList(
                new JDBAbbr(Boolean.TRUE, Boolean.TRUE, "smpl_albm", "sample_album"),
                new JDBAbbr(Boolean.TRUE, Boolean.FALSE, "tplt", "template"))));

        return conn;
    }

    /** name of the sample database, without the suffix H2 appends itself. */
    private static final String SAMPLE_DB = "sample_h2.db";

    /**
     * (Re)create the sample database the main window shows: two tables with a
     * foreign key between them. The database is dropped first, so a run always
     * starts from the same content whatever an earlier run left behind.
     *
     * @throws Exception
     *             when the H2 driver cannot be loaded or the tables cannot be
     *             created.
     */
    private static void createSampleDatabase() throws Exception {
        for (String suffix: new String[]{ ".mv.db", ".trace.db", ".lock.db" }) {
            File f = new File(dataDir, SAMPLE_DB + suffix);
            if (f.exists() && !f.delete())
                throw new IllegalStateException("cannot delete '" + f + "'.");
        }
        File jar = new File(AppDirs.resolvePath(
                AppDirs.DRIVERS_DIR + "/" + new File(require("jdbgen.shots.h2jar")).getName()));
        try (java.net.URLClassLoader cl = new java.net.URLClassLoader(
                new java.net.URL[]{ jar.toURI().toURL() }, ScreenshotTool.class.getClassLoader())) {
            Driver driver = (Driver) Class.forName("org.h2.Driver", true, cl)
                    .getDeclaredConstructor().newInstance();
            String url = "jdbc:h2:" +
                    new File(dataDir, SAMPLE_DB).getAbsolutePath().replace('\\', '/');
            try (Connection c = driver.connect(url, new Properties());
                    Statement st = c.createStatement()) {
                st.execute("create table T_SAMPLE_ALBUM (" +
                        "ALBUM_ID bigint not null primary key, " +
                        "TITLE varchar(200) not null, " +
                        "ARTIST varchar(100), " +
                        "RELEASE_DATE date, " +
                        "CREATED_AT timestamp)");
                st.execute("create table T_SAMPLE_MUSIC (" +
                        "MUSIC_ID bigint not null primary key, " +
                        "ALBUM_ID bigint, " +
                        "TITLE varchar(200) not null, " +
                        "TRACK_NO int, " +
                        "PLAY_TIME int, " +
                        "constraint FK_SAMPLE_MUSIC_ALBUM foreign key (ALBUM_ID) " +
                        "references T_SAMPLE_ALBUM (ALBUM_ID))");
            }
        }
        System.out.println("sample db    : " + new File(dataDir, SAMPLE_DB));
    }

    // -----------------------------------------------------------------------
    // the shots
    // -----------------------------------------------------------------------

    /**
     * <code>master_password_set.png</code> and <code>master_password.png</code>:
     * the prompt of the very first start, which asks for a confirmation, and the
     * one of every later start.
     *
     * @throws Exception when a shot cannot be taken.
     */
    private static void shootPasswordDialogs() throws Exception {
        passwordShot(I18n.t("common.config.password.new"), true, "sw0rdf1sh",
                "master_password_set.png");
        passwordShot(I18n.t("common.config.password.enter"), false, null,
                "master_password.png");
    }

    /**
     * photograph one master password prompt. The prompt blocks the thread that
     * shows it, so it is opened on the event dispatch thread - which keeps
     * pumping events while a modal dialog is up - and dismissed from here once
     * it has been photographed. Whatever it returns is thrown away.
     *
     * @param title
     *            title of the prompt, which is what identifies its window.
     * @param isFirst
     *            ask for a confirmation as well.
     * @param text
     *            text to type into the password fields, or <code>null</code> to
     *            leave them empty.
     * @param name
     *            file name of the screenshot.
     * @throws Exception when the shot cannot be taken.
     */
    private static void passwordShot(String title, boolean isFirst, String text, String name)
            throws Exception {
        EventQueue.invokeLater(() -> UIUtils.password(title, isFirst));
        Window dlg = awaitWindow(name,
                w -> w instanceof JDialog && title.equals(((JDialog) w).getTitle()));
        onEdt(() -> {
            if (text != null) {
                for (JPasswordField pf: findAll(dlg, JPasswordField.class))
                    pf.setText(text);
            }
            dlg.setLocation(WIN_X, WIN_Y);
        });
        shot(dlg, name);
        onEdt(dlg::dispose);
        Thread.sleep(400);
    }

    /**
     * <code>progress.png</code>: the undecorated progress window, caught while a
     * generation run is half way through. The log lines are the ones the real
     * generation worker publishes.
     *
     * @throws Exception when the shot cannot be taken.
     */
    private static void shootProgress() throws Exception {
        final CountDownLatch hold = new CountDownLatch(1);
        final String[] tpls = { "Java Model", "MyBatis mapper", "PHP CI Model" };
        final String[] tables = { "T_SAMPLE_ALBUM", "T_SAMPLE_MUSIC" };
        final int total = tpls.length * tables.length;

        ProcessProgress.Worker worker = new ProcessProgress.Worker() {
            @Override
            protected Boolean doInBackground() throws Exception {
                publish(I18n.t("generatorMain.progress.readingColumns"));
                int progress = 0;
                // stop half way, so that the bar is caught in an in-progress
                // state rather than at 100%
                outer:
                for (String tpl: tpls) {
                    publish(I18n.t("generatorMain.progress.templateProcessing", tpl));
                    for (String table: tables) {
                        progress++;
                        setProgress(Math.min(100, progress * 100 / total));
                        publish(I18n.t("generatorMain.progress.applying", tpl, table));
                        Thread.sleep(120);
                        if (progress >= total / 2)
                            break outer;
                    }
                }
                hold.await();
                return true;
            }
        };

        AtomicReference<ProcessProgress> ref = new AtomicReference<>();
        JFrame owner = new JFrame();
        onEdt(() -> {
            ProcessProgress pp = new ProcessProgress(owner, true, worker);
            pp.setLocation(WIN_X, WIN_Y);
            ref.set(pp);
            pp.start();
        });
        EventQueue.invokeLater(() -> ref.get().setVisible(true));
        awaitShowing("progress.png", ref::get);
        Thread.sleep(1500);
        onEdt(() -> ref.get().setLocation(WIN_X, WIN_Y));
        shot(ref.get(), "progress.png");
        hold.countDown();
        Thread.sleep(500);
        onEdt(() -> {
            ref.get().dispose();
            owner.dispose();
        });
    }

    /**
     * <code>abbreviation.png</code>: the abbreviation mapping dialog with the
     * two sample rules and the trailing empty row.
     *
     * @throws Exception when the shot cannot be taken.
     */
    private static void shootAbbreviations() throws Exception {
        AtomicReference<JDBAbbreviationMapper> ref = new AtomicReference<>();
        onEdt(() -> ref.set(JDBAbbreviationMapper.getInstance(null)));
        showAndShoot(ref::get, "abbreviation.png", null, null);
    }

    /**
     * <code>driver_manager.png</code> and <code>driver_custom.png</code>: the two
     * tabs of the driver manager, with the H2 Embedded driver selected.
     *
     * @throws Exception when a shot cannot be taken.
     */
    private static void shootDriverManager() throws Exception {
        int idx = 0;
        List<JDBDriver> drivers = JDBGenConfig.getInstance(true).getDrivers();
        for (int i = 0; i < drivers.size(); i++) {
            if ("H2 Embedded".equals(drivers.get(i).getName()))
                idx = i;
        }
        final int driverIdx = idx;

        AtomicReference<JDBDriverManager> ref = new AtomicReference<>();
        onEdt(() -> {
            JDBDriverManager dm = JDBDriverManager.getInstance();
            dm.setDriverIndex(driverIdx);
            ref.set(dm);
        });
        EventQueue.invokeLater(() -> ref.get().setVisible(true));
        awaitShowing("driver_manager.png", ref::get);
        onEdt(() -> {
            JTabbedPane tabs = get(ref.get(), "jTabbedPane1");
            tabs.setSelectedIndex(0);
            place(ref.get());
        });
        Thread.sleep(700);
        JList<String> lstDrivers = get(ref.get(), "lstDrivers");
        shot(ref.get(), "driver_manager.png", lstDrivers);

        onEdt(() -> {
            JTabbedPane tabs = get(ref.get(), "jTabbedPane1");
            tabs.setSelectedIndex(1);
        });
        Thread.sleep(500);
        shot(ref.get(), "driver_custom.png", lstDrivers);
        hide(ref.get());
    }

    /**
     * <code>maven_repository.png</code>: the maven explorer showing the search
     * for the H2 driver. The search needs the network; when it does not answer
     * the dialog is photographed empty and that is reported.
     *
     * @throws Exception when the shot cannot be taken.
     */
    private static void shootMavenExplorer() throws Exception {
        AtomicReference<MavenExplorer> ref = new AtomicReference<>();
        onEdt(() -> {
            MavenExplorer me = MavenExplorer.getInstance();
            me.setLocation(WIN_X, WIN_Y);
            ref.set(me);
        });
        EventQueue.invokeLater(() -> ref.get().setVisible(true));
        awaitShowing("maven_repository.png", ref::get);

        JList<String> results = get(ref.get(), "lstSearchResult");
        JList<String> versions = get(ref.get(), "lstVersion");
        // search.maven.org throttles, and a throttled request only fails after
        // the one minute timeout of the shared client - so the search is retried
        // rather than reported as broken on the first miss.
        boolean found = retry("the maven search",
                () -> onEdt(() -> {
                    JTextField q = get(ref.get(), "txtSearch");
                    q.setText("h2database");
                    click(ref.get(), "btnSearch");
                }),
                () -> edt(() -> results.getModel().getSize() > 0), ref.get());
        if (!found) {
            REPORT.add("WARNING maven_repository.png: the maven search returned nothing " +
                    "(search.maven.org unreachable), the dialog is shown empty.");
        } else {
            boolean versioned = retry("the maven version list",
                    () -> onEdt(() -> {
                        results.clearSelection();
                        for (int i = 0; i < results.getModel().getSize(); i++) {
                            if ("com.h2database:h2".equals(results.getModel().getElementAt(i))) {
                                results.setSelectedIndex(i);
                                results.ensureIndexIsVisible(i);
                                return;
                            }
                        }
                        results.setSelectedIndex(0);
                    }),
                    () -> edt(() -> versions.getModel().getSize() > 0), ref.get());
            if (!versioned)
                REPORT.add("WARNING maven_repository.png: no versions were returned.");
        }
        // the loading overlay of the search has to be gone before the shot
        Thread.sleep(1200);
        onEdt(() -> place(ref.get()));
        shot(ref.get(), "maven_repository.png", results);
        hide(ref.get());
    }

    /**
     * <code>template_preset.png</code>: the preset dialog with the sample preset
     * selected. It is opened the way the main window opens it - on a template
     * table whose first column is the generation tick, hence the offset of one.
     *
     * @throws Exception when the shot cannot be taken.
     */
    private static void shootPresets() throws Exception {
        AtomicReference<JDBPresets> ref = new AtomicReference<>();
        onEdt(() -> {
            JTable owner = new JTable(new DefaultTableModel(
                    new Object[]{ "Select", "Name", "Template File", "Out Template" }, 0));
            JDBPresets pr = new JDBPresets(owner, 1);
            pr.setModal(true);
            ref.set(pr);
        });
        showAndShoot(ref::get, "template_preset.png", () -> {
            JList<String> lst = get(ref.get(), "lstPresets");
            if (lst.getModel().getSize() > 0)
                lst.setSelectedIndex(0);
        }, () -> get(ref.get(), "lstPresets"));
    }

    /**
     * <code>connection_manager.png</code> and <code>generator_main.png</code>:
     * the start up sequence of the application. The main window opens the
     * connection manager itself, so both windows are photographed while it runs
     * its normal course - the manager is confirmed with its connect button and
     * the schema the main window then shows is selected in the tree.
     *
     * @param conn
     *            the sample connection, only used to report what was expected.
     * @throws Exception when a shot cannot be taken.
     */
    private static void shootMainWindow(JDBConnection conn) throws Exception {
        AtomicReference<JDBGeneratorMain> ref = new AtomicReference<>();
        onEdt(() -> {
            JDBGeneratorMain win = new JDBGeneratorMain();
            win.setLocation(WIN_X, WIN_Y);
            ref.set(win);
            win.setVisible(true);
        });

        // the main window queued the connection manager while it was built
        Window cm = awaitWindow("connection_manager.png", w -> w instanceof JDBConnectionManager);
        onEdt(() -> {
            ((JDBConnectionManager) cm).setSelection(conn);
            cm.setLocation(WIN_X, WIN_Y);
        });
        Thread.sleep(600);
        shot(cm, "connection_manager.png", get(cm, "lstConnections"));

        // confirming it hides the dialog and lets the main window connect
        EventQueue.invokeLater(() -> click(cm, "btnConnect"));
        if (!await(() -> edt(() -> !cm.isShowing()), 10000))
            REPORT.add("WARNING the connection manager did not close.");

        if (!await(() -> get(ref.get(), "dbmeta") != null, 30000))
            REPORT.add("WARNING generator_main.png: no database connection was opened.");

        JTree tree = get(ref.get(), "treSchemas");
        await(() -> edt(() -> tree.getModel().getRoot() != null), 10000);
        onEdt(() -> selectSchema(tree, "PUBLIC"));

        JList<String> tabs = get(ref.get(), "lstTables");
        if (!await(() -> edt(() -> tabs.getModel().getSize() > 0), 20000))
            REPORT.add("WARNING generator_main.png: the table list stayed empty.");

        onEdt(() -> {
            // left empty on purpose: the placeholder text is what tells the
            // reader what the field above the table list is for
            JTextField filter = get(ref.get(), "txtTableFilter");
            filter.setText("");
            place(ref.get());
        });
        Thread.sleep(800);
        shot(ref.get(), "generator_main.png", tree);
    }

    /**
     * expand the root of the schema tree and select the schema of the given
     * name in it. EDT only.
     *
     * @param tree
     *            the schema tree of the main window.
     * @param schema
     *            name of the schema to select.
     */
    private static void selectSchema(JTree tree, String schema) {
        TreeModel model = tree.getModel();
        Object root = model.getRoot();
        if (root == null)
            return;
        tree.expandPath(new TreePath(root));
        for (int i = 0; i < model.getChildCount(root); i++) {
            Object child = model.getChild(root, i);
            Object user = child instanceof DefaultMutableTreeNode
                    ? ((DefaultMutableTreeNode) child).getUserObject() : child;
            // the schema nodes carry a DBSchema, whose toString() is the
            // generated one of a @Data class - ask for the name itself
            String name = user instanceof DBSchema
                    ? ((DBSchema) user).getName() : String.valueOf(user);
            if (schema.equals(name)) {
                TreePath path = new TreePath(new Object[]{ root, child });
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
                return;
            }
        }
        REPORT.add("WARNING the schema '" + schema + "' is not in the tree.");
    }

    // -----------------------------------------------------------------------
    // window plumbing
    // -----------------------------------------------------------------------

    /**
     * show a modal dialog, prepare it, photograph it and close it again. The
     * dialog is shown from the event dispatch thread, which keeps pumping events
     * while it is up, so everything after that still runs.
     *
     * @param dialog
     *            supplies the dialog, which has to exist already.
     * @param name
     *            file name of the screenshot.
     * @param prepare
     *            what to set up on the shown dialog before it is photographed,
     *            run on the event dispatch thread; may be <code>null</code>.
     * @param focus
     *            the component the keyboard focus is left in, see
     *            {@link #shot(Window, String, Component)}; may be
     *            <code>null</code>.
     * @throws Exception when the shot cannot be taken.
     */
    private static void showAndShoot(Supplier<? extends Window> dialog, String name,
            Runnable prepare, Supplier<Component> focus) throws Exception {
        EventQueue.invokeLater(() -> dialog.get().setVisible(true));
        awaitShowing(name, dialog);
        onEdt(() -> {
            if (prepare != null)
                prepare.run();
            place(dialog.get());
        });
        Thread.sleep(700);
        shot(dialog.get(), name, focus == null ? null : focus.get());
        hide(dialog.get());
    }

    /**
     * move a window to the fixed capture position, keeping it on the screen.
     * EDT only.
     *
     * @param w the window to move.
     */
    private static void place(Window w) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = w.getSize();
        int x = Math.max(0, Math.min(WIN_X, screen.width - size.width));
        int y = Math.max(0, Math.min(WIN_Y, screen.height - size.height));
        w.setLocation(x, y);
    }

    /**
     * hide a window and wait for it to be gone, so that the next shot cannot
     * catch it.
     *
     * @param w the window to hide.
     * @throws Exception when the wait is interrupted.
     */
    private static void hide(Window w) throws Exception {
        EventQueue.invokeLater(() -> w.setVisible(false));
        await(() -> edt(() -> !w.isShowing()), 5000);
        Thread.sleep(400);
    }

    /**
     * photograph a window and write the result to {@link #outDir}.
     *
     * @param w
     *            the window to photograph, which has to be showing.
     * @param name
     *            file name of the screenshot.
     * @throws Exception when the file cannot be written.
     */
    private static void shot(Window w, String name) throws Exception {
        shot(w, name, null);
    }

    /**
     * photograph a window and write the result to {@link #outDir}.
     *
     * @param w
     *            the window to photograph, which has to be showing.
     * @param name
     *            file name of the screenshot.
     * @param focus
     *            the component to leave the keyboard focus in, or
     *            <code>null</code> for whichever has it. A list, a table or a
     *            tree paints its selection grey while it is not focused, which
     *            is not what a screenshot pointing at that selection wants.
     * @throws Exception when the file cannot be written.
     */
    private static void shot(Window w, String name, Component focus) throws Exception {
        dismissMessageDialogs(w);
        raise(w);
        // the window has to be the active one, or its title bar is painted
        // greyed out
        clickTitleBar(edt(w::getBounds));
        raise(w);
        if (focus != null) {
            onEdt(focus::requestFocusInWindow);
            Thread.sleep(400);
        }
        BufferedImage img = grab(w);
        File f = new File(outDir, name);
        if (!ImageIO.write(img, "png", f))
            throw new IllegalStateException("cannot write '" + f + "'.");
        REPORT.add(String.format("%-26s %4d x %-4d  %s", name, img.getWidth(), img.getHeight(), f));
    }

    /**
     * bring a window to the very front and let it repaint. Being "on top" is not
     * enough on Windows, where a process that is not the foreground one cannot
     * raise its windows above the ones of the process that is - hence the
     * always-on-top flag, which is not subject to that rule.
     *
     * @param w the window to raise.
     * @throws Exception when the wait is interrupted.
     */
    private static void raise(Window w) throws Exception {
        onEdt(() -> {
            w.setAlwaysOnTop(true);
            w.toFront();
            w.requestFocus();
            w.repaint();
        });
        Thread.sleep(600);
    }

    /**
     * click the title bar of the window being photographed, which is what makes
     * it the active window - an inactive window is painted with a greyed out
     * title bar. The click is a plain press and release without any movement in
     * between, so it cannot drag the window anywhere.
     *
     * @param bounds the bounds of the window, whose title bar is its top edge.
     * @throws Exception when the wait is interrupted.
     */
    private static void clickTitleBar(Rectangle bounds) throws Exception {
        // left of the window buttons, right of the icon and of an embedded menu
        // bar: the middle of the title bar is free on every window here.
        int x = bounds.x + bounds.width / 2;
        int y = bounds.y + 10;
        robot.mouseMove(x, y);
        robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(60);
        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        // out of the way, so that no hover effect is photographed
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        robot.mouseMove(Math.min(screen.width - 2, bounds.x + bounds.width + 60),
                Math.min(screen.height - 2, bounds.y + bounds.height + 60));
        Thread.sleep(500);
    }

    /**
     * The picture of a window: the window paints itself into an image instead of
     * being cut out of the screen.
     *
     * <p>A screen grab would be at the mercy of everything else on the desktop -
     * another window on top of this one, the drop shadow and the invisible
     * resize border Windows adds around a window, the scaling of the display.
     * Painting the window itself has none of those problems, and it works
     * because the title bar is part of the window here: FlatLaf decorates its
     * windows itself, so the whole picture is Swing.</p>
     *
     * @param w
     *            the window to paint, which has to be showing so that it has
     *            been laid out.
     * @return the picture of the window, with the fully transparent border of
     *         the rounded window corners cut off.
     */
    private static BufferedImage grab(Window w) {
        return edt(() -> {
            BufferedImage img = new BufferedImage(Math.max(1, w.getWidth()),
                    Math.max(1, w.getHeight()), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            try {
                w.printAll(g);
            } finally {
                g.dispose();
            }
            return crop(img);
        });
    }

    /**
     * cut the fully transparent rows and columns off the edges of an image -
     * what a window paints outside its rounded corners and inside the invisible
     * resize border.
     *
     * @param img the image to trim.
     * @return the trimmed image, or <code>img</code> when there was nothing to
     *         trim.
     */
    private static BufferedImage crop(BufferedImage img) {
        int top = 0;
        int bottom = img.getHeight() - 1;
        int left = 0;
        int right = img.getWidth() - 1;
        while (top < bottom && rowIsBlank(img, top))
            top++;
        while (bottom > top && rowIsBlank(img, bottom))
            bottom--;
        while (left < right && columnIsBlank(img, left))
            left++;
        while (right > left && columnIsBlank(img, right))
            right--;
        if (top == 0 && left == 0 && right == img.getWidth() - 1 && bottom == img.getHeight() - 1)
            return img;
        return img.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    /**
     * @param img the image to look at.
     * @param y the row to look at.
     * @return every pixel of the row is fully transparent.
     */
    private static boolean rowIsBlank(BufferedImage img, int y) {
        for (int x = 0; x < img.getWidth(); x++) {
            if ((img.getRGB(x, y) >>> 24) != 0)
                return false;
        }
        return true;
    }

    /**
     * @param img the image to look at.
     * @param x the column to look at.
     * @return every pixel of the column is fully transparent.
     */
    private static boolean columnIsBlank(BufferedImage img, int x) {
        for (int y = 0; y < img.getHeight(); y++) {
            if ((img.getRGB(x, y) >>> 24) != 0)
                return false;
        }
        return true;
    }

    /**
     * wait for a window matching <code>filter</code> to be on screen.
     *
     * @param what
     *            what the window is needed for, named in the warning of a
     *            timeout.
     * @param filter
     *            the window to look for.
     * @return the window, or <code>null</code> when none appeared in time.
     * @throws Exception when the wait is interrupted.
     */
    private static Window awaitWindow(String what, Predicate<Window> filter) throws Exception {
        AtomicReference<Window> ref = new AtomicReference<>();
        boolean ok = await(() -> edt(() -> {
            for (Window w: Window.getWindows()) {
                if (w.isShowing() && filter.test(w)) {
                    ref.set(w);
                    return true;
                }
            }
            return false;
        }), 30000);
        if (!ok)
            throw new IllegalStateException("no window appeared for " + what);
        return ref.get();
    }

    /**
     * wait until the given window is on screen.
     *
     * @param what
     *            what the window is needed for, named in the failure.
     * @param window
     *            supplies the window.
     * @throws Exception when the wait is interrupted.
     */
    private static void awaitShowing(String what, Supplier<? extends Window> window)
            throws Exception {
        if (!await(() -> edt(() -> window.get().isShowing()), 30000))
            throw new IllegalStateException("the window of " + what + " never appeared.");
    }

    /**
     * A task that may fail, so that a lambda taking no argument and returning
     * nothing may still throw.
     */
    private interface Attempt {
        /**
         * run the attempt.
         *
         * @throws Exception whatever the attempt failed with.
         */
        void run() throws Exception;
    }

    /**
     * run something that reaches out to the network until it has an answer.
     * <code>search.maven.org</code> throttles, and a throttled request only
     * fails after the one minute timeout of the shared HTTP client, so a single
     * miss says nothing about whether the repository can be reached at all.
     *
     * @param what
     *            what is being waited for, named in the progress report.
     * @param attempt
     *            what starts the request.
     * @param done
     *            whether the answer has arrived.
     * @param keep
     *            the window being photographed; every other message box that
     *            appeared meanwhile is a failure report of an earlier attempt
     *            and is closed.
     * @return the answer arrived or not.
     * @throws Exception when the wait is interrupted.
     */
    private static boolean retry(String what, Attempt attempt, BooleanSupplier done, Window keep)
            throws Exception {
        for (int i = 1; i <= 3; i++) {
            System.out.println("waiting for " + what + ", attempt " + i);
            attempt.run();
            if (await(done, 70000))
                return true;
            dismissMessageDialogs(keep);
            Thread.sleep(3000);
        }
        return false;
    }

    /**
     * close every message box that is on screen except the window being
     * photographed - a failed request reports itself with one, and it would
     * swallow the mouse click that activates the window of the next shot.
     *
     * @param keep the window that must stay open.
     * @throws Exception when the wait is interrupted.
     */
    private static void dismissMessageDialogs(Window keep) throws Exception {
        List<Window> boxes = edt(() -> {
            List<Window> res = new ArrayList<>();
            for (Window w: Window.getWindows()) {
                if (w != keep && w.isShowing() && w instanceof JDialog
                        && !findAll(w, javax.swing.JOptionPane.class).isEmpty())
                    res.add(w);
            }
            return res;
        });
        for (Window w: boxes) {
            EventQueue.invokeLater(w::dispose);
            REPORT.add("NOTE closed a message box that was in the way of the screenshots.");
        }
        if (!boxes.isEmpty())
            Thread.sleep(500);
    }

    /**
     * poll a condition.
     *
     * @param cond
     *            the condition to wait for.
     * @param millis
     *            how long to wait at most.
     * @return the condition became true or not.
     * @throws Exception when the wait is interrupted.
     */
    private static boolean await(BooleanSupplier cond, long millis) throws Exception {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            if (cond.getAsBoolean())
                return true;
            Thread.sleep(100);
        }
        return cond.getAsBoolean();
    }

    // -----------------------------------------------------------------------
    // small helpers
    // -----------------------------------------------------------------------

    /**
     * run a task on the event dispatch thread and wait for it.
     *
     * @param task what to run.
     * @throws Exception when the task fails or the wait is interrupted.
     */
    private static void onEdt(Runnable task) throws Exception {
        if (EventQueue.isDispatchThread())
            task.run();
        else
            EventQueue.invokeAndWait(task);
    }

    /**
     * read a value on the event dispatch thread.
     *
     * @param task the value to read.
     * @param <T> type of the value.
     * @return whatever <code>task</code> returned.
     */
    private static <T> T edt(Supplier<T> task) {
        if (EventQueue.isDispatchThread())
            return task.get();
        AtomicReference<T> ref = new AtomicReference<>();
        try {
            EventQueue.invokeAndWait(() -> ref.set(task.get()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return ref.get();
    }

    /**
     * read a private field of a window of the application. The user interface
     * fields of the generated forms are private, and this tool has to reach
     * them to set up the state a screenshot has to show.
     *
     * @param target
     *            the object holding the field.
     * @param name
     *            name of the field.
     * @param <T>
     *            type of the field.
     * @return the value of the field.
     */
    @SuppressWarnings("unchecked")
    private static <T> T get(Object target, String name) {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return (T) f.get(target);
            } catch (NoSuchFieldException ignored) {
                // keep looking up the hierarchy
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("no field '" + name + "' on " + target.getClass());
    }

    /**
     * press a button of a window of the application, named by its private
     * field. EDT only.
     *
     * @param target
     *            the window holding the button.
     * @param name
     *            name of the button field.
     */
    private static void click(Object target, String name) {
        ((AbstractButton) get(target, name)).doClick();
    }

    /**
     * every component of the given type below a container.
     *
     * @param root
     *            where to start looking.
     * @param type
     *            the type to look for.
     * @param <T>
     *            the type to look for.
     * @return the components found, in no particular order.
     */
    private static <T extends Component> List<T> findAll(Component root, Class<T> type) {
        List<T> res = new ArrayList<>();
        collect(root, type, res);
        return res;
    }

    /**
     * @param c the component to look at.
     * @param type the type to look for.
     * @param res receives what was found.
     * @param <T> the type to look for.
     */
    private static <T extends Component> void collect(Component c, Class<T> type, List<T> res) {
        if (type.isInstance(c))
            res.add(type.cast(c));
        if (c instanceof Container) {
            for (Component child: ((Container) c).getComponents())
                collect(child, type, res);
        }
    }
}

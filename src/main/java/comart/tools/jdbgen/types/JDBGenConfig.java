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
package comart.tools.jdbgen.types;

import comart.tools.jdbgen.types.maven.MavenConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import comart.utils.AppDirs;
import comart.utils.I18n;
import comart.utils.ObjUtils;
import comart.utils.StrUtils;
import comart.utils.UIUtils;
import java.awt.Container;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * The whole application configuration, held as a singleton and stored as a
 * single JSON file below the user data directory. The connection URL, user name
 * and password of every connection are encrypted with a master password, which
 * is asked for once while the singleton is created; everything else is plain
 * JSON.
 *
 * @author comart
 */
@Slf4j
@Data
public class JDBGenConfig {
    /** name of the sample database shipped with the release. */
    static final String SAMPLE_DB_FILE = "sample_h2.db.mv.db";
    /** the H2 database name the sample connection opens, without a suffix. */
    static final String SAMPLE_DB_NAME = "sample_h2.db";
    /** the singleton, built on the first call of {@link #getInstance(boolean)}. */
    private static JDBGenConfig INSTANCE = null;
    /** whether the user interface uses the dark theme. */
    private boolean isDarkUI = false;
    /** the configured database connections. */
    private List<JDBConnection> connections;
    /** the configured JDBC drivers, predefined ones included. */
    private List<JDBDriver> drivers;
    /** the configured template presets. */
    private List<JDBPreset> presets;
    /** the abbreviation rules applied while identifiers are turned into names. */
    private List<JDBAbbr> abbrs = new ArrayList<>();
    /** URLs of the Maven repository the driver jars are searched and downloaded from. */
    private MavenConfig maven;
    /** whether {@link #abbrs} is applied at all. */
    private boolean applyAbbr = false;
    /**
     * user interface language: <code>null</code>, an empty value or
     * <code>"system"</code> keep the operating system locale, anything else is
     * a language tag such as <code>"en"</code> or <code>"ko"</code>.
     */
    private String language = null;
    /**
     * size, position, maximized state and divider positions the main window is
     * restored to, see {@link WindowState}. Never <code>null</code> once the
     * configuration has been normalized.
     */
    private WindowState mainWindow;

    /**
     * the configuration singleton, loading it from the configuration file on
     * first use.
     *
     * @return the shared configuration instance.
     * @see #getInstance(boolean)
     */
    public static JDBGenConfig getInstance() {
        return getInstance(false);
    }

    /**
     * The configuration file, below the user data directory of the operating
     * system - the installation directory may well be read only.
     *
     * @return the configuration file, which need not exist yet.
     * @see AppDirs#userDataDir()
     */
    public static File configFile() {
        return AppDirs.userDataFile(AppDirs.CONFIG_NAME);
    }

    /**
     * Read the <code>language</code> setting out of the configuration file,
     * without asking for the master password.
     *
     * @return the stored language tag, or <code>null</code> when there is none.
     * @see #peekLanguage(File)
     */
    public static String peekLanguage() {
        return peekLanguage(configFile());
    }

    /**
     * Read the <code>language</code> setting out of <code>f</code>.
     *
     * <p>The language has to be known before any dialog is shown, and the
     * master password prompt is a dialog itself. Only the three connection
     * fields of the configuration are encrypted, so the file parses as plain
     * JSON and this single entry can be read without a password.</p>
     *
     * @param f the configuration file to read, may be <code>null</code>.
     * @return the stored language tag, or <code>null</code> when there is no
     *         configuration, it cannot be parsed or it carries no language.
     */
    public static String peekLanguage(File f) {
        if (f == null || !f.isFile())
            return null;
        try (FileReader fr = new FileReader(f, StandardCharsets.UTF_8)) {
            JsonObject obj = new Gson().fromJson(fr, JsonObject.class);
            if (obj == null)
                return null;
            JsonElement el = obj.get("language");
            if (el == null || !el.isJsonPrimitive())
                return null;
            String lang = el.getAsString();
            return StrUtils.isEmpty(lang) ? null : lang.trim();
        } catch (Exception e) {
            // a broken or unreadable configuration is reported later on, by
            // the load that actually needs it; startup just keeps the system
            // language here.
            log.warn("cannot read the language setting of '{}': {}",
                    f, e.getLocalizedMessage());
            return null;
        }
    }


    /**
     * the configuration singleton, creating it on first use.
     *
     * <p>Unless <code>useDefault</code> is given, the master password is asked
     * for and the configuration file is read with it; a wrong password may be
     * retried as often as the user wants, and the existing file is only
     * replaced by the built-in default configuration after the user explicitly
     * agrees to it. When the configuration still carries passwords in the
     * superseded encryption format, it is rewritten so that they are upgraded
     * in place.</p>
     *
     * <p>The method terminates the application when the user cancels the
     * password prompt or the default configuration cannot be built.</p>
     *
     * @param useDefault <code>true</code> to build the configuration from the
     *                   bundled defaults without reading or writing any file.
     * @return the shared configuration instance.
     */
    public static synchronized JDBGenConfig getInstance(boolean useDefault) {
        if (INSTANCE == null) {
            File f = configFile();
            String confPath = f.getAbsolutePath();
            log.info("config path: {}", confPath);
            Gson gson = new Gson();
            if (!useDefault) {
                // the password may be retried as often as the user wants: an
                // existing configuration is never discarded just because a few
                // attempts in a row failed.
                int cnt = 0;
                while (true) {
                    boolean isNew = !(f.exists() && f.isFile());
                    String message = I18n.t(isNew
                            ? "common.config.password.new"
                            : "common.config.password.enter");
                    String master = UIUtils.password(message, isNew);
                    if (master == null)
                        System.exit(1);
                    StrUtils.setMaster(master);
                    if (!(f.exists() && f.isFile()))
                        break;
                    try (FileReader fr = new FileReader(f, StandardCharsets.UTF_8)) {
                        INSTANCE = (JDBGenConfig)gson.fromJson(fr, JDBGenConfig.class);
                        if (INSTANCE != null)
                            break;
                        throw new IOException("configuration '" + confPath + "' is empty");
                    } catch (Exception e) {
                        log.error("cannot load configuration '" + confPath + "'", e);
                        cnt++;
                        if (cnt < 3) {
                            UIUtils.error(null, I18n.t("common.config.password.incorrect"));
                            continue;
                        }
                        boolean retry = UIUtils.confirm(null,
                                I18n.t("common.config.error.title"),
                                I18n.t("common.config.error.message", cnt,
                                        describe(e)));
                        if (retry) {
                            cnt = 0;
                            continue;
                        }
                        // discarding the current configuration needs an explicit
                        // acknowledgement, it is the user's only copy of it.
                        boolean isOk = UIUtils.confirm(null,
                                I18n.t("common.config.default.title"),
                                I18n.t("common.config.default.message", confPath));
                        if (!isOk)
                            System.exit(1);
                        master = UIUtils.password(I18n.t("common.config.password.new"), true);
                        if (master == null)
                            System.exit(1);
                        StrUtils.setMaster(master);
                        break;
                    }
                }
            }

            if (INSTANCE == null) {
                log.info("config file not found or not loadable, creating default one.");
                try {
                    INSTANCE = loadBundledDefaults();
                    INSTANCE.connections = new ArrayList<>(Arrays.asList(createSampleConnection()));
                } catch (Exception e) {
                    UIUtils.error(null, I18n.t("common.config.default.loadFailed", describe(e)));
                    log.error("cannot recover previous error.", e);
                    System.exit(1);
                }
                if (!useDefault)
                    replaceWithDefaultConfig(f);
            }
            normalize(INSTANCE);

            // passwords stored by an older release use a weaker scheme; rewrite
            // the whole configuration so that they are upgraded in place.
            if (!useDefault && StrUtils.hasLegacyEncryption()) {
                log.info("configuration contains passwords in the superseded encryption format, re-encrypting.");
                if (saveInstance(null))
                    StrUtils.clearLegacyEncryption();
            }
        }

        return INSTANCE;
    }

    /**
     * the configuration shipped with the release, read from
     * <code>/defaultConfig.json</code> on the class path.
     *
     * @return the parsed bundled configuration, never <code>null</code>.
     * @throws IOException if the resource is missing, unreadable or not the
     *                     configuration it is expected to be.
     */
    static JDBGenConfig loadBundledDefaults() throws IOException {
        InputStream is = JDBGenConfig.class.getResourceAsStream("/defaultConfig.json");
        if (is == null)
            throw new IOException("'/defaultConfig.json' is not on the class path");
        try (InputStreamReader ir = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JDBGenConfig res = new Gson().fromJson(ir, JDBGenConfig.class);
            if (res == null)
                throw new IOException("'/defaultConfig.json' is empty");
            return res;
        }
    }

    /**
     * the drivers shipped with the release, by name.
     *
     * @return an empty map when the bundled configuration cannot be read; a
     *         missing default is worth a log line, but never worth failing a
     *         start over.
     */
    private static Map<String, JDBDriver> bundledDrivers() {
        try {
            List<JDBDriver> drivers = loadBundledDefaults().getDrivers();
            if (drivers == null)
                return Collections.emptyMap();
            Map<String, JDBDriver> res = new HashMap<>();
            drivers.forEach(d -> {
                if (!StrUtils.isEmpty(d.getName()))
                    res.put(d.getName(), d);
            });
            return res;
        } catch (Exception e) {
            log.warn("cannot read the bundled default configuration: {}",
                    e.getLocalizedMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * carry the Maven coordinate of a shipped driver over into a configuration
     * written by a release that did not know it yet, so that its download
     * button fetches the jar instead of opening the search dialog, and point a
     * shipped driver without a jar at the jar bundled with the installation
     * where there is one.
     *
     * <p>Only the drivers marked as stock items are filled in, and only where
     * the coordinate or the jar is missing: whatever the user has put there
     * stays.</p>
     *
     * @param drivers the configured drivers, may be <code>null</code>.
     */
    static void fillStockMavenArtifacts(List<JDBDriver> drivers) {
        if (drivers == null)
            return;
        boolean needed = drivers.stream()
                .anyMatch(d -> d.isStockItem() && (StrUtils.isEmpty(d.getMavenArtifact())
                        || StrUtils.isEmpty(d.getJdbcJar())));
        if (!needed)
            return;
        Map<String, JDBDriver> defaults = bundledDrivers();
        if (defaults.isEmpty())
            return;
        drivers.forEach(d -> {
            if (!d.isStockItem())
                return;
            JDBDriver stock = defaults.get(d.getName());
            if (stock == null)
                return;
            if (StrUtils.isEmpty(d.getMavenArtifact()) && !StrUtils.isEmpty(stock.getMavenArtifact()))
                d.setMavenArtifact(stock.getMavenArtifact());
            // the jar of a driver shipped with the release - the H2 driver of
            // the sample database - is only taken over when it is actually
            // there: an older installation does not carry it, and a jar the
            // user has downloaded is never replaced.
            if (StrUtils.isEmpty(d.getJdbcJar()) && !StrUtils.isEmpty(stock.getJdbcJar())) {
                File jar = AppDirs.resolve(stock.getJdbcJar());
                if (jar != null && jar.isFile())
                    d.setJdbcJar(stock.getJdbcJar());
            }
        });
    }

    /**
     * the technical detail of a failure, appended to a translated message. It
     * is not translated itself: the exception text comes from the JDK or from
     * a driver.
     *
     * @param t the failure to describe.
     * @return the simple class name of the exception and its localized message.
     */
    private static String describe(Throwable t) {
        return t.getClass().getSimpleName() + ": " + t.getLocalizedMessage();
    }

    /**
     * The sample connection of a fresh configuration.
     *
     * <p>Every path it carries is absolute: the templates and the icons are
     * read out of the installation, everything that is written - the sample
     * database and the generated sources - lives below the user data
     * directory, which is writable even when the application is installed
     * below <code>C:\Program Files</code>.</p>
     *
     * @return a connection opening the bundled sample H2 database with the
     *         three sample templates.
     */
    static JDBConnection createSampleConnection() {
        JDBConnection jcon = new JDBConnection();
        jcon.setAuthor(ObjUtils.getLoginUserId());
        jcon.setConnectionProps(new HashMap<>());
        jcon.setConnectionUrl("jdbc:h2:" + sampleDatabaseUrlPath());
        jcon.setDriverType("H2 Embedded");
        jcon.setIcon("stock:h2.png");
        jcon.setName("Sample H2 Embedded");
        jcon.setOutputDir(AppDirs.userDataFile("output").getAbsolutePath());
        List<JDBTemplate> templates = new ArrayList<>(Arrays.asList(
            new JDBTemplate("Java Model", templatePath("java_model.java"),
                    "${name.suffix.pascal}Model.java"),
            new JDBTemplate("MyBatis mapper", templatePath("mybatis_mapper.xml"),
                    "${name.suffix.camel}-mapper.xml"),
            new JDBTemplate("PHP CI Model", templatePath("php_ci.php"),
                    "${name.suffix.lower}_ci_model.php")
        ));
        jcon.setTemplates(templates);
        return jcon;
    }

    /**
     * the absolute path of a template shipped with the installation.
     *
     * @param name file name of the template below the templates directory.
     * @return the absolute path of the template file.
     */
    private static String templatePath(String name) {
        return AppDirs.installResourceFile("templates/" + name).getAbsolutePath();
    }

    /**
     * copy the sample database shipped with the release next to the
     * configuration, so that the sample connection can write to it, and name
     * the copy the way an H2 URL does - without the <code>.mv.db</code> suffix
     * H2 appends itself, and with '/' separators, which H2 understands on
     * every platform.
     *
     * <p>A release without the sample database - or a copy that fails - only
     * means that the sample connection has nothing to open yet, which is
     * reported when it is used.</p>
     *
     * @return the database path to put behind <code>jdbc:h2:</code>.
     */
    private static String sampleDatabaseUrlPath() {
        File target = AppDirs.userDataFile(SAMPLE_DB_FILE);
        File source = AppDirs.installResourceFile(SAMPLE_DB_FILE);
        if (!target.exists() && source.isFile()) {
            try {
                Files.copy(source.toPath(), target.toPath());
                log.info("copied the sample database to '{}'", target);
            } catch (Exception e) {
                log.warn("cannot copy the sample database '{}' to '{}': {}",
                        source, target, e.getLocalizedMessage());
            }
        }
        return AppDirs.userDataFile(SAMPLE_DB_NAME).getAbsolutePath().replace('\\', '/');
    }

    /**
     * write the freshly built default configuration over an existing one, in a
     * way that can never leave the user without a configuration file: the old
     * file is moved aside first and moved back when the write fails.
     *
     * <p>The application is terminated when neither the new nor the previous
     * configuration can be put in place.</p>
     *
     * @param f the configuration file to write.
     */
    private static void replaceWithDefaultConfig(File f) {
        Path backup = null;
        if (f.exists() && f.isFile()) {
            backup = backupExistingConfig(f);
            if (backup == null) {
                UIUtils.error(null, I18n.t("common.config.backup.failed", f.getAbsolutePath()));
                System.exit(1);
            }
        }

        if (saveInstance(null)) {
            if (backup != null) {
                UIUtils.info(null, I18n.t("common.config.backup.kept",
                        backup.toAbsolutePath().toString(), f.getName()));
            }
            return;
        }

        // the default configuration could not be written: put the user's file
        // back where it was rather than leaving no configuration at all.
        String message = I18n.t("common.config.write.failed", f.getAbsolutePath());
        if (backup != null) {
            if (restoreBackup(backup, f)) {
                message += I18n.t("common.config.write.restored");
            } else {
                message += I18n.t("common.config.write.keptBackup",
                        backup.toAbsolutePath().toString(), f.getName());
            }
        }
        UIUtils.error(null, message);
        System.exit(1);
    }

    /**
     * move an existing, unloadable configuration file aside so that writing a
     * fresh default configuration cannot destroy the user's data.
     *
     * @param f the configuration file to move aside.
     * @return the path the configuration was moved to, or null when it could
     *         not be moved - the caller must not overwrite it in that case.
     */
    static Path backupExistingConfig(File f) {
        if (!(f.exists() && f.isFile()))
            return null;
        Path backup = f.toPath().toAbsolutePath().resolveSibling(f.getName() + "." +
                StrUtils.dateFormat("yyyyMMdd_HHmmss") + ".bak");
        try {
            Files.move(f.toPath(), backup, StandardCopyOption.REPLACE_EXISTING);
            log.warn("existing configuration could not be loaded, backed up to '{}'", backup);
            return backup;
        } catch (Exception e) {
            log.error("cannot back up existing configuration '" + f.getAbsolutePath() + "'", e);
            return null;
        }
    }

    /**
     * undo {@link #backupExistingConfig(File)} after a failed attempt to write
     * a replacement configuration.
     *
     * @param backup the path {@link #backupExistingConfig(File)} returned.
     * @param f the configuration file the backup is moved back to.
     * @return <code>true</code> when the previous configuration is in place
     *         again.
     */
    static boolean restoreBackup(Path backup, File f) {
        if (backup == null || !Files.isRegularFile(backup))
            return false;
        try {
            Files.move(backup, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("restored previous configuration from '{}'", backup);
            return true;
        } catch (Exception e) {
            log.error("cannot restore previous configuration from '" + backup + "'", e);
            return false;
        }
    }

    /**
     * fill in collections omitted from the configuration file, so that callers
     * never have to null-check them.
     *
     * <p>The lists of the configuration itself and the collections of every
     * connection, driver and preset in them are replaced by empty ones where
     * they are missing. The Maven coordinate of the shipped drivers is carried
     * over from the bundled defaults as well, see
     * {@link #fillStockMavenArtifacts(List)}. The stored window state is filled
     * in the same way, with one that has nothing stored in it.</p>
     *
     * @param conf the configuration to fill in, may be <code>null</code>.
     */
    private static void normalize(JDBGenConfig conf) {
        if (conf == null)
            return;
        if (conf.connections == null) conf.connections = new ArrayList<>();
        if (conf.drivers == null) conf.drivers = new ArrayList<>();
        if (conf.presets == null) conf.presets = new ArrayList<>();
        if (conf.abbrs == null) conf.abbrs = new ArrayList<>();
        if (conf.mainWindow == null) conf.mainWindow = new WindowState();
        conf.connections.forEach(c -> {
            if (c.getTemplates() == null) c.setTemplates(new ArrayList<>());
            if (c.getCustomVars() == null) c.setCustomVars(new LinkedHashMap<>());
            if (c.getConnectionProps() == null) c.setConnectionProps(new LinkedHashMap<>());
        });
        conf.drivers.forEach(d -> {
            if (d.getProps() == null) d.setProps(new LinkedHashMap<>());
        });
        fillStockMavenArtifacts(conf.drivers);
        conf.presets.forEach(p -> {
            if (p.getTemplates() == null) p.setTemplates(new ArrayList<>());
        });
    }

    /**
     * write the configuration singleton back to the configuration file as
     * pretty printed JSON. A failure is logged and reported to the user rather
     * than thrown.
     *
     * @param parent unused; kept so that callers can pass the window the save
     *               was triggered from.
     * @return <code>true</code> when the configuration was written.
     */
    public static synchronized boolean saveInstance(Container parent) {
        Gson gson = (new GsonBuilder()).setPrettyPrinting().create();

        File conf = configFile();
        try {
            String json = gson.toJson(INSTANCE);
            try (FileWriter fw = new FileWriter(conf, StandardCharsets.UTF_8)) {
                fw.write(json);
            }
            return true;
        } catch (Exception e) {
            // not only IOException: encryption of the stored passwords may fail
            // as well, and it must not escape into the caller's error handling.
            log.error("cannot save configuration '" + conf.getAbsolutePath() + "'", e);
            UIUtils.error(null, I18n.t("common.config.save.failed", describe(e)));
        }

        return false;
    }
}

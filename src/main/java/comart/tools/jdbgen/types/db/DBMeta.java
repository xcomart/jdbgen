/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.db;

import comart.tools.jdbgen.types.JDBConnection;
import comart.tools.jdbgen.types.JDBDriver;
import comart.utils.AppDirs;
import comart.utils.StrUtils;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;

/**
 * An open database connection together with the metadata read through it.
 *
 * <p>The JDBC driver is loaded from the jar named by the {@link JDBDriver} in
 * a class loader of its own, so drivers of different connections cannot clash.
 * Schemas, tables and columns are read on demand and cached; a driver that
 * declares custom queries has them used instead of the corresponding
 * <code>DatabaseMetaData</code> calls.</p>
 *
 * <p>A JDBC connection is not thread safe, so every use of it - the optional
 * keep-alive timer included - is serialized internally. Closing the instance
 * stops the keep-alive, closes the connection and releases the driver class
 * loader; every metadata call afterwards is refused with an
 * <code>SQLException</code>.</p>
 *
 * @author comart
 */
@Slf4j
public class DBMeta implements AutoCloseable {
    /** the open database connection. */
    private final Connection conn;
    /** class loader holding the driver jar, closed together with the connection. */
    private final URLClassLoader child;

    // a JDBC connection is not thread safe, and the keep-alive timer runs
    // concurrently with whatever the generator is doing, so every use of
    // 'conn' - including the timer itself - goes through this lock
    /** guards every use of {@link #conn}. */
    private final ReentrantLock connLock = new ReentrantLock();
    /** the timer running the keep-alive, <code>null</code> when there is none. */
    private final ScheduledExecutorService keepAliveExec;
    /** the statement the keep-alive executes, <code>null</code> when there is none. */
    private final String keepAliveQuery;
    /** whether {@link #close()} was called; keeps the keep-alive off the closed connection. */
    private volatile boolean closed = false;

    /** the driver definition this connection was opened with. */
    private final JDBDriver driver;
    /** the driver metadata, read on first use. */
    private DatabaseMetaData dbmeta = null;
    /** all schemas of the database, <code>null</code> until they are read. */
    private ArrayList<DBSchema> schemas = null;
    /** the same schemas grouped by catalog, filled in together with {@link #schemas}. */
    private LinkedHashMap<String, List<DBSchema>> tree = null;

    /**
     * load the JDBC driver out of its jar and open the connection described by
     * <code>jconn</code>. When the connection asks for a keep-alive and its
     * settings are usable, a daemon timer executing the keep-alive query is
     * started as well; a keep-alive that cannot be started is logged and does
     * not fail the connection.
     *
     * @param driver the driver definition naming the jar, the driver class and
     *               the custom metadata queries.
     * @param jconn the connection settings: URL, credentials, driver properties
     *              and keep-alive. A user name or a password that is not set is
     *              left out of the driver properties rather than passed as
     *              <code>null</code>.
     * @throws Exception if the driver jar or class cannot be loaded, or the
     *         connection cannot be opened - including when the driver does not
     *         accept the connection URL. The driver class loader is released
     *         before the failure is passed on.
     */
    public DBMeta(JDBDriver driver, JDBConnection jconn) throws Exception {
        this.child = new URLClassLoader(
                new URL[] {AppDirs.resolve(driver.getJdbcJar()).toURI().toURL()},
                this.getClass().getClassLoader()
        );
        try {
            Class<?> driverClass = Class.forName(driver.getDriverClass(), true, child);
            Driver sqldriver = (Driver)driverClass.getDeclaredConstructor().newInstance();

            Properties props = new Properties();
            // a connection of a 'no auth' driver - and a freshly created one -
            // carries no credentials at all; Properties does not take a null
            // value, so an unset one is simply not passed to the driver
            if (jconn.getUserName() != null)
                props.setProperty("user", jconn.getUserName());
            if (jconn.getUserPassword() != null)
                props.setProperty("password", jconn.getUserPassword());
            if (jconn.getConnectionProps() != null)
                props.putAll(jconn.getConnectionProps());

            Connection c = sqldriver.connect(jconn.getConnectionUrl(), props);
            if (c == null) {
                // per the JDBC spec, Driver.connect returns null when the driver
                // does not understand the given URL
                throw new SQLException("Driver '" + driver.getDriverClass() +
                        "' does not accept the connection URL: " +
                        jconn.getConnectionUrl());
            }
            this.conn = c;
        } catch (Exception e) {
            closeChild();
            throw e;
        }
        this.driver = driver;

        int interval = keepAliveSeconds(jconn);
        ScheduledExecutorService exec = null;
        if (interval > 0) {
            this.keepAliveQuery = jconn.getKeepAliveQuery();
            try {
                exec = startKeepAlive(jconn.getName(), interval,
                        TimeUnit.SECONDS, this::keepAlive);
            } catch (Exception e) {
                // a keep-alive that cannot be started must not take down an
                // otherwise healthy connection
                log.warn("cannot start keep-alive for connection '{}'",
                        jconn.getName(), e);
            }
        } else {
            this.keepAliveQuery = null;
        }
        this.keepAliveExec = exec;
    }

    /**
     * the keep-alive interval configured on a connection. An interval that is
     * not a positive number is logged and turns the keep-alive off.
     *
     * @param jconn the connection settings, may be <code>null</code>.
     * @return the keep-alive interval in seconds, or 0 when this connection
     *         has no usable keep-alive configuration.
     */
    static int keepAliveSeconds(JDBConnection jconn) {
        if (jconn == null || !jconn.isUseKeepAlive() ||
                StrUtils.isEmpty(jconn.getKeepAliveSec()) ||
                StrUtils.isEmpty(jconn.getKeepAliveQuery()))
            return 0;
        try {
            int sec = Integer.parseInt(jconn.getKeepAliveSec().trim());
            if (sec > 0)
                return sec;
        } catch (NumberFormatException e) {
            // fall through to the warning below
        }
        log.warn("invalid keep-alive interval '{}' on connection '{}', keep-alive disabled",
                jconn.getKeepAliveSec(), jconn.getName());
        return 0;
    }

    /**
     * start a single threaded daemon timer running <code>action</code> at a
     * fixed rate. Anything the action throws is caught and logged, because
     * <code>scheduleAtFixedRate</code> would otherwise stop rescheduling it.
     *
     * @param connName name of the connection, used in the thread name and in
     *                 the log messages.
     * @param interval delay before the first run and between the runs.
     * @param unit unit of <code>interval</code>.
     * @param action the keep-alive action.
     * @return the scheduler running the action; the caller shuts it down.
     */
    static ScheduledExecutorService startKeepAlive(
            String connName, long interval, TimeUnit unit, Runnable action) {
        String threadName = "jdbgen-keepalive-" +
                (StrUtils.isEmpty(connName) ? "connection" : connName.trim());
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, threadName);
            // the timer must never hold the JVM up after the window is closed
            t.setDaemon(true);
            return t;
        });
        // scheduleAtFixedRate silently stops rescheduling as soon as a task
        // throws, so nothing may escape the task body
        exec.scheduleAtFixedRate(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                log.warn("keep-alive failed on connection '{}'", connName, t);
            }
        }, interval, interval, unit);
        return exec;
    }

    /**
     * one round of the keep-alive: execute the keep-alive statement unless the
     * instance is closed or the connection is busy with another query, in which
     * case the round is skipped rather than waited out. A failing statement is
     * logged only, so that the timer keeps running.
     */
    private void keepAlive() {
        if (closed)
            return;
        // a query in flight already keeps the connection busy, so there is
        // nothing to gain from waiting for it - skip this round instead
        if (!connLock.tryLock())
            return;
        try {
            if (closed || conn.isClosed())
                return;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(keepAliveQuery);
            }
        } catch (Exception e) {
            log.warn("keep-alive statement failed", e);
        } finally {
            connLock.unlock();
        }
    }

    /**
     * shut the keep-alive timer down, if one was started. A failure is logged
     * only, it must not keep the connection from being closed.
     */
    private void stopKeepAlive() {
        if (keepAliveExec == null)
            return;
        try {
            keepAliveExec.shutdownNow();
        } catch (Exception e) {
            log.warn("cannot stop keep-alive scheduler", e);
        }
    }

    /**
     * release the class loader holding the driver jar. A failure is logged
     * only.
     */
    private void closeChild() {
        try {
            child.close();
        } catch (IOException e) {
            log.warn("cannot close jdbc driver class loader", e);
        }
    }

    /**
     * stop the keep-alive, close the database connection and release the class
     * loader holding the driver jar. The class loader is released even when
     * closing the connection fails.
     *
     * @throws SQLException if the database connection cannot be closed.
     */
    @Override
    public void close() throws SQLException {
        closed = true;
        stopKeepAlive();
        try {
            connLock.lock();
            try {
                if (conn != null) {
                    conn.close();
                }
            } finally {
                connLock.unlock();
            }
        } finally {
            closeChild();
        }
    }

    /**
     * refuse to read anything through a connection that was closed. The driver
     * class loader is released together with the connection, so a metadata call
     * on a closed instance would otherwise fail somewhere inside the driver -
     * typically with a <code>NoClassDefFoundError</code> - instead of saying
     * what happened.
     *
     * @throws SQLException if {@link #close()} was called on this instance.
     */
    private void checkOpen() throws SQLException {
        if (closed)
            throw new SQLException("connection is closed");
    }

    /**
     * the driver metadata of the connection, asked for once and cached
     * afterwards.
     *
     * @return the <code>DatabaseMetaData</code> of the connection.
     * @throws SQLException if the metadata cannot be obtained.
     */
    private DatabaseMetaData getMetaData() throws SQLException {
        connLock.lock();
        try {
            if (dbmeta == null)
                dbmeta = conn.getMetaData();
        } finally {
            connLock.unlock();
        }
        return dbmeta;
    }

    /**
     * all schemas of the database, read once and cached afterwards.
     *
     * <p>The schemas are collected per catalog, a catalog the driver reports
     * without a name being called <code>"Default Catalog"</code>. A database
     * that has no catalogs at all - Oracle, say, whose driver answers
     * <code>getCatalogs()</code> with an empty result - has its schemas read
     * without one and grouped under <code>"Default Catalog"</code>. A catalog
     * without schemas gets a placeholder schema carrying only the catalog name,
     * and a database that reports neither catalogs nor schemas is represented by
     * a single <code>"Default Schema"</code> entry, so the list is never
     * empty.</p>
     *
     * @return the schemas, in catalog order.
     * @throws SQLException if the metadata cannot be read, or when this
     *         instance was closed.
     */
    public List<DBSchema> getSchemas() throws SQLException {
        checkOpen();
        connLock.lock();
        try {
            if (schemas == null) {
                ArrayList<DBSchema> res = new ArrayList<>();
                tree = new LinkedHashMap<>();
                DatabaseMetaData dbm = getMetaData();
                try (ResultSet rs = dbm.getCatalogs()) {
                    while (rs.next()) {
                        String catalog = rs.getString(1);
                        if (catalog == null)
                            catalog = "Default Catalog";
                        log.debug("catalog: {}", catalog);
                        tree.put(catalog, new ArrayList<>());
                    }
                }
                if (tree.isEmpty()) {
                    // a database without catalogs still has schemas; ask for
                    // them without one rather than falling straight through to
                    // the placeholder below
                    ArrayList<DBSchema> noCatalog = new ArrayList<>();
                    try (ResultSet rs = dbm.getSchemas(null, null)) {
                        while (rs.next()) {
                            DBSchema scheme = new DBSchema(rs);
                            log.debug("schema: {}", scheme);
                            noCatalog.add(scheme);
                        }
                    }
                    if (!noCatalog.isEmpty())
                        tree.put("Default Catalog", noCatalog);
                }
                for (Map.Entry<String, List<DBSchema>> ent:tree.entrySet()) {
                    if (ent.getValue().isEmpty()) {
                        String cat = ent.getKey();
                        if ("Default Catalog".equals(cat))
                            cat = null;
                        try (ResultSet rs = dbm.getSchemas(cat, null)) {
                            while (rs.next()) {
                                DBSchema scheme = new DBSchema(rs);
                                log.debug("schema: {}", scheme);
                                ent.getValue().add(scheme);
                            }
                        }
                    }
                    if (ent.getValue().isEmpty()) {
                        DBSchema scheme = new DBSchema();
                        scheme.setCatalog(ent.getKey());
                        ent.getValue().add(scheme);
                    }
                    res.addAll(ent.getValue());
                }
                if (res.isEmpty()) {
                    DBSchema scheme = new DBSchema();
                    scheme.setName("Default Schema");
                    res.add(scheme);
                    tree.put("Default Catalog", res);
                }
                schemas = res;
            }
        } finally {
            connLock.unlock();
        }
        return schemas;
    }
    
    /**
     * the same schemas as {@link #getSchemas()}, grouped by catalog for the
     * database tree.
     *
     * @return the schemas per catalog name, in the order the driver reports the
     *         catalogs.
     * @throws SQLException if the metadata cannot be read, or when this
     *         instance was closed.
     */
    public Map<String, List<DBSchema>> getSchemaTree() throws SQLException {
        checkOpen();
        getSchemas(); // to ensure build tree
        return tree;
    }
    
    /**
     * read the tables of a schema, either through
     * <code>DatabaseMetaData.getTables()</code> or, when the driver declares
     * one, through its custom table query, whose <code>${...}</code> variables
     * are filled in from the schema. When the driver declares a table comment
     * query, the comments it returns are applied to the tables found by name.
     *
     * @param schema the schema to read.
     * @return the tables of the schema, of every type the database reports.
     * @throws Exception if the metadata or one of the custom queries fails.
     */
    private ArrayList<DBTable> getTables(DBSchema schema) throws Exception {
        ArrayList<DBTable> tables = new ArrayList<>();
        HashMap<String, DBTable> tableMap = new HashMap<>();
        if (driver.isUseTables()) {
            String sql = driver.getTablesSql();
            sql = StrUtils.replaceWith(sql, schema, "${", "}");
            try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql))
            {
                while (rs.next()) {
                    DBTable table = new DBTable(rs);
                    tables.add(table);
                    tableMap.put(table.getName(), table);
                }
            }
            
        } else {
            DatabaseMetaData dbm = getMetaData();
            try (ResultSet rs = dbm.getTables(
                    schema.getCatalog(), schema.getSchema(), null, null)) {
                while (rs.next()) {
                    DBTable table = new DBTable(rs);
                    tables.add(table);
                    tableMap.put(table.getName(), table);
                }
            }
        }
        if (driver.isUseTableComments()) {
            String sql = driver.getTableCommentsSql();
            sql = StrUtils.replaceWith(sql, schema, "${", "}");
            try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql))
            {
                while (rs.next()) {
                    String tname = rs.getString(1);
                    String remarks = rs.getString(2);
                    DBTable table = tableMap.get(tname);
                    if (table != null)
                        table.setRemarks(remarks);
                }
            }
        }
        return tables;
    }
    
    /**
     * the tables of a schema, read once per schema and cached on it. Depending
     * on the driver definition they come from
     * <code>DatabaseMetaData.getTables()</code> or from the custom table query,
     * and their comments from the custom table comment query.
     *
     * @param schema the schema to list, also the cache of the result.
     * @param includeViews <code>true</code> to return views next to tables.
     * @return the tables of the schema, views included when asked for.
     * @throws SQLException when this instance was closed.
     * @throws Exception if the metadata or one of the custom queries fails.
     */
    public List<DBTable> getTables(
            DBSchema schema, boolean includeViews) throws Exception {
        checkOpen();
        synchronized(schema) {
            if (schema.getTables() == null) {
                connLock.lock();
                try {
                    ArrayList<DBTable> tables = getTables(schema);
                    schema.setTables(tables);
                } finally {
                    connLock.unlock();
                }
            }
        }
        return schema.getTables().stream()
                .filter(s -> "TABLE".equals(s.getType())|| (includeViews && "VIEW".equals(s.getType())))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * the columns of a table, read once per table and cached on it. Reading
     * them also fills in the primary key flag of the columns and the key and
     * non-key lists of the table, the key list in the order the key was
     * declared in. Depending on the driver definition the columns come from
     * <code>DatabaseMetaData</code> or from the custom column and column
     * comment queries.
     *
     * @param table the table to list, also the cache of the result.
     * @return the columns of the table, in the order the database reports them.
     * @throws SQLException when this instance was closed.
     * @throws Exception if the metadata or one of the custom queries fails.
     */
    public List<DBColumn> getTableColumns(DBTable table) throws Exception {
        checkOpen();
        synchronized(table) {
            if (table.getColumns() == null) {
                connLock.lock();
                try {
                    loadColumns(table);
                } finally {
                    connLock.unlock();
                }
            }
        }
        return table.getColumns();
    }
    
    /**
     * read the columns of a table and store them, together with the key and the
     * non-key columns, on the table itself.
     *
     * <p>The columns come either from <code>DatabaseMetaData</code> - the
     * primary key being read separately - or from the custom column query of
     * the driver, which reports the key flag in its <code>IS_KEY</code> column.
     * Every column is numbered in the order it is read, and a custom column
     * comment query, where the driver declares one, replaces the comments.</p>
     *
     * <p>The key list is in the order the key was declared in: the metadata
     * path sorts the rows of <code>getPrimaryKeys()</code>, which arrive by
     * column name, by their <code>KEY_SEQ</code>, while the custom column query
     * keeps the order its own result set is in.</p>
     *
     * @param table the table to read the columns of.
     * @throws Exception if the metadata or one of the custom queries fails.
     */
    private void loadColumns(DBTable table) throws Exception {
        DatabaseMetaData dbm = getMetaData();
        HashMap<String,DBColumn> colmap = new HashMap<>();

        ArrayList<DBColumn> columns = new ArrayList<>();
        ArrayList<DBColumn> keyFields = new ArrayList<>();
        ArrayList<DBColumn> notKeys = new ArrayList<>();
        if (driver.isUseColumns()) {
            String sql = driver.getColumnsSql();
            sql = StrUtils.replaceWith(sql, table, "${", "}");
            try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql))
            {
                while (rs.next()) {
                    DBColumn column = new DBColumn(rs);
                    columns.add(column);
                    column.setNo(columns.size());
                    colmap.put(column.getName(), column);
                    if (StrUtils.toInt(rs.getString("IS_KEY")) != 0) {
                        column.setKey(true);
                        keyFields.add(column);
                    }
                }
            }
        } else {
            try (ResultSet rs = dbm.getColumns(
                    table.getCatalog(), table.getSchema(), table.getTable(), null)) {
                while (rs.next()) {
                    DBColumn column = new DBColumn(rs);
                    columns.add(column);
                    column.setNo(columns.size());
                    colmap.put(column.getName(), column);
                }
            }

            // getPrimaryKeys() is ordered by COLUMN_NAME, so the rows do not
            // arrive in the order the key was declared in; KEY_SEQ carries that
            // order and is what the key list is sorted by
            ArrayList<int[]> keyOrder = new ArrayList<>();
            try (ResultSet rs = dbm.getPrimaryKeys(
                    table.getCatalog(), table.getSchema(), table.getTable())) {
                while (rs.next()) {
                    String key = rs.getString("COLUMN_NAME");
                    DBColumn column = colmap.get(key);
                    if (column != null) {
                        column.setKey(true);
                        keyOrder.add(new int[] { rs.getShort("KEY_SEQ"), keyFields.size() });
                        keyFields.add(column);
                    } else {
                        log.warn("primary key column '{}' of table '{}' not found "+
                                "in column list - the driver may report it with "+
                                "different letter case.", key, table.getTable());
                    }
                }
            }
            // a driver that reports no usable KEY_SEQ leaves the rows where they
            // are, because the sort is stable on the second element
            keyOrder.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
                    : Integer.compare(a[1], b[1]));
            ArrayList<DBColumn> ordered = new ArrayList<>(keyFields.size());
            for (int[] pos: keyOrder)
                ordered.add(keyFields.get(pos[1]));
            keyFields.clear();
            keyFields.addAll(ordered);
        }
        
        if (driver.isUseColumnComments()) {
            String sql = driver.getColumnCommentsSql();
            sql = StrUtils.replaceWith(sql, table, "${", "}");
            Map<String,String> comments = new HashMap<>();
            try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql))
            {
                while (rs.next())
                    comments.put(rs.getString(1), rs.getString(2));
            }
            columns.forEach(c -> c.setRemarks(comments.get(c.getColumn())));
        }

        columns.forEach(c -> {
            if (!keyFields.contains(c))
                notKeys.add(c);
        });
        table.setColumns(columns);
        table.setKeys(keyFields);
        table.setNotKeys(notKeys);
    }
}

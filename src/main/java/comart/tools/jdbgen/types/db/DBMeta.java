/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comart.tools.jdbgen.types.db;

import comart.tools.jdbgen.types.JDBConnection;
import comart.tools.jdbgen.types.JDBDriver;
import comart.utils.StrUtils;
import java.io.File;
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
 *
 * @author comart
 */
@Slf4j
public class DBMeta implements AutoCloseable {
    private final Connection conn;
    private final URLClassLoader child;

    // a JDBC connection is not thread safe, and the keep-alive timer runs
    // concurrently with whatever the generator is doing, so every use of
    // 'conn' - including the timer itself - goes through this lock
    private final ReentrantLock connLock = new ReentrantLock();
    private final ScheduledExecutorService keepAliveExec;
    private final String keepAliveQuery;
    private volatile boolean closed = false;

    private final JDBDriver driver;
    private DatabaseMetaData dbmeta = null;
    private ArrayList<DBSchema> schemas = null;
    private LinkedHashMap<String, List<DBSchema>> tree = null;

    public DBMeta(JDBDriver driver, JDBConnection jconn) throws Exception {
        this.child = new URLClassLoader(
                new URL[] {new File(driver.getJdbcJar()).toURI().toURL()},
                this.getClass().getClassLoader()
        );
        try {
            Class<?> driverClass = Class.forName(driver.getDriverClass(), true, child);
            Driver sqldriver = (Driver)driverClass.getDeclaredConstructor().newInstance();

            Properties props = new Properties();
            props.setProperty("user", jconn.getUserName());
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

    private void stopKeepAlive() {
        if (keepAliveExec == null)
            return;
        try {
            keepAliveExec.shutdownNow();
        } catch (Exception e) {
            log.warn("cannot stop keep-alive scheduler", e);
        }
    }

    private void closeChild() {
        try {
            child.close();
        } catch (IOException e) {
            log.warn("cannot close jdbc driver class loader", e);
        }
    }

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

    public List<DBSchema> getSchemas() throws SQLException {
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
                for (Map.Entry<String, List<DBSchema>> ent:tree.entrySet()) {
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
    
    public Map<String, List<DBSchema>> getSchemaTree() throws SQLException {
        getSchemas(); // to ensure build tree
        return tree;
    }
    
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
    
    public List<DBTable> getTables(
            DBSchema schema, boolean includeViews) throws Exception {
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
    
    public List<DBColumn> getTableColumns(DBTable table) throws Exception {
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

            try (ResultSet rs = dbm.getPrimaryKeys(
                    table.getCatalog(), table.getSchema(), table.getTable())) {
                while (rs.next()) {
                    String key = rs.getString("COLUMN_NAME");
                    DBColumn column = colmap.get(key);
                    if (column != null) {
                        column.setKey(true);
                        keyFields.add(column);
                    } else {
                        log.warn("primary key column '{}' of table '{}' not found "+
                                "in column list - the driver may report it with "+
                                "different letter case.", key, table.getTable());
                    }
                }
            }
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

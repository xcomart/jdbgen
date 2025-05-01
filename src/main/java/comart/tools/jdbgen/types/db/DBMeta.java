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
import java.util.logging.Logger;

/**
 *
 * @author comart
 */
public class DBMeta implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(DBMeta.class.getName());
    private final Connection conn;
    
    private JDBDriver driver = null;
    private DatabaseMetaData dbmeta = null;
    private ArrayList<DBSchema> schemas = null;
    private LinkedHashMap<String, List<DBSchema>> tree = null;
    
    public DBMeta(JDBDriver driver, JDBConnection jconn) throws Exception {
        URLClassLoader child = new URLClassLoader(
                new URL[] {new File(driver.getJdbcJar()).toURI().toURL()},
                this.getClass().getClassLoader()
        );
        Class driverClass = Class.forName(driver.getDriverClass(), true, child);
        Driver sqldriver = (Driver)driverClass.getDeclaredConstructor().newInstance();

        Properties props = new Properties();
        props.setProperty("user", jconn.getUserName());
        props.setProperty("password", jconn.getUserPassword());
        props.putAll(jconn.getConnectionProps());

        this.conn = sqldriver.connect(jconn.getConnectionUrl(), props);
        this.driver = driver;
    }
    
    @Override
    public void close() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }
    
    private DatabaseMetaData getMetaData() throws SQLException {
        synchronized (this) {
            if (dbmeta == null)
                dbmeta = conn.getMetaData();
        }
        return dbmeta;
    }
    
    public List<DBSchema> getSchemas() throws SQLException {
        synchronized (this) {
            if (schemas == null) {
                ArrayList<DBSchema> res = new ArrayList<>();
                tree = new LinkedHashMap<>();
                DatabaseMetaData dbm = getMetaData();
                try (ResultSet rs = dbm.getCatalogs()) {
                    while (rs.next()) {
                        String catalog = rs.getString(1);
                        if (catalog == null)
                            catalog = "Default Catalog";
                        System.out.println("catalog: " + catalog);
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
                            System.out.println("schema: " + scheme);
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
                ArrayList<DBTable> tables = getTables(schema);
                schema.setTables(tables);
            }
        }
        return schema.getTables().stream()
                .filter(s -> "TABLE".equals(s.getType())|| (includeViews && "VIEW".equals(s.getType())))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<DBColumn> getTableColumns(DBTable table) throws Exception {
        synchronized(table) {
            if (table.getColumns() == null) {
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
                        DBColumn column = new DBColumn(rs);
                        columns.add(column);
                        column.setNo(columns.size());
                        colmap.put(column.getName(), column);
                        if (StrUtils.toInt(rs.getString("IS_KEY")) == 0) {
                            notKeys.add(column);
                        } else {
                            keyFields.add(column);
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
                            column.setKey(true);
                            keyFields.add(column);
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
        return table.getColumns();
    }
}

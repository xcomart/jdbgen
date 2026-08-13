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

import comart.utils.StrUtils;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A JDBC driver definition of the configuration: the driver jar and class used
 * to open a connection, together with the optional SQL statements that replace
 * the metadata calls of <code>DatabaseMetaData</code> for databases that do not
 * report tables, columns or comments the way the generator needs them.
 *
 * @author comart
 */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder=true)
@NoArgsConstructor
@AllArgsConstructor
public class JDBDriver extends JDBListBase {
    /** whether this is a predefined driver; its name, class and icon cannot be edited or deleted. */
    private boolean stockItem;
    /** connection URL skeleton offered when a connection picks this driver. */
    private String urlTemplate;
    /** path of the jar holding the driver classes. */
    private String jdbcJar;
    /** fully qualified name of the <code>java.sql.Driver</code> implementation. */
    private String driverClass;
    /** Maven search term proposed when the driver jar is downloaded. */
    private String defaultQuery;
    /** default connection properties offered to connections of this driver. */
    private Map<String, String> props;
    /** whether the database takes no user name and password. */
    private boolean noAuth;
    /** whether table comments are read with {@link #tableCommentsSql} instead of the driver metadata. */
    private boolean useTableComments;
    /** query returning table name and comment per row. */
    private String tableCommentsSql;
    /** whether column comments are read with {@link #columnCommentsSql} instead of the driver metadata. */
    private boolean useColumnComments;
    /** query returning column name and comment per row. */
    private String columnCommentsSql;
    /** whether the table list is read with {@link #tablesSql} instead of the driver metadata. */
    private boolean useTables;
    /** query returning the table list, with the schema fields available as <code>${...}</code> variables. */
    private String tablesSql;
    /** whether the column list is read with {@link #columnsSql} instead of the driver metadata. */
    private boolean useColumns;
    /** query returning the column list, with the table fields available as <code>${...}</code> variables. */
    private String columnsSql;
    
    /**
     * whether this driver carries the settings needed to open a connection: a
     * driver jar, a driver class and, for every custom query that is turned on
     * - the table and column lists as well as their comments - the statement
     * itself.
     *
     * @return <code>true</code> when the driver may be used as it is.
     */
    public boolean validate() {
        return !StrUtils.isEmpty(jdbcJar) &&
                !StrUtils.isEmpty(driverClass) &&
                (!useTables || !StrUtils.isEmpty(tablesSql)) &&
                (!useColumns || !StrUtils.isEmpty(columnsSql)) &&
                (!useTableComments || !StrUtils.isEmpty(tableCommentsSql)) &&
                (!useColumnComments || !StrUtils.isEmpty(columnCommentsSql));
    }
}

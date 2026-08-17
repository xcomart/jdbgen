/*
 * The MIT License
 *
 * Copyright 2024 comart.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package comart.tools.jdbgen.types.db;

import comart.tools.jdbgen.template.TestResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single result set row backed by a map, so that the models reading a
 * <code>ResultSet</code> can be tested without a database.
 *
 * <p>A column that is not in the map is answered the way a driver answers an
 * unknown label - with a {@link SQLException} - while a column mapped to
 * <code>null</code> is a column that is there and holds no value.</p>
 */
public class MapResultSet extends TestResultSet {

    private final Map<String, Object> values = new LinkedHashMap<>();

    /**
     * add a column to this row.
     *
     * @param label column label, as a driver reports it.
     * @param value value of the column, may be <code>null</code>.
     * @return this row, so that columns can be chained.
     */
    public MapResultSet with(String label, Object value) {
        values.put(label, value);
        return this;
    }

    private Object value(String label) throws SQLException {
        if (!values.containsKey(label))
            throw new SQLException("no such column: " + label);
        return values.get(label);
    }

    /** the columns in the order they were added, one based as in JDBC. */
    private Object value(int index) throws SQLException {
        if (index < 1 || index > values.size())
            throw new SQLException("no such column: " + index);
        return values.values().toArray()[index - 1];
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        Object val = value(columnLabel);
        return val == null ? null : String.valueOf(val);
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        Object val = value(columnIndex);
        return val == null ? null : String.valueOf(val);
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        Object val = value(columnLabel);
        return val == null ? 0 : Short.parseShort(String.valueOf(val));
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        Object val = value(columnLabel);
        return val == null ? 0 : Integer.parseInt(String.valueOf(val));
    }

    @Override
    public boolean wasNull() throws SQLException {
        return false;
    }
}

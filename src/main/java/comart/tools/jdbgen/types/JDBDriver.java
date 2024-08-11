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
 *
 * @author comart
 */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder=true)
@NoArgsConstructor
@AllArgsConstructor
public class JDBDriver extends JDBListBase {
    private boolean stockItem;
    private String urlTemplate;
    private String jdbcJar;
    private String driverClass;
    private String defaultQuery;
    private Map<String, String> props;
    private boolean noAuth;
    private boolean useTableComments;
    private String tableCommentsSql;
    private boolean useColumnComments;
    private String columnCommentsSql;
    private boolean useTables;
    private String tablesSql;
    private boolean useColumns;
    private String columnsSql;
    
    public boolean validate() {
        return !StrUtils.isEmpty(jdbcJar) &&
                !StrUtils.isEmpty(driverClass) &&
                (!useTableComments || !StrUtils.isEmpty(tableCommentsSql)) &&
                (!useColumnComments || !StrUtils.isEmpty(columnCommentsSql));
    }
}

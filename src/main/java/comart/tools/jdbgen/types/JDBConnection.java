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

import com.google.gson.annotations.JsonAdapter;
import comart.utils.EncryptionAdapter;
import comart.utils.StrUtils;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A database connection of the configuration: everything needed to open a JDBC
 * connection plus the templates and the output directory the generator uses for
 * it. The URL, the user name and the password are stored encrypted with the
 * master password.
 *
 * @author comart
 */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder=true)
@NoArgsConstructor
@AllArgsConstructor
public class JDBConnection extends JDBListBase {
    /** name of the {@link JDBDriver} this connection is opened with. */
    private String driverType;
    /** JDBC connection URL, stored encrypted. */
    @JsonAdapter(EncryptionAdapter.class)
    private String connectionUrl;
    /** database user name, stored encrypted. */
    @JsonAdapter(EncryptionAdapter.class)
    private String userName;
    /** database password, stored encrypted. */
    @JsonAdapter(EncryptionAdapter.class)
    private String userPassword;
    /** additional properties handed to the JDBC driver on connect. */
    private Map<String, String> connectionProps;
    /** whether the connection is held open by a periodic keep-alive query. */
    private boolean useKeepAlive;
    /** keep-alive interval in seconds, as text. */
    private String keepAliveSec;
    /** the statement executed on every keep-alive round. */
    private String keepAliveQuery;
    /** the templates generated for the tables of this connection. */
    private List<JDBTemplate> templates;
    /** directory the generated sources are written to. */
    private String outputDir;
    /** author name made available to the templates. */
    private String author;
    /** user defined variables made available to the templates. */
    private Map<String, String> customVars;
    
    /**
     * whether this connection carries the settings needed to open it: a driver
     * type, a connection URL, an output directory and, when the keep-alive is
     * turned on, a keep-alive query.
     *
     * @return <code>true</code> when the connection may be used as it is.
     */
    public boolean validate() {
        return !StrUtils.isEmpty(driverType) &&
                !StrUtils.isEmpty(connectionUrl) &&
                !StrUtils.isEmpty(outputDir) &&
                (!useKeepAlive || !StrUtils.isEmpty(keepAliveQuery));
    }
}

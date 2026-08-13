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
package comart.tools.jdbgen.types;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single abbreviation rule of the abbreviation mapper. While a database
 * identifier is turned into a template variable, every rule that is enabled
 * replaces its <code>abbr</code> with <code>replaceTo</code>, either on the
 * whole identifier or on one underscore separated word of it.
 *
 * @author comart
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JDBAbbr {
    /** whether this rule takes part in the replacement. */
    Boolean check;
    /** <code>true</code> to match the whole identifier, <code>false</code> to match a single word of it. */
    Boolean totalName;
    /** the abbreviation to look for, matched case insensitively. */
    String abbr;
    /** the text the abbreviation is replaced with. */
    String replaceTo;
    
    /**
     * this rule as a row of the abbreviation table model.
     *
     * @return the field values in table column order: check, total name,
     *         abbreviation and replacement.
     */
    public Object[] getRowArray() {
        return new Object[]{ check, totalName, abbr, replaceTo };
    }
    
    /**
     * a short debugging representation of this rule.
     *
     * @return the rule as <code>{abbr:replaceTo}</code>.
     */
    @Override
    public String toString() {
        return "{" + abbr + ":" + replaceTo + "}";
    }
    
    /** word level rules, keyed by the lower cased abbreviation. */
    public static Map<String, String> abbrMap = null;
    /** whole identifier rules, keyed by the lower cased abbreviation. */
    public static Map<String, String> abbrNameMap = null;
    /**
     * rebuild {@link #abbrMap} and {@link #abbrNameMap} from the rules of the
     * current configuration. Only rules whose <code>check</code> is
     * <code>true</code> are taken over, and a rule with an unset
     * <code>totalName</code> is treated as a word level rule.
     */
    public static void buildMap() {
        abbrMap = new HashMap<>();
        abbrNameMap = new HashMap<>();
        JDBGenConfig.getInstance().getAbbrs().forEach(a -> {
            if (Boolean.TRUE.equals(a.check)) {
                if (a.totalName == null) a.totalName = false;
                if (a.totalName) {
                    abbrNameMap.put(a.abbr.toLowerCase(), a.replaceTo);
                } else {
                    abbrMap.put(a.abbr.toLowerCase(), a.replaceTo);
                }
            }
        });
    }
}

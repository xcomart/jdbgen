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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Common base of the configuration entries that are presented as an icon and a
 * name in a list, such as connections, drivers and presets. The name doubles as
 * the display title, so subclasses only have to add their own settings.
 *
 * @author comart
 */
@Data
@SuperBuilder(toBuilder=true)
@NoArgsConstructor
@AllArgsConstructor
public class JDBListBase implements HasIcon, HasTitle, Cloneable {
    /** user given name of this entry, also used as its display title. */
    private String name;
    /** icon locator of this entry, in the form described by {@link HasIcon#getIcon()}. */
    private String icon;
    
    /**
     * display title of this entry, which is its name.
     *
     * @return the value of <code>name</code>.
     */
    @Override
    public String getTitle() {
        return getName();
    }
    
    /**
     * the title of this entry, so that a plain list model renders it correctly.
     *
     * @return the value of {@link #getTitle()}.
     */
    @Override
    public String toString() {
        return getTitle();
    }
    
    /**
     * a shallow copy of this entry.
     *
     * @return the cloned entry; subclasses that hold mutable collections
     *         override this to copy them as well.
     * @throws CloneNotSupportedException if a subclass does not support
     *         cloning.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

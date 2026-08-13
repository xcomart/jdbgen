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

/**
 * One generation rule: the template file to render for each selected table and
 * the pattern that names the file it is written to.
 *
 * @author comart
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JDBTemplate implements Cloneable {
    /** user given name of this template entry. */
    private String name;
    /** path of the template file that is rendered. */
    private String templateFile;
    /** name pattern of the generated file, with <code>${...}</code> variables such as <code>${name.suffix.pascal}</code>. */
    private String outTemplate;
    
    /**
     * this entry as a row of the template table model.
     *
     * @return the field values in table column order: name, template file and
     *         output name pattern.
     */
    public Object[] getRowArray() {
        return new Object[]{ name, templateFile, outTemplate };
    }
    
    /**
     * a copy of this entry. All fields are immutable strings, so the shallow
     * copy is independent of the original.
     *
     * @return the cloned template entry.
     * @throws CloneNotSupportedException if cloning fails.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

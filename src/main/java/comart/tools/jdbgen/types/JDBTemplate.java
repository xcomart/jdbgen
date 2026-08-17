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
public class JDBTemplate implements Cloneable {
    /** user given name of this template entry. */
    private String name;
    /** path of the template file that is rendered. */
    private String templateFile;
    /** name pattern of the generated file, with <code>${...}</code> variables such as <code>${name.suffix.pascal}</code>. */
    private String outTemplate;
    /**
     * whether this entry is ticked for generation in the main window. It is
     * part of the stored connection, so the ticks of the last run are offered
     * again on the next start. A configuration written before this field
     * existed simply leaves it <code>false</code>.
     */
    private boolean selected;

    /**
     * a template entry that is not ticked for generation.
     *
     * @param name
     *            user given name of the entry.
     * @param templateFile
     *            path of the template file that is rendered.
     * @param outTemplate
     *            name pattern of the generated file.
     */
    public JDBTemplate(String name, String templateFile, String outTemplate) {
        this(name, templateFile, outTemplate, false);
    }

    /**
     * a template entry with every field given.
     *
     * @param name
     *            user given name of the entry.
     * @param templateFile
     *            path of the template file that is rendered.
     * @param outTemplate
     *            name pattern of the generated file.
     * @param selected
     *            whether the entry is ticked for generation.
     */
    public JDBTemplate(String name, String templateFile, String outTemplate,
            boolean selected) {
        this.name = name;
        this.templateFile = templateFile;
        this.outTemplate = outTemplate;
        this.selected = selected;
    }

    /**
     * this entry as a row of a three column template table model, such as the
     * one of the preset dialog. The selection flag is not part of it, that
     * column only exists in the template table of the main window.
     *
     * @return the field values in table column order: name, template file and
     *         output name pattern.
     */
    public Object[] getRowArray() {
        return new Object[]{ name, templateFile, outTemplate };
    }
    
    /**
     * a copy of this entry. Every field is a string or a primitive, so the
     * shallow copy is independent of the original.
     *
     * @return the cloned template entry.
     * @throws CloneNotSupportedException if cloning fails.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

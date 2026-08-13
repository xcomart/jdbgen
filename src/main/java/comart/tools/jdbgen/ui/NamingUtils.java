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
package comart.tools.jdbgen.ui;

import comart.tools.jdbgen.types.JDBListBase;
import java.util.List;

/**
 * naming helpers for the list based editors of the user interface. The
 * dialogs which manage lists of named items(connections, templates and so
 * on) use these methods to keep the <code>name</code> property of every
 * <code>JDBListBase</code> element unique inside its list.
 *
 * @author comart
 */
public class NamingUtils {
    /**
     * check whether an item named <code>name</code> is already contained in
     * <code>list</code>.
     *
     * @param list
     *            list of items to be searched.
     * @param name
     *            name to be looked up, compared with
     *            <code>JDBListBase.getName()</code>.
     * @return <code>true</code> if any element of <code>list</code> carries
     *         the given name, <code>false</code> otherwise.
     */
    public static boolean nameExists(List<? extends JDBListBase> list, String name) {
        return list.stream().anyMatch((d) -> {
            return name.equals(d.getName());
        });
    }

    /**
     * build a name which does not collide with any item of <code>list</code>.
     * <code>name</code> itself is returned when it is still unused, otherwise
     * a counter is appended as <code>name + " - " + count</code>, starting at
     * <code>0</code> and increased until the resulting name is free.
     *
     * @param list
     *            list the new name has to be unique in.
     * @param name
     *            preferred name.
     * @return <code>name</code>, or a suffixed variant of it which no element
     *         of <code>list</code> uses yet.
     */
    public static String nextNameOf(List<? extends JDBListBase> list, String name) {
        int count = 0;

        String ret;
        for(ret = name; nameExists(list, ret); count++) {
            ret = name + " - " + count;
        }

        return ret;
    }
    
}

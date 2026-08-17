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

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Where the main window was and how it was divided when it was last closed, so
 * that the next start comes up the way the user left it.
 *
 * <p>Every entry carries a value that means "nothing stored yet", which is what
 * a fresh configuration - and a configuration written by a release that did not
 * know this entry - reads back as: the geometry is then left to
 * <code>pack()</code> and to the default divider positions.</p>
 *
 * @author comart
 */
@Data
@NoArgsConstructor
public class WindowState {
    /** window width in pixels, zero or less while nothing is stored. */
    private int width = 0;
    /** window height in pixels, zero or less while nothing is stored. */
    private int height = 0;
    /**
     * x position of the window. It may well be negative on a screen left of the
     * primary one, so it is only read when {@link #width} and {@link #height}
     * say that a geometry has been stored at all - the position is always
     * written together with the size.
     */
    private int x = 0;
    /**
     * y position of the window, read under the same condition as {@link #x}.
     */
    private int y = 0;
    /**
     * whether the window was maximized. The size and the position keep the
     * values the window had before it was maximized, which is what restoring it
     * down has to come back to.
     */
    private boolean maximized = false;
    /**
     * divider position between the schema tree and the rest of the work area,
     * in pixels. Zero or less while nothing is stored.
     */
    private int schemaDivider = -1;
    /**
     * divider position between the table list and the generation options, in
     * pixels. Zero or less while nothing is stored.
     */
    private int optionsDivider = -1;

    /**
     * whether a window size has been stored, which is also what says that
     * {@link #x} and {@link #y} carry a position.
     *
     * @return <code>true</code> when both the width and the height are set.
     */
    public boolean hasBounds() {
        return width > 0 && height > 0;
    }
}

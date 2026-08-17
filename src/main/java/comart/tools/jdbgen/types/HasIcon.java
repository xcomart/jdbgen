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

/**
 * Marker for model objects that can be rendered with an icon in the user
 * interface. List and tree renderers look for this interface and decorate the
 * cell with the icon named by {@link #getIcon()}.
 *
 * @author comart
 */
public interface HasIcon {
    /**
     * icon locator of this item. Besides a plain file path, the following
     * prefixed forms are understood by the icon loader: <code>stock:</code>
     * for an icon bundled with the application, <code>fa:</code> for a Font
     * Awesome glyph, <code>color:</code> for a solid color swatch and
     * <code>http</code> for a remote image.
     *
     * @return icon locator, or <code>null</code>/empty when the item has no
     *         icon.
     */
    String getIcon();
}

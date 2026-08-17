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
package comart.utils;

import comart.utils.tuple.Pair;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two element tuple. {@link comart.utils.UIUtils} keeps the components an
 * icon was put on in a <code>HashSet</code> of pairs, so value equality and a
 * hash code that agrees with it are what this type is used for.
 */
public class PairTest {

    @Test
    public void bothElementsAreReadableAndWritable() {
        Pair<String, Integer> pair = new Pair<>("key", 1);

        assertEquals("key", pair.getFirst());
        assertEquals(Integer.valueOf(1), pair.getSecond());

        pair.setFirst("other");
        pair.setSecond(2);
        assertEquals("other", pair.getFirst());
        assertEquals(Integer.valueOf(2), pair.getSecond());
    }

    @Test
    public void twoPairsWithEqualElementsAreEqual() {
        Pair<String, Integer> one = new Pair<>("key", 1);
        Pair<String, Integer> same = new Pair<>("key", 1);

        assertEquals(one, same);
        assertEquals(one.hashCode(), same.hashCode());
        assertNotEquals(one, new Pair<>("key", 2));
        assertNotEquals(one, new Pair<>("other", 1));
        assertNotEquals(one, "key");
    }

    @Test
    public void aPairIsUsableAsASetElement() {
        Set<Pair<String, Integer>> set = new HashSet<>();
        set.add(new Pair<>("key", 1));
        set.add(new Pair<>("key", 1));
        set.add(new Pair<>("key", 2));

        assertEquals(2, set.size(), "an equal pair is the same element");
        assertTrue(set.contains(new Pair<>("key", 1)));
    }

    @Test
    public void bothElementsMayBeMissing() {
        Pair<String, String> pair = new Pair<>(null, null);

        assertNull(pair.getFirst());
        assertNull(pair.getSecond());
        assertEquals(new Pair<String, String>(null, null), pair);
        assertNotEquals(new Pair<>("a", null), pair);
    }

    @Test
    public void bothElementsAreNamedInTheTextForm() {
        String text = new Pair<>("key", 1).toString();

        assertTrue(text.contains("key"), text);
        assertTrue(text.contains("1"), text);
    }
}

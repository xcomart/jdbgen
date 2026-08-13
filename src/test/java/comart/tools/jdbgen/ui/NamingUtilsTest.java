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
package comart.tools.jdbgen.ui;

import comart.tools.jdbgen.types.JDBConnection;
import comart.tools.jdbgen.types.JDBListBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every list of the configuration is addressed by name, so the editors must
 * never be able to put two entries of the same name into one list.
 */
public class NamingUtilsTest {

    private static List<JDBListBase> listOf(String... names) {
        List<JDBListBase> res = new ArrayList<>();
        for (String name: names)
            res.add(new JDBListBase(name, null));
        return res;
    }

    @Test
    public void aNameOfTheListIsFound() {
        List<JDBListBase> list = listOf("Sample", "Other");

        assertTrue(NamingUtils.nameExists(list, "Sample"));
        assertTrue(NamingUtils.nameExists(list, "Other"));
    }

    @Test
    public void aNameThatIsNotInTheListIsNotFound() {
        assertFalse(NamingUtils.nameExists(listOf("Sample"), "Missing"));
        assertFalse(NamingUtils.nameExists(new ArrayList<>(), "Sample"),
                "nothing collides in an empty list");
    }

    @Test
    public void namesAreComparedExactly() {
        List<JDBListBase> list = listOf("Sample");

        assertFalse(NamingUtils.nameExists(list, "sample"), "the case is part of the name");
        assertFalse(NamingUtils.nameExists(list, "Sample "), "so are the blanks");
    }

    @Test
    public void anEntryWithoutANameCollidesWithNothing() {
        List<JDBListBase> list = listOf("Sample", null);

        assertFalse(NamingUtils.nameExists(list, "Other"),
                "an entry without a name must not break the comparison");
        assertTrue(NamingUtils.nameExists(list, "Sample"));
    }

    @Test
    public void aNameThatIsNotThereCollidesWithNothing() {
        // the lookup used to dereference the name it was given
        assertFalse(NamingUtils.nameExists(listOf("Sample"), null));
        assertFalse(NamingUtils.nameExists(new ArrayList<>(), null));
        // not even with an entry which carries no name either: an entry that is
        // not named yet is not an entry named 'nothing'
        assertFalse(NamingUtils.nameExists(listOf("Sample", null), null));
    }

    @Test
    public void aFreeNameIsKeptAsItIs() {
        assertEquals("Sample", NamingUtils.nextNameOf(listOf("Other"), "Sample"));
        assertEquals("Sample", NamingUtils.nextNameOf(new ArrayList<>(), "Sample"));
    }

    @Test
    public void aTakenNameGetsACounter() {
        assertEquals("Sample - 0", NamingUtils.nextNameOf(listOf("Sample"), "Sample"));
    }

    @Test
    public void theCounterIsRaisedUntilTheNameIsFree() {
        List<JDBListBase> list = listOf("Sample", "Sample - 0", "Sample - 1");

        assertEquals("Sample - 2", NamingUtils.nextNameOf(list, "Sample"));
    }

    @Test
    public void aGapInTheCountingIsUsed() {
        List<JDBListBase> list = listOf("Sample", "Sample - 1");

        assertEquals("Sample - 0", NamingUtils.nextNameOf(list, "Sample"),
                "the first free name wins, the counter is no serial number");
    }

    @Test
    public void copyingAnEntryTwiceYieldsTwoDifferentNames() {
        List<JDBListBase> list = listOf("Sample");

        String first = NamingUtils.nextNameOf(list, "Sample");
        list.add(new JDBListBase(first, null));
        String second = NamingUtils.nextNameOf(list, "Sample");

        assertEquals("Sample - 0", first);
        assertEquals("Sample - 1", second);
    }

    @Test
    public void anyKindOfConfigurationEntryIsAccepted() {
        // the helpers work on the common base class, so a list of connections
        // is checked the same way as a list of drivers
        JDBConnection conn = new JDBConnection();
        conn.setName("Sample");
        List<JDBConnection> list = new ArrayList<>(Arrays.asList(conn));

        assertTrue(NamingUtils.nameExists(list, "Sample"));
        assertEquals("Sample - 0", NamingUtils.nextNameOf(list, "Sample"));
    }
}

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
package comart.tools.jdbgen.template;

import comart.tools.jdbgen.types.JDBGenConfig;
import comart.utils.AppDirs;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A broken template has to say what is wrong with it and where, instead of
 * failing somewhere deep inside the engine with a NullPointerException or an
 * IndexOutOfBoundsException.
 */
public class TemplateManagerErrorTest {

    private final Map<String, String> customs = new HashMap<>();

    @BeforeAll
    public static void isolateConfiguration(@TempDir Path dir) {
        String data = System.getProperty(AppDirs.DATA_DIR_PROPERTY);
        String base = System.getProperty(AppDirs.RESOURCE_BASE_PROPERTY);
        System.setProperty(AppDirs.DATA_DIR_PROPERTY, dir.resolve("data").toString());
        System.setProperty(AppDirs.RESOURCE_BASE_PROPERTY, dir.resolve("install").toString());
        try {
            JDBGenConfig.getInstance(true);
        } finally {
            if (data == null) System.clearProperty(AppDirs.DATA_DIR_PROPERTY);
            else System.setProperty(AppDirs.DATA_DIR_PROPERTY, data);
            if (base == null) System.clearProperty(AppDirs.RESOURCE_BASE_PROPERTY);
            else System.setProperty(AppDirs.RESOURCE_BASE_PROPERTY, base);
        }
    }

    /** a model with one member and one collection. */
    private Map<String, Object> model() {
        Map<String, Object> model = new HashMap<>();
        model.put("name", "tb_user");
        model.put("no", 3);
        model.put("rows", new ArrayList<>(Arrays.asList(model("a"), model("b"))));
        return model;
    }

    private Map<String, Object> model(String name) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", name);
        return row;
    }

    /** the failure of parsing <code>template</code>. */
    private ParseException parseFailure(String template) {
        return assertThrows(ParseException.class,
                () -> new TemplateManager(template, customs));
    }

    /** the failure of rendering <code>template</code> against the model. */
    private <T extends Throwable> T renderFailure(Class<T> type, String template) {
        return assertThrows(type, () -> new TemplateManager(template, customs)
                .applyMapper(model()));
    }

    // ------------------------------------------------------------ parse errors

    @Test
    public void anUnknownPlaceholderTypeIsReportedWithItsName() {
        ParseException ex = parseFailure("${nosuch:key=name}");

        assertTrue(ex.getMessage().contains("nosuch"), ex.getMessage());
        assertTrue(ex.getMessage().contains("Unknown template"), ex.getMessage());
    }

    @Test
    public void aPlaceholderThatIsNeverClosedIsReported() {
        ParseException ex = parseFailure("select ${item:key=name from dual");

        assertTrue(ex.getMessage().contains("'}' not found"), ex.getMessage());
    }

    @Test
    public void aForStatementThatIsNeverClosedIsReported() {
        ParseException ex = parseFailure("${for:key=rows}${item:key=name}");

        assertTrue(ex.getMessage().contains("for statements not closed"), ex.getMessage());
    }

    @Test
    public void anIfStatementThatIsNeverClosedIsReported() {
        ParseException ex = parseFailure("${if:key=name, equals='x'}yes");

        assertTrue(ex.getMessage().contains("if statements not closed"), ex.getMessage());
    }

    @Test
    public void anElifThatIsNeverClosedIsReported() {
        ParseException ex = parseFailure("${if:key=name, equals='x'}yes${elif:key=name, equals='y'}no");

        assertTrue(ex.getMessage().contains("if statements not closed"), ex.getMessage());
    }

    @Test
    public void aMisspelledConditionIsRefusedInsteadOfSilentlyHolding() {
        // 'startWith' is not 'startsWith' - without the check the condition
        // would simply be ignored and the branch always rendered
        ParseException ex = parseFailure("${if:key=name, startWith='tb_'}yes${endif}");

        assertTrue(ex.getMessage().contains("Unknown if condition"), ex.getMessage());
    }

    @Test
    public void aMisspelledConditionOfAnElifIsRefusedAsWell() {
        ParseException ex = parseFailure(
                "${if:key=name, equals='x'}a${elif:key=name, containz='y'}b${endif}");

        assertTrue(ex.getMessage().contains("Unknown if condition"), ex.getMessage());
    }

    @Test
    public void aDanglingEscapeCharacterIsReported() {
        ParseException ex = parseFailure("${item:key=name, prepend=a\\}");

        assertTrue(ex.getMessage().contains("Dangling escape character"), ex.getMessage());
    }

    @Test
    public void anAttributeWithTwoValuesIsReported() {
        ParseException ex = parseFailure("${item:key=name=other}");

        assertTrue(ex.getMessage().contains("Name value pair not matched"), ex.getMessage());
    }

    @Test
    public void anAttributeWithoutANameIsReported() {
        ParseException ex = parseFailure("${item:key=name, ='x'}");

        assertTrue(ex.getMessage().contains("Name value pair not matched"), ex.getMessage());
    }

    @Test
    public void theReportedLineIsTheLineTheErrorIsOn() {
        ParseException ex = parseFailure("first\nsecond\n${nosuch:key=name}\nfourth");

        assertEquals(2, ex.getErrorOffset(),
                "the line is counted from zero, so the third line is 2");
    }

    @Test
    public void theMessagePointsAtTheTextTheErrorWasFoundIn() {
        ParseException ex = parseFailure("${nosuch:key=name} and the rest of the line");

        assertTrue(ex.getMessage().contains("and the rest of the line"),
                "the user has to be able to find the place: " + ex.getMessage());
    }

    // ----------------------------------------------------------- render errors

    @Test
    public void aPlaceholderWithoutAKeyIsReported() {
        ParseException ex = renderFailure(ParseException.class, "${for:instr=','}x${endfor}");

        assertTrue(ex.getMessage().contains("'key' or 'item' is required"), ex.getMessage());
    }

    @Test
    public void anIfWithoutAKeyIsReported() {
        ParseException ex = renderFailure(ParseException.class,
                "${if:equals='x'}yes${endif}");

        assertTrue(ex.getMessage().contains("'key' or 'item' is required"), ex.getMessage());
    }

    @Test
    public void anUnknownStringProcessorIsReportedWithTheValidOnes() {
        RuntimeException ex = renderFailure(RuntimeException.class, "${name.capitalize}");

        assertTrue(ex.getMessage().contains("capitalize"), ex.getMessage());
        assertTrue(ex.getMessage().contains("camel") && ex.getMessage().contains("replace"),
                "the valid processors are listed: " + ex.getMessage());
    }

    @Test
    public void aForOverSomethingTheModelDoesNotHaveIsReported() {
        RuntimeException ex = renderFailure(RuntimeException.class,
                "${for:key=nowhere}${item:key=name}${endfor}");

        assertTrue(ex.getMessage().contains("nowhere"), ex.getMessage());
        assertTrue(ex.getMessage().contains("Model has no"), ex.getMessage());
    }

    @Test
    public void containsOnAValueThatIsNeitherACollectionNorATextIsReported() {
        RuntimeException ex = renderFailure(RuntimeException.class,
                "${if:key=no, contains='3'}yes${endif}");

        assertTrue(ex.getMessage().contains("collection"), ex.getMessage());
    }

    @Test
    public void aPaddingThatIsNoNumberIsReported() {
        NumberFormatException ex = renderFailure(NumberFormatException.class,
                "${item:key=name, padSize=wide}");

        assertTrue(ex.getMessage().contains("wide"), ex.getMessage());
    }

    @Test
    public void anIndentThatIsNoNumberIsReported() {
        NumberFormatException ex = renderFailure(NumberFormatException.class,
                "${for:key=rows, indent=two}${item:key=name}${endfor}");

        assertTrue(ex.getMessage().contains("two"), ex.getMessage());
    }

    @Test
    public void anInvalidDateFormatIsReported() {
        IllegalArgumentException ex = renderFailure(IllegalArgumentException.class,
                "${date:yyyy-bb}");

        assertTrue(ex.getMessage().toLowerCase().contains("pattern"), ex.getMessage());
    }
}

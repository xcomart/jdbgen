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

import comart.tools.jdbgen.types.JDBAbbr;
import comart.tools.jdbgen.types.JDBGenConfig;
import comart.tools.jdbgen.types.db.DBMetaModel;
import comart.utils.AppDirs;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The parts of the template syntax the original test does not reach: the
 * elif/else chain, the negated conditions, the remaining string processors and
 * the way an attribute list is read.
 */
public class TemplateManagerSyntaxTest {

    /** a model whose members are read as public fields. */
    public static class Row extends DBMetaModel {
        public String name;
        public String type;
        public List<Row> children = new ArrayList<>();
        public Row(String name) {
            this(name, "");
        }
        public Row(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    private final Map<String, String> customs = new HashMap<String, String>() {{
        put("author", "John Doe");
        put("project", "jdbgen");
    }};

    /** the abbreviation setting is global; every test starts from a known one. */
    private List<JDBAbbr> previousAbbrs;
    private boolean previousApply;

    /**
     * build the configuration singleton - if it is not there yet - below a
     * temporary directory, so that no test writes into the user data directory
     * of whoever runs the build.
     */
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

    @BeforeEach
    public void rememberConfiguration() {
        JDBGenConfig conf = JDBGenConfig.getInstance(true);
        previousAbbrs = conf.getAbbrs();
        previousApply = conf.isApplyAbbr();
        conf.setApplyAbbr(false);
        conf.setAbbrs(new ArrayList<>());
        JDBAbbr.buildMap();
    }

    @AfterEach
    public void restoreConfiguration() {
        JDBGenConfig conf = JDBGenConfig.getInstance(true);
        conf.setAbbrs(previousAbbrs == null ? new ArrayList<>() : previousAbbrs);
        conf.setApplyAbbr(previousApply);
        JDBAbbr.buildMap();
    }

    private String render(String template, Object model) throws Exception {
        return new TemplateManager(template, customs).applyMapper(model);
    }

    // ---------------------------------------------------------------- processors

    @Test
    public void theRemainingCaseProcessorsRewriteTheValue() throws Exception {
        Row row = new Row("abc_def_ghi");

        assertEquals("ABC_DEF_GHI", render("${name.screaming}", row));
        // 'kebab' is the second name of 'skewer'
        assertEquals("abc-def-ghi", render("${name.kebab}", row));
        assertEquals(render("${name.skewer}", row), render("${name.kebab}", row));
    }

    @Test
    public void prefixAndSuffixKeepAValueWithoutAnUnderscore() throws Exception {
        Row row = new Row("plain");

        assertEquals("plain", render("${name.prefix}", row));
        assertEquals("plain", render("${name.suffix}", row));
    }

    @Test
    public void prefixCutsAtTheLastAndSuffixAtTheFirstUnderscore() throws Exception {
        Row row = new Row("a_b_c");

        assertEquals("a_b", render("${name.prefix}", row));
        assertEquals("b_c", render("${name.suffix}", row));
    }

    @Test
    public void processorNamesAreCaseInsensitive() throws Exception {
        Row row = new Row("abc_def");

        assertEquals("AbcDef", render("${name.PASCAL}", row));
        assertEquals("abcDef", render("${name.Camel}", row));
    }

    @Test
    public void aProcessorChainIsAppliedFromLeftToRight() throws Exception {
        Row row = new Row("tb_user_account");

        // suffix first, then the case conversion of what is left
        assertEquals("UserAccount", render("${name.suffix.pascal}", row));
        // the other way round the underscore is gone before 'suffix' sees it
        assertEquals("TbUserAccount", render("${name.pascal.suffix}", row));
    }

    @Test
    public void abbreviationsAreAppliedPerWordAndKeepTheSeparators() throws Exception {
        JDBGenConfig conf = JDBGenConfig.getInstance(true);
        conf.setAbbrs(new ArrayList<>(Arrays.asList(
                new JDBAbbr(true, false, "usr", "user"),
                new JDBAbbr(true, false, "acct", "account"),
                new JDBAbbr(false, false, "tb", "table"),
                new JDBAbbr(true, true, "tb_sys", "system"))));
        JDBAbbr.buildMap();

        Row row = new Row("tb-usr_acct");
        // '-' and '_' both separate words and are kept where they were,
        // 'tb' is not replaced because its rule is turned off
        assertEquals("tb-user_account", render("${name.abbr}", row));
        // a whole name rule wins over the per word ones
        assertEquals("system", render("${name.abbr}", new Row("TB_SYS")));
        // a name without any known word is handed through unchanged
        assertEquals("other_name", render("${name.abbr}", new Row("other_name")));
    }

    // ---------------------------------------------------------------- attributes

    @Test
    public void attributeNamesAreCaseInsensitive() throws Exception {
        Row row = new Row("abc");

        assertEquals("[abc]", render("${item:KEY=name, PrePend='[', POSTPEND=']'}", row));
    }

    @Test
    public void quotedAttributeValuesMayHoldCommasAndEscapes() throws Exception {
        Row row = new Row("abc");

        assertEquals("a,b|abc", render("${item:key=name, prepend='a,b|'}", row));
        assertEquals("abc\n\t", render("${item:key=name, postpend='\\n\\t'}", row));
        // an escaped quote does not end the value
        assertEquals("it's abc", render("${item:key=name, prepend='it\\'s '}", row));
    }

    @Test
    public void aValueWrappedInParenthesesKeepsThem() throws Exception {
        Row row = new Row("abc");

        // '(' only groups the commas, it is not a quote character
        assertEquals("(a,b)abc", render("${item:key=name, prepend=(a,b)}", row));
    }

    @Test
    public void quoteSurroundsTheValueAndIsOverriddenPerSide() throws Exception {
        Row row = new Row("abc");

        assertEquals("\"abc\"", render("${item:key=name, quote='\"'}", row));
        // 'prepend' replaces the opening quote only
        assertEquals("<abc\"", render("${item:key=name, quote='\"', prepend='<'}", row));
        assertEquals("\"abc>", render("${item:key=name, quote='\"', postpend='>'}", row));
    }

    @Test
    public void paddingCountsDoubleByteCharactersTwice() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("name", "가나"); // two Hangul syllables, four EUC-KR bytes

        String padded = render("${item:key=name, padSize=10}", model);

        assertEquals("가나" + "      ", padded,
                "a double byte character occupies two columns of a fixed width font");
    }

    @Test
    public void aValueLongerThanThePaddingIsNotCutOff() throws Exception {
        Row row = new Row("abcdefghij");

        assertEquals("abcdefghij", render("${item:key=name, padSize=4}", row));
    }

    @Test
    public void theDecorationsSurroundTheValueBeforeItIsPadded() throws Exception {
        Row row = new Row("abc");

        // '(abc)' is five characters, so five spaces are left of ten
        assertEquals("(abc)     ", render("${item:key=name, prepend='(', postpend=')', padSize=10}", row));
        assertEquals("     (abc)", render(
                "${item:key=name, prepend='(', postpend=')', padSize=10, padDir='left'}", row));
    }

    @Test
    public void aKeyMayAlsoBeWrittenAsTheItemAttribute() throws Exception {
        Row row = new Row("abc");

        assertEquals("ABC", render("${item:item=name.upper}", row));
    }

    // ---------------------------------------------------------------- values

    @Test
    public void aKeyTheModelDoesNotAnswerFallsBackToTheCustomVariables() throws Exception {
        Row row = new Row("abc");

        assertEquals("JDBGEN", render("${item:key=project.upper}", row));
    }

    @Test
    public void anUnknownKeyRendersAsNothing() throws Exception {
        Row row = new Row("abc");

        assertEquals("[]", render("[${item:key=nowhere}]", row));
    }

    @Test
    public void textOutsideOfAPlaceholderIsCopiedVerbatim() throws Exception {
        Row row = new Row("abc");

        assertEquals("-- abc --\nend", render("-- ${name} --\nend", row));
    }

    @Test
    public void aLiteralIsFollowedByTheRestOfTheTemplate() throws Exception {
        Row row = new Row("abc");

        // this is how a template writes a '${' of its own
        assertEquals("${name} is abc", render("${'${name}'} is ${name}", row));
    }

    @Test
    public void anEmptyLiteralStandsForEmptyText() throws Exception {
        Row row = new Row("abc");

        // '${""}' is how a template writes nothing at a place a placeholder
        // would otherwise be read - it is a literal, not a broken placeholder
        assertEquals("[]", render("[${''}]", row));
        assertEquals("[]", render("[${\"\"}]", row));
        assertEquals("[abc]", render("[${''}${name}${\"\"}]", row));
    }

    // ---------------------------------------------------------------- if / elif

    @Test
    public void theElseBranchIsRenderedWhenTheConditionFails() throws Exception {
        Row row = new Row("abc", "VIEW");

        assertEquals("view", render("${if:key=type, equals='TABLE'}table${else}view${endif}", row));
        assertEquals("table", render("${if:key=type, equals='VIEW'}table${else}view${endif}", row));
    }

    @Test
    public void anElifChainPicksTheFirstMatchingBranch() throws Exception {
        String tpl = "${if:key=type, equals='TABLE'}T"
                + "${elif:key=type, equals='VIEW'}V"
                + "${elif:key=type, equals='SYNONYM'}S"
                + "${else}?${endif}";

        assertEquals("T", render(tpl, new Row("a", "TABLE")));
        assertEquals("V", render(tpl, new Row("a", "VIEW")));
        assertEquals("S", render(tpl, new Row("a", "SYNONYM")));
        assertEquals("?", render(tpl, new Row("a", "SEQUENCE")));
    }

    @Test
    public void anElifChainWithoutAnElseRendersNothingWhenNoBranchMatches() throws Exception {
        String tpl = "[${if:key=type, equals='TABLE'}T${elif:key=type, equals='VIEW'}V${endif}]";

        assertEquals("[]", render(tpl, new Row("a", "SEQUENCE")));
        assertEquals("[V]", render(tpl, new Row("a", "VIEW")));
    }

    @Test
    public void theNegatedConditionsAreTheOppositeOfTheirCounterparts() throws Exception {
        Row row = new Row("tb_user");

        assertEquals("", render("${if:key=name, notstartswith='tb_'}x${endif}", row));
        assertEquals("x", render("${if:key=name, notstartswith='vw_'}x${endif}", row));
        assertEquals("", render("${if:key=name, notendswith='user'}x${endif}", row));
        assertEquals("x", render("${if:key=name, notendswith='role'}x${endif}", row));
        assertEquals("", render("${if:key=name, notmatches='[a-z_]+'}x${endif}", row));
        assertEquals("x", render("${if:key=name, notmatches='[0-9]+'}x${endif}", row));
    }

    @Test
    public void valueIsAnotherNameOfTheEqualsCondition() throws Exception {
        Row row = new Row("abc", "TABLE");

        assertEquals("x", render("${if:key=type, value='TABLE'}x${endif}", row));
        assertEquals("", render("${if:key=type, value='VIEW'}x${endif}", row));
    }

    @Test
    public void everyConditionButMatchesIgnoresTheCase() throws Exception {
        Row row = new Row("TB_User");

        assertEquals("x", render("${if:key=name, equals='tb_user'}x${endif}", row));
        assertEquals("x", render("${if:key=name, startswith='tb_'}x${endif}", row));
        assertEquals("x", render("${if:key=name, endswith='USER'}x${endif}", row));
        // 'matches' is the only case sensitive one
        assertEquals("", render("${if:key=name, matches='tb_user'}x${endif}", row));
        assertEquals("x", render("${if:key=name, matches='TB_User'}x${endif}", row));
    }

    @Test
    public void matchesComparesTheWholeValue() throws Exception {
        Row row = new Row("abc_def");

        assertEquals("", render("${if:key=name, matches='abc'}x${endif}", row),
                "a partial match is no match");
        assertEquals("x", render("${if:key=name, matches='abc.*'}x${endif}", row));
    }

    @Test
    public void theKeyOfAnIfMayCarryProcessors() throws Exception {
        Row row = new Row("TB_USER");

        assertEquals("x", render("${if:key=name.lower.suffix, equals='user'}x${endif}", row));
    }

    @Test
    public void anIfMayBeNestedInsideAnotherOne() throws Exception {
        String tpl = "${if:key=type, equals='TABLE'}"
                + "${if:key=name, startswith='tb_'}both${else}outer${endif}"
                + "${else}none${endif}";

        assertEquals("both", render(tpl, new Row("tb_a", "TABLE")));
        assertEquals("outer", render(tpl, new Row("a", "TABLE")));
        assertEquals("none", render(tpl, new Row("tb_a", "VIEW")));
    }

    // ---------------------------------------------------------------- for

    @Test
    public void anEmptyCollectionRendersNothingAtAll() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("rows", new ArrayList<>());

        assertEquals("[]", render("[${for:key=rows, instr=','}${item:key=name}${endfor}]", model));
    }

    @Test
    public void aSkipListDropsEveryNameItHolds() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("rows", new ArrayList<>(Arrays.asList(
                new Row("a"), new Row("b"), new Row("c"), new Row("d"))));

        assertEquals("a,d", render(
                "${for:key=rows, instr=',', skipList='b, c'}${item:key=name}${endfor}", model),
                "the skip list is a comma separated list, blanks around a name included");
    }

    @Test
    public void theSeparatorIsIndentedToTheColumnTheLoopStartsIn() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("rows", new ArrayList<>(Arrays.asList(new Row("a"), new Row("b"))));

        // the loop starts behind four characters, so the second line lines up
        // with the first one plus the two extra columns of 'indent'
        assertEquals("x\n--- a,\n      b", render(
                "x\n--- ${for:key=rows, instr=',\\n', indent=2}${item:key=name}${endfor}", model));
    }

    @Test
    public void aNestedLoopReachesTheOuterModelThroughSuper() throws Exception {
        Row parent = new Row("parent");
        Row child = new Row("child");
        child.children.add(new Row("leaf"));
        parent.children.add(child);
        Map<String, Object> model = new HashMap<>();
        model.put("children", new ArrayList<>(Arrays.asList(parent)));

        String result = render("${for:key=children}"
                + "${for:key=children}"
                + "${super:key=name}/${item:key=name}"
                + "${endfor}${endfor}", model);

        assertEquals("parent/child", result);
    }

    @Test
    public void theItemNumberFollowsThePositionInTheCollection() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("rows", new ArrayList<>(Arrays.asList(
                new Row("a"), new Row("b"), new Row("c"))));

        assertEquals("1a2b3c", render(
                "${for:key=rows}${item:key=no}${item:key=name}${endfor}", model));
    }

    @Test
    public void theItemNumberCountsTheRenderedElementsOnly() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("rows", new ArrayList<>(Arrays.asList(
                new Row("a"), new Row("b"), new Row("c"), new Row("d"))));

        // 'b' is skipped, so 'c' is the second element that is rendered - a
        // numbered column list would otherwise have a hole in it
        assertEquals("1a2c3d", render(
                "${for:key=rows, skipList='b'}${item:key=no}${item:key=name}${endfor}", model));
    }

    @Test
    public void aDecorationOfTheLoopBodyIsAppliedToEveryElement() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("rows", new ArrayList<>(Arrays.asList(new Row("a"), new Row("b"))));

        assertEquals("'a', 'b'", render(
                "${for:key=rows, instr=', '}${item:key=name, quote=\"'\"}${endfor}", model));
    }

    @Test
    public void anIfInsideALoopIsEvaluatedPerElement() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("rows", new ArrayList<>(Arrays.asList(
                new Row("a", "TABLE"), new Row("b", "VIEW"), new Row("c", "TABLE"))));

        assertEquals("a c ", render(
                "${for:key=rows}${if:key=type, equals='TABLE'}${item:key=name} ${endif}${endfor}",
                model));
    }

    @Test
    public void containsLooksAtTheNamesOfACollection() throws Exception {
        Row row = new Row("parent");
        row.children.add(new Row("id"));
        row.children.add(new Row("name"));

        assertEquals("x", render("${if:key=children, contains='ID'}x${endif}", row),
                "the element names are compared ignoring the case");
        assertEquals("", render("${if:key=children, contains='other'}x${endif}", row));
        assertEquals("x", render("${if:key=children, notcontains='other'}x${endif}", row));
    }

    // ---------------------------------------------------------------- others

    @Test
    public void theLineEndOfTheTemplateIsUsedByTheEngineItself() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("rows", new ArrayList<>(Arrays.asList(new Row("a"), new Row("b"))));

        String windows = render("x\r\n${for:key=rows, instr=',\\n'}${item:key=name}${endfor}", model);
        assertEquals("x\r\na,\r\nb", windows,
                "a template written on windows keeps its carriage returns");

        String unix = render("x\n${for:key=rows, instr=',\\n'}${item:key=name}${endfor}", model);
        assertEquals("x\na,\nb", unix);
    }

    @Test
    public void aTemplateMayBeAppliedToMoreThanOneModel() throws Exception {
        TemplateManager tm = new TemplateManager("${name.upper}", customs);

        assertEquals("A", tm.applyMapper(new Row("a")));
        assertEquals("B", tm.applyMapper(new Row("b")));
    }

    @Test
    public void theDateFormatMayAlsoBeWrittenAsAnAttribute() throws Exception {
        String year = new java.text.SimpleDateFormat("yyyy").format(new java.util.Date());

        assertEquals(year, render("${date:format=yyyy}", new Row("a")));
        assertEquals("[" + year + "]",
                render("${date:format=yyyy, prepend='[', postpend=']'}", new Row("a")));
    }
}

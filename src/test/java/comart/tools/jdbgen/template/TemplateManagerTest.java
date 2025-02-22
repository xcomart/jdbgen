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
import comart.tools.jdbgen.types.db.DBColumn;
import comart.tools.jdbgen.types.db.DBTable;
import comart.utils.ObjUtils;
import comart.utils.StrUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author comart
 */
public class TemplateManagerTest {
    @Data
    public static class SampleObject{
        public String name;
        public SampleObject(String name) {
            this.name = name;
        }
    }
    
    private final SampleObject mapObj;
    private final SampleObject mapObj2;
    
    private final Map<String,String> custVars = new HashMap<String, String>() {{
        put("author", "John Doe");
    }};
    
    public TemplateManagerTest() {
        mapObj = new SampleObject("abc_def_ghi_jkl");
        mapObj2 = new SampleObject("mno_pqr_stu_vwx");
        
        JDBGenConfig conf = JDBGenConfig.getInstance(true);
        
        ArrayList<JDBAbbr> abbrs = new ArrayList<>();
        abbrs.add(new JDBAbbr(true, false, "def", "default"));
        abbrs.add(new JDBAbbr(true, true, "mno_pqr_stu_vwx", "mnopqr"));
        conf.setAbbrs(abbrs);
        JDBAbbr.buildMap();
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    /**
     * Test of template decorators
     */
    @Test
    public void testTemplateDecorator() throws Exception {
        // base test
        TemplateManager tm = new TemplateManager("a${name}b", custVars);
        String result = tm.applyMapper(mapObj);
        if (!"aabc_def_ghi_jklb".equals(result))
            fail("${name} mapper result fail."+result+" must be aabc_def_ghi_jklb");
        
        // function suffix test
        tm = new TemplateManager("${name.suffix}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"def_ghi_jkl".equals(result))
            fail("${name.suffix} mapper result fail."+result+" must be def_ghi_jkl");
        
        // function prefix test
        tm = new TemplateManager("${name.prefix}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"abc_def_ghi".equals(result))
            fail("${name.prefix} mapper result fail."+result+" must be abc_def_ghi");
        
        // function lower test
        tm = new TemplateManager("${name.lower}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"abc_def_ghi_jkl".equals(result))
            fail("${name.lower} mapper result fail."+result+" must be abc_def_ghi_jkl");
        
        // function upper test
        tm = new TemplateManager("${name.upper}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"ABC_DEF_GHI_JKL".equals(result))
            fail("${name.upper} mapper result fail."+result+" must be ABC_DEF_GHI_JKL");
        
        // function pascal test
        tm = new TemplateManager("${name.pascal}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"AbcDefGhiJkl".equals(result))
            fail("${name.pascal} mapper result fail."+result+" must be AbcDefGhiJkl");
        
        // function camel test
        tm = new TemplateManager("${name.camel}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"abcDefGhiJkl".equals(result))
            fail("${name.camel} mapper result fail."+result+" must be abcDefGhiJkl");
        
        // function snake test
        tm = new TemplateManager("${name.snake}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"abc_def_ghi_jkl".equals(result))
            fail("${name.snake} mapper result fail."+result+" must be abc_def_ghi_jkl");
        
        // function skewer test
        tm = new TemplateManager("${name.skewer}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"abc-def-ghi-jkl".equals(result))
            fail("${name.skewer} mapper result fail."+result+" must be abc-def-ghi-jkl");
        
        // function replace test
        tm = new TemplateManager("${name.replace('ghi','mno')}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"abc_def_mno_jkl".equals(result))
            fail("${name.replace('ghi','mno')} mapper result fail."+result+" must be abc_def_mno_jkl");
        
        // function abbr test
        tm = new TemplateManager("${name.abbr}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"abc_default_ghi_jkl".equals(result))
            fail("${name.abbr} mapper result fail."+result+" must be abc_default_ghi_jkl");

        // function abbr name replace test
        tm = new TemplateManager("${name.abbr}", custVars);
        result = tm.applyMapper(mapObj2);
        if (!"mnopqr".equals(result))
            fail("${name.abbr} mapper result fail."+result+" must be mnopqr");
        
        // default abbr set test
        JDBGenConfig.getInstance().setApplyAbbr(true);
        tm = new TemplateManager("${name}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"abc_default_ghi_jkl".equals(result))
            fail("${name} with abbr default mapper result fail."+result+" must be abc_default_ghi_jkl");

        // function abbr name replace test
        tm = new TemplateManager("${name}", custVars);
        result = tm.applyMapper(mapObj2);
        if (!"mnopqr".equals(result))
            fail("${name.abbr} mapper result fail."+result+" must be mnopqr");
        JDBGenConfig.getInstance().setApplyAbbr(false);
        
        // function combination test
        tm = new TemplateManager("${name.suffix.camel}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"defGhiJkl".equals(result))
            fail("${name.suffix.camel} mapper result fail."+result+" must be defGhiJkl");
        
        // function camel test with item tag
        tm = new TemplateManager("${item:key=name.camel}", custVars);
        result = tm.applyMapper(mapObj);
        if (!"abcDefGhiJkl".equals(result))
            fail("${item:key=name.camel} mapper result fail."+result+" must be abcDefGhiJkl");
    }

    /**
     * Test of template extra decorators
     */
    @Test
    public void testTemplateExtraDecorator() throws Exception {
        
        // padding right decoration test
        TemplateManager tm = new TemplateManager("${item:key=name, padSize=20, padDir='right'}", custVars);
        String result = tm.applyMapper(mapObj);
        if (result.length() != 20 || !(mapObj.name).equals(StrUtils.trimRight(result)))
            fail("${item:key=name, padSize=20, padDir='right'} mapper result fail.");
        
        // padding left decoration test
        tm = new TemplateManager("${item:key=name, padSize=20, padDir='left'}", custVars);
        result = tm.applyMapper(mapObj);
        if (result.length() != 20 || !(mapObj.name).equals(StrUtils.trimLeft(result)))
            fail("${item:key=name, padSize=20, padDir='left'} mapper result fail.");
        
        // quote decoration test
        tm = new TemplateManager("${item:key=name, quote=\"'\"}", custVars);
        result = tm.applyMapper(mapObj);
        if (!("'"+mapObj.name+"'").equals(result))
            fail("${item:key=name, quote=\"'\"} mapper result fail.");

        // prepend decoration test
        tm = new TemplateManager("${item:key=name, prepend='xyz_'}", custVars);
        result = tm.applyMapper(mapObj);
        if (!("xyz_"+mapObj.name).equals(result))
            fail("${item:key=name, prepend='xyz_'} mapper result fail.");

        // postpend decoration test
        tm = new TemplateManager("${item:key=name, postpend='_xyz'}", custVars);
        result = tm.applyMapper(mapObj);
        if (!(mapObj.name+"_xyz").equals(result))
            fail("${item:key=name, postpend='_xyz'} mapper result fail.");
    }

    /**
     * Test of template extra decorators
     */
    @Test
    public void testTemplateControlTest() throws Exception {
        
        Map<String, Object> mapper = new HashMap<>();
        StringBuilder forTarget = new StringBuilder();
        StringBuilder forInstrTarget = new StringBuilder();
        StringBuilder superTarget = new StringBuilder();
        List<Object> collection = new ArrayList<>();
        for (int i=0; i<5; i++) {
            String name = "sample"+i;
            collection.add(new SampleObject(name));
            forTarget.append(name);
            if (forInstrTarget.length() > 0)
                forInstrTarget.append(",");
            forInstrTarget.append(name);
            superTarget.append("VALUE");
        }
        mapper.put("collection", collection);
        mapper.put("single", "SINGLE_VALUE");
        
        // for control statement test
        TemplateManager tm = new TemplateManager("${for:key=collection}${item:key=name}${endfor}", custVars);
        String result = tm.applyMapper(mapper);
        if (!forTarget.toString().equals(result))
            fail("${for:key=collection}${item:key=name}${endfor} mapper result fail.");

        // for instr control statement test
        tm = new TemplateManager("${for:key=collection, instr=','}${item:key=name}${endfor}", custVars);
        result = tm.applyMapper(mapper);
        if (!forInstrTarget.toString().equals(result))
            fail("${for:key=collection, instr=','}${item:key=name}${endfor} mapper result fail.");

        // for indent control statement test
        tm = new TemplateManager("${for:key=collection, instr=',\n', indent=4}${item:key=name}${endfor}", custVars);
        result = tm.applyMapper(mapper);
        if (!StrUtils.replace(forInstrTarget.toString(), ",", ",\n    ").equals(result))
            fail("${for:key=collection, instr=',\n', indent=4}${item:key=name}${endfor} mapper result fail.");

        // for skiplist control statement test
        tm = new TemplateManager("${for:key=collection, skipList='sample2'}${item:key=name}${endfor}", custVars);
        result = tm.applyMapper(mapper);
        if (!StrUtils.replace(forTarget.toString(), "sample2", "").equals(result))
            fail("${for:key=collection, skipList='sample2'}${item:key=name}${endfor} mapper result fail.");

        // for super statement test
        tm = new TemplateManager("${for:key=collection}${super:key=single.suffix}${endfor}", custVars);
        result = tm.applyMapper(mapper);
        if (!superTarget.toString().equals(result))
            fail("${for:key=collection}${super:key=single.suffix}${endfor} mapper result fail.");
        
        
        
        // if equals control statement test
        tm = new TemplateManager("${if:key=single, equals='SINGLE_VALUE'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"True".equals(result))
            fail("${if:key=single, equals='SINGLE_VALUE'}True${endif} mapper result fail.");
        
        tm = new TemplateManager("${if:key=single, equals='DUMMY'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"".equals(result))
            fail("${if:key=single, equals='DUMMY'}True${endif} mapper result fail.");


        // if notEquals control statement test
        tm = new TemplateManager("${if:key=single, notEquals='SINGLE_VALUE'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"".equals(result))
            fail("${if:key=single, notEquals='SINGLE_VALUE'}True${endif} mapper result fail.");
        
        tm = new TemplateManager("${if:key=single, notEquals='DUMMY'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"True".equals(result))
            fail("${if:key=single, notEquals='DUMMY'}True${endif} mapper result fail.");
        

        // if startsWith control statement test
        tm = new TemplateManager("${if:key=single, startsWith='SINGLE'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"True".equals(result))
            fail("${if:key=single, startsWith='SINGLE'}True${endif} mapper result fail.");
        
        tm = new TemplateManager("${if:key=single, startsWith='DUMMY'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"".equals(result))
            fail("${if:key=single, startsWith='DUMMY'}True${endif} mapper result fail.");
        

        // if endsWith control statement test
        tm = new TemplateManager("${if:key=single, endsWith='VALUE'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"True".equals(result))
            fail("${if:key=single, endsWith='SINGLE'}True${endif} mapper result fail.");
        
        tm = new TemplateManager("${if:key=single, endsWith='DUMMY'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"".equals(result))
            fail("${if:key=single, endsWith='DUMMY'}True${endif} mapper result fail.");
        

        // if contains collection control statement test
        tm = new TemplateManager("${if:key=collection, contains='sample1'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"True".equals(result))
            fail("${if:key=collection, contains='sample1'}True${endif} mapper result fail.");
        
        tm = new TemplateManager("${if:key=collection, contains='sample9'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"".equals(result))
            fail("${if:key=collection, contains='sample9'}True${endif} mapper result fail.");


        // if contains string items control statement test
        tm = new TemplateManager("${if:key=single.suffix.lower, contains='sample, value'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"True".equals(result))
            fail("${if:key=single.suffix.lower, contains='sample, value'}True${endif} mapper result fail.");
        
        tm = new TemplateManager("${if:key=single.suffix.lower, contains='sample, proc'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"".equals(result))
            fail("${if:key=single.suffix.lower, contains='sample, proc'}True${endif} mapper result fail.");
        

        // if notcontains collection control statement test
        tm = new TemplateManager("${if:key=collection, notcontains='sample1'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"".equals(result))
            fail("${if:key=collection, notcontains='sample1'}True${endif} mapper result fail.");
        
        tm = new TemplateManager("${if:key=collection, notcontains='sample9'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"True".equals(result))
            fail("${if:key=collection, notcontains='sample9'}True${endif} mapper result fail.");


        // if notcontains string items control statement test
        tm = new TemplateManager("${if:key=single.suffix.lower, notcontains='sample, value'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"".equals(result))
            fail("${if:key=single.suffix.lower, notcontains='sample, value'}True${endif} mapper result fail.");
        
        tm = new TemplateManager("${if:key=single.suffix.lower, notcontains='sample, proc'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"True".equals(result))
            fail("${if:key=single.suffix.lower, notcontains='sample, proc'}True${endif} mapper result fail.");


        // if matches regex control statement test
        tm = new TemplateManager("${if:key=single.lower, matches='[a-z]+_[a-z]+'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"True".equals(result))
            fail("${if:key=single.lower, matches='[a-z]+_[a-z]+'}True${endif} mapper result fail.");
        
        tm = new TemplateManager("${if:key=single, matches='[a-z]+_[a-z]+'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"".equals(result))
            fail("${if:key=single.lower, matches='[a-z]+_[a-z]+'}True${endif} mapper result fail.");
        

        // if multi condition statement test
        tm = new TemplateManager("${if:key=single, startsWith='SINGLE', endsWith='VALUE'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"True".equals(result))
            fail("${if:key=single, startsWith='SINGLE', endsWith='VALUE'}True${endif} mapper result fail.");
        
        tm = new TemplateManager("${if:key=single, startsWith='SINGLE', endsWith='DUMMY'}True${endif}", custVars);
        result = tm.applyMapper(mapper);
        if (!"".equals(result))
            fail("${if:key=single, startsWith='SINGLE', endsWith='DUMMY'}True${endif} mapper result fail.");
    }

    @Test
    public void testTemplateOthers() throws Exception {
        // custom variable / author test
        TemplateManager tm = new TemplateManager("${author}", custVars);
        String result = tm.applyMapper(mapObj);
        if (!(custVars.get("author")).equals(result))
            fail("${author} mapper result fail.");
        
        // user test
        tm = new TemplateManager("${user}", custVars);
        result = tm.applyMapper(mapObj);
        if (!(ObjUtils.getLoginUserId()).equals(result))
            fail("${user} mapper result fail.");
        
        // date test
        tm = new TemplateManager("${date:yyyy-MM}", custVars);
        result = tm.applyMapper(mapObj);
        if (!StrUtils.dateFormat("yyyy-MM").equals(result))
            fail("${date:yyyy-MM-dd} mapper result fail.");
        
        // string literal test
        String test = "Test sample with ${author}";
        tm = new TemplateManager("${'"+test+"'}", custVars);
        result = tm.applyMapper(mapObj);
        if (!test.equals(result))
            fail("${'"+test+"'} mapper result fail.");
    }
    
}

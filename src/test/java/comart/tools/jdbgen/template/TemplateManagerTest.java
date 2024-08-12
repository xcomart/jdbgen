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

import comart.tools.jdbgen.types.db.DBColumn;
import comart.tools.jdbgen.types.db.DBTable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author comart
 */
public class TemplateManagerTest {
    private String template = ""+
            "Sample Text"+
            "${name}"+
            "${name.suffix}"+
            "${name.suffix.lower}"+
            ""+
            ""+
            ""+
            "";
    
    private DBTable createDBTable() {
        DBTable tbl = new DBTable();
        tbl.setCatalog("catalog");
        tbl.setSchema("public");
        tbl.setName("t_sample_table");
        tbl.setTable("t_sample_table");
        tbl.setType("TABLE");
        tbl.setRemarks("Sample Informations");
        
        ArrayList<DBColumn> cols = new ArrayList<>();
        ArrayList<DBColumn> keys = new ArrayList<>();
        ArrayList<DBColumn> notKeys = new ArrayList<>();
        for (int i=0; i<5; i++) {
            DBColumn clm = new DBColumn();
            clm.setNo(i);
            clm.setCatalog(tbl.getCatalog());
            clm.setColumn("col_name_"+(i+1));
            clm.setName("col_name_"+(i+1));
            cols.add(clm);
            if (i < 2) {
                keys.add(clm);
            } else {
                notKeys.add(clm);
            }
        }
        tbl.setColumns(cols);
        tbl.setKeys(keys);
        tbl.setNotKeys(notKeys);
        tbl.setNo(0);
        return tbl;
    }
    
    public TemplateManagerTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }

//    /**
//     * Test of applyMapper method, of class TemplateManager.
//     */
//    @Test
//    public void testUsingDBTable() throws Exception {
//        System.out.println("DBTable mapper");
//        Object mapper = createDBTable();
//        TemplateManager instance = null;
//        String expResult = "";
//        String result = instance.applyMapper(mapper);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//    
//    /**
//     * Test of applyMapper method, of class TemplateManager.
//     */
//    @Test
//    public void testApplyMapper() throws Exception {
//        System.out.println("applyMapper");
//        Object mapper = null;
//        TemplateManager instance = null;
//        String expResult = "";
//        String result = instance.applyMapper(mapper);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    
}

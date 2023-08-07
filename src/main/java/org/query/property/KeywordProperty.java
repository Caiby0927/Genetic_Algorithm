package org.query.property;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @author CAI
 * @date 2023/5/14
 **/
public class KeywordProperty {

    private static KeywordProperty p = null;

    public final String keywordFilepath;

    public final String testFilepath = "F:\\query-reformulation\\Keyword-T5\\data\\test_3.csv";
//    public final String testFilepath = "F:\\query-reformulation\\keyword_extraction\\data\\test_3.csv";

    public String resFilepath;

    public final Dictionary<String, String> dict = new Hashtable<>();

    public KeywordProperty(String keywordFilepath, boolean is_pagerank, boolean is_drop, boolean is_prompt) {
        this.keywordFilepath = keywordFilepath;

        if (is_pagerank)
            this.resFilepath = "F:\\query-reformulation\\keyword_extraction\\eval_data\\pagerank_res.csv";
        else if (is_drop)
            this.resFilepath = "F:\\query-reformulation\\keyword_extraction\\eval_data\\eval_res_drop.csv";
//            this.resFilepath = "F:\\query-reformulation\\Keyword-T5\\eval_data\\eval_res_drop.csv";
        else if (is_prompt)
            this.resFilepath = "F:\\query-reformulation\\keyword_extraction\\eval_data\\eval_res_drop_prompt.csv";
        else
            this.resFilepath = "F:\\query-reformulation\\Keyword-T5\\eval_data\\eval_res_refine.csv";

        dict.put("AspectJ", "E:\\query-reformulation\\dataset\\AspectJ\\data");
        dict.put("Tomcat", "E:\\query-reformulation\\dataset\\Tomcat\\data");
        dict.put("Birt", "E:\\query-reformulation\\dataset\\Birt\\data");
        dict.put("SWT", "D:\\query-reformulation\\dataset\\SWT\\data");
        dict.put("Eclipse_Platform_UI", "F:\\query-reformulation\\dataset\\Eclipse_Platform_UI\\data");
        dict.put("JDT", "F:\\query-reformulation\\dataset\\JDT\\data");
    }

    public static void createInstance(String keywordFilepath, boolean is_pagerank, boolean is_drop, boolean is_prompt) {
        if (p == null) {
            p = new KeywordProperty(keywordFilepath, is_pagerank, is_drop, is_prompt);
        }
    }

    public static KeywordProperty getInstance() {
        return p;
    }
}

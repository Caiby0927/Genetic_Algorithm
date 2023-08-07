package org.query.util;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import org.query.entity.BugReport;
import org.query.search_engine.LuceneSearch;

import java.io.*;
import java.util.*;

/**
 * @author CAI
 * @date 2023/6/14
 **/
public class keyword_refinement {

    private final String project = "AspectJ";

    private Set<String> low_quality_token_list = new HashSet<>();

    private final String GA_result_file_path = "E:\\query-reformulation\\dataset\\" + project + "\\data\\GA_result\\GA_total_result_low_quality.csv";
//    private final String GA_result_file_path = "E:\\query-reformulation\\dataset\\" + project + "\\data\\GA_result\\GA_total_result.csv";

    private final String refinement_result_file_path = "E:\\query-reformulation\\dataset\\" + project + "\\data\\refinement_result_low_quality_br.csv";

    private final String low_quality_token_file_path = "E:\\query-reformulation\\dataset\\" + project + "\\data\\project_level_low_quality_tokens_2.txt";

    private final String human_choose_token_file_path = "E:\\query-reformulation\\dataset\\" + project + "\\data\\useless_word.txt";

    private final String modified_human_choose_token_file_path = "E:\\query-reformulation\\dataset\\" + project + "\\data\\modified_useless_word.txt";

    private Set<String> human_choose_token_list = new HashSet<>();

    private List<String[]> new_keywords = new ArrayList<>();

    private List<String> all_bug_id_list = new ArrayList<>();

    private List<String> added_bug_id_list = new ArrayList<>();

    private final Dictionary<String, String[]> keyword_dict = new Hashtable<>();

    public final Dictionary<String, String> dict = new Hashtable<>();

    keyword_refinement() {
        dict.put("AspectJ", "E:\\query-reformulation\\dataset\\AspectJ\\data");
        dict.put("Tomcat", "E:\\query-reformulation\\dataset\\Tomcat\\data");
        dict.put("Birt", "E:\\query-reformulation\\dataset\\Birt\\data");
        dict.put("SWT", "D:\\query-reformulation\\dataset\\SWT\\data");
        dict.put("Eclipse_Platform_UI", "F:\\query-reformulation\\dataset\\Eclipse_Platform_UI\\data");
        dict.put("JDT", "F:\\query-reformulation\\dataset\\JDT\\data");

        try {
            CsvReader reader = new CsvReader(GA_result_file_path);

            reader.readHeaders();
            while (reader.readRecord()) {
                String[] record = reader.getValues();
                String bugId = record[0];
                all_bug_id_list.add(bugId);
                String effectiveness = record[1];
                String keyword = record[2];

                keyword_dict.put(bugId, new String[]{effectiveness, keyword});
            }

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void refine(String dataFilePath) {
        try {
            CsvReader reader = new CsvReader(dataFilePath);

            reader.readHeaders();

            String preBugId = "";
            List<String[]> sameBugList = new ArrayList<>();
            while (reader.readRecord()) {
                String[] record = reader.getValues();
                if (record.length != 5)
                    continue;
                String bugId = record[1];
                bugId = bugId.replaceAll("[^0-9]", "");
                System.out.println(bugId);
                String effectiveness = record[2];
//                System.out.println(effectiveness);
                String keywords = record[4].replaceAll("[^a-zA-Z\s0-9]", "").trim();
//                System.out.println(keywords);

//                System.out.println("pre bugId: " + preBugId + " , cur bugId: " + bugId +" , the same? " + preBugId.equals(bugId));
//                System.out.println(preBugId);
                if (preBugId.equals("")) {
                    preBugId = bugId;
                    sameBugList.add(new String[]{effectiveness, keywords});
                }
                else if (preBugId.equals(bugId)) {
                    sameBugList.add(new String[]{effectiveness, keywords});
                }
                else {
                    refinement(preBugId, sameBugList);
                    sameBugList.clear();
                    sameBugList.add(new String[]{effectiveness, keywords});
                    preBugId = bugId;
                }
            }
//            System.out.println(sameBugList.get(0)[0]);
//            System.out.println(sameBugList.get(0)[1]);
            refinement(preBugId, sameBugList);
            removeProjectLevelToken();

//            处理人工提取的可能无效的词语
//            readHumanToken();
//            removeHumanToken();

            addLeftToken();

            outputResult();
            outputProjectLevelToken();
//            outputModifiedHumanToken();

            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String dropDupKeyword(String dupKeyword) {
        String[] keywordArr = dupKeyword.split(" ");
        List<String> dropDupKeywordList = new ArrayList<>();

        for (String keyword: keywordArr) {
            keyword = keyword.replaceAll("[^a-zA-Z]", "").trim();
            if (!dropDupKeywordList.contains(keyword)) {
                dropDupKeywordList.add(keyword);
            }
        }

        return String.join(" ", dropDupKeywordList).trim();
    }

    private void refinement(String bugId, List<String[]> keywordList) throws Exception {
        String[] GA_result = keyword_dict.get(bugId);
        added_bug_id_list.add(bugId);

        int GA_effectiveness = Integer.parseInt(GA_result[0]);
        String GA_keywords = GA_result[1].trim();
        int GA_size = GA_keywords.trim().split(" ").length;
//        GA_keywords = dropDupKeyword(GA_keywords);

        Set<String> low_quality_token = new HashSet<>();

        String bugReportFile = dict.get(project) + "//BugCorpus//" + bugId + ".txt";
        String luceneIndexPath = dict.get(project) + "//lucene_index";

        BugReport bugReport = new BugReport(bugReportFile, Integer.parseInt(bugId), project, dict);
        LuceneSearch luceneSearch = new LuceneSearch(bugReport, false, luceneIndexPath);

//        System.out.println("the size of keyword list: " + keywordList.size());

        assert keywordList.size() != 0;

        String[] instance = keywordList.get(0);
        int bestEffectiveness = (int) Float.parseFloat(instance[0]);
        String bestKeywords = "";
        int bestKeywordsNum = Integer.MAX_VALUE;
        if (bestEffectiveness != -1) {
            bestKeywords = instance[1].trim();
//            bestKeywords = dropDupKeyword(bestKeywords);
            bestKeywordsNum = bestKeywords.split(" ").length;
        } else {
            bestEffectiveness = Integer.MAX_VALUE;
        }


//        遍历寻找低质量词语
        for (int index = 0; index < keywordList.size() - 1; index ++) {
            String[] instance_pre = keywordList.get(index);
            int effectiveness_pre = (int) Float.parseFloat(instance_pre[0]);
            String keywords_pre = instance_pre[1].trim();
//            keywords_pre = dropDupKeyword(keywords_pre);

            String[] instance_post = keywordList.get(index + 1);
            int effectiveness_post = (int) Float.parseFloat(instance_post[0]);
            String keywords_post = instance_post[1].trim();
//            keywords_post = dropDupKeyword(keywords_post);

//            System.out.println("the pre keywords: " + keywords_pre);
//            System.out.println("the post keywords: " + keywords_post);

            if (!keywords_pre.equals(keywords_post)) {
                List<String> k1 = Arrays.asList(keywords_pre.split(" "));
                List<String> k2 = Arrays.asList(keywords_post.split(" "));

//                System.out.println("the pre effectiveness: " + effectiveness_pre);
//                System.out.println("the post effectiveness: " + effectiveness_post);

//                更新最优关键词信息
                if (effectiveness_post != -1 && effectiveness_post <= bestEffectiveness) {
                    if (effectiveness_post != bestEffectiveness || k2.size() < bestKeywordsNum) {
                        bestEffectiveness = effectiveness_post;
//                    System.out.println("the best effectiveness: " + bestEffectiveness);
                        bestKeywordsNum = k2.size();
                        bestKeywords = keywords_post;
                    }

                }

//                比较前后两组关键词，获取低质量词语列表
                List<String> not_good_tokens = compareKeywords(k1, k2, effectiveness_pre, effectiveness_post);
                if (not_good_tokens != null)
                    low_quality_token.addAll(not_good_tokens);
            }
        }

        List<String> bestKeywordList;
//        最优关键词的effectiveness优于GA的，则不考虑原有的GA关键词
        if ((bestEffectiveness < GA_effectiveness) || (bestEffectiveness == GA_effectiveness && bestKeywordsNum < GA_size)) {
            bestKeywordList = Arrays.asList(bestKeywords.trim().split(" "));
        }
//        最优关键词的effectiveness比GA的差，则考虑原有的GA关键词
        else {
            bestKeywordList = Arrays.asList(GA_keywords.split(" "));
            bestEffectiveness = GA_effectiveness;
            bestKeywords = GA_keywords;
            bestKeywordsNum = GA_size;
//            if (keywordList.size() == 1  || low_quality_token.size() == 0) {
//                low_quality_token.addAll(Arrays.asList(bestKeywords.trim().split(" ")));
//            }
        }
        System.out.println("GA keyword num: " + GA_size);
        System.out.println("best keyword num: " + bestKeywordsNum);
//        System.out.println(low_quality_token);
//            找到原本应该被移除的关键词
        low_quality_token.retainAll(bestKeywordList);
//            移除bug-level的低质量关键词
        bestKeywordList = removeTokens(bestKeywordList, low_quality_token);
//        bestKeywordList.removeAll(low_quality_token);
        int newKeywordNum = bestKeywordList.size();
        int new_eff = search(luceneSearch, String.join(" ", bestKeywordList));
//            将低质量的词语添加后，效果提升，则直接存入
        if (new_eff != -1 && ((new_eff < bestEffectiveness) || (new_eff == bestEffectiveness && newKeywordNum < bestKeywordsNum))) {
//            System.out.println("the best effectiveness: " + new_eff);
            System.out.println("new keyword num: " + newKeywordNum);
            System.out.println("new eff: " + String.join(" ", bestKeywordList));
            new_keywords.add(new String[]{bugId, String.valueOf(new_eff), String.join(" ", bestKeywordList)});
        }
        else {
//                先移除其中project-level的低质量关键词
            low_quality_token.removeAll(low_quality_token_list);
//            System.out.println(low_quality_token);

            int tmp_best_keyword_num = bestKeywordList.size();
            String tmp_best_keyword = String.join(" ", bestKeywordList);
            int tmp_best_eff = new_eff;
//            System.out.println("tmp keywords: " + tmp_best_keyword);
            List<String> low_quality_tokens = new ArrayList<>(low_quality_token);
            for (int i = 0; i < low_quality_tokens.size(); i++) {
                String token = low_quality_tokens.get(i);
                bestKeywordList.add(token);
                int tmp_eff = search(luceneSearch, String.join(" ", bestKeywordList));

//                    效果提升，保留该词，并继续与其他词进行组合
//                System.out.println("tmp eff: " + tmp_eff + " ; tmp best eff: " + tmp_best_eff);
                if (tmp_eff != -1 && tmp_eff < tmp_best_eff) {
                    tmp_best_eff = tmp_eff;
                    tmp_best_keyword_num = bestKeywordList.size();
                    tmp_best_keyword = String.join(" ", bestKeywordList).trim();
                } else if (tmp_eff != -1) {
                    bestKeywordList = removeToken(bestKeywordList, token);
                    continue;
                } else {
                    low_quality_token_list.add(token);
                    bestKeywordList = removeToken(bestKeywordList, token);
                    continue;
                }

//                    与其他词进行组合
                for (int j = i + 1; j < low_quality_tokens.size(); j++) {
                    String token_1 = low_quality_tokens.get(j);
                    bestKeywordList.add(token_1);
                    tmp_eff = search(luceneSearch, String.join(" ", bestKeywordList).trim());

//                        效果好继续保留
                    if (tmp_eff != -1 && tmp_eff < tmp_best_eff) {
                        tmp_best_eff = tmp_eff;
                        tmp_best_keyword_num = bestKeywordList.size();
                        tmp_best_keyword = String.join(" ", bestKeywordList).trim();
//                        效果不好则移除，不继续进行组合
                    } else if (tmp_eff != -1) {
                        bestKeywordList = removeToken(bestKeywordList, token_1);
//                        bestKeywordList.remove(token_1);
                    } else {
                        low_quality_token_list.add(token_1);
                        bestKeywordList = removeToken(bestKeywordList, token_1);
//                        bestKeywordList.remove(token_1);
                    }
                }
            }

//            System.out.println("the best effectiveness: " + tmp_best_eff);
            if (tmp_best_eff != -1 && ((tmp_best_eff < bestEffectiveness) || (tmp_best_eff == bestEffectiveness && tmp_best_keyword_num < newKeywordNum))) {
                System.out.println("temp eff: " + String.join(" ", bestKeywordList));
                System.out.println("temp keyword num: " + newKeywordNum);
                assert tmp_best_keyword_num <= GA_size || tmp_best_eff < bestEffectiveness;
                new_keywords.add(new String[]{bugId, String.valueOf(tmp_best_eff), tmp_best_keyword});
            }
            else {
                System.out.println("best eff: " + bestKeywords);
                System.out.println("best keyword num: " + bestKeywordsNum);
                assert bestKeywordsNum <= GA_size || bestEffectiveness < GA_effectiveness;
                new_keywords.add(new String[]{bugId, String.valueOf(bestEffectiveness), bestKeywords});
            }
        }

    }

    private void removeProjectLevelToken() throws Exception {
        for (int index = 0; index < new_keywords.size(); index++) {
            String[] item = new_keywords.get(index);

            String bugId = item[0];

            String bugReportFile = dict.get(project) + "//BugCorpus//" + bugId + ".txt";
            String luceneIndexPath = dict.get(project) + "//lucene_index";

            BugReport bugReport = new BugReport(bugReportFile, Integer.parseInt(bugId), project, dict);
            LuceneSearch luceneSearch = new LuceneSearch(bugReport, false, luceneIndexPath);

            String keywords_before = item[2];
//            keywords_before = dropDupKeyword(keywords_before);
            int eff_before = (int) Float.parseFloat(item[1]);
            List<String> keywords_before_list = Arrays.asList(keywords_before.split(" "));
            int len_before = keywords_before_list.size();

            keywords_before_list = removeTokens(keywords_before_list, low_quality_token_list);

            int len_after = keywords_before_list.size();
            int eff_after = search(luceneSearch, String.join(" ", keywords_before_list));

            if (eff_after != -1 && eff_after <= eff_before) {
                assert len_after <= len_before;
                if (eff_after == eff_before && len_after == len_before)
                    continue;
                item[2] = String.join(" ", keywords_before_list).trim();
                item[1] = String.valueOf(eff_after);
                new_keywords.set(index, item);
            }
            else {
                int len_tmp = len_after;
                int eff_tmp = eff_after;
                String keywords_tmp = String.join(" ", keywords_before_list);
                List<String> low_quality_tokens = new ArrayList<>(List.of(keywords_before.split(" ")));
                low_quality_tokens.retainAll(low_quality_token_list);

                for (int t_index = 0; t_index < low_quality_tokens.size(); t_index++) {
                    String token = low_quality_tokens.get(t_index);
                    keywords_before_list.add(token);
                    int eff = search(luceneSearch, String.join(" ", keywords_before_list).trim());

                    if (eff != -1 && eff < eff_tmp) {
                        eff_tmp = eff;
                        len_tmp = keywords_before_list.size();
                        keywords_tmp = String.join(" ", keywords_before_list);
                        low_quality_token_list.remove(token);
                    }
                    else {
                        keywords_before_list = removeToken(keywords_before_list, token);
                        continue;
                    }

                    for (int t_j = t_index + 1; t_j < low_quality_tokens.size(); t_j++) {
                        String token1 = low_quality_tokens.get(t_j);
                        keywords_before_list.add(token1);
                        eff = search(luceneSearch, String.join(" ", keywords_before_list).trim());

                        if (eff != -1 && eff < eff_tmp) {
                            eff_tmp = eff;
                            len_tmp = keywords_before_list.size();
                            keywords_tmp = String.join(" ", keywords_before_list);
                            low_quality_token_list.remove(token1);
                        }
                        else {
                            keywords_before_list = removeToken(keywords_before_list, token1);
                        }
                    }
                }

                if ((eff_tmp < eff_before) || (eff_tmp == eff_before && len_before < len_tmp)) {
                    assert len_tmp <= len_before || eff_tmp < eff_before;
                    item[1] = String.valueOf(eff_tmp);
                    item[2] = keywords_tmp;
                    new_keywords.set(index, item);
                }
            }
        }
    }

    private List<String> removeTokens(Collection<String> c1, Collection<String> c2) {
        List<String> c = new ArrayList<>();
        for (String token: c1) {
            if (!c2.contains(token)) {
                c.add(token);
            }
        }
        return c;
    }

    private List<String> removeToken(Collection<String> c1, String c2) {
        List<String> c = new ArrayList<>();
        for (String token: c1) {
            if (!token.equals(c2)) {
                c.add(token);
            }
        }
        return c;
    }

    private int search(LuceneSearch luceneSearch, String keywords) throws Exception {
        return luceneSearch.computeEffectiveness(keywords);
    }

    private void readHumanToken() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(human_choose_token_file_path));

            String token;
            while ((token = reader.readLine()) != null) {
                human_choose_token_list.add(token.trim());
            }

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeHumanToken() throws Exception {
        for (int index = 0; index < new_keywords.size(); index++) {
            String[] item = new_keywords.get(index);

            String bugId = item[0];

            String bugReportFile = dict.get(project) + "//BugCorpus//" + bugId + ".txt";
            String luceneIndexPath = dict.get(project) + "//lucene_index";

            BugReport bugReport = new BugReport(bugReportFile, Integer.parseInt(bugId), project, dict);
            LuceneSearch luceneSearch = new LuceneSearch(bugReport, false, luceneIndexPath);

            String keywords_before = item[2];
//            keywords_before = dropDupKeyword(keywords_before);
            int eff_before = (int) Float.parseFloat(item[1]);
            List<String> keywords_before_list = Arrays.asList(keywords_before.split(" "));
            int len_before = keywords_before_list.size();

            keywords_before_list = removeTokens(keywords_before_list, human_choose_token_list);

            int len_after = keywords_before_list.size();
            int eff_after = search(luceneSearch, String.join(" ", keywords_before_list));

            if (eff_after != -1 && eff_after <= eff_before) {
                if (eff_after == eff_before && len_after == len_before)
                    continue;
                item[2] = String.join(" ", keywords_before_list).trim();
                item[1] = String.valueOf(eff_after);
                new_keywords.set(index, item);
            }
            else {
                int eff_tmp = eff_after;
                String keywords_tmp = String.join(" ", keywords_before_list);
                List<String> low_quality_tokens = new ArrayList<>(List.of(keywords_before.split(" ")));
                low_quality_tokens.retainAll(human_choose_token_list);

                for (int t_index = 0; t_index < low_quality_tokens.size(); t_index++) {
                    String token = low_quality_tokens.get(t_index);
                    keywords_before_list.add(token);
                    int eff = search(luceneSearch, String.join(" ", keywords_before_list).trim());

                    if (eff != -1 && eff < eff_tmp) {
                        eff_tmp = eff;
                        keywords_tmp = String.join(" ", keywords_before_list);
                        human_choose_token_list.remove(token);
                    }
                    else {
                        keywords_before_list = removeToken(keywords_before_list, token);
                        continue;
                    }

                    for (int t_j = t_index + 1; t_j < low_quality_tokens.size(); t_j++) {
                        String token1 = low_quality_tokens.get(t_j);
                        keywords_before_list.add(token1);
                        eff = search(luceneSearch, String.join(" ", keywords_before_list).trim());

                        if (eff != -1 && eff < eff_tmp) {
                            eff_tmp = eff;
                            keywords_tmp = String.join(" ", keywords_before_list);
                            human_choose_token_list.remove(token1);
                        }
                        else {
                            keywords_before_list = removeToken(keywords_before_list, token1);
                        }
                    }
                }

                if (eff_tmp <= eff_before) {
                    item[1] = String.valueOf(eff_tmp);
                    item[2] = keywords_tmp;
                    new_keywords.set(index, item);
                }
            }
        }
    }

    private void addLeftToken() {
        for (String bugId : all_bug_id_list) {
            if (!added_bug_id_list.contains(bugId)) {
                String[] item = keyword_dict.get(bugId);
                new_keywords.add(new String[]{bugId, item[0], item[1]});
            }
        }
    }

    private void outputResult() {
        try {
            File resFile = new File(refinement_result_file_path);

            if (!resFile.exists()) {
                if (resFile.createNewFile()) {
                    System.out.println("Successfully create file " + resFile.getName());
                } else {
                    System.out.println("Fail to create file " + resFile.getName());
                }
            }

            FileWriter resFileWriter = new FileWriter(resFile);

            CsvWriter csvWriter = new CsvWriter(resFileWriter, ',');

            String[] header = new String[]{"bugId", "effectiveness", "keywords"};
            csvWriter.writeRecord(header);

            for (String[] record: new_keywords) {
                csvWriter.writeRecord(record);
            }

            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void outputProjectLevelToken() throws IOException {
        File file = new File(low_quality_token_file_path);
        file.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (String token: low_quality_token_list) {
            writer.write(token);
            writer.write("\n");
        }

        writer.close();
    }

    private void outputModifiedHumanToken() {
        try {
            File file = new File(modified_human_choose_token_file_path);
            file.createNewFile();

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            for (String token: human_choose_token_list) {
                writer.write(token);
                writer.write("\n");
            }

            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> compareKeywords(List<String> k1, List<String> k2, int e1, int e2) {

        if (e1 == -1) {
            low_quality_token_list.addAll(k1);
            return k1;
        }
        else if (e2 == -1) {
            low_quality_token_list.addAll(k2);
            return k2;
        }

        List<String> shared_k = new ArrayList<>(k1);
        shared_k.retainAll(k2);

        List<String> low_quality = new ArrayList<>();

//        没有重合词的情况
        if (shared_k.size() == 0) {
            return null;
        }
//        有重合词的情况
        else {
//            k2在k1的基础上新增了词语
            if (k1.size() == shared_k.size()) {
//                效果下降或者保持不变，说明主要的关键词大概率在k1当中
                if (e1 <= e2) {
                    for (String token: k2) {
                        if (!shared_k.contains(token))
                            low_quality.add(token);
                    }
                }
                else {
                    for (String token: k1) {
                        if (!shared_k.contains(token))
                            low_quality.add(token);
                    }
                }
                return low_quality;
            }
//            k2在k1的基础上减少了词语
            else if (k2.size() == shared_k.size()){
                if (e1 >= e2) {
                    for (String token: k1) {
                        if (!shared_k.contains(token))
                            low_quality.add(token);
                    }
                }
                else {
                    for (String token: k2) {
                        if (!shared_k.contains(token))
                            low_quality.add(token);
                    }
                }
                return low_quality;
            }
//            k1与k2相交
            else {
//                相交部分的效果是相当的，效果差说明特有的部分词语质量不高
                if (e1 <= e2) {
                    for (String token: k2) {
                        if (!shared_k.contains(token))
                            low_quality.add(token);
                    }
                }
                else {
                    for (String token: k1) {
                        if (!shared_k.contains(token))
                            low_quality.add(token);
                    }
                }
                return low_quality;
            }
        }
    }

    public static void main(String[] args) {
        keyword_refinement kr = new keyword_refinement();

        String filePath = "E:\\query-reformulation\\dataset\\"+ kr.project + "\\data\\low_quality_expand_result.csv";

        kr.refine(filePath);
//        kr.outputResult();
    }

}

package org.query.util;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import org.query.entity.BugReport;
import org.query.property.KeywordProperty;
import org.query.search_engine.LuceneSearch;

import java.io.*;
import java.util.*;

/**
 * @author CAI
 * @date 2023/5/10
 **/
public class EvalDataProcess {
    private final KeywordProperty keywordProperty = KeywordProperty.getInstance();

    private List<String> keywords = new ArrayList<>();

    private List<String[]> bugCorpusFilePaths = new ArrayList<>();

    String project = "AspectJ";

    private void readKeywordFile() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(keywordProperty.keywordFilepath));

            String lineContent;
            Set<String> wordSet = new HashSet<>();
            while ((lineContent = in.readLine()) != null) {
                StringBuilder stringBuilder = new StringBuilder();
                wordSet.addAll(List.of(lineContent.split(" ")));
                for (String word: wordSet) {
                    stringBuilder.append(word).append(" ");
                }
                lineContent = stringBuilder.toString().trim();
                keywords.add(lineContent);
                wordSet.clear();
            }

            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    private void readKeywordFile() {
//        try {
//            BufferedReader in = new BufferedReader(new FileReader(keywordProperty.keywordFilepath));
//
//            StringBuilder stringBuilder = new StringBuilder();
//
//            String lineContent;
//            while ((lineContent = in.readLine()) != null) {
//                if (!lineContent.equals("")) {
//                    String[] contents = lineContent.split("\t");
//                    if (contents[2].equals("1")) {
//                        stringBuilder.append(contents[0]).append(" ");
//                    }
//                }
//                else {
//                    String keyword = stringBuilder.toString().trim();
//                    keywords.add(keyword);
//                    stringBuilder = new StringBuilder();
//                }
//            }
//
//            in.close();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private void readTestFile() {
        try {
            CsvReader reader = new CsvReader(keywordProperty.testFilepath);

            reader.readHeaders();

            while (reader.readRecord()) {
                String[] record = reader.getValues();
                String project = record[0];
                String bugId = record[1];

                bugCorpusFilePaths.add(new String[]{bugId, project});
            }

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readFile() {

        try {
            CsvReader reader = new CsvReader("E:\\query-reformulation\\dataset\\" + project + "\\data\\low_quality_expand_result.csv");

            reader.readHeaders();

            while (reader.readRecord()) {
                String[] record = reader.getValues();
                String bugId = record[0];

                if (record[1].equals(""))
                    continue;

                String[] text = record[1].split(" ");
                String[] label = record[2].split(" ");
                StringBuilder stringBuilder = new StringBuilder();

                for (int index = 0; index < text.length; index++) {
                    if (Objects.equals(label[index], "1")) {
                        stringBuilder.append(text[index]).append(" ");
                    }
                }

                String keyword = stringBuilder.toString().trim();

                bugCorpusFilePaths.add(new String[]{bugId, project});
                keywords.add(keyword);
            }

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readRefinementKeywordFile() {
        try {
//            CsvReader reader = new CsvReader("E:\\query-reformulation\\dataset\\" + project + "\\data\\t.csv");
            CsvReader reader = new CsvReader("E:\\query-reformulation\\dataset\\" + project + "\\data\\high_quality_multi_sent.csv");
//            CsvReader reader = new CsvReader("E:\\query-reformulation\\dataset\\" + project + "\\data\\refinement_result_with_human_1.csv");

            reader.readHeaders();

            while (reader.readRecord()) {
                String[] record = reader.getValues();
                String bugId = record[0];

                if (record[3].equals(""))
                    continue;

                String keyword = record[3];
//                String[] keywordArr = record[3].split(" ");
//                List<String> keywordList = new ArrayList<>();
//                for (String k: keywordArr) {
//                    k = k.replaceAll("[^a-z]", "");
//                    if (!k.equals("") && !keywordList.contains(k)) {
//                        keywordList.add(k);
//                    }
//                }
//
//                keyword = String.join(" ", keywordList).trim();
//                System.out.println(keyword);

                bugCorpusFilePaths.add(new String[]{bugId, project});
                keywords.add(keyword);
            }

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void searchForCluster() {
        assert keywords.size() == bugCorpusFilePaths.size();

        try {

            File resFile = new File("E:\\query-reformulation\\dataset\\" + project + "\\data\\cluster.csv");
//            File resFile = new File(keywordProperty.resFilepath);

            if (!resFile.exists()) {
                if (resFile.createNewFile()) {
                    System.out.println("Successfully create file " + resFile.getName());
                } else {
                    System.out.println("Fail to create file " + resFile.getName());
                }
            }

            FileWriter resFileWriter = new FileWriter(resFile);

            CsvWriter csvWriter = new CsvWriter(resFileWriter, ',');

            String[] header = new String[]{"Project", "BugId", "cluster", "Keyword"};
//            String[] header = new String[]{"Project", "BugId", "Effectiveness", "AP", "Keyword"};
            csvWriter.writeRecord(header);

            for (int i = 0; i < bugCorpusFilePaths.size(); i++) {
                String project = bugCorpusFilePaths.get(i)[1];
                int bugId = Integer.parseInt(bugCorpusFilePaths.get(i)[0].replaceAll("[^0-9]", ""));

                String bugReportFile = keywordProperty.dict.get(project) + "//BugCorpus//" + String.valueOf(bugId) + ".txt";
                String luceneIndexPath = keywordProperty.dict.get(project) + "//lucene_index";

                BugReport bugReport = new BugReport(bugReportFile, bugId, project);

                LuceneSearch luceneSearch = new LuceneSearch(bugReport, false, luceneIndexPath);

                String keyword = keywords.get(i);
                keyword = keyword.replaceAll("[^a-zA-Z\s0-9]", "").trim();
                System.out.println(keyword);

                List<String[]> searchRes = luceneSearch.searchCode(keyword);
                int codeId = luceneSearch.getFirstHitId(searchRes);
//                float[] metrics = luceneSearch.computeAll(searchRes);
//                float[] metrics = luceneSearch.computeAllMetrics(searchRes);

                String[] record = new String[]{project, String.valueOf(bugId), String.valueOf(codeId), keyword};
                csvWriter.writeRecord(record);
            }

            csvWriter.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void search() {
        assert keywords.size() == bugCorpusFilePaths.size();

        try {

//            File resFile = new File("E:\\query-reformulation\\dataset\\" + project + "\\data\\GA_metrics.csv");
            File resFile = new File("E:\\query-reformulation\\dataset\\" + project + "\\data\\refinement_result_low_quality.csv");
//            File resFile = new File(keywordProperty.resFilepath);

            if (!resFile.exists()) {
                if (resFile.createNewFile()) {
                    System.out.println("Successfully create file " + resFile.getName());
                } else {
                    System.out.println("Fail to create file " + resFile.getName());
                }
            }

            FileWriter resFileWriter = new FileWriter(resFile);

            CsvWriter csvWriter = new CsvWriter(resFileWriter, ',');

//            String[] header = new String[]{"Project", "BugId", "Effectiveness", "AP", "RR", "Keyword"};
            String[] header = new String[]{"Project", "BugId", "Effectiveness", "AP", "Keyword"};
            csvWriter.writeRecord(header);

            for (int i = 0; i < bugCorpusFilePaths.size(); i++) {
                String project = bugCorpusFilePaths.get(i)[1];
                int bugId = Integer.parseInt(bugCorpusFilePaths.get(i)[0].replaceAll("[^0-9]", ""));

                String bugReportFile = keywordProperty.dict.get(project) + "//BugCorpus//" + bugId + ".txt";
                String luceneIndexPath = keywordProperty.dict.get(project) + "//lucene_index";

                BugReport bugReport = new BugReport(bugReportFile, bugId, project);

                LuceneSearch luceneSearch = new LuceneSearch(bugReport, false, luceneIndexPath);

                String keyword = keywords.get(i);
                keyword = keyword.replaceAll("[^a-zA-Z\s0-9]", "").trim();
                System.out.println(keyword);

                List<String[]> searchRes = luceneSearch.searchCode(keyword);
//                float[] metrics = luceneSearch.computeAll(searchRes);
                float[] metrics = luceneSearch.computeAllMetrics(searchRes);

                String[] record = new String[]{project, String.valueOf(bugId), String.valueOf(metrics[0]), String.valueOf(metrics[1]), keyword};
//                String[] record = new String[]{project, String.valueOf(bugId), String.valueOf(metrics[0]), String.valueOf(metrics[1]), String.valueOf(metrics[2]), keyword};
                csvWriter.writeRecord(record);
            }

            csvWriter.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void evalData() {

//        readTestFile();
//        readKeywordFile();
        readFile();
//        readRefinementKeywordFile();
        search();

//        readRefinementKeywordFile();
//        searchForCluster();

    }

    public static void main(String[] args) {
//        KeywordProperty.createInstance("F:\\query-reformulation\\Keyword-T5\\eval_data\\pagerank.txt", true, false, false);
//        KeywordProperty.createInstance("F:\\query-reformulation\\Keyword-T5\\eval_data\\res_drop_prompt.txt", false, false, true);
//        KeywordProperty.createInstance("F:\\query-reformulation\\Keyword-T5\\eval_data\\res_drop.txt", false, true, false);
//        KeywordProperty.createInstance("F:\\query-reformulation\\keyword_extraction\\output\\bert_bilstm_crf\\token_labels_test.txt", false, false, false);
        KeywordProperty.createInstance("F:\\query-reformulation\\Keyword-T5\\eval_data\\res_refine.txt", false, false, false);

        new EvalDataProcess().evalData();
    }
}

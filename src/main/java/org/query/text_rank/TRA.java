package org.query.text_rank;

import com.csvreader.CsvWriter;
import org.query.entity.BugReport;
import org.query.property.KeywordProperty;
import org.query.search_engine.LuceneSearch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 将text rank提取出来的关键词放到lucene中进行检索
 * @author CAI
 * @date 2023/04/13
 **/
public class TRA {

    private final String project = "Tomcat";

//    存放text rank提取的关键词和最后保存检索结果的文件夹路径
    private final String resDir = "E:\\query-reformulation\\dataset\\" + project + "\\data";
//    存放bug报告数据的文件夹路径
    private final List<String[]> bugCorpusDir = new ArrayList<>();

    private final KeywordProperty keywordProperty = KeywordProperty.getInstance();

    /**
     * 检索
     * @throws Exception
     */
    private void Search() throws Exception {
//        存放某个bug报告检索结果的文件夹路径
        String searchResDir = resDir + "\\textrank_metrics.csv";
        List<String> keywords = readKeywordsFromFile();

        try {
            File resFile = new File(searchResDir);

            if (!resFile.exists()) {
                if (resFile.createNewFile()) {
                    System.out.println("Successfully create file " + resFile.getName());
                } else {
                    System.out.println("Fail to create file " + resFile.getName());
                }
            }

            FileWriter resFileWriter = new FileWriter(resFile);

            CsvWriter csvWriter = new CsvWriter(resFileWriter, ',');

            String[] header = new String[]{"Project", "BugId", "Effectiveness", "AP", "RR", "keyword"};
            csvWriter.writeRecord(header);

            for (int i = 0; i < bugCorpusDir.size(); i++) {
                int bugId = Integer.parseInt(bugCorpusDir.get(i)[0].replaceAll("[^0-9]", ""));
                String bugReportFile = keywordProperty.dict.get(project) + "//BugCorpus//" + bugId + ".txt";
                String luceneIndexPath = keywordProperty.dict.get(project) + "//lucene_index";

                BugReport bugReport = new BugReport(bugReportFile, bugId, project);
                LuceneSearch luceneSearch = new LuceneSearch(bugReport, false, luceneIndexPath);

                String keyword = keywords.get(i);
                keyword = keyword.replaceAll("[^a-zA-Z\s0-9]", "").trim();

                List<String[]> searchRes = luceneSearch.searchCode(keyword);
                float[] metrics = luceneSearch.computeAll(searchRes);

                String[] record = new String[]{project, String.valueOf(bugId), String.valueOf(metrics[0]), String.valueOf(metrics[1]), String.valueOf(metrics[2]), keyword};
                csvWriter.writeRecord(record);
            }

            csvWriter.close();
            resFileWriter.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 从文件中读取TextRank提取的关键词，以字符串的形式返回
     * @return
     */
    private List<String> readKeywordsFromFile() {
        List<String> keywords = new ArrayList<>();

        String filename = resDir + "\\textrank.txt";

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));

            String keyword;
            String bugId;
            while ((keyword = bufferedReader.readLine()) != null) {
                if (!keyword.equals("")) {
                    bugId = keyword.split("\t")[0];
                    try {
                        keyword = keyword.split("\t")[1];
                    } catch (Exception e) {
                        continue;
                    }
                    bugCorpusDir.add(new String[]{bugId, project});
                    keywords.add(keyword);
                }
            }

            bufferedReader.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return keywords;
    }

    public static void main(String[] args) throws Exception {
        KeywordProperty.createInstance("F:\\query-reformulation\\keyword_extraction\\output\\bert_bilstm_crf\\token_labels_test.txt", false, false, false);
        TRA tra = new TRA();
        tra.Search();
    }
}
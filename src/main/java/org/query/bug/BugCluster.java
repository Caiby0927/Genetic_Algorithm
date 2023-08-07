package org.query.bug;

import com.csvreader.CsvReader;
import org.query.entity.BugReport;
import org.query.property.KeywordProperty;
import org.query.search_engine.LuceneSearch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author CAI
 * @date 2023/7/24
 **/
public class BugCluster {

    private final KeywordProperty keywordProperty = KeywordProperty.getInstance();

    private final String project = "Birt";

    private final Map<Integer, String> keywordDict = new HashMap<>();

    private final Map<Integer, List<Integer>> clusterDict = new HashMap<>();

    public void computeCluster() throws Exception {
        String commonPath = "F:\\query-reformulation\\dataset\\" + project + "\\data";
        BufferedReader reader = new BufferedReader(new FileReader(commonPath + "\\SortedId.txt"));

        String line = null;
        List<Integer> idList = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            String idStr = line.substring(0, line.indexOf("\t"));
            idList.add(Integer.parseInt(idStr));
        }
        reader.close();

        FileWriter writer = new FileWriter(commonPath + "\\BugCluster.txt");

        for (Integer bugId: idList) {
            String bugReportFile = keywordProperty.dict.get(project) + "//BugCorpus//" + bugId + ".txt";
//            System.out.println(bugReportFile);
            String luceneIndexPath = keywordProperty.dict.get(project) + "//lucene_index";

            BugReport bugReport = new BugReport(bugReportFile, bugId, project);
            LuceneSearch luceneSearch = new LuceneSearch(bugReport, false, luceneIndexPath);

            String keyword = "";

            if (keywordDict.containsKey(bugId)) {
                keyword = keywordDict.get(bugId);
            } else {
                continue;
            }

            List<String[]> searchRes = luceneSearch.searchCode(keyword);
            int codeId = luceneSearch.getFirstHitId(searchRes);

            StringBuilder historyIds = new StringBuilder();
            List<Integer> historyIdList;

            if (clusterDict.containsKey(codeId)) {
                historyIdList = clusterDict.get(codeId);
                for (Integer historyId: historyIdList) {
                    historyIds.append(historyId).append(" ");
                }
            } else {
                historyIdList = new ArrayList<>();
            }

            historyIdList.add(bugId);
            clusterDict.put(codeId, historyIdList);

            String output = bugId + ":" + historyIds;

            writer.write(output.trim() + "\n");
            writer.flush();
        }

        writer.close();
    }

    private void readKeyword() throws IOException {
        CsvReader reader = new CsvReader(keywordProperty.dict.get(project) + "\\refinement_result_with_human.csv");

        reader.readHeaders();
        while (reader.readRecord()) {
            String[] record = reader.getValues();
            String idStr = record[0].replaceAll("[^0-9]", "");
            String keyword = record[2].trim();

            keywordDict.put(Integer.parseInt(idStr), keyword);
        }

        reader.close();
    }

    public static void main(String[] args) throws Exception {
        KeywordProperty.createInstance("", false, false, false);

        BugCluster cluster = new BugCluster();
        cluster.readKeyword();
        cluster.computeCluster();
    }
}

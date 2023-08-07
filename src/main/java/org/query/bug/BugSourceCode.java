package org.query.bug;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import org.query.entity.BugReport;
import org.query.entity.SourceCode;
import org.query.property.KeywordProperty;

import java.io.*;
import java.util.*;

/**
 * @author CAI
 * @date 2023/7/27
 **/
public class BugSourceCode {

    private final KeywordProperty keywordProperty = KeywordProperty.getInstance();

    private final String project = "Birt";

    private final String commonPath = "F:\\query-reformulation\\dataset\\" + project + "\\data";

    private final String bugTextPath = commonPath + "\\BugText.csv";

    public void getTargetCode() throws Exception {

        BufferedReader reader = new BufferedReader(new FileReader(commonPath + "\\SortedId.txt"));

        String line;
        List<Integer> idList = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            String idStr = line.substring(0, line.indexOf("\t"));
            idList.add(Integer.parseInt(idStr));
        }
        reader.close();

        String resFilePath = commonPath + "\\BugTargetCode.csv";

        CsvWriter writer = new CsvWriter(resFilePath);

        writer.writeRecord(new String[]{"bugId", "text", "targetCodeIds", "targetCodeToken"});

        for (Integer bugId: idList) {
            String bugReportFile = keywordProperty.dict.get(project) + "\\BugCorpus\\" + bugId + ".txt";

            BugReport bugReport = new BugReport(bugReportFile, bugId, project);
            List<Integer> targetCodeIds = bugReport.getTarget_source_code();
            StringBuilder codeIds = new StringBuilder();

            for (int codeId: targetCodeIds) {
                codeIds.append(codeId).append(" ");
            }

//            找target code对应的tokens
            String targetCodeToken = getTargetCodeTokens(bugReport, targetCodeIds);

//            找bug报告自身的文本
            String bugText = bugReport.getTokensString();

            String[] output = new String[] {String.valueOf(bugId), bugText, codeIds.toString().trim(), targetCodeToken};

            writer.writeRecord(output);
            writer.flush();
        }
        writer.close();

    }

//    找bug报告与source file之间的重合词
    private void findSharedTokens() throws IOException {
        BufferedReader readerSimilarBug = new BufferedReader(new FileReader(commonPath + "\\BugSimilarity_0.6.txt"));
        CsvReader readerText = new CsvReader(bugTextPath);

        String line;
        while ((line = readerSimilarBug.readLine()) != null) {
            String[] lineContent = line.split(":");

            if (!lineContent[1].trim().equals("")) {
                String[] similarBugs = lineContent[1].trim().split(" ");
                for (String similarBug: similarBugs) {

                }
            }
        }

        readerSimilarBug.close();
        readerText.close();
    }

    private String getTargetCodeTokens(BugReport bugReport, List<Integer> targetCodeIds) {
        List<SourceCode> sourceCodeList = bugReport.getSource_code_list();
//        用set的目的就是将token都进行去重处理
        Set<String> tokenSet = new HashSet<>();


        for (SourceCode sourceCode: sourceCodeList) {
            int codeId = sourceCode.getCode_id();
//            找到target code的数据
            if (targetCodeIds.contains(codeId)) {
//                将token添加到set中
                String tokens = sourceCode.getTokensString();
                tokenSet.addAll(List.of(tokens.split(" ")));
            }
        }

        return String.join(" ", tokenSet).trim();
    }

    public static void main(String[] args) throws Exception {
        KeywordProperty.createInstance("", false, false, false);

        BugSourceCode bugSourceCode = new BugSourceCode();
        bugSourceCode.getTargetCode();
    }
}

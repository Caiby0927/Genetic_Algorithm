package org.query.util;

import com.csvreader.CsvWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author CAI
 * @date 2023/4/17
 **/
public class Statistic {

//    数据集文件夹路径
    private final String dataDir = "F:\\query-reformulation\\zookeeper\\data";
//    检索结果文件夹路径
    private final String resDir = "F:\\query-reformulation\\pre_experiment\\zookeeper";
//    bug报告id列表
    private List<String> bugIdList;
//    bug报告id与目标代码文件id映射表
    private Map<String, String> codeIdMap;
//    bug报告id与initial effectiveness的对应关系
    private Map<String, String> initialEffMap;
//    bug报告id与text rank effectiveness的对应关系
    private Map<String, String> textRankEffMap;
//    bug报告id与genetic algorithm effectiveness的对应关系
    private Map<String, String> geneticAlgorithmMap;
//    GA算法提取的关键词列表
    private List<String> geneticKeywords = new ArrayList<>();
//    TextRank算法提取的关键词列表
    private List<String> textRankKeywords = new ArrayList<>();
//    目标源代码的内容
    private List<String> targetCodeContent = new ArrayList<>();

    private List<String> GAUnique = new ArrayList<>();
    private List<String> TRUnique = new ArrayList<>();
    private List<String> both = new ArrayList<>();

    private String GAUniqueWithCode = "";
    private String TRUniqueWithCode = "";
    private String GAInvalidKeywords = "";
    private String TRInvalidKeywords = "";

    private Map<String, String> geneticAlgorithmRelevantDocMap;
    private Map<String, String> textRankRelevantDocMap;
    private Map<String, String> initialRelevantDocMap;

    public Statistic() {
        readBugId();
        readInitialEff();
        readTextRankEff();
        readGAEff();

        initialRelevantDocMap = new HashMap<>();
        String rankListDirPrefix = resDir + "//GA_result";
        countRelevantDocs(initialRelevantDocMap, rankListDirPrefix, true);

        textRankRelevantDocMap = new HashMap<>();
        rankListDirPrefix = resDir + "//TextRank_result";
        countRelevantDocs(textRankRelevantDocMap, rankListDirPrefix, false);

        geneticAlgorithmRelevantDocMap = new HashMap<>();
//        rankListDirPrefix = resDir + "//GA_result";
//        rankListDirPrefix = resDir + "//GA_result_consider_num";
        rankListDirPrefix = resDir + "//GA_base_GA_result";
        countRelevantDocs(geneticAlgorithmRelevantDocMap, rankListDirPrefix, false);
    }

    /**
     * 获取bug报告Id列表
     */
    private void readBugId() {
        bugIdList = new ArrayList<>();
        codeIdMap = new HashMap<>();

        String bugCorpusDir = dataDir + "//BugCorpus";

        File dataFolder = new File(bugCorpusDir);
        File[] bugFiles = dataFolder.listFiles();

        assert bugFiles != null;
        for (File bugFile: bugFiles) {
            if (bugFile.isFile()) {
                String bugId = bugFile.getName().split("\\.")[0].replaceAll("[^0-9]", "");
                bugIdList.add(bugId);
                readCodeId(bugId);
            }
        }
    }

    /**
     * 建立bugId与codeId的对应关系
     * @param bugId
     */
    private void readCodeId(String bugId) {
//        存放bugId与target code name的对应关系文件
        String fixLinkFile = dataDir + "//FixLink.txt";
//        存放codeId与code name的对应关系文件
        String classNameFile = dataDir + "//BugLocator_" + bugId + "//ClassName.txt";

        List<String> codeNameList = new ArrayList<>();

        try {
            BufferedReader in = new BufferedReader(new FileReader(fixLinkFile));

            StringBuilder stringBuilder = new StringBuilder();

//            先获取target code name
            String content;
            while ((content = in.readLine()) != null) {
                String[] contents = content.split("\t");
//                判断bugId是否匹配
                if (bugId.equals(contents[0])) {
                    String codeName = contents[1];

//                一开始提取出来的codeName包含code的完整路径
                    contents = codeName.split("/");
                    codeName = contents[contents.length - 1].split("\\.")[0];
                    codeNameList.add(codeName);
                }
            }

//            获取codeId
            in = new BufferedReader(new FileReader(classNameFile));
            while ((content = in.readLine()) != null) {
                String[] contents = content.split("\t");
                String codeId = contents[0];
                String codeName = contents[1];

                contents = codeName.split("\\.");
                codeName = contents[contents.length - 2];

//                codeName比对
                if (codeNameList.contains(codeName)) {
                    stringBuilder.append(codeId).append(", ");
                }
            }

            in.close();

//            将stringBuilder转化为string
            String codeIdSequence = stringBuilder.toString().trim();
//            添加映射关系
            codeIdMap.put(bugId, codeIdSequence);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 建立bugId与initial effectiveness的对应关系
     */
    private void readInitialEff() {
        initialEffMap = new HashMap<>();
//        文件夹前缀
        String initialResDirPrefix = resDir + "//GA_result//BugReport_";
        for (String bugId: bugIdList) {
            String fileName = initialResDirPrefix + bugId + "//initial_effectiveness.txt";

            try {
                BufferedReader in = new BufferedReader(new FileReader(fileName));

                String effectiveness;
                effectiveness = in.readLine();
                initialEffMap.put(bugId, effectiveness);

                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 建立bugId与text rank effectiveness的对应关系
     */
    private void readTextRankEff() {
        textRankEffMap = new HashMap<>();

//        文件夹前缀
        String textRankResDirPrefix = resDir + "//TextRank_result//BugReport_";

        for (String bugId: bugIdList) {
            String filename = textRankResDirPrefix + bugId + "//result.txt";

            try {
                BufferedReader in = new BufferedReader(new FileReader(filename));

                String effectiveness;
                effectiveness = in.readLine();
                textRankEffMap.put(bugId, effectiveness);

                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 建立bugId与genetic algorithm effectiveness的对应关系
     */
    private void readGAEff() {
        geneticAlgorithmMap = new HashMap<>();

//        String geneticAlgorithmResDirPrefix = resDir + "//GA_base_GA_result//BugReport_";
//        String geneticAlgorithmResDirPrefix = resDir + "//GA_result_consider_num//BugReport_";
        String geneticAlgorithmResDirPrefix = resDir + "//GA_result//BugReport_";
        for (String bugId: bugIdList) {
            String fileName = geneticAlgorithmResDirPrefix + bugId + "//best_keywords.txt";

            String effectiveness = "---";

            File file = new File(fileName);
//            对于那些initial search结果在top-10以内的bug报告，没有用GA算法再检索一次，因此可能文件不存在
            if (file.exists()) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(fileName));

                    String firstLine;

                    firstLine = in.readLine();
//                第一行的形式是“***** effectiveness is 18.0 *****”
//                使用正则表达式提取
                    Pattern pattern = Pattern.compile("[0-9]*\\.[0-9]");
                    Matcher matcher = pattern.matcher(firstLine);
                    if (matcher.find())
                        effectiveness = matcher.group();

                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            geneticAlgorithmMap.put(bugId, effectiveness);
        }
    }

    /**
     * 读取特定bug报告对应的GA算法关键词
     * @param bugId
     */
    private void readGAKeywords(String bugId) {
        geneticKeywords.clear();

        String resFilename = resDir + "//GA_base_GA_result//BugReport_" + bugId + "//best_keywords.txt";
//        String resFilename = resDir + "//GA_result_consider_num//BugReport_" + bugId + "//best_keywords.txt";
//        String resFilename = resDir + "//GA_result//BugReport_" + bugId + "//best_keywords.txt";

        File resFile = new File(resFilename);
        if (resFile.exists()) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(resFilename));

                String line;
                while ((line = in.readLine()) != null) {
//                    文件第一行，直接跳过
                    if (line.startsWith("***** effectiveness")) {
                        continue;
                    }
//                    另外一组关键词的第一行，结束读文件
                    else if (line.equals("***** another best keywords *****")) {
                        break;
                    }
                    geneticKeywords.add(line.trim());
                }

                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 读取特定bug报告对应的TextRank算法关键词
     * @param bugId
     */
    private void readTextRankKeywords(String bugId) {
        textRankKeywords.clear();

        String textRankTopKeywordsFilename = resDir + "//TextRank_result//BugReport_" + bugId + "//text_rank_top.txt";

        File textRankTopKeywordsFile = new File(textRankTopKeywordsFilename);
        if (textRankTopKeywordsFile.exists()) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(textRankTopKeywordsFilename));

                String line;
                while ((line = in.readLine()) != null) {
                    textRankKeywords.add(line.trim());
                }

                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String readCodeContent(String bugId, String codeId) {
        StringBuilder stringBuilder = new StringBuilder();

        String codeFilename = dataDir + "//BugLocator_" + bugId + "//SourceCorpus//" + codeId + ".txt";

        try {
            BufferedReader in = new BufferedReader(new FileReader(codeFilename));

            String line;
            while ((line = in.readLine()) != null) {
                stringBuilder.append(line).append(" ");
            }

            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return stringBuilder.toString().trim();
    }

    private void countRelevantDocs(Map<String, String> map, String filePath, boolean isInitial) {

        for (String bugId: bugIdList) {
            String rankListFilename;
            if (!isInitial)
                rankListFilename = filePath + "//BugReport_" + bugId + "//best_rank_list.txt";
            else
                rankListFilename = filePath + "//BugReport_" + bugId + "//initial_rank_list.txt";

            File rankListFile = new File(rankListFilename);
            String lineNum = "---";
            if (rankListFile.exists()) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(rankListFilename));

                    int lineCount = 0;
                    while (in.readLine() != null) {
                        lineCount += 1;
                    }

                    in.close();
                    lineNum = String.valueOf(lineCount);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            map.put(bugId, lineNum);
        }
    }

    private void compareTextRankAndGA() {

        GAUnique = new ArrayList<>(geneticKeywords);
        TRUnique = new ArrayList<>(textRankKeywords);
        both = new ArrayList<>(geneticKeywords);

//        获取unique和both列表
        GAUnique.removeAll(textRankKeywords);
        TRUnique.removeAll(geneticKeywords);
        both.removeAll(GAUnique);

        StringBuilder GAWithCodeStringBuilder = new StringBuilder();
        StringBuilder TRWithCodeStringBuilder = new StringBuilder();

        StringBuilder GAInvalidStringBuilder = new StringBuilder();
        StringBuilder TRInvalidStringBuilder = new StringBuilder();

        for (String codeContent: targetCodeContent) {
            List<String> code = Arrays.asList(codeContent.split(" "));

//            获取GA_unique和Code共同的词汇
            List<String> temp = new ArrayList<>(GAUnique);
            temp.removeAll(code);
            List<String> withCode = new ArrayList<>(GAUnique);
            withCode.removeAll(temp);
            for (String token: withCode) {
                GAWithCodeStringBuilder.append(token).append(" ");
            }
            GAWithCodeStringBuilder.append("=== ");

//            获取GA和Code不匹配的关键词
            temp = new ArrayList<>(geneticKeywords);
            temp.removeAll(code);
            for (String token: temp) {
                GAInvalidStringBuilder.append(token).append(" ");
            }
            GAInvalidStringBuilder.append("=== ");

//            获取TR_unique和Code的共同词汇
            temp = new ArrayList<>(code);
            temp.removeAll(TRUnique);
            withCode = new ArrayList<>(TRUnique);
            withCode.removeAll(temp);
            for (String token: withCode) {
                TRWithCodeStringBuilder.append(token).append(" ");
            }
            TRWithCodeStringBuilder.append("=== ");

//            获取TextRank和Code不匹配的关键词
            temp = new ArrayList<>(textRankKeywords);
            temp.removeAll(code);
            for (String token: temp) {
                TRInvalidStringBuilder.append(token).append(" ");
            }
            TRInvalidStringBuilder.append("=== ");
        }

        GAUniqueWithCode = GAWithCodeStringBuilder.toString().trim();
        TRUniqueWithCode = TRWithCodeStringBuilder.toString().trim();

        GAInvalidKeywords = GAInvalidStringBuilder.toString().trim();
        TRInvalidKeywords = TRInvalidStringBuilder.toString().trim();
    }

    private void outputResults() {

//        构建数据
        List<String[]> dataLines = new ArrayList<>();

        for (String bugId: bugIdList) {
            String initialEff = initialEffMap.get(bugId);
            String textRankEff = textRankEffMap.get(bugId);
            String geneticAlgorithmEff = geneticAlgorithmMap.get(bugId);
            String targetCode = codeIdMap.get(bugId);
//            System.out.println(targetCode);
            String initialRelevantDocNum = initialRelevantDocMap.get(bugId);
            String textRankRelevantDocNum = textRankRelevantDocMap.get(bugId);
            String geneticAlgorithmRelevantDocNum = geneticAlgorithmRelevantDocMap.get(bugId);

            String[] codeIds = targetCode.replaceAll(",", "").split(" ");
//            System.out.println(codeIds[0]);

            if (!geneticAlgorithmEff.equals("---")) {
                targetCodeContent.clear();

//                获取两种算法的关键词
                readGAKeywords(bugId);
                readTextRankKeywords(bugId);

//                获取目标源文件的内容
                for (String codeId: codeIds) {
                    String codeContent = readCodeContent(bugId, codeId);
                    targetCodeContent.add(codeContent);
                }

                compareTextRankAndGA();
            }
            else {
//                清空这些缓存，避免写入文件中
                geneticKeywords.clear();
                both.clear();
                TRUniqueWithCode = "";
                GAUniqueWithCode = "";
                TRInvalidKeywords = "";
                GAInvalidKeywords = "";

                continue;
            }

            dataLines.add(new String[]{bugId, initialEff, textRankEff, geneticAlgorithmEff, String.valueOf(geneticKeywords.size()), both.toString(), TRUniqueWithCode, GAUniqueWithCode, TRInvalidKeywords, GAInvalidKeywords, initialRelevantDocNum, textRankRelevantDocNum, geneticAlgorithmRelevantDocNum, targetCode});
//            System.out.println(GAInvalidKeywords);

//            break;
        }

//        String statisticFile = resDir + "\\statistic_GA_base_GA_result.csv";
//        String statisticFile = resDir + "\\statistic_consider_keywords_num.csv";
        String statisticFile = resDir + "\\statistic_without_consider_keywords_num.csv";

        CsvWriter writer = new CsvWriter(statisticFile, ',', StandardCharsets.UTF_8);

        try {
//            定义文件头
            String[] header = {"BugId", "InitialEff", "TR_Eff", "GA_Eff", "GA_keywords_num", "Overlap_keywords", "TR_unique_valid_keywords", "GA_unique_valid_keywords", "TR_invalid_keywords", "GA_invalid_keywords", "Initial_relevant_doc_num", "TextRank_relevant_doc_num", "Genetic_algorithm_relevant_doc_num", "Target_code"};
//            写入文件头
            writer.writeRecord(header);

            for (String[] dataLine: dataLines) {
//                逐条数据写入
                writer.writeRecord(dataLine);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
//            关闭writer
            writer.close();
        }

    }

    public static void main(String[] args) {
        Statistic statistic = new Statistic();
        statistic.outputResults();
    }

}

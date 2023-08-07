package org.query.util;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import org.query.property.Property;

import java.io.*;
import java.util.*;

/**
 * @author CAI
 * @date 2023/5/4
 **/
public class DataProcess {
    private final Property property = Property.getInstance();

    private List<String> sortedIdList;

    private List<String[]> trainingKeywordsList;

    private List<String[]> validateKeywordsList;

    private List<String[]> testKeywordsList;

    private String expandFilePath;

//    private final String[] filteredWords = {"abstract","assert","boolean","break","byte","case","catch","char","class","continue","default","do","double","else","enum","extends","final","finally","float","for","if","implements","import","int","interface","instanceof","long","native","new","package","private","protected","public","return","short","static","strictfp","super","switch","synchronized","this","throw","throws","transient","try","void","volatile","while", "goto", "const"};

    private String uselessWordsFilePrefix = "F:\\query-reformulation\\keyword_extraction\\data\\useless_words_";

    private void getSortedId() {
        String sortedFilePath = property.projectDataPath + "\\SortedId.txt";

        sortedIdList = new ArrayList<>();

        try {
            BufferedReader in = new BufferedReader(new FileReader(sortedFilePath));

            String lineContent;
            while ((lineContent = in.readLine()) != null) {
                String bugId = lineContent.split("\t")[0].replaceAll("[^0-9]", "");
                sortedIdList.add(bugId);
            }

            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (sortedIdList.size() == 0) {
            throw new RuntimeException("Sorted id list is null! Please check the SortedId.txt file.");
        }

    }

    private List<String> getUselessWords() {
        List<String> uselessWords = new ArrayList<>();

        try {
            BufferedReader in = new BufferedReader(new FileReader(uselessWordsFilePrefix));

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                uselessWords.add(line);
            }

            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return uselessWords;
    }

    private List<String[]> readData() throws Exception {

        getSortedId();

        List<String[]> allData = new ArrayList<>();

        CsvReader reader = new CsvReader(property.GAResultFilePath);

        reader.readHeaders();

        while (reader.readRecord()) {
            String[] record = reader.getValues();
            String bugId = record[0];
//            剔除一些无法通过提取关键词检索到buggy文件的bug报告
            int effectiveness = Integer.parseInt(record[1]);
//            剔除GA结果较差的实例
            if (effectiveness >= 500)
                continue;

            String keywords = record[3];
//            keywords = dropDupKeywords(keywords);

            String[] keyword_list = keywords.trim().split(" ");
            Set<String> keyword_set = new HashSet<>(Arrays.asList(keyword_list));

            StringBuilder stringBuilder = new StringBuilder();

            List<String> filter_list = getUselessWords();

            String tokens = record[4];

            Map<String, List<Integer>> count_dict = new HashMap<>();

//            获取标签
            String[] token_list = tokens.trim().split(" ");
            for (int index = 0; index < token_list.length; index++) {
                String token = token_list[index];
                if (!count_dict.containsKey(token)) {
                    List<Integer> list = new ArrayList<>();
                    list.add(index);
                    count_dict.put(token, list);
                }
                else {
                    List<Integer> list = count_dict.get(token);
                    list.add(index);
                    count_dict.put(token, list);
                }
            }

            String[] labels_array = new String[token_list.length];
            Random random = new Random();

            for (int i = 0; i < token_list.length; i++) {
                String token = token_list[i];
//                过滤掉Java关键字
                if (keyword_set.contains(token) && !filter_list.contains(token.toLowerCase())) {
                    List<Integer> list = count_dict.get(token);
//                    随机抽取一个位置标记为1
                    if (list.size() > 1) {
                        int num = (list.size() * 2) / 3;
                        while (num > 0) {
                            int pos = random.nextInt(list.size());
                            labels_array[list.get(pos)] = "1";
                            list.remove(pos);
                            num--;
                        }
                        for (int index: list) {
                            labels_array[index] = "0";
                        }
                        list.clear();
                        list.add(-1);
                        count_dict.put(token, list);
                    } else if (list.get(0) != -1) {
                        for (int index: list) {
                            labels_array[index] = "1";
                        }
                    }
//                    stringBuilder.append("1").append(" ");
//                    labels_array[i] = "1";
                } else {
//                    stringBuilder.append("0").append(" ");
                    labels_array[i] = "0";
                }
            }

            for (String label: labels_array) {
                stringBuilder.append(label).append(" ");
            }

            String labels = stringBuilder.toString().trim();

            String index = String.valueOf(sortedIdList.indexOf(bugId));

            String[] singleData = new String[]{bugId, index, keywords, tokens, labels};
            allData.add(singleData);
        }

        reader.close();

        return allData;
    }

    private List<String[]> readExpandData() throws Exception {
        getSortedId();

        List<String[]> allData = new ArrayList<>();

        CsvReader reader = new CsvReader(this.expandFilePath);

        reader.readHeaders();

        while (reader.readRecord()) {
            String[] record = reader.getValues();

            String bugId = record[0];
            String index = String.valueOf(sortedIdList.indexOf(record[0]));
            String text = record[1];
            String label = record[2];

            String[] singleData = new String[]{bugId, index, text, label};

            allData.add(singleData);
        }

        return allData;
    }

    private String dropDupKeywords(String keywords) {
        String[] keys = keywords.split(" ");

        Set<String> set = new HashSet<>(Arrays.asList(keys));

        StringBuilder stringBuilder = new StringBuilder();
        for (String key: set) {
            stringBuilder.append(key).append(" ");
        }

        return stringBuilder.toString().trim();
    }

    private List<String[]> sortData() throws Exception {
        List<String[]> dataList = readExpandData();
//        List<String[]> dataList = readData();

        dataList.sort(new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                int index1 = Integer.parseInt(o1[1]);
                int index2 = Integer.parseInt(o2[1]);
                int diff = index1 - index2;
                return Integer.compare(diff, 0);
            }
        });

        return dataList;
    }

    public void splitData() throws Exception {
        List<String[]> dataList = sortData();

        int dataSize = dataList.size();
        int trainingDataSize = (int) (dataSize * 0.8);
        int validateDataSize = (int) (dataSize * 0.1);

        trainingKeywordsList = new ArrayList<>();
        validateKeywordsList = new ArrayList<>();
        testKeywordsList = new ArrayList<>();

        for (int index = 0; index < dataSize; index++) {
            String bugId = dataList.get(index)[0];
//            String keywords = dataList.get(index)[2];
            String text = dataList.get(index)[2];
//            String text = dataList.get(index)[3];
            String label = dataList.get(index)[3];
//            String label = dataList.get(index)[4];
            if (index < trainingDataSize) {
                trainingKeywordsList.add(new String[]{property.project, bugId, text, label});
//                trainingKeywordsList.add(new String[]{property.project, bugId, keywords, text, label});
            } else if (index < trainingDataSize + validateDataSize) {
                validateKeywordsList.add(new String[]{property.project, bugId, text, label});
//                validateKeywordsList.add(new String[]{property.project, bugId, keywords, text, label});
            } else {
                testKeywordsList.add(new String[]{property.project, bugId, text, label});
//                testKeywordsList.add(new String[]{property.project, bugId, keywords, text, label});
            }
        }

        writeDataset(false, 1);
        writeDataset(false, 2);
        writeDataset(false, 3);

        writeDataset(true, 1);
        writeDataset(true, 2);
        writeDataset(true, 3);
    }

    private void writeDataset(boolean merge, int type) throws Exception{
        File dataFile = null;
//        是否合并为一个大数据集
        if (merge) {
            switch (type) {
                case 1 -> dataFile = new File(property.experimentProjectPath + "\\train_3.csv");
                case 2 -> dataFile = new File(property.experimentProjectPath + "\\validate_3.csv");
                case 3 -> dataFile = new File(property.experimentProjectPath + "\\test_3.csv");
//                case 1 -> dataFile = new File(property.experimentProjectPath + "\\train.csv");
//                case 2 -> dataFile = new File(property.experimentProjectPath + "\\validate.csv");
//                case 3 -> dataFile = new File(property.experimentProjectPath + "\\test.csv");
            }
        } else {
            switch (type) {
                case 1 -> dataFile = new File(property.projectDataPath + "\\train_3.csv");
                case 2 -> dataFile = new File(property.projectDataPath + "\\validate_3.csv");
                case 3 -> dataFile = new File(property.projectDataPath + "\\test_3.csv");
//                case 1 -> dataFile = new File(property.projectDataPath + "\\train.csv");
//                case 2 -> dataFile = new File(property.projectDataPath + "\\validate.csv");
//                case 3 -> dataFile = new File(property.projectDataPath + "\\test.csv");
            }
        }

        boolean fileExist = false;

        if (dataFile != null && !dataFile.exists()) {
            if (!dataFile.getParentFile().exists()) {
                if (dataFile.getParentFile().mkdirs()) {
                    System.out.println("Successfully create parent directories " + dataFile.getParent());
                    if (dataFile.createNewFile()) {
                        System.out.println("Successfully create file " + dataFile.getName());
                    } else {
                        System.out.println("Fail to create file " + dataFile.getName());
                    }
                } else {
                    System.out.println("Fail to create parent directories " + dataFile.getParent());
                }
            }
        } else {
            fileExist = true;
        }

        assert dataFile != null;
        FileWriter fileWriter = new FileWriter(dataFile, true);

        CsvWriter writer = new CsvWriter(fileWriter, ',');

        if (!fileExist) {
            String[] header = new String[]{"project", "bugId", "text", "labels"};
//            String[] header = new String[]{"project", "bugId", "keywords", "text", "labels"};
            writer.writeRecord(header);
        }

        switch (type) {
            case 1:
                for (String[] record: trainingKeywordsList) {
                    writer.writeRecord(record);
                }
                break;
            case 2:
                for (String[] record: validateKeywordsList) {
                    writer.writeRecord(record);
                }
                break;
            case 3:
                for (String[] record: testKeywordsList) {
                    writer.writeRecord(record);
                }
                break;
        }

        writer.close();
    }

    public static void main(String[] args) throws Exception {
        String project = "AspectJ";

        Property.createInstance(
                "F:\\query-reformulation\\keyword_extraction\\data",
//                "F:\\query-reformulation\\Keyword-T5\\data",
                "F:\\query-reformulation\\dataset\\" + project + "\\data",
                "F:\\query-reformulation\\dataset\\" + project + "\\data\\lucene_index",
                "F:\\query-reformulation\\dataset\\" + project + "\\data\\GA_result",
//                "F:\\query-reformulation\\dataset\\" + project + "\\GA_total_result.csv",
                "D:\\query-reformulation\\dataset\\" + project + "\\data\\GA_result\\GA_total_result.csv",
                "",
                project);
        DataProcess process = new DataProcess();
//        process.uselessWordsFilePrefix = process.uselessWordsFilePrefix + project + ".txt";
//        System.out.println(process.uselessWordsFilePrefix);
        process.expandFilePath = "E:\\query-reformulation\\dataset\\" + project + "\\data\\data_expand_4.csv";
        process.splitData();
    }
}

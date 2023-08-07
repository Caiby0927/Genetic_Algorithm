package org.query.entity;

import org.query.property.KeywordProperty;
import org.query.property.Property;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

/**
 * @author CAI
 * @date 2023/4/14
 **/
public class BugReport {

    Property property = Property.getInstance();

    KeywordProperty keywordProperty = KeywordProperty.getInstance();

    Dictionary<String, String> dictionary;

    private String projectName;
//    存储bug报告的长度
    private int token_num;
//    存储当前版本中源文件的数量
    private int source_file_num;
//    存储bug报告的词
    private List<String> tokens = new ArrayList<>();
//    存储bug报告的序号
    private final int bugId;
//    存储目标文件的序号
    private List<Integer> target_source_code = new ArrayList<>();
//    存储用于检索的源代码数据
    private List<SourceCode> source_code_list = new ArrayList<>();
//    存储最佳的关键词数量
    private int best_keywords_num = Integer.MAX_VALUE;
//    存储最佳的关键词列表
    private List<String> best_keywords = new ArrayList<>();

    private int initialEffectiveness;

    public BugReport(String[] info) {
        String bugReportFile = info[0];
        bugId = Integer.parseInt(info[1]);

//        System.out.println(bugReportFile);
//        System.out.println(bugId);

        read_BR(bugReportFile);

        String fixLinkFile = property.projectDataPath + "/GroundTruth/" + property.project + "/" + bugId + ".txt";
        List<String> target_classname_list = read_source_filename(fixLinkFile);
        read_code(target_classname_list);
    }

    /**
     * 构造函数
     * @param bugReportFile
     * @param bugId
     */
    public BugReport(String bugReportFile, int bugId) {
        read_BR_from_file(bugReportFile);
        this.bugId = bugId;
        String fixLinkFile = property.projectDataPath + "\\FixedLink.txt";
//        read_target_id_from_file(fixLinkFile);

        List<String> target_classname_list = read_target_classname_from_file(fixLinkFile);
//        read_target_file();
        read_code_corpus_from_file(target_classname_list);

        System.out.println(target_source_code.toString());
    }

    public BugReport(String bugReportFile, int bugId, String projectName) {
        read_BR_from_file(bugReportFile);
        this.bugId = bugId;
        this.projectName = projectName;

        String projectDataPath = keywordProperty.dict.get(projectName);
        String fixLinkFile = projectDataPath + "\\FixedLink.txt";
//        read_target_id_from_file(fixLinkFile);

        List<String> target_classname_list = read_target_classname_from_file(fixLinkFile);
//        read_target_file();
        read_code_corpus_from_file(target_classname_list);

        System.out.println(target_source_code.toString());
    }

    public BugReport(String bugReportFile, int bugId, String projectName, Dictionary<String, String> dict) {
        read_BR_from_file(bugReportFile);
        this.bugId = bugId;
        this.projectName = projectName;
        this.dictionary = dict;

        String projectDataPath = dict.get(projectName);
        String fixLinkFile = projectDataPath + "\\FixedLink.txt";
//        read_target_id_from_file(fixLinkFile);

        List<String> target_classname_list = read_target_classname_from_file(fixLinkFile);
//        read_target_file();
        read_code_corpus_from_file(target_classname_list);

//        System.out.println(target_source_code.toString());
    }

    private void read_BR(String filename) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));

            String str;
            while ((str = in.readLine()) != null) {
                str = str.strip();
                tokens.add(str);
            }

//            System.out.println(tokens);
            token_num = tokens.size();
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 从txt文件中读取bug报告的文本内容
     * @param filename
     */
    private void read_BR_from_file(String filename) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));

            String str;
            String[] token_list;
            while ((str = in.readLine()) != null) {
                if (str.startsWith("*")) {
                    continue;
                }
                token_list = str.split(" ");
                tokens.addAll(Arrays.asList(token_list));
            }

            in.close();

            token_num = tokens.size();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 从txt文件中读取目标源代码文件的序号
     * @param filename
     */
    private void read_target_id_from_file(String filename) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));

            List<String> target_filename = new ArrayList<>();
            String str;
            while ((str = in.readLine()) != null) {
                String[] content = str.split("\t");
                int bug_id = Integer.parseInt(content[0]);
                if (bug_id == this.bugId) {
//                    code_filename的格式为“zookeeper-server/src/main/java/org/apache/zookeeper/ClientCnxn.java”
                    String code_filename = content[1];
                    content = code_filename.split("/");
                    code_filename = content[content.length - 1].split("\\.")[0];
                    target_filename.add(code_filename);
                }
            }

//            ClassName.txt文件中存放的是源代码文件序号和名称
            String classname_file = property.projectDataPath + "//BugLocator_" + this.bugId + "//ClassName.txt";
            in = new BufferedReader(new FileReader(classname_file));
            while ((str = in.readLine()) != null) {
                String[] content = str.split("\t");
                int code_id = Integer.parseInt(content[0]);
//                code_filename的格式为"org.apache.zookeeper.jmx.CommonNames.java"
                String code_filename = content[1];
                content = code_filename.split("\\.");
                code_filename = content[content.length - 2];
                if (target_filename.contains(code_filename)) {
                    this.target_source_code.add(code_id);
                }
            }

            in.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> read_source_filename(String filename) {
        List<String> filenameList = new ArrayList<>();

        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));

            String line;
            while ((line = in.readLine()) != null) {
                line = line.replaceAll(Matcher.quoteReplacement("\\"), "/");
                filenameList.add(line);
            }
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return filenameList;
    }

    /**
     * 从文件中读取bug报告对应的目标源文件名
     * @param filename
     * @return
     */
    private List<String> read_target_classname_from_file(String filename) {

        List<String> classnameList = new ArrayList<>();

        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));

            String lineContent;

//            System.out.println("target code file name:");

            while ((lineContent = in.readLine()) != null) {
                String[] content = lineContent.split("\t");

                int bugId = Integer.parseInt(content[0]);
                if (bugId == this.bugId) {
//                    classname的格式为：data/org.eclipse.birt.data/src/org/eclipse/birt/data/engine/impl/PreparedQuery.java
                    String classname = content[1];
                    classnameList.add(classname);

//                    System.out.println(classname);
                }
            }

//            System.out.println("==================================");
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return classnameList;
    }

    private void read_code(List<String> target_classname_list) {
        String code_corpus_file_path = property.projectDataPath + "/ProcessedCode/" + property.project + ".txt";

        try {
            BufferedReader in = new BufferedReader(new FileReader(code_corpus_file_path));

            String lineContent;
            while ((lineContent = in.readLine()) != null) {
                String[] content = lineContent.split("\t");
//                System.out.println(content.length);
                if (content.length <= 2)
                    continue;

                int codeId = Integer.parseInt(content[0].replaceAll("[^0-9]", ""));
                String classname = content[1].replaceAll(Matcher.quoteReplacement("\\"), "/");
                String codeContent = content[2];

//                System.out.println(codeId);
//                System.out.println(classname);
//                System.out.println(codeContent);

                if (target_classname_list.contains(classname)) {
                    target_source_code.add(codeId);
                }

                SourceCode sourceCode = new SourceCode(codeId, classname, codeContent);
                source_code_list.add(sourceCode);
            }

            in.close();

            this.source_file_num = source_code_list.size();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void read_code_corpus_from_file(List<String> target_classname_list) {
        String code_corpus_file_path;
        String projectDataPath = "";
        if (property != null)
            code_corpus_file_path = property.projectDataPath + "//BugLocator_" + this.bugId + "//CodeCorpus.txt";
        else if (keywordProperty != null){
            projectDataPath = keywordProperty.dict.get(projectName);
            code_corpus_file_path = projectDataPath + "//BugLocator_" + this.bugId + "//CodeCorpus.txt";
        } else {
            projectDataPath = dictionary.get(projectName);
            code_corpus_file_path = projectDataPath + "//BugLocator_" + this.bugId + "//CodeCorpus.txt";
        }

        try {
            BufferedReader in = new BufferedReader(new FileReader(code_corpus_file_path));

            String lineContent;
            while ((lineContent = in.readLine()) != null) {
//                包含三个部分：类名标号，完整类名，code tokens
                String[] content = lineContent.split("\t");
                int codeId = Integer.parseInt(content[0].replaceAll("[^0-9]", ""));
//                classname的格式为：bundles\org.eclipse.core.commands\src\org\eclipse\core\commands\AbstractHandler.java
                String className = content[1].replaceAll(Matcher.quoteReplacement("\\"), "/");



                className = className.replaceAll("/main/java", "");
                className = className.replaceAll("/test/java", "");
//                System.out.println(className);
                String codeContent = content[2];

//                有些source code文件内容为空，忽略该文件
                if (codeContent == null) {
                    continue;
                }

                if (target_classname_list.contains(className)) {
                    target_source_code.add(codeId);
                }

                SourceCode sourceCode = new SourceCode(codeId, className, codeContent);
                source_code_list.add(sourceCode);
            }

            in.close();

            this.source_file_num = source_code_list.size();

//            System.out.println(target_source_code.size());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 读取source code文件信息
     * 针对每个code文件都单独存放的情况
     */
    private void read_target_file() {
        String source_corpus_dir = property.projectDataPath + "//BugLocator_" + this.bugId + "//SourceCorpus";
        File source_corpus = new File(source_corpus_dir);
        File[] source_files = source_corpus.listFiles();

        assert source_files != null;
        for (File source_file: source_files) {
            String filename = source_file.getName().split("\\.")[0].replaceAll("[^0-9]", "");

//            文件夹中有一个totalTokensNum.txt文件，因此需要有判空操作
            if (!filename.equals("")) {
                int codeId = Integer.parseInt(filename);
                SourceCode sourceCode = new SourceCode(codeId, source_file.getAbsolutePath());
                source_code_list.add(sourceCode);
            }
        }

        this.source_file_num = source_code_list.size();
    }

    /**
     * 将Solution中选中的keyword输出到文件中
     * @param solution
     * @throws IOException
     */
    public String outputKeywords(BinarySolution solution) throws IOException {

//        清除原有的数据
        int keyword_num = 0;
        best_keywords.clear();

        StringBuilder stringBuilder = new StringBuilder();

        BitSet bitSet = solution.variables().get(0);

//        File bestKeywords = new File(dir + "//best_keywords.txt");
//
//        FileWriter fileWriter = new FileWriter(bestKeywords);
//        PrintWriter printWriter = new PrintWriter(fileWriter, true);
//
//        printWriter.println("***** inverse average precision is " + solution.objectives()[0] + " *****");

//        添加关键词写到文件中, 对关键词进行去重操作
        for (int index = 0; index < bitSet.length(); index++) {
            if (bitSet.get(index)) {
                keyword_num++;
                String keyword = tokens.get(index);
                stringBuilder.append(keyword).append(" ");
                best_keywords.add(keyword);
            }
        }

        best_keywords_num = keyword_num;

        return stringBuilder.toString().trim();
    }

    /**
     * 将同样能得到最佳effectiveness的keywords写入文件中
     * @param solution
     * @param dir
     * @throws IOException
     */
    public String addOutputKeywords(BinarySolution solution, String dir) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();

        BitSet bitSet = solution.variables().get(0);

        File bestKeywords = new File(dir + "//best_keywords.txt");

        FileWriter fileWriter = new FileWriter(bestKeywords, true);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

//        先输入一行分割行
        printWriter.println("***** another best keywords *****");

        for (int index = 0; index < bitSet.length(); index++) {
            if (bitSet.get(index)) {
                String keyword = tokens.get(index);
                stringBuilder.append(keyword).append(" ");
                printWriter.println(keyword);
            }
        }

        printWriter.close();
        fileWriter.close();

        System.out.println("Another best keywords have been appended to file best_keywords.txt");

        return stringBuilder.toString().trim();
    }

    /**
     * 比较当前Solution的keywords数量与最佳keywords数量，数量相同时，比较是否选取的关键词也相同
     * @param solution
     * @return
     */
    public int compareKeywords (BinarySolution solution) {
        int keyword_num = 0;

        BitSet bitSet = solution.variables().get(0);
        boolean same = true;

        for (int index = 0; index < bitSet.length(); index++) {
            if (bitSet.get(index)) {
                keyword_num++;
                if (same && !best_keywords.contains(tokens.get(index))) {
                    same = false;
                }
            }
        }

//        数量相同且包含的关键词不完全相同，则返回0
        if (keyword_num == best_keywords_num && !same) {
            return 0;
        } else if (keyword_num < best_keywords_num) {
            return 1;
        } else {
            return -1;
        }
    }

    public int getToken_num() {
        return token_num;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public String getTokensString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String token: tokens) {
            stringBuilder.append(token).append(" ");
        }

        String tokensString = stringBuilder.toString().trim();
        return tokensString;
    }

    public int getBugId() {
        return bugId;
    }

    public List<Integer> getTarget_source_code() {
        return target_source_code;
    }

    public List<SourceCode> getSource_code_list() {
        return source_code_list;
    }

    public int getSource_file_num() {
        return source_file_num;
    }

    public int getInitialEffectiveness() {
        return initialEffectiveness;
    }

    public void setInitialEffectiveness(int initialEffectiveness) {
        this.initialEffectiveness = initialEffectiveness;
    }
}

/**
 * org.aspectj.ajdt.core/src/org/aspectj/ajdt/internal/compiler/lookup/EclipseFactory.java
 * org.aspectj.ajdt.core/src/main/java/org/aspectj/ajdt/internal/compiler/lookup/EclipseFactory.java
 *
 * tests/src/org/aspectj/systemtest/ajc150/Ajc150Tests.java
 * tests/src/test/java/org/aspectj/systemtest/ajc150/Ajc150Tests.java
 *
 *
 */

package org.query.search_engine;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.query.entity.BugReport;
import org.query.entity.SourceCode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CAI
 * @date 2023/4/14
 **/
public class LuceneSearch {

//    索引存储路径
    private String indexPath;
//    源代码文件总数
    private int fileNum;
//    目标文件的序号
    private List<Integer> targetCodeIdList;

    /**
     * 构造函数
     * @param bugReport
     * @throws IOException
     */
    public LuceneSearch(BugReport bugReport, boolean needIndex, String indexPrefix) throws Exception {
        fileNum = bugReport.getSource_file_num();
//        System.out.println("total document num:" + fileNum);

        targetCodeIdList = bugReport.getTarget_source_code();
//        System.out.println(targetCodeIdList.toString());

        List<SourceCode> sourceCodeList = bugReport.getSource_code_list();

        indexPath = indexPrefix + "/" + bugReport.getBugId();
        if (needIndex)
            createIndex(sourceCodeList);
    }

    /**
     * 原始查询，也即baseline查询，将查询结果和effectiveness分别写入文件中
     * @param initialQuery
     * @param resultDir
     * @throws Exception
     */
    public int initialSearch(String initialQuery, String resultDir) throws Exception {
        List<String[]> initialRankList = searchCode(initialQuery);

//        写rank list文件
        File initialResult = new File(resultDir + "/initial_rank_list.txt");

        FileWriter fileWriter = new FileWriter(initialResult);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

        for (int index = 0; index < initialRankList.size(); index++) {
            String[] entry = initialRankList.get(index);
            int rank = index + 1;
            printWriter.println(rank + "\t" + entry[0] + "\t" + entry[1]);
        }

        printWriter.close();
        fileWriter.close();

//        写baseline query 指标文件
        int initialEffectiveness = computeEffectiveness(initialRankList);
        float initialInverseAveragePrecision = computeInverseAveragePrecision(initialRankList);

        File initialMetric = new File(resultDir + "/initial_metric.txt");

        FileWriter writer = new FileWriter(initialMetric);
        PrintWriter printer = new PrintWriter(writer, true);

        printer.println(initialEffectiveness);
        printer.println(initialInverseAveragePrecision);

        printer.close();
        writer.close();

        System.out.println("initial query effectiveness: " + initialEffectiveness);
        System.out.println("initial query inverse average precision: " + initialInverseAveragePrecision);
//        JMetalLogger.logger.info("initial query effectiveness: " + initialEffectiveness);
//        JMetalLogger.logger.info("initial query inverse average precision: " + initialInverseAveragePrecision);

        return initialEffectiveness;

    }

    /**
     * 创建索引
     * @param sourceCodeList
     * @throws IOException
     */
    private void createIndex(List<SourceCode> sourceCodeList) throws IOException {
//        先判断文件夹是否存在，不存在则创建
        File folder = new File(indexPath);
        if (folder.exists()) {
            boolean success = folder.delete();
            if (success) {
                System.out.println("文件夹" + indexPath + "删除成功");
            }
            else {
                System.out.println("文件夹" + indexPath + "删除失败");
            }
        }
        if (!folder.exists() && !folder.isDirectory()) {
            boolean success = folder.mkdirs();
            if (success) {
                System.out.println("文件夹" + indexPath + "创建成功");
            }
            else {
                System.out.println("文件夹" + indexPath + "创建失败");
            }
        }
//        创建Director对象，保存到磁盘中
        Directory directory = FSDirectory.open(folder.toPath());

//        创建IndexWriter对象
        IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig());

        for (SourceCode sourceCode: sourceCodeList) {
//            创建Document对象
            Document document = new Document();

            document.add(new TextField("codeId", sourceCode.getCode_id() + "", Field.Store.YES));
            document.add(new TextField("codeContent", sourceCode.getTokensString(), Field.Store.YES));

//            将文档对象写入索引库
            indexWriter.addDocument(document);
        }

//        释放资源
        indexWriter.close();
    }

    /**
     * 检索文档
     * @param keywords
     * @return
     * @throws Exception
     */
    public List<String[]> searchCode(String keywords) throws Exception {
//        创建索引库目录位置对象，指定索引位置
        Directory directory = FSDirectory.open(new File(indexPath).toPath());
//        创建索引读取对象，用于读取索引
        IndexReader indexReader = DirectoryReader.open(directory);
//        创建索引搜索对象，用于执行索引
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
//        创建分析器对象
        Analyzer analyzer = new StandardAnalyzer();
//        创建查询解析器对象
        QueryParser queryParser = new QueryParser("codeContent", analyzer);

        if (keywords.equals(""))
            return null;

        boolean retry = true;
        Query query;
        TopDocs topDocs = null;

        while (retry) {
            try {
                retry = false;
//                  使用查询解析器，实例化Query对象
                query = queryParser.parse(keywords);

//                  执行搜索，返回搜索结果集
                topDocs = indexSearcher.search(query, fileNum);
            } catch (ParseException parseException) {
                System.out.println("catch too many clauses exception!");
                BooleanQuery.setMaxClauseCount(BooleanQuery.getMaxClauseCount() * 2);
                System.out.println("current max clauses count: " + BooleanQuery.getMaxClauseCount());
                retry = true;
            }
        }

        List<String[]> rankList = new ArrayList<>();

        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            String[] searched_doc = new String[2];
//            文档的搜索得分
            String score = String.valueOf(scoreDoc.score);
//            根据文档id，查询文档信息
            Document document = indexSearcher.doc(scoreDoc.doc);
            String codeId = document.get("codeId");

            searched_doc[0] = codeId;
            searched_doc[1] = score;

            rankList.add(searched_doc);
        }

        return rankList;
    }

    public float[] computeAllMetrics(List<String[]> rankList) {
        if (rankList == null) {
            return new float[]{-1, -1.0f};
        }
        int rank = rankList.size();

        boolean firstHit = false;
        int hitCounts = 0;
        float precision = 0.0f;

        for (int index = 0; index < rankList.size(); index++) {
            int codeId = Integer.parseInt(rankList.get(index)[0]);
            if (targetCodeIdList.contains(codeId) && !firstHit) {
//                effectiveness
                rank = index + 1;
                firstHit = true;
                hitCounts ++;
                precision += ((float) hitCounts / (index + 1));
            } else if (targetCodeIdList.contains(codeId)) {
                hitCounts ++;
                precision += ((float) hitCounts / (index + 1));
            }
        }

        if (precision == 0.0f)
            return new float[]{-1, -1.0f};
        return new float[]{rank, precision / (float) targetCodeIdList.size()};
    }

    public float[] computeAll(List<String[]> rankList) {
        if (rankList == null) {
            return new float[] {-1, 0.0f, 0.0f};
        }
        int rank = rankList.size();

        boolean firstHit = false;
        int hitCounts = 0;
        float precision = 0.0f;
        float rr = 0.0f;

        for (int index = 0; index < rankList.size(); index++) {
            int codeId = Integer.parseInt(rankList.get(index)[0]);
            if (targetCodeIdList.contains(codeId) && !firstHit) {
                rank = index + 1;
                firstHit = true;
                hitCounts ++;
                precision += ((float) hitCounts / (index + 1));
                rr = 1.0f / (float) rank;
            } else if (targetCodeIdList.contains(codeId)) {
                hitCounts ++;
                precision += ((float) hitCounts / (index + 1));
            }
        }

        if (precision == 0.0f)
            return new float[]{-1, 0.0f, 0.0f};
        return new float[]{rank, precision / (float) targetCodeIdList.size(), rr};
    }

    /**
     * 计算effectiveness指标
     * @param keywords
     * @return
     * @throws Exception
     */
    public int computeEffectiveness(String keywords) throws Exception {
        int rank = fileNum;

//        找到第一个命中的文件排名
        List<String[]> rankList = searchCode(keywords);
        if (rankList == null) {
//            由于fitness函数考虑的keyword数量，所以在检索不到目标文件的情况下，可能会出现0个keyword的情况，因此需要将这种情况下的effectiveness扩大
            return fileNum;
        }
        for (int index = 0; index < rankList.size(); index++) {
            int codeId = Integer.parseInt(rankList.get(index)[0]);
            if (targetCodeIdList.contains(codeId)) {
                rank = index + 1;
                break;
            }
        }

        return rank;
    }

    private int computeEffectiveness(List<String[]> rankList) {
        int rank = fileNum;
        if (rankList == null) {
            return rank;
        }

        for (int index = 0; index < rankList.size(); index++) {
            int codeId = Integer.parseInt(rankList.get(index)[0]);
            if (targetCodeIdList.contains(codeId)) {
                rank = index + 1;
                break;
            }
        }

        return rank;
    }

    /**
     * 计算inverse average precision指标
     * @param keywords
     * @return
     * @throws Exception
     */
    public float[] computeInverseAveragePrecision(String keywords) throws Exception {
        List<String[]> rankList = searchCode(keywords);
//        无法找到相关的文档
        if (rankList == null) {
            return new float[]{0.0f, 0.0f, (float) (fileNum), Integer.MAX_VALUE};
        }

        int effectiveness = computeEffectiveness(rankList);

//        计算每个命中位置的precision
        float precision = 0.0f;
        int hitCounts = 0;
        for (int index = 0; index < rankList.size(); index++) {
            int codeId = Integer.parseInt(rankList.get(index)[0]);
            if (targetCodeIdList.contains(codeId)) {
                hitCounts++;
                precision += ((float) hitCounts / (index + 1));
            }
        }
//        System.out.println("hit counts: " + hitCounts);
//        System.out.println("precision: " + precision);

//        返回inverse average precision
        if (precision != 0.0)
            return new float[]{(float)hitCounts, precision, (float) targetCodeIdList.size() / precision, effectiveness};
        return new float[]{0.0f, 0.0f, (float) (fileNum), Integer.MAX_VALUE};
    }

    private float computeInverseAveragePrecision(List<String[]> rankList) {
        if (rankList == null) {
            return (float) (fileNum);
        }
//        计算每个命中位置的precision
        float precision = 0.0f;
        int hitCounts = 0;

        System.out.println(targetCodeIdList.toString());
        System.out.println("rank list size: " + rankList.size());

        for (int index = 0; index < rankList.size(); index++) {
            int codeId = Integer.parseInt(rankList.get(index)[0]);
//            System.out.println("rank list No." + index + ": " + codeId);
            if (targetCodeIdList.contains(codeId)) {
                hitCounts++;
                precision += ((float) hitCounts / (index + 1));
            }
        }

        System.out.println("precision: " + precision);

//        返回inverse average precision
        if (precision > 0.0f)
            return (float) targetCodeIdList.size() / precision;
        return (float) (fileNum);
    }

    /**
     * 输出最佳effectiveness对应的rank list
     * @param keywords
     * @param dir
     * @throws Exception
     */
    public int outputRankList(String keywords, String dir) throws Exception {
        List<String[]> rankList = searchCode(keywords);

        int effectiveness = computeEffectiveness(rankList);
        float inverseAveragePrecision = computeInverseAveragePrecision(rankList);

        System.out.println("inverse average precision: " + inverseAveragePrecision);

        File bestRankList = new File(dir + "//best_rank_list.txt");

        FileWriter fileWriter = new FileWriter(bestRankList);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

        if (rankList == null) {
            return fileNum;
        }

        for (int index = 0; index < rankList.size(); index++) {
            String[] entry = rankList.get(index);
            int rank = index + 1;
            printWriter.println(rank + "\t" + entry[0] + "\t" + entry[1]);
        }

        printWriter.close();
        fileWriter.close();

        System.out.println("Rank list has been written to file best_rank_list.txt");

        return effectiveness;
    }

    public void addOutputRankList(String keywords, String dir) throws Exception {
        List<String[]> rankList = searchCode(keywords);

        File bestRankList = new File(dir + "//best_rank_list.txt");

        FileWriter fileWriter = new FileWriter(bestRankList, true);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

        printWriter.println("***** another best rank list *****");

        for (int index = 0; index < rankList.size(); index++) {
            String[] entry = rankList.get(index);
            int rank = index + 1;
            printWriter.println(rank + "\t" + entry[0] + "\t" + entry[1]);
        }

        printWriter.close();
        fileWriter.close();

        System.out.println("Rank list has been written to file best_rank_list.txt");
    }

    public int getFirstHitId(List<String[]> rankList) {
        if (rankList == null) {
            return -1;
        }

        for (String[] strings : rankList) {
            int codeId = Integer.parseInt(strings[0]);
            if (targetCodeIdList.contains(codeId)) {
                return codeId;
            }
        }

        return -1;
    }
}

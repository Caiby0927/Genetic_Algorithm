package org.query.genetic_algorithm;

import com.csvreader.CsvWriter;
import org.query.entity.BugReport;
import org.query.genetic_algorithm.operator.selection.RouletteWheelSelection;
import org.query.genetic_algorithm.problem.BugLocalizationProblem;
import org.query.property.Property;
import org.query.search_engine.LuceneSearch;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.algorithm.singleobjective.geneticalgorithm.GeneticAlgorithmBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SinglePointCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.BitFlipMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 使用单目标遗传算法提取bug报告的关键词（单线程）
 * @author CAI
 * @date 2023/4/13
 **/
public class SingleObjectiveGASingleThread {

    private static final Property property = Property.getInstance();

//    private float bestInverseAveragePrecision = (float) Integer.MAX_VALUE;

    private void run_GA(BugReport bugReport, CsvWriter writer) throws Exception {

//        更新bestEffectiveness的值，否则会因为前一个bug报告的GA结果太好，导致后一个bug报告的结果未写入文件
//        bestInverseAveragePrecision = (float) Integer.MAX_VALUE;

        String filepath = property.GAResultPath + "\\BugReport_" + bugReport.getBugId();

//        需要先检查文件夹是否存在
        File folder = new File(filepath);
        if (!folder.exists() && !folder.isDirectory()) {
            boolean success = folder.mkdirs();
            if (success) {
                System.out.println("文件夹"+filepath+"创建成功");
            }
            else {
                System.out.println("文件夹"+filepath+"创建失败");
            }
        }

        BugLocalizationProblem problem;

        LuceneSearch luceneSearch = new LuceneSearch(bugReport, false, property.luceneIndexPath);

//        进行初始query检索，若结果在10以内，则不执行GA算法
        String initialQuery = bugReport.getTokensString();
        int initialEffectiveness = luceneSearch.initialSearch(initialQuery, filepath);
        System.out.println("initialEffectiveness: " + initialEffectiveness);
        bugReport.setInitialEffectiveness(initialEffectiveness);

//        过滤掉effectiveness不适合的bug报告
//        if (initialEffectiveness <= 10) {
//            return;
//        }

        problem = new BugLocalizationProblem(bugReport, luceneSearch);

        Algorithm<BinarySolution> algorithm;
        CrossoverOperator<BinarySolution> crossover;
        MutationOperator<BinarySolution> mutation;
        SelectionOperator<List<BinarySolution>, BinarySolution> selection;

        crossover = new SinglePointCrossover<>(0.9);

        double mutationProbability = 1.0 / problem.number_of_bits;
        mutation = new BitFlipMutation<>(mutationProbability);

        selection = new RouletteWheelSelection<>();

        algorithm = new GeneticAlgorithmBuilder<>(problem, crossover, mutation)
                .setPopulationSize(500)
                .setMaxEvaluations(30000)
                .setSelectionOperator(selection)
                .build();

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();


        BinarySolution solution = algorithm.getResult();
//        float inverseAveragePrecision = (float) solution.objectives()[0];
//        System.out.println("GA inverse average precision: " + inverseAveragePrecision);
        int effectiveness = (int) solution.objectives()[0];
        System.out.println("GA effectiveness: " + effectiveness);

        String bugId = String.valueOf(bugReport.getBugId());
//        String averagePrecision = String.valueOf(1.0 / inverseAveragePrecision);

//            当effectiveness小于当前最小的effectiveness时，则直接替换；当effectiveness等于当前最小的effectiveness时，则对比他们的关键词数量
//        if (inverseAveragePrecision < bestInverseAveragePrecision) {
//            bestInverseAveragePrecision = inverseAveragePrecision;
//        输出关键词文件
        String keywords = bugReport.outputKeywords(solution);
////        输出rank list文件
//        int effectiveness = luceneSearch.outputRankList(keywords, filepath);
//        System.out.println("GA effectiveness: " + effectiveness);

        String bugReportTokens = bugReport.getTokensString();

//        结果添加到列表中
        String[] result = new String[]{bugId, String.valueOf(effectiveness), keywords, bugReportTokens};
//        String[] result = new String[]{bugId, String.valueOf(effectiveness), averagePrecision, keywords, bugReportTokens};
        writer.writeRecord(result);
        writer.flush();
//        }
//        else if (inverseAveragePrecision == bestInverseAveragePrecision) {
////                比较关键词数量
//            int comRes = bugReport.compareKeywords(solution);
//            if (comRes == 1) {
////                    当前的关键词数量更少
//                String keywords = bugReport.outputKeywords(solution, filepath);
//                luceneSearch.outputRankList(keywords, filepath);
//            } else if (comRes == 0) {
////                    关键词数量相同且关键词不相同
//                String keywords = bugReport.addOutputKeywords(solution, filepath);
//                luceneSearch.addOutputRankList(keywords, filepath);
//            }
//        }

//        List<BinarySolution> population = new ArrayList<>(1);
//        population.add(solution);

//        long computingTime = algorithmRunner.getComputingTime();

//        new SolutionListOutput(population)
//                .setVarFileOutputContext(new DefaultFileOutputContext(filepath + "\\VAR_" + count + ".tsv"))
//                .setFunFileOutputContext(new DefaultFileOutputContext(filepath + "\\FUN_" + count + ".tsv"))
//                .print();

//        JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
//
//        JMetalLogger.logger.info("Fitness: " + solution.objectives()[0]) ;
//        JMetalLogger.logger.info("Solution: " + solution.variables().get(0)) ;
//        JMetalLogger.logger.info("=======================================================");

    }

    private void run_initial_search(BugReport bugReport, String resultDir, String indexPath) throws Exception {
        LuceneSearch luceneSearch = new LuceneSearch(bugReport, false, indexPath);

        String filepath = resultDir + "\\BugReport_" + bugReport.getBugId();

//        进行初始query检索，若结果在10以内，则不执行GA算法
        String initialQuery = bugReport.getTokensString();
        luceneSearch.initialSearch(initialQuery, filepath);
    }

    private void simply_run_GA(CsvWriter writer) throws Exception {

//        进入bug报告语料库的文件夹，遍历其中的文件
//        File project_folder = new File(property.projectDataPath + "\\BugCorpus");
//        File[] files = project_folder.listFiles();
//
//        assert files != null;
//        for (File file: files) {
//            if (file.isFile()) {
////                原始的id里面可能包含了一些不可见的非数字字符，因此用正则表达式先删去
//                String idStr = file.getName().split("\\.")[0];
//                idStr = idStr.replaceAll("[^0-9]", "");
//                int bugId = Integer.parseInt(idStr);
//
////                跑一半停下来，用于过滤那些已经执行过的bug报告
////                int[] already = new int[]{3156, 3144, 3131, 3127, 3125, 3117, 3113, 3104, 3082, 3072, 3059};
////                List<Integer> alreadyList = Arrays.stream(already).boxed().collect(Collectors.toList());
////                if (alreadyList.contains(bugId)) {
////                    continue;
////                }
////                System.out.println(bugId);
//                BugReport bugReport = new BugReport(file.getAbsolutePath(), bugId, property.projectDataPath);
////                System.out.println(bugReport.getTarget_source_code().toString());
//
//                run_GA(bugReport, resultDir, property.projectDataPath, writer, property.luceneIndexPath);
//
////                ga.run_initial_search(bugReport);
//
//                break;
//
//            }
//        }

        BufferedReader in = new BufferedReader(new FileReader(property.projectDataPath + "\\SortedId.txt"));

        try {

            String lineContent;
            boolean meetStopId = false;

            while ((lineContent = in.readLine()) != null) {
                String idStr = lineContent.split("\t")[0];
                idStr = idStr.replaceAll("[^0-9]", "");

                if (property.stopBugId.equals("")) {

                    int bugId = Integer.parseInt(idStr);
                    System.out.println(bugId);

                    File bugFile = new File(property.projectDataPath + "\\BugCorpus\\" + idStr + ".txt");
                    if (!bugFile.exists())
                        continue;

//                匹配不到
                    BugReport bugReport = new BugReport(bugFile.getAbsolutePath(), bugId);
                    if (bugReport.getTarget_source_code().size() == 0) {
                        continue;
                    }

                    run_GA(bugReport, writer);
                }

                else {
                    if (idStr.equals(property.stopBugId)) {
//                    下一个bug报告才是需要处理的
                        meetStopId = true;
                        continue;
                    }
                    if (meetStopId) {

                        int bugId = Integer.parseInt(idStr);

                        File bugFile = new File(property.projectDataPath + "\\BugCorpus\\" + idStr + ".txt");
                        if (!bugFile.exists())
                            continue;

//                匹配不到
                        BugReport bugReport = new BugReport(bugFile.getAbsolutePath(), bugId);
                        if (bugReport.getTarget_source_code().size() == 0) {
                            continue;
                        }

                        run_GA(bugReport, writer);
                    }
                }

//                break;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            in.close();
        }
    }

    private void run_GA_upon_best_keywords(CsvWriter writer) throws Exception {
        String initial_res_dir = "F:\\query-reformulation\\pre_experiment\\zookeeper\\GA_result";
        File res_dir = new File(initial_res_dir);
        File[] result_dirs = res_dir.listFiles();

        for (File result_dir: result_dirs) {
            String dir_name = result_dir.getName();
            if (result_dir.isDirectory()) {
                File keyword_file = new File(result_dir.getAbsolutePath() + "//best_keywords.txt");
                if (keyword_file.exists()) {
                    String bugIdStr = dir_name.split("_")[1].replaceAll("[^0-9]", "");
                    int bugId = Integer.parseInt(bugIdStr);

//                    跑一半停下来，用于过滤那些已经执行过的bug报告
//                    int[] already = new int[]{3156, 3144, 3131, 3127, 3125, 3117, 3113, 3104, 3082, 3072, 3059, 3051, 3041, 3001, 2988, 2982};
//                    List<Integer> alreadyList = Arrays.stream(already).boxed().collect(Collectors.toList());
//                    if (alreadyList.contains(bugId)) {
//                        continue;
//                    }

                    BugReport bugReport = new BugReport(keyword_file.getAbsolutePath(), bugId);

                    run_GA(bugReport, writer);

//                    break;
                }

            }
        }
    }

    public void run_GA_process() throws Exception {
        SingleObjectiveGASingleThread ga = new SingleObjectiveGASingleThread();

        File resultFile = new File(property.GAResultFilePath);
        if (!resultFile.exists()) {
            if (!resultFile.getParentFile().exists()) {
                if (resultFile.getParentFile().mkdirs()) {
                    System.out.println("successfully create parent directories " + resultFile.getParent());
                    if (resultFile.createNewFile()) {
                        System.out.println("successfully create file " + property.GAResultFilePath);
                    } else {
                        System.out.println("fail to create file " + property.GAResultFilePath);
                    }
                } else {
                    System.out.println("fail to create parent directories " + resultFile.getParent());
                }
            }
        }

        FileWriter fileWriter = new FileWriter(property.GAResultFilePath, true);

        CsvWriter writer = new CsvWriter(fileWriter, ',');

        try {
            if (property.stopBugId.equals("")) {
//                stopBugId不为空的情况下，不需要再写一次header
                String[] header = new String[]{"bugId", "effectiveness", "keywords", "tokens"};
//                String[] header = new String[]{"bugId", "effectiveness", "averagePrecision", "keywords", "tokens"};

                writer.writeRecord(header);
            }

            ga.simply_run_GA(writer);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            writer.close();
        }
    }
}

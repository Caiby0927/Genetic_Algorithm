package org.query.genetic_algorithm;

import com.csvreader.CsvWriter;
import org.checkerframework.checker.units.qual.C;
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
import java.util.List;

/**
 * @author CAI
 * @date 2023/7/10
 **/
public class GA {

    private static final Property property = Property.getInstance();

    private void run_GA(BugReport bugReport, CsvWriter writer) throws Exception {
        String filepath = property.GAResultPath + "/" + bugReport.getBugId();

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

        LuceneSearch luceneSearch = new LuceneSearch(bugReport, true, property.luceneIndexPath);

        String initialQuery = bugReport.getTokensString();
        int initialEffectiveness = luceneSearch.initialSearch(initialQuery, filepath);
        bugReport.setInitialEffectiveness(initialEffectiveness);

        problem = new BugLocalizationProblem(bugReport, luceneSearch);

        Algorithm<BinarySolution> algorithm;
        CrossoverOperator<BinarySolution> crossover;
        MutationOperator<BinarySolution> mutation;
        SelectionOperator<List<BinarySolution>, BinarySolution> selection;

        crossover = new SinglePointCrossover<>(0.9);

        double mutationProbability = 1.0d / (double) problem.number_of_bits;
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
//        输出rank list文件
//        int effectiveness = luceneSearch.outputRankList(keywords, filepath);
//        System.out.println("GA effectiveness: " + effectiveness);

        String bugReportTokens = bugReport.getTokensString();

//        结果添加到列表中
        String[] result = new String[]{bugId, String.valueOf(effectiveness), keywords, bugReportTokens};
//        String[] result = new String[]{bugId, String.valueOf(effectiveness), averagePrecision, keywords, bugReportTokens};
        writer.writeRecord(result);
    }

    private void prepare(CsvWriter writer) {
        try {
//            BufferedReader in = new BufferedReader(new FileReader(property.projectDataPath + "\\SortedId.txt"));
            BufferedReader in = new BufferedReader(new FileReader(property.projectDataPath + "/BugId/" + property.project + ".txt"));

            String lineContent;
            boolean meetStopId = false;

            while ((lineContent = in.readLine()) != null) {
                String idStr = lineContent.replaceAll("[^0-9]", "");

                if (property.stopBugId.equals("")) {
//                    File bugFile = new File(property.projectDataPath + "\\BugCorpus\\" + idStr + ".txt");
                    File bugFile = new File(property.projectDataPath + "/ProcessedBugReports/" + property.project + "/" + idStr + ".txt");
    
                    BugReport bugReport = new BugReport(new String[]{bugFile.getAbsolutePath(), idStr});
                    if (bugReport.getTarget_source_code().size() == 0) {
                        continue;
                    }
    
                    run_GA(bugReport, writer);
                }

                else {
                    if (idStr.equals(property.stopBugId)) {
                        meetStopId = true;
                    }
                    if (meetStopId) {
                        File bugFile = new File(property.projectDataPath + "/ProcessedBugReports/" + property.project + "/" + idStr + ".txt");
    
                        BugReport bugReport = new BugReport(new String[]{bugFile.getAbsolutePath(), idStr});
                        if (bugReport.getTarget_source_code().size() == 0) {
                            continue;
                        }
        
                        run_GA(bugReport, writer);
                    }
                }

            }

            in.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void run() throws Exception {
        GA ga = new GA();

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
                String[] header = new String[]{"bugId", "effectiveness", "keywords", "tokens"};
//                String[] header = new String[]{"bugId", "effectiveness", "averagePrecision", "keywords", "tokens"};

                writer.writeRecord(header);
            }

            ga.prepare(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            writer.close();
        }
    }
}

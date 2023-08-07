package org.query.genetic_algorithm.problem;

import org.query.search_engine.LuceneSearch;
import org.query.entity.BugReport;
import org.uma.jmetal.problem.binaryproblem.impl.AbstractBinaryProblem;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.solution.binarysolution.impl.DefaultBinarySolution;
import org.uma.jmetal.util.binarySet.BinarySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * @author CAI
 * @date 2023/4/13
 **/
public class BugLocalizationProblem extends AbstractBinaryProblem {

    public int number_of_bits;
    private List<String> text_list;
    private LuceneSearch luceneSearch;
    private int initialEffectiveness;

    private BinarySet bestBinarySet = null;

    public BugLocalizationProblem(BugReport bugReport, LuceneSearch luceneSearch) throws Exception {
        super();
        number_of_bits = bugReport.getToken_num();
        text_list = bugReport.getTokens();
        this.luceneSearch = luceneSearch;
        this.initialEffectiveness = bugReport.getInitialEffectiveness();
    }

    @Override
    public List<Integer> listOfBitsPerVariable() {
        return Arrays.asList(number_of_bits);
    }

    @Override
    public int numberOfVariables() {
        return 1;
    }

    @Override
    public int numberOfObjectives() {
        return 1;
    }

    @Override
    public int numberOfConstraints() {
        return 1;
    }

    @Override
    public BinarySolution createSolution() {
        return new DefaultBinarySolution(listOfBitsPerVariable(), numberOfObjectives());
    }

    @Override
    public int bitsFromVariable(int index) {
        return number_of_bits;
    }

    @Override
    public String name() {
        return "BugLocalizationProblem";
    }

    @Override
    public BinarySolution evaluate(BinarySolution binarySolution) {
        List<String> keyword_list = new ArrayList<>();

        BinarySet bitSet = binarySolution.variables().get(0);

//        获取提取出来的关键词
        for (int index = 0; index < bitSet.length(); index++) {
            if (bitSet.get(index)) {
                String chosen_keyword = text_list.get(index);
                keyword_list.add(chosen_keyword);
            }
        }

//        将关键词列表转化为字符串的形式
        StringBuilder stringBuilder = new StringBuilder();
        for (String keyword: keyword_list) {
            stringBuilder.append(keyword).append(" ");
        }
        String keywords = stringBuilder.toString().trim();

//        用提取出来的关键词进行Lucene检索

//        int effectiveness;
//        try {
//            effectiveness = luceneSearch.computeEffectiveness(keywords);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

        float inverseAveragePrecision;
        float effectiveness;
        try {
            float[] result = luceneSearch.computeInverseAveragePrecision(keywords);
            inverseAveragePrecision = result[2];
            effectiveness = result[3];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        binarySolution.objectives()[0] = inverseAveragePrecision;

//        使用effectiveness指标作为fitness函数
//        binarySolution.objectives()[0] = 1.0 * effectiveness;
        if (effectiveness != 0.0 && effectiveness != -1.0  && effectiveness <= initialEffectiveness) {
//            使用inverse average precision指标作为fitness函数
//            binarySolution.objectives()[0] = effectiveness;
            initialEffectiveness = (int) effectiveness;
            bestBinarySet = bitSet;
        }
        else {
            if (bestBinarySet != null) {
                binarySolution.variables().set(0, bestBinarySet);
            }
            else {
                BinarySet bitSet1 = new BinarySet(bitSet.length());
                for (int index = 0; index < bitSet1.length(); index++)
                    bitSet1.set(index, true);
//                    bitSet1.set(index, 1);
                binarySolution.variables().set(0, bitSet1);
            }

        }
//        binarySolution.objectives()[0] = effectiveness;
        binarySolution.objectives()[0] = 1.0 * initialEffectiveness;

        return binarySolution;
    }
}

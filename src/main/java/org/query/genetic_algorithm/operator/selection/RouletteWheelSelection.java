package org.query.genetic_algorithm.operator.selection;

import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;

import java.util.List;

/**
 * @author CAI
 * @date 2023/4/14
 **/
public class RouletteWheelSelection<S extends Solution<?>> implements SelectionOperator<List<S>, S> {

    @Override
    public S execute(List<S> solutionList) {
//        检查是否为null或为空列表
        Check.notNull(solutionList);
        Check.collectionIsNotEmpty(solutionList);

        double maximum = 0.0;
        for (S solution: solutionList) {
            maximum += solution.objectives()[0];
        }

//        生成随机数
        double rand = Math.random() * maximum;
        double value = 0.0;

        for (S solution: solutionList) {
            value += solution.objectives()[0];

            if (value > rand) {
                return solution;
            }
        }

        return null;
    }
}

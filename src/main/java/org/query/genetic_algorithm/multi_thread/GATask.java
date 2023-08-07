package org.query.genetic_algorithm.multi_thread;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.JMetalLogger;

import java.util.concurrent.Callable;

/**
 * 自定义执行GA算法的Callable
 * @author CAI
 * @date 2023/4/16
 **/
public class GATask implements Callable<Algorithm<BinarySolution>> {

    private final Algorithm<BinarySolution> algorithm;

    public GATask(Algorithm<BinarySolution> algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public Algorithm<BinarySolution> call() throws Exception {
        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

        long computingTime = algorithmRunner.getComputingTime();
        JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");

        return algorithm;
    }
}

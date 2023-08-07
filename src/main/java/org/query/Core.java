package org.query;

import org.query.genetic_algorithm.GA;
import org.query.genetic_algorithm.SingleObjectiveGASingleThread;
import org.query.util.DataProcess;
import org.query.util.EvalDataProcess;

/**
 * @author CAI
 * @date 2023/4/28
 **/
public class Core {
    public void process() {
        try {
            System.out.println("run genetic algorithm...");

            new SingleObjectiveGASingleThread().run_GA_process();
//            new GA().run();

        } catch (Exception e) {
            e.printStackTrace();
        }

//        try {
//            System.out.println("process dataset...");
//
//            new DataProcess().splitData();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        try {
//            System.out.println("use keywords to query");
//
//            new EvalDataProcess().evalData();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}

package org.query;

import org.query.property.Property;

/**
 * @author CAI
 * @date 2023/4/28
 **/
public class QueryReformulation {

    public static void main(String[] args) {
        try {
//            if (args.length == 0)
//                throw null;

//            String[] parameters = new String[] {"-b", "E:\\query-reformulation\\dataset\\AspectJ\\data"};
            String[] parameters = new String[] {"-b", "D:\\query-reformulation\\query-reformulation\\dataset\\Birt\\data"};
            boolean isLegal = parseArgs(parameters);

//            boolean isLegal = parseArgs(args);
            if (!isLegal)
                throw null;

            Core core = new Core();
            core.process();

        } catch (Exception e) {
            showHelp();
        }
    }

    /**
     * -b E:\\query-reformulation\\dataset\\AspectJ\\data
     * -i 12345
     * -s AspectJ
     */

    private static void showHelp() {
        String usage = "Usage: java -jar QueryReformulation [-options] \r\n"
                + "where options must include:\r\n"
                + "-b\tindicates the project path\r\n"
                + "-i\tindicates the bug id which was not processed last time\r\n"
                + "-s\tindicates the project name";
        System.out.println(usage);
    }

    private static boolean parseArgs(String[] args) {
        boolean isLegal = true;

        String experimentProjectPath = "F:\\query-reformulation\\dataset";
        String projectDataPath = "";
        String luceneIndexPath = "";
        String GAResultPath = "";
        String GAResultFilePath = "";
        String stopBugId = "";
        String project = "";

        int i = 0;
        while (i < args.length - 1) {
//            System.out.println(args[i]);
            if (args[i].equals("-b")) {
                i++;
                System.out.println(args[i]);
                projectDataPath = args[i];
//                System.out.println("project data path: " + projectDataPath);
            }
            if (args[i].equals("-i")) {
                i++;
                stopBugId = args[i];
                System.out.println("stop bug id: " + stopBugId);
            }
            if (args[i].equals("-s")) {
                i++;
                project = args[i];
                System.out.println("project: " + project);
            }
            i++;
        }

        if (projectDataPath.equals("")) {
            isLegal = false;
            System.out.println("you must indicate the project path");
        }

        luceneIndexPath = projectDataPath + "\\lucene_index";
        GAResultPath = projectDataPath + "\\GA_result";
        GAResultFilePath = GAResultPath + "\\GA_total_result_1.csv";

//        luceneIndexPath = projectDataPath + "/index/" + project + "/lucene_index";
//        GAResultPath = projectDataPath + "/GA/" + project + "/GA_result";
//        GAResultFilePath = GAResultPath + "/GA_total_result_high_quality.csv";

        Property.createInstance(experimentProjectPath, projectDataPath, luceneIndexPath, GAResultPath, GAResultFilePath, stopBugId, project);

        return isLegal;
    }
}

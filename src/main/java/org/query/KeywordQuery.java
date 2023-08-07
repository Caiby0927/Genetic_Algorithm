package org.query;

import org.query.property.KeywordProperty;

/**
 * @author CAI
 * @date 2023/5/14
 **/
public class KeywordQuery {

    public static void main(String[] args) {
        try {
            if (args.length == 0)
                throw null;

            boolean isLegal = parseArgs(args);
            if (!isLegal)
                throw null;

            Core core = new Core();
            core.process();
        } catch (Exception e) {
            showHelp();
        }
    }

    private static void showHelp() {
        String usage = "Usage: java -jar QueryReformulation [-options] \r\n"
                + "where options must include:\r\n"
                + "-b\tindicates the keyword file path\r\n";
        System.out.println(usage);
    }

    private static boolean parseArgs(String[] args) {
        boolean isLegal = true;

        String keywordFilepath = "";

        int i = 0;
        while (i < args.length - 1) {
            if (args[i].equals("-b")) {
                i++;
                keywordFilepath = args[i];
                System.out.println("keyword file path: " + keywordFilepath);
            }
            i++;
        }

        if (keywordFilepath.equals("")) {
            isLegal = false;
            System.out.println("please indicate the keyword file path");
        }

        if (isLegal)
            KeywordProperty.createInstance(keywordFilepath, false, false, false);

        return isLegal;
    }
}

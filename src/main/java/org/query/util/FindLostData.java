package org.query.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CAI
 * @date 2023/4/28
 **/
public class FindLostData {
    private final String dataDir = "F:\\query-reformulation\\dataset\\Eclipse_Platform_UI\\data";
//    private final String dataDir = "D:\\query-reformulation\\dataset\\SWT\\data";

    private List<String> fullIdList = new ArrayList<>();

    private List<String> partIdList = new ArrayList<>();

    public List<String> getFullData() {
        File dataFolder = new File(dataDir + "\\BugCorpus");
        File[] files = dataFolder.listFiles();

        assert files != null;
        for (File file: files) {
            if (file.isFile()) {
                String idStr = file.getName().split("\\.")[0];
                idStr = idStr.replaceAll("[^0-9]", "");

                fullIdList.add(idStr);
            }
        }

        return fullIdList;
    }

    public List<String> getPartData() {
        File[] files = new File(dataDir).listFiles();

        assert files != null;
        for (File file: files) {
            if (file.isDirectory()) {
                String fileName = file.getName();
                if (!fileName.startsWith("BugLocator_")) {
                    continue;
                }
                fileName = fileName.substring(11);
                fileName = fileName.replaceAll("[^0-9]", "");

                partIdList.add(fileName);
            }
        }

        return partIdList;
    }

    public List<String> getFullIdList() {
        return fullIdList;
    }

    public List<String> getPartIdList() {
        return partIdList;
    }

    public static void main(String[] args) {
        FindLostData findLostData = new FindLostData();

        List<String> fullList = findLostData.getFullData();
        List<String> partList = findLostData.getPartData();

        for (String id: fullList) {
            if (!partList.contains(id))
                System.out.println(id);
        }
    }
}

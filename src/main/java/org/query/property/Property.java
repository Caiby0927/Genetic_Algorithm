package org.query.property;

/**
 * @author CAI
 * @date 2023/4/28
 **/
public class Property {

    private static Property p = null;

    public final String experimentProjectPath;

    public final String projectDataPath;

    public final String luceneIndexPath;

    public final String GAResultPath;

    public final String GAResultFilePath;

    public final String stopBugId;

    public final String project;

    public Property(String experimentProjectPath, String projectDataPath, String luceneIndexPath, String GAResultPath, String GAResultFilePath, String stopBugId, String project) {
        this.experimentProjectPath = experimentProjectPath;
        this.projectDataPath = projectDataPath;
        this.luceneIndexPath = luceneIndexPath;
        this.GAResultPath = GAResultPath;
        this.GAResultFilePath = GAResultFilePath;
        this.stopBugId = stopBugId;
        this.project = project;
    }

    public static void createInstance(String experimentProjectPath, String projectDataPath, String indexPath, String GAResultPath, String GAResultFilePath, String stopBugId, String project) {
        if (p == null) {
            p = new Property(experimentProjectPath, projectDataPath, indexPath, GAResultPath, GAResultFilePath, stopBugId, project);
        }
    }

    public static Property getInstance() {
        return p;
    }
}

package gitlet;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import gitlet.GitletConstant.GitletFile;

/**
 * Update Or Add Or Delete executor for command.
 * @author Yunshun Zhong
 */
public class UpdateExecutor {

    /**
     * Save the commit tree.
     *
     * @param commitTree commit tree.
     * @param branchName branch name.
     */
    void saveCommit(CommitTree commitTree, String branchName) {
        byte[] bytes = Utils.serialize(commitTree);
        String commitId = saveContent(bytes, "");
        String branchPath = saveBranch(branchName, commitId);
        saveHEAD(branchPath);
        saveLogs(commitTree, commitId, branchName, true);
    }

    /**
     * Save the content to objects.
     *
     * @param content    the content file.
     * @param sha1Prefix the prefix when get the sha1 hash.
     * @return ths sha1 hash of file.
     */
    String saveContent(byte[] content, String sha1Prefix) {
        String sha1 = Utils.sha1(sha1Prefix, content);
        StringBuilder filepath = new StringBuilder(GitletConstant.GITLET_PATH);
        filepath.append(GitletFile.OBJECTS.getPath()).append("/")
                .append(sha1, 0, 2);
        String fileName = sha1.substring(2);
        File fileDir = new File(filepath.toString());
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        File object = new File(filepath.append("/").toString()
                + fileName);
        Utils.writeContents(object, content);

        return sha1;
    }

    /**
     * save the current branch to HEAD.
     *
     * @param refs the current branch.
     */
    private void saveHEAD(String refs) {
        File file = new File(GitletConstant.GITLET_PATH
                + GitletFile.HEAD_FILE.getPath());
        Utils.writeContents(file, refs);
    }

    /**
     * save the commit to the current branch.
     *
     * @param branchName branch name.
     * @param commitSha  the sha1 hash of commit.
     * @return the current branch path.
     */
    private String saveBranch(String branchName, String commitSha) {
        String branchPath = GitletFile.REF_HEADS.getPath() + "/"
                + branchName;
        File file = new File(GitletConstant.GITLET_PATH + branchPath);
        Utils.writeContents(file, commitSha);
        return branchPath;
    }

    /**
     * Save the logs of commit.
     * @param commitTree commit tree message.
     * @param commitSha  the sha hash of commit.
     * @param branchName the current branch.
     * @param logHead    write to log head file when is true.
     */
    public void saveLogs(CommitTree commitTree, String commitSha,
                         String branchName, boolean logHead) {
        String preCommitId = commitTree.getPreCommitTreeId();
        if (null == preCommitId) {
            preCommitId = "0000000000000000000000000000000000000000";
        }

        String mergeOne = commitTree.getMergeCommitIdOne();
        String mergeTwo = commitTree.getMergeCommitIdTwo();
        String merged = "";
        if (null != mergeOne && null != mergeTwo) {
            merged = mergeOne.substring(0, 7) + " " + mergeTwo.substring(0, 7);
        }

        String date = DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss yyyy",
                Locale.US).format(commitTree.getDate()) + " -0800";

        StringBuilder log = new StringBuilder();
        log.append(preCommitId).append("\t")
                .append(commitSha).append("\t")
                .append("0").append("\t")
                .append(merged).append("\t")
                .append(date).append("\t")
                .append(commitTree.getMessage()).append("\n");

        String logPath = GitletConstant.GITLET_PATH + GitletFile.LOG_HEADS
                .getPath() + "/" + branchName;
        File logFile = new File(logPath);
        Utils.writeContentsAppend(logFile, log.toString());

        if (logHead) {
            File logHeadFile = new File(GitletConstant.GITLET_PATH
                    + GitletFile.LOG_HEAD_FILE.getPath());
            Utils.writeContentsAppend(logHeadFile, log.toString());
        }
    }

    /**
     * Save the logs of commit.
     * @param preCommitId   the previous commit id.
     * @param curCommitId   the current commit id.
     * @param logMessage    the log message.
     * @param branchLogPath the branch log path,
     *                      if null, do not log to the branch.
     */
    public void saveLogs(String preCommitId, String curCommitId,
                         String logMessage, String branchLogPath) {
        String date = DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss yyyy",
                Locale.US).format(ZonedDateTime.now(ZoneId.of("US/Pacific")))
                + " -0800";
        StringBuilder log = new StringBuilder();
        log.append(preCommitId).append("\t")
                .append(curCommitId).append("\t")
                .append("1").append("\t\t")
                .append(date).append("\t")
                .append(logMessage).append("\n");
        File logHeadFile = new File(GitletConstant.GITLET_PATH
                + GitletFile.LOG_HEAD_FILE.getPath());
        Utils.writeContentsAppend(logHeadFile, log.toString());

        if (null != branchLogPath) {
            File branchLogFile = new File(GitletConstant.GITLET_PATH
                    + branchLogPath);
            if (branchLogFile.exists() && branchLogFile.isFile()) {
                Utils.writeContentsAppend(branchLogFile, log.toString());
            }
        }
    }

    /**
     * Save stage to index.
     *
     * @param blobTree blob tree.
     */
    public void saveStage(BlobTree blobTree) {
        String path = GitletConstant.GITLET_PATH
                + GitletFile.INDEX_FILE.getPath();
        File indexFile = new File(path);
        Utils.writeObject(indexFile, blobTree);
    }
}

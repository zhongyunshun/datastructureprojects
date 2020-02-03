package gitlet;

import java.io.File;

import gitlet.GitletConstant.GitletFile;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The Executor of Command.
 *
 * @author Yunshun Zhong
 */
public class CommandExecutor {

    /**
     * ValidateExecutor.
     */
    private ValidateExecutor validateExecutor = new ValidateExecutor();

    /**
     * QueryExecutor.
     */
    private QueryExecutor queryExecutor = new QueryExecutor();

    /**
     * UpdateExecutor.
     */
    private UpdateExecutor updateExecutor = new UpdateExecutor();

    /**
     * Print the commit log.
     *
     * @param commitTree commit tree.
     * @param commitId   commit tree.
     */
    public void printCommitLog(CommitTree commitTree, String commitId) {
        System.out.println("===");
        System.out.println("commit " + commitId);
        if (commitTree.getMergeCommitIdOne() != null
                && commitTree.getMergeCommitIdTwo() != null) {
            String merge = commitTree.getMergeCommitIdOne().substring(0, 7)
                    + " " + commitTree.getMergeCommitIdTwo().substring(0, 7);
            System.out.println("Merge: " + merge);
        }
        String date = DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss yyyy",
                Locale.US).format(commitTree.getDate()) + " -0800";
        System.out.println("Date: " + date);
        System.out.println(commitTree.getMessage().trim());
        System.out.println();
    }

    /**
     * Print the commit log.
     *
     * @param commitLog commit log message.
     */
    public void printCommitLog(String[] commitLog) {
        System.out.println("===");
        System.out.println("commit " + commitLog[1].trim());
        if (!"".equals(commitLog[3].trim())) {
            System.out.println("Merge: " + commitLog[3].trim());
        }
        System.out.println("Date: " + commitLog[4].trim());
        System.out.println(commitLog[5].trim());
        System.out.println();
    }

    /**
     * Print the branch message.
     */
    public void printBranch() {
        String curBranchName = queryExecutor.getCurBranchName();
        File branchDirFile = new File(GitletConstant.GITLET_PATH
                + GitletFile.REF_HEADS.getPath());
        File[] branchFiles = branchDirFile.listFiles();
        System.out.println("=== Branches ===");
        for (File branchFile : branchFiles) {
            if (branchFile.isFile()) {
                String branchName = branchFile.getName();
                if (curBranchName.equals(branchName)) {
                    System.out.println("*" + branchName);
                } else {
                    System.out.println(branchName);
                }
            }
        }
        System.out.println();
    }

    /**
     * Get file name from file path.
     *
     * @param filePath file path.
     * @return file name.
     */
    String getFileNameByPath(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.getName();
        }
        String path = System.getProperty("user.dir");
        int idx = filePath.indexOf(path);
        return filePath.substring(idx + path.length() + 1);
    }

    /**
     * Checkout the given branch.
     *
     * @param branchName the given branch.
     */
    public void checkoutBranch(String branchName) {
        String curBranchName = queryExecutor.getCurBranchName();
        if (curBranchName.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        String branchPath = GitletFile.REF_HEADS.getPath() + "/" + branchName;
        File branchFile = new File(GitletConstant.GITLET_PATH + branchPath);
        if (!branchFile.exists()) {
            branchPath = GitletFile.REF_REMOTES.getPath() + "/" + branchName;
            branchFile = new File(GitletConstant.GITLET_PATH + branchPath);
            if (!branchFile.exists()) {
                System.out.println("No such branch exists.");
                System.exit(0);
            }
        }

        String curCommitId = queryExecutor.getCurCommitId();
        String branchCommitId = Utils.readContentsAsString(branchFile);

        checkoutByCommitId(curCommitId, branchCommitId);

        resetStageAndLog(curCommitId, branchCommitId, null);
        File head = new File(GitletConstant.GITLET_PATH
                + GitletFile.HEAD_FILE.getPath());
        Utils.writeContents(head, branchPath);
    }

    /**
     * Checkout from fromCommitId to toCommitId.
     * @param fromCommitId  source commit id.
     * @param toCommitId    target commit id.
     */
    void checkoutByCommitId(String fromCommitId, String toCommitId) {
        Map<String, Stage> curBlobs = queryExecutor.convertStage(
                queryExecutor.getStageByCommitId(fromCommitId));
        Map<String, Stage> workBlobs = queryExecutor.convertStage(
                queryExecutor.getStageFromWork());
        Map<String, Stage> branchBlobs = queryExecutor.convertStage(
                queryExecutor.getStageByCommitId(toCommitId));

        validateExecutor.canBeCheckout(curBlobs, workBlobs, branchBlobs);
        rewriteForBranch(curBlobs, workBlobs, branchBlobs);
    }

    /**
     * Rewrite the head file and log the rewrite.
     *
     * @param curCommitId the current commit id.
     * @param toCommitId  the to commit id.
     * @param branchName  the branch name,
     *                    if null, do not log to branch log file.
     */
    void resetStageAndLog(String curCommitId, String toCommitId,
                          String branchName) {
        CommitTree branchCommit = queryExecutor.getCommitById(toCommitId);
        BlobTree stageTree = new BlobTree();
        if (null != branchCommit.getBlobTreeId()) {
            stageTree = queryExecutor.getBlobById(branchCommit.getBlobTreeId());
        }
        updateExecutor.saveStage(stageTree);
        String logMessage = "checkout the commitId";
        String branchLogPath = null;
        if (null != branchName) {
            branchLogPath = GitletConstant.GITLET_PATH + GitletFile.LOG_HEADS
                    .getPath() + "/" + branchName;
        }
        updateExecutor.saveLogs(curCommitId, toCommitId, logMessage,
                branchLogPath);
    }

    /**
     * Checkout the file from the commit id.
     *
     * @param commitTree commit tree.
     * @param commitId   the given commit id.
     * @param fileName   file name.
     */
    public void checkoutFileByCommit(CommitTree commitTree, String commitId,
                                     String fileName) {
        String blobTreeId = commitTree.getBlobTreeId();
        if (null == blobTreeId) {
            validateExecutor.exitByFile();
        }
        BlobTree blobTree = queryExecutor.getBlobById(blobTreeId);
        if (blobTree == null) {
            validateExecutor.exitByFile();
        }
        Stage commitBlob = getStageByFileName(blobTree.getStages(), fileName);
        if (null == commitBlob) {
            validateExecutor.exitByFile();
        }
        String commitBlobId = commitBlob.getBlobId();
        byte[] commitContent = getBlobContentById(commitBlobId);
        if (null == commitContent) {
            validateExecutor.exitByFile();
        }
        BlobTree stageTree = queryExecutor.getStage();
        Stage stageBlob = null;
        if (null != stageTree && null != stageTree.getStages()) {
            stageBlob = getStageByFileName(stageTree.getStages(), fileName);
        }
        String filepath = System.getProperty("user.dir") + "/" + fileName;
        File file = new File(filepath);
        if (!file.exists()) {
            validateExecutor.exitByFile();
        }
        byte[] curContent = Utils.readContents(file);
        String curBlobId = Utils.sha1(filepath, curContent);
        if (curBlobId.equals(commitBlobId)) {
            return;
        }
        Utils.writeContents(new File(filepath), commitContent);
        if (null == stageBlob || null == stageBlob.getBlobId()
                || commitBlobId.equals(stageBlob.getBlobId())) {
            return;
        }
        String curCommitId = queryExecutor.getCurCommitId();
        if (curCommitId.equals(commitId)) {
            stageBlob.setBlobId(curBlobId);
            stageBlob.setStatus(commitBlob.getStatus());
            updateExecutor.saveStage(stageTree);
            return;
        }
        CommitTree curCommitTree = queryExecutor.getCommitById(curCommitId);
        BlobTree curBlobTree = queryExecutor.getBlobById(
                curCommitTree.getBlobTreeId());
        Stage curCommitBlob = getStageByFileName(curBlobTree.getStages(),
                fileName);
        if (null == curCommitBlob) {
            stageTree.getStages().remove(stageBlob);
        } else {
            stageBlob.setBlobId(curCommitBlob.getBlobId());
            stageBlob.setStatus(curCommitBlob.getStatus());
        }
        updateExecutor.saveStage(stageTree);
    }

    /**
     * Get stage from stages by fileName.
     *
     * @param stages   the set of stage.
     * @param fileName file name.
     * @return stage.
     */
    private Stage getStageByFileName(List<Stage> stages, String fileName) {
        for (Stage stage : stages) {
            String stageName = getFileNameByPath(stage.getFileName());
            if (stageName.equals(fileName)) {
                return stage;
            }
        }
        return null;
    }

    /**
     * Get the blob content from the blobId.
     *
     * @param blobId blobId.
     * @return content.
     */
    private byte[] getBlobContentById(String blobId) {
        String path = GitletConstant.GITLET_PATH + GitletFile.OBJECTS.getPath()
                + "/" + blobId.substring(0, 2) + "/" + blobId.substring(2);
        try {
            return Utils.readContents(new File(path));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Rewrite the file for checkout branch.
     *
     * @param curBlobs     the current branch stages, not null.
     * @param workBlobs    work files in current work, not null.
     * @param branchBlobs  files in given branch, not null.
     */
    void rewriteForBranch(Map<String, Stage> curBlobs,
            Map<String, Stage> workBlobs, Map<String, Stage> branchBlobs) {
        for (Map.Entry<String, Stage> work : workBlobs.entrySet()) {
            Stage workBlob = work.getValue();
            Stage branchBlob = branchBlobs.get(work.getKey());
            Stage commitBlob = curBlobs.get(work.getKey());

            if (workBlob.equals(commitBlob) && branchBlob == null) {
                Utils.restrictedDelete(new File(workBlob.getFileName()));
            }
        }

        for (Map.Entry<String, Stage> branch : branchBlobs.entrySet()) {
            Stage branchBlob = branch.getValue();
            Stage workBlob = workBlobs.get(branch.getKey());
            Stage commitBlob = curBlobs.get(branch.getKey());
            if (!branchBlob.equals(workBlob) && (workBlob == null
                    || workBlob.equals(commitBlob))) {
                byte[] content = getBlobContentById(branchBlob.getBlobId());
                File file = new File(branchBlob.getFileName());
                Utils.writeContents(file, content);
            }
        }
    }

    /**
     * Print the file which is staged or removed.
     * @param commitBlobs   the files of current commit.
     * @param stageBlobs    the files of stage.
     */
    void printStatusForCommitAndStage(Map<String, Stage> commitBlobs,
                                      Map<String, Stage> stageBlobs) {
        Set<String> staged = new HashSet<>();
        Set<String> removed = new HashSet<>();
        Set<String> comparedFile = new HashSet<>();
        for (Map.Entry<String, Stage> commit : commitBlobs.entrySet()) {
            String filePath = commit.getKey();
            comparedFile.add(filePath);
            Stage stage = stageBlobs.get(filePath);
            if (stage == null || "1".equals(stage.getStatus())) {
                removed.add(getFileNameByPath(filePath));
            } else if (!stage.equals(commit.getValue())) {
                staged.add(getFileNameByPath(filePath));
            }
        }
        for (Map.Entry<String, Stage> stage : stageBlobs.entrySet()) {
            if (!comparedFile.contains(stage.getKey())
                    && "0".equals(stage.getValue().getStatus())) {
                staged.add(getFileNameByPath(stage.getKey()));
            }
        }
        System.out.println("=== Staged Files ===");
        printStatusFile(staged);
        System.out.println();
        System.out.println("=== Removed Files ===");
        printStatusFile(removed);
        System.out.println();
    }

    /**
     * Print the file name sequentially.
     * @param fileNames set of fileName.
     */
    void printStatusFile(Set<String> fileNames) {
        if (fileNames.isEmpty()) {
            return;
        }
        Set<String> sortSet = new TreeSet<>(Comparator.naturalOrder());
        sortSet.addAll(fileNames);
        sortSet.forEach(System.out::println);
    }
}

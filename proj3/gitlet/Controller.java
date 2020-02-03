package gitlet;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;

import gitlet.GitletConstant.GitletFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Controller for Gitlet.
 *
 * @author Yunshun Zhong
 */
public class Controller {

    /**
     * CommandExecutor.
     */
    private CommandExecutor commandExecutor;

    /**
     * QueryExecutor.
     */
    private QueryExecutor queryExecutor;

    /**
     * UpdateExecutor.
     */
    private UpdateExecutor updateExecutor;

    /**
     * ValidateExecutor.
     */
    private ValidateExecutor validateExecutor;

    /**
     * RemoteExecutor.
     */
    private RemoteExecutor remoteExecutor;

    /**
     * The path of work directory.
     */
    private String workPath;

    /**
     * The constructor of Controller.
     */
    public Controller() {
        this.commandExecutor = new CommandExecutor();
        this.queryExecutor = new QueryExecutor();
        this.validateExecutor = new ValidateExecutor();
        this.updateExecutor = new UpdateExecutor();
        this.remoteExecutor = new RemoteExecutor();
        this.workPath = System.getProperty("user.dir");
    }

    /**
     * Do the print.
     * @param args args.
     */
    public void doPrint(String[] args) {
        String content = Utils.readContentsAsString(new File(args[1]));
        System.out.println(content);
    }

    /**
     * Do the init, include create the directory and init the commit.
     */
    public void doInit() {
        File gitlet = new File(GitletConstant.GITLET_PATH);
        gitlet.mkdir();

        for (GitletFile gitletFile : GitletFile.values()) {
            File file = new File(GitletConstant.GITLET_PATH
                    + gitletFile.getPath());
            if ("directory".equals(gitletFile.getType())) {
                file.mkdir();
            }
        }

        CommitTree commitTree = new CommitTree();
        commitTree.setBlobTreeId(null);
        commitTree.setPreCommitTreeId(null);
        commitTree.setMessage("initial commit");
        commitTree.setDate(Instant.ofEpochMilli(0L)
                .atZone(ZoneId.of("US/Pacific")));
        commitTree.setMergeCommitIdOne(null);
        commitTree.setMergeCommitIdTwo(null);

        updateExecutor.saveCommit(commitTree, GitletConstant.DEFAULT_BRANCH);
    }

    /**
     * Do the add command.
     *
     * @param args args.
     */
    public void doAdd(String[] args) {
        File file = validateExecutor.checkFile(args, workPath);
        byte[] content = Utils.readContents(file);
        String blobId = updateExecutor.saveContent(content, file.getPath());

        Stage stage = new Stage();
        stage.setBlobId(blobId);
        stage.setFileName(file.getPath());
        stage.setStatus("0");

        BlobTree blobTree = queryExecutor.getStage();
        List<Stage> stages = new ArrayList<>();
        boolean add = true;
        if (null == blobTree) {
            blobTree = new BlobTree();
        } else {
            stages = blobTree.getStages();
            if (null == stages) {
                stages = new ArrayList<>();
            }
            for (Stage existStage : stages) {
                if (existStage.getFileName().equals(stage.getFileName())) {
                    existStage.setBlobId(stage.getBlobId());
                    existStage.setStatus(stage.getStatus());
                    add = false;
                }
            }
        }
        if (add) {
            stages.add(stage);
        }
        blobTree.setStages(stages);
        updateExecutor.saveStage(blobTree);
    }

    /**
     * Do the commit command.
     *
     * @param args args.
     */
    public void doCommit(String[] args) {
        validateExecutor.checkArgsShouldBeTwo(args);
        String message = args[1].trim();
        if ("".equals(message)) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        BlobTree blobTree = queryExecutor.getStage();
        if (null == blobTree || null == blobTree.getStages()
                || blobTree.getStages().isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        blobTree.getStages().removeIf(stage -> "1".equals(stage.getStatus()));
        updateExecutor.saveStage(blobTree);
        byte[] bytes = Utils.serialize(blobTree);
        String sha1 = updateExecutor.saveContent(bytes, "");

        String preCommitId = queryExecutor.getCurCommitId();
        CommitTree preCommit = queryExecutor.getCommitById(preCommitId);

        if (sha1.equals(preCommit.getBlobTreeId())) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        CommitTree commitTree = new CommitTree();
        commitTree.setBlobTreeId(sha1);
        commitTree.setPreCommitTreeId(preCommitId);
        commitTree.setMessage(message);
        commitTree.setDate(Instant.now().atZone(ZoneId.of("US/Pacific")));

        String branchName = queryExecutor.getCurBranchName();

        updateExecutor.saveCommit(commitTree, branchName);

    }

    /**
     * Do the remove command.
     *
     * @param args args.
     */
    public void doRemove(String[] args) {
        validateExecutor.checkArgsShouldBeTwo(args);
        String fileName = args[1];

        BlobTree stageTree = queryExecutor.getStage();
        boolean deleteAble = false;
        if (null != stageTree && null != stageTree.getStages()) {
            for (Stage stage : stageTree.getStages()) {
                if (fileName.equals(commandExecutor.getFileNameByPath(
                        stage.getFileName()))) {
                    stage.setStatus("1");
                    updateExecutor.saveStage(stageTree);
                    deleteAble = true;
                    break;
                }
            }
        }

        File file = new File(System.getProperty("user.dir") + "/" + fileName);
        if (!file.exists()) {
            return;
        }

        String commitId = queryExecutor.getCurCommitId();
        CommitTree commitTree = queryExecutor.getCommitById(commitId);
        BlobTree blobTree = queryExecutor.getBlobById(
                commitTree.getBlobTreeId());
        if (null != blobTree) {
            for (Stage stage : blobTree.getStages()) {
                if (stage.getFileName().equals(file.getPath())) {
                    Utils.restrictedDelete(file);
                    deleteAble = true;
                }
            }
        }
        if (!deleteAble) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    /**
     * Do the log command.
     * @param args args.
     */
    public void doLog(String[] args) {
        validateExecutor.checkArgsShouldBeOne(args);
        String commitId = queryExecutor.getCurCommitId();
        while (null != commitId) {
            CommitTree commitTree = queryExecutor.getCommitById(commitId);
            commandExecutor.printCommitLog(commitTree, commitId);
            commitId = commitTree.getPreCommitTreeId();
        }
    }

    /**
     * Do the global-log command.
     * @param args args.
     */
    public void doGlobalLog(String[] args) {
        File branchDirFile = new File(GitletConstant.GITLET_PATH
                + GitletFile.LOG_HEADS.getPath());
        File[] logFiles = branchDirFile.listFiles();
        if (logFiles == null) {
            return;
        }
        Set<String> commitIds = new HashSet<>();
        for (File logFile : logFiles) {
            if (logFile.isFile()) {
                String log = Utils.readContentsAsString(logFile);
                String[] records = log.split("\n");
                for (int idx = records.length - 1; idx >= 0; idx--) {
                    String[] logs = records[idx].split("\t");
                    String commitId = logs[1];
                    String logType = logs[2];
                    if ("1".equals(logType) || commitIds.contains(commitId)) {
                        continue;
                    } else {
                        commandExecutor.printCommitLog(logs);
                        commitIds.add(commitId);
                    }
                }
            }
        }
    }

    /**
     * Do the find command.
     * @param args args.
     */
    public void doFind(String[] args) {
        validateExecutor.checkArgsShouldBeTwo(args);
        String commitMessage = args[1];
        File logHeadFile = new File(GitletConstant.GITLET_PATH
                + GitletFile.LOG_HEAD_FILE.getPath());
        Set<String> commitIds = new HashSet<>();
        boolean exist = false;
        if (logHeadFile.isFile()) {
            String log = Utils.readContentsAsString(logHeadFile);
            String[] records = log.split("\n");
            for (int idx = records.length - 1; idx >= 0; idx--) {
                String[] commitLog = records[idx].split("\t");
                String commitId = commitLog[1];
                String commitMsg = commitLog[5].trim();
                if ("1".equals(commitLog[2]) || commitIds.contains(commitId)) {
                    continue;
                } else if (commitMsg.equals(commitMessage)) {
                    System.out.println(commitId);
                    exist = true;
                }
                commitIds.add(commitId);
            }
        }
        if (!exist) {
            System.out.println("Found no commit with that message.");
        }
    }

    /**
     * Do the status command.
     * @param args args.
     */
    public void doStatus(String[] args) {
        commandExecutor.printBranch();

        String commitId = queryExecutor.getCurCommitId();
        Map<String, Stage> commitBlobs = queryExecutor.convertStage(
                queryExecutor.getStageByCommitId(commitId));
        Map<String, Stage> stageBlobs = queryExecutor.convertStage(
                queryExecutor.getStageBlobs());
        Map<String, Stage> workBlobs = queryExecutor.convertStage(
                queryExecutor.getStageFromWork());

        commandExecutor.printStatusForCommitAndStage(commitBlobs, stageBlobs);
        Set<String> modified = new HashSet<>();
        Set<String> untracked = new HashSet<>();
        Set<String> comparedFile = new HashSet<>();
        for (Map.Entry<String, Stage> work : workBlobs.entrySet()) {
            String filePath = work.getKey();
            comparedFile.add(filePath);
            Stage stage = stageBlobs.get(filePath);
            Stage commit = commitBlobs.get(filePath);
            if ((stage == null || "1".equals(stage.getStatus()))
                    && commit == null) {
                untracked.add(commandExecutor.getFileNameByPath(filePath));
            } else if (!work.getValue().equals(stage)) {
                modified.add(commandExecutor.getFileNameByPath(filePath)
                        + " (modified)");
            }
        }
        for (Map.Entry<String, Stage> stage : stageBlobs.entrySet()) {
            if (!comparedFile.contains(stage.getKey())
                    && "0".equals(stage.getValue().getStatus())) {
                modified.add(commandExecutor.getFileNameByPath(stage.getKey())
                        + " (deleted)");
            }
        }
        System.out.println("=== Modifications Not Staged For Commit ===");
        commandExecutor.printStatusFile(modified);
        System.out.println();
        System.out.println("=== Untracked Files ===");
        commandExecutor.printStatusFile(untracked);
        System.out.println();
    }

    /**
     * Do the checkout command.
     * @param args args.
     */
    public void doCheckout(String[] args) {
        if (args.length == 2) {
            String branchName = args[1];
            commandExecutor.checkoutBranch(branchName);
        } else if (args.length == 3 && "--".equals(args[1])) {
            String commitId = queryExecutor.getCurCommitId();
            CommitTree commitTree = queryExecutor.getCommitById(commitId);
            String fileName = args[2];
            commandExecutor.checkoutFileByCommit(commitTree, commitId,
                    fileName);
        } else if (args.length == 4 && "--".equals(args[2])) {
            String commitId = validateExecutor.getCommitBySix(args[1]);
            String fileName = args[3];
            CommitTree commitTree = queryExecutor.getCommitById(commitId);
            if (null != commitTree) {
                commandExecutor.checkoutFileByCommit(commitTree, commitId,
                        fileName);
            } else {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    /**
     * Do the branch command.
     * @param args args.
     */
    public void doBranch(String[] args) {
        validateExecutor.checkArgsShouldBeTwo(args);

        String branchName = args[1];
        String branchPath = GitletFile.REF_HEADS.getPath() + "/" + branchName;
        File branchFile = new File(GitletConstant.GITLET_PATH + branchPath);
        if (branchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        String commitId = queryExecutor.getCurCommitId();

        Utils.writeContents(branchFile, commitId);

        CommitTree commitTree = queryExecutor.getCommitById(commitId);
        updateExecutor.saveLogs(commitTree, commitId, branchName, false);
    }

    /**
     * Do the rm-branch command.
     * @param args args.
     */
    public void doRemoveBranch(String[] args) {
        validateExecutor.checkArgsShouldBeTwo(args);
        String branchName = args[1];
        String curBranchName = queryExecutor.getCurBranchName();

        if (curBranchName.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        File rmBranchFile = new File(GitletConstant.GITLET_PATH
                + GitletFile.REF_HEADS.getPath() + "/" + branchName);
        if (!rmBranchFile.exists() || rmBranchFile.isDirectory()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        rmBranchFile.delete();
        File branchLogFile = new File(GitletConstant.GITLET_PATH
                + GitletFile.LOG_HEADS.getPath() + "/" + branchName);
        if (branchLogFile.exists() && !branchLogFile.isDirectory()) {
            branchLogFile.delete();
        }
    }

    /**
     * Do the reset command.
     * @param args args.
     */
    public void doReset(String[] args) {
        validateExecutor.checkArgsShouldBeTwo(args);
        String commitId = validateExecutor.getCommitBySix(args[1]);
        CommitTree commitTree = queryExecutor.getCommitById(commitId);
        if (null == commitTree) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        String curCommitId = queryExecutor.getCurCommitId();
        commandExecutor.checkoutByCommitId(curCommitId, commitId);
        String curBranchName = queryExecutor.getCurBranchName();
        commandExecutor.resetStageAndLog(curCommitId, commitId, curBranchName);
        File branchFile = new File(GitletConstant.GITLET_PATH
                + queryExecutor.getCurBranch());
        Utils.writeContents(branchFile, commitId);
        String logMessage = "Reset " + curCommitId.substring(0, 6) + " to "
                + commitId.substring(0, 6);
        updateExecutor.saveLogs(curCommitId, commitId, logMessage,
                GitletFile.LOG_HEADS.getPath() + "/" + curBranchName);
    }

    /**
     * Do the merge command.
     * @param args args.
     */
    public void doMerge(String[] args) {
        validateExecutor.checkArgsShouldBeTwo(args);
        String givenBranch = args[1];
        String givenCommitId = queryExecutor.getCommitIdByBranch(givenBranch);

        MergeExecutor mergeExecutor = new MergeExecutor(givenCommitId);
        mergeExecutor.checkBranchName(givenBranch);

        BlobTree stageTree = queryExecutor.getStage();
        mergeExecutor.checkUnCommitted(stageTree);

        Set<Stage> curBlobs = queryExecutor.getStageByCommitId(
                mergeExecutor.getCurCommitId());
        Set<Stage> workBlobs = queryExecutor.getStageFromWork();
        validateExecutor.canBeCheckout(curBlobs, workBlobs);

        BlobTree mergeTree = mergeExecutor.merge(curBlobs, workBlobs);
        mergeExecutor.commitForMerge(givenBranch, mergeTree);
    }

    /**
     * Do the add-remote command.
     * @param args args.
     */
    public void doAddRemote(String[] args) {
        validateExecutor.checkAddRemoteCommand(args);
        String remoteName = args[1];
        String remotePath = args[2];
        remoteExecutor.addRemote(remoteName, remotePath);
    }

    /**
     * Do the rm-remote command.
     * @param args args.
     */
    public void doRemoveRemote(String[] args) {
        validateExecutor.checkArgsShouldBeTwo(args);
        String remoteName = args[1];
        validateExecutor.checkRemoteNameExist(remoteName);
        remoteExecutor.removeRemote(remoteName);
    }

    /**
     * Do the push command.
     * @param args args.
     */
    public void doPush(String[] args) {
        validateExecutor.checkArgsShouldBeThree(args);
        String remoteName = args[1];
        validateExecutor.checkRemoteNameExist(remoteName);
        validateExecutor.checkRemoteExist(
                remoteExecutor.getRemotePath(remoteName));
        remoteExecutor.push(remoteName, args[2]);
    }

    /**
     * Do the fetch command.
     * @param args args.
     */
    public void doFetch(String[] args) {
        validateExecutor.checkArgsShouldBeThree(args);
        String remoteName = args[1];
        validateExecutor.checkRemoteNameExist(remoteName);
        validateExecutor.checkRemoteExist(
                remoteExecutor.getRemotePath(remoteName));
        remoteExecutor.fetch(remoteName, args[2]);
    }

    /**
     * Do the pull command.
     * @param args args.
     */
    public void doPull(String[] args) {
        validateExecutor.checkArgsShouldBeThree(args);
        String remoteName = args[1];
        validateExecutor.checkRemoteNameExist(remoteName);
        validateExecutor.checkRemoteExist(
                remoteExecutor.getRemotePath(remoteName));
        remoteExecutor.pull(remoteName, args[2]);
    }
}

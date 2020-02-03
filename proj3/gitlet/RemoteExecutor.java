package gitlet;

import java.io.File;

import gitlet.GitletConstant.GitletFile;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Set;

/**
 * The Executor of Remote Command.
 *
 * @author Yunshun Zhong
 */
public class RemoteExecutor {

    /**
     * QueryExecutor.
     */
    private QueryExecutor queryExecutor = new QueryExecutor();

    /**
     * Add remote gitlet info, assume the param is legal.
     * @param remoteName    remote name.
     * @param remotePath    remote path.
     */
    void addRemote(String remoteName, String remotePath) {
        StringBuilder config = new StringBuilder();
        config.append("remote").append("\t")
                .append(remoteName).append("\t")
                .append("url = ").append("\t")
                .append(remotePath).append("\n");
        File configFile = new File(GitletConstant.GITLET_PATH
                + GitletFile.CONFIG_FILE.getPath());
        if (!configFile.exists()) {
            createNewFile(configFile);
        }
        Utils.writeContentsAppend(configFile, config.toString());

        String localRemoteDirPath = GitletConstant.GITLET_PATH
                + GitletFile.REF_REMOTES.getPath() + "/" + remoteName;
        File localRemoteDirFile = new File(localRemoteDirPath);
        localRemoteDirFile.mkdir();

        File remoteBranch = new File(remotePath
                + GitletFile.REF_HEADS.getPath());
        File[] remoteBranches = remoteBranch.listFiles();
        if (remoteBranches != null) {
            for (File remoteBranchFile : remoteBranches) {
                File localFile = new File(localRemoteDirPath + "/"
                        + remoteBranchFile.getName());
                copyFile(remoteBranchFile, localFile);
            }
        }
    }

    /**
     * Remove the remote name.
     * @param remoteName remote name, and it exists.
     */
    void removeRemote(String remoteName) {
        File remoteDir = new File(GitletConstant.GITLET_PATH
                + GitletFile.REF_REMOTES.getPath() + "/" + remoteName);
        File config = new File(GitletConstant.GITLET_PATH
                + GitletFile.CONFIG_FILE.getPath());
        String content = Utils.readContentsAsString(config);
        StringBuilder rewrite = new StringBuilder();
        for (String remoteInfo : content.split("\n")) {
            String[] infos = remoteInfo.split("\t");
            if (infos.length == 4 && infos[1].trim().equals(remoteName)) {
                continue;
            }
            rewrite.append(remoteInfo).append("\n");
        }
        Utils.writeContents(config, rewrite.toString());

        File[] files = remoteDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    /**
     * Push local to remote.
     * @param remoteName    remote name.
     * @param branchName    remote branch name.
     */
    void push(String remoteName, String branchName) {
        String remotePath = getRemotePath(remoteName);
        File branchFile = new File(remotePath + GitletFile.REF_HEADS.getPath()
                + "/" + branchName);
        String remoteCommitId = null;
        if (branchFile.exists()) {
            remoteCommitId = Utils.readContentsAsString(branchFile);
        } else {
            createNewFile(branchFile);
        }
        String localCommitId = queryExecutor.getCurCommitId();
        if (localCommitId.equals(remoteCommitId)) {
            return;
        }
        checkPushLegal(remoteCommitId, localCommitId);
        syncObjects(localCommitId, remoteCommitId, GitletConstant.GITLET_PATH,
                remotePath);
        Utils.writeContents(branchFile, localCommitId);
        syncLog(branchName, GitletConstant.GITLET_PATH + "/"
                        + GitletFile.LOG_HEADS.getPath(),
                remotePath + GitletFile.LOG_REFS.getPath());
    }

    /**
     * Fetch remote to local.
     * @param remoteName    remote name.
     * @param branchName    remote branch name.
     */
    void fetch(String remoteName, String branchName) {
        String remotePath = getRemotePath(remoteName);
        File branchFile = new File(remotePath + GitletFile.REF_HEADS.getPath()
                + "/" + branchName);
        String remoteCommitId = null;
        if (branchFile.exists()) {
            remoteCommitId = Utils.readContentsAsString(branchFile);
        } else {
            System.out.println("That remote does not have that branch.");
            System.exit(0);
        }
        String localCommitId = null;
        File localBranchFile = new File(GitletConstant.GITLET_PATH
                + GitletFile.REF_REMOTES.getPath() + "/" + remoteName
                + "/" + branchName);
        if (localBranchFile.exists()) {
            localCommitId = Utils.readContentsAsString(localBranchFile);
        } else {
            createNewFile(localBranchFile);
        }
        syncObjects(remoteCommitId, localCommitId, remotePath,
                GitletConstant.GITLET_PATH);
        Utils.writeContents(localBranchFile, remoteCommitId);
        syncLog(branchName, remotePath + "/" + GitletFile.LOG_HEADS.getPath(),
                GitletConstant.GITLET_PATH + "/"
                        + GitletFile.LOG_REFS.getPath());
    }

    /**
     * Pull command, first fetch, then merge.
     * @param remoteName    remote name.
     * @param branchName    remote branch name.
     */
    void pull(String remoteName, String branchName) {
        fetch(remoteName, branchName);
        String remotePath = getRemotePath(remoteName);
        File branchFile = new File(remotePath + GitletFile.REF_HEADS.getPath()
                + "/" + remoteName + "/" + branchName);
        String remoteCommitId = Utils.readContentsAsString(branchFile);
        MergeExecutor mergeExecutor = new MergeExecutor(remoteCommitId);

        BlobTree stageTree = queryExecutor.getStage();
        mergeExecutor.checkUnCommitted(stageTree);
        String commitId = mergeExecutor.getCurCommitId();
        Set<Stage> curBlobs = queryExecutor.getStageByCommitId(commitId);
        Set<Stage> workBlobs = queryExecutor.getStageFromWork();
        ValidateExecutor validateExecutor = new ValidateExecutor();
        validateExecutor.canBeCheckout(curBlobs, workBlobs);
        stageTree = mergeExecutor.merge(curBlobs, workBlobs);

        String givenBranchName = remoteName + "/" + branchName;
        mergeExecutor.commitForMerge(givenBranchName, stageTree);
    }

    /**
     * Get remote path by remote name from config file.
     * @param remoteName    remote name.
     * @return  remote path.
     */
    String getRemotePath(String remoteName) {
        File config = new File(GitletConstant.GITLET_PATH
                + GitletFile.CONFIG_FILE.getPath());
        String content = Utils.readContentsAsString(config);
        for (String remoteInfo : content.split("\n")) {
            String[] infos = remoteInfo.split("\t");
            if (infos.length == 4 && infos[1].trim().equals(remoteName)) {
                return infos[3].trim();
            }
        }
        System.out.println("Remote directory not found.");
        System.exit(0);
        return null;
    }

    /**
     * Copy file from remote to local.
     * @param sourceFile    remote file.
     * @param targetFile    local file.
     */
    private void copyFile(File sourceFile, File targetFile) {
        try {
            if (!targetFile.exists()) {
                targetFile.createNewFile();
            }
            FileInputStream fis = new FileInputStream(sourceFile);
            FileOutputStream fos = new FileOutputStream(targetFile);

            FileChannel sourceCh = fis.getChannel();
            FileChannel targetCh = fos.getChannel();

            MappedByteBuffer mbb = sourceCh.map(FileChannel.MapMode.READ_ONLY,
                    0, sourceCh.size());
            targetCh.write(mbb);
            sourceCh.close();
            targetCh.close();
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Check if the remote branch's head is in the history of
     * the current local head.
     * @param remoteCommitId    remote branch's head.
     * @param localCommitId     current local head.
     */
    private void checkPushLegal(String remoteCommitId, String localCommitId) {
        if (remoteCommitId == null) {
            return;
        }
        String commitId = localCommitId;
        while (commitId != null) {
            if (commitId.equals(remoteCommitId)) {
                return;
            }
            CommitTree commitTree = queryExecutor.getCommitById(commitId);
            if (null == commitTree) {
                System.out.println("Please pull down remote changes "
                        + "before pushing.");
                System.exit(0);
            }
            commitId = commitTree.getPreCommitTreeId();
        }
        System.out.println("Please pull down remote changes before pushing.");
        System.exit(0);
    }

    /**
     * Synchronize the files from commit id to the target object.
     * @param commitId          the head commit id of source.
     * @param endCommitId       the head commit id of target.
     * @param sourcePathPrefix  the source object path.
     * @param targetPathPrefix  the target object path.
     */
    private void syncObjects(String commitId, String endCommitId,
                             String sourcePathPrefix, String targetPathPrefix) {
        String headId = commitId;
        while (headId != null
                && headId.length() == GitletConstant.SHA_HASH_SIZE) {
            syncBySha(headId, sourcePathPrefix, targetPathPrefix);
            CommitTree commitTree = queryExecutor.getCommitById(headId);
            if (null != commitTree) {
                String blobTreeId = commitTree.getBlobTreeId();
                if (null != blobTreeId) {
                    syncBySha(blobTreeId, sourcePathPrefix, targetPathPrefix);
                    BlobTree blobTree = queryExecutor.getBlobById(blobTreeId);
                    if (null != blobTree) {
                        blobTree.getStages().forEach(stage -> syncBySha(
                                stage.getBlobId(), sourcePathPrefix,
                                targetPathPrefix));
                    }
                }
                headId = commitTree.getPreCommitTreeId();
            }
        }
    }

    /**
     * Synchronize the file by sha hash.
     * @param shaId             the hash of sha.
     * @param sourcePathPrefix  the source object path.
     * @param targetPathPrefix  the target object path.
     */
    private void syncBySha(String shaId, String sourcePathPrefix,
                           String targetPathPrefix) {
        String targetDirPath = targetPathPrefix + GitletFile.OBJECTS.getPath()
                + "/" + shaId.substring(0, 2);
        File targetDirFile = new File(targetDirPath);
        if (!targetDirFile.exists()) {
            targetDirFile.mkdir();
        }
        File sourceFile = new File(sourcePathPrefix
                + GitletFile.OBJECTS.getPath() + "/"
                + shaId.substring(0, 2) + "/" + shaId.substring(2));
        File targetFile = new File(targetDirPath + "/" + shaId.substring(2));
        copyFile(sourceFile, targetFile);
    }

    /**
     * Synchronize the log file.
     * @param branchName    branchName
     * @param sourcePathPrefix  the source log path.
     * @param targetPathPrefix  the target log path.
     */
    private void syncLog(String branchName, String sourcePathPrefix,
                         String targetPathPrefix) {
        String sourcePath = sourcePathPrefix + "/" + branchName;
        String targetPath = targetPathPrefix + "/" + branchName;
        File sourceFile = new File(sourcePath);
        File targetFile = new File(targetPath);
        createNewFile(sourceFile);
        createNewFile(targetFile);
        copyFile(sourceFile, targetFile);
    }

    /**
     * Create new File if the file is not exist.
     * @param file file.
     */
    private void createNewFile(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }
}

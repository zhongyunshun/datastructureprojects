package gitlet;

import java.io.File;

import gitlet.GitletConstant.GitletFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Query executor for command.
 * @author Yunshun Zhong
 */
public class QueryExecutor {

    /**
     * Get the current commit id.
     *
     * @return commit id.
     */
    String getCurCommitId() {
        String branchPath = getCurBranch();
        File file = new File(GitletConstant.GITLET_PATH + branchPath);
        return Utils.readContentsAsString(file);
    }

    /**
     * Get the commit id from branchName.
     * @param branchName the given branch name.
     * @return commit id.
     */
    String getCommitIdByBranch(String branchName) {
        File file = new File(GitletConstant.GITLET_PATH
                + GitletFile.REF_HEADS.getPath() + "/" + branchName);
        if (file.exists()) {
            return Utils.readContentsAsString(file);
        } else {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        return null;
    }

    /**
     * Get the commit by commit tree id.
     *
     * @param commitId commit tree id.
     * @return CommitTree.
     */
    CommitTree getCommitById(String commitId) {
        String path = GitletConstant.GITLET_PATH + GitletFile.OBJECTS.getPath()
                + "/" + commitId.substring(0, 2) + "/" + commitId.substring(2);
        File file = new File(path);
        if (file.exists()) {
            try {
                return Utils.readObject(file, CommitTree.class);
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Get the current branch path.
     *
     * @return branch path.
     */
    String getCurBranch() {
        File file = new File(GitletConstant.GITLET_PATH
                + GitletFile.HEAD_FILE.getPath());
        return Utils.readContentsAsString(file);
    }

    /**
     * Get the current branch name.
     *
     * @return branch name.
     */
    String getCurBranchName() {
        File file = new File(GitletConstant.GITLET_PATH
                + GitletFile.HEAD_FILE.getPath());
        String branchPath = Utils.readContentsAsString(file);
        int index = branchPath.lastIndexOf("/");
        return branchPath.substring(index + 1).trim();
    }

    /**
     * Get the blob by bolb tree id.
     *
     * @param blobId bolb tree id.
     * @return BlobTree.
     */
    BlobTree getBlobById(String blobId) {
        if (null == blobId) {
            return null;
        }
        String path = GitletConstant.GITLET_PATH + GitletFile.OBJECTS.getPath()
                + "/" + blobId.substring(0, 2) + "/" + blobId.substring(2);
        File file = new File(path);
        try {
            return Utils.readObject(file, BlobTree.class);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get the stages from index.
     *
     * @return
     */
    BlobTree getStage() {
        String path = GitletConstant.GITLET_PATH
                + GitletFile.INDEX_FILE.getPath();
        File indexFile = new File(path);
        if (indexFile.exists()) {
            try {
                return Utils.readObject(indexFile, BlobTree.class);
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Get the files by commit id.
     * @param commitId commit id.
     * @return a set of file from commit id.
     */
    Set<Stage> getStageByCommitId(String commitId) {
        if (commitId == null) {
            throw new IllegalArgumentException("Something is wrong.");
        }
        CommitTree commitTree = getCommitById(commitId);
        if (null == commitTree) {
            throw new IllegalArgumentException("Something is wrong.");
        }
        Set<Stage> stages = new HashSet<>();
        if (null != commitTree.getBlobTreeId()) {
            BlobTree blobTree = getBlobById(
                    commitTree.getBlobTreeId());
            if (null != blobTree && null != blobTree.getStages()) {
                stages = new HashSet<>(blobTree.getStages());
            }
        }
        return stages;
    }

    /**
     * Get the blobs in stage.
     * @return a set of file from stage.
     */
    Set<Stage> getStageBlobs() {
        Set<Stage> stages = new HashSet<>();
        BlobTree blobTree = getStage();
        if (null != blobTree && null != blobTree.getStages()) {
            stages = new HashSet<>(blobTree.getStages());
        }
        return stages;
    }

    /**
     * Get the files in work directory.
     * @return a set of file from work directory.
     */
    Set<Stage> getStageFromWork() {
        Set<Stage> stages = new HashSet<>();
        File workRoot = new File(System.getProperty("user.dir"));
        File[] workFiles = workRoot.listFiles();
        for (File work : workFiles) {
            if (work.isFile()) {
                String sha1 = Utils.sha1(work.getPath(),
                        Utils.readContents(work));
                Stage stage = new Stage();
                stage.setBlobId(sha1);
                stage.setStatus("0");
                stage.setFileName(work.getPath());
                stages.add(stage);
            }
        }
        return stages;
    }

    /**
     * Convert set<stage> to map<fileName, stage>.
     * @param stages    the set of stage.
     * @return  the map of stage.
     */
    Map<String, Stage> convertStage(Set<Stage> stages) {
        Map<String, Stage> map = new HashMap<>();
        stages.forEach(stage -> map.put(stage.getFileName(), stage));
        return map;
    }

    /**
     * Read the blob from blob id, by line.
     * @param blobId blob id.
     * @return the content, split by \n.
     */
    String[] readBlobLine(String blobId) {
        byte[] content = readBlob(blobId);
        if (null != content) {
            String text = new String(content, StandardCharsets.UTF_8);
            return text.split("\n");
        }
        return null;
    }

    /**
     * Read the blob from blob id, by line.
     * @param blobId blob id.
     * @return the content.
     */
    byte[] readBlob(String blobId) {

        if (null == blobId || blobId.length() != GitletConstant.SHA_HASH_SIZE) {
            return null;
        }
        String path = GitletConstant.GITLET_PATH + GitletFile.OBJECTS.getPath()
                + "/" + blobId.substring(0, 2) + "/" + blobId.substring(2);
        File file = new File(path);
        if (file.exists()) {
            return Utils.readContents(file);
        }
        return null;
    }
}

package gitlet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;

import gitlet.GitletConstant.GitletFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The Executor for Merge Command.
 *
 * @author Yunshun Zhong
 */
public class MergeExecutor {

    /**
     * QueryExecutor.
     */
    private QueryExecutor queryExecutor;

    /**
     * UpdateExecutor.
     */
    private UpdateExecutor updateExecutor;

    /**
     * The current branch name.
     */
    private String curBranchName;

    /**
     * The current commit id.
     */
    private String curCommitId;

    /**
     * The commit id of given branch.
     */
    private String givenCommitId;

    /**
     * The conflict file, if none, there is no conflict.
     */
    private Set<Stage> conflict = new HashSet<>();

    /**
     * Constructor Method.
     *
     * @param givenCommitId1 the commit id of given branch.
     */
    public MergeExecutor(String givenCommitId1) {
        this.queryExecutor = new QueryExecutor();
        this.updateExecutor = new UpdateExecutor();
        this.curBranchName = queryExecutor.getCurBranchName();
        this.curCommitId = queryExecutor.getCurCommitId();
        this.givenCommitId = givenCommitId1;
    }

    /**
     * Get the current commit id.
     *
     * @return current commit id.
     */
    public String getCurCommitId() {
        return curCommitId;
    }

    /**
     * Execute the merge command.
     *
     * @param curBlobs the blobs of current commit.
     * @param workBlobs the blobs of work directory.
     * @return blobTree which is the stage after merged.
     */
    BlobTree merge(Set<Stage> curBlobs, Set<Stage> workBlobs) {
        String ancestorCommitId = getAncestor();
        if (null == ancestorCommitId) {
            throw new IllegalArgumentException("Commit Message is broken");
        }
        BlobTree mergeTree = null;
        if (ancestorCommitId.equals(givenCommitId)) {
            System.out.println("Given branch is an ancestor of "
                    + "the current branch.");
            System.exit(0);
        } else if (ancestorCommitId.equals(curCommitId)) {
            File branchFile = new File(GitletConstant.GITLET_PATH
                    + GitletFile.REF_HEADS.getPath() + "/" + curBranchName);
            Utils.writeContents(branchFile, givenCommitId);
            CommitTree commitTree = queryExecutor.getCommitById(givenCommitId);
            mergeTree = queryExecutor.getBlobById(
                    commitTree.getBlobTreeId());
            resetWorkDirectory(mergeTree, workBlobs);
            System.out.println("Current branch fast-forwarded.");
        } else {
            Map<String, Stage[]> map = new HashMap<>();
            mergeBlob(map, queryExecutor.getStageByCommitId(ancestorCommitId),
                    0);
            mergeBlob(map, curBlobs, 1);
            mergeBlob(map, queryExecutor.getStageByCommitId(givenCommitId), 2);
            mergeTree = compareAndMerge(map);
            resetWorkDirectory(mergeTree, workBlobs);
            if (!conflict.isEmpty()) {
                System.out.println("Encountered a merge conflict.");
            }
        }
        return mergeTree;
    }

    /**
     * Check the given branch name if same with the current branch.
     * @param givenBranchName the given branch name.
     */
    void checkBranchName(String givenBranchName) {
        if (givenBranchName.equals(curBranchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
    }

    /**
     * Check if there is uncommitted changes.
     *
     * @param stageTree the current stage.
     */
    void checkUnCommitted(BlobTree stageTree) {
        CommitTree commitTree = queryExecutor.getCommitById(curCommitId);
        if (null != commitTree) {
            BlobTree blobTree = new BlobTree();
            String blobId = commitTree.getBlobTreeId();
            if (null != blobId) {
                blobTree = queryExecutor.getBlobById(blobId);
            }
            String stageSha = Utils.sha1(Utils.serialize(stageTree));
            String commitSha = Utils.sha1(Utils.serialize(blobTree));
            if (stageSha.equals(commitSha)) {
                return;
            }
        }
        System.out.println("You have uncommitted changes.");
        System.exit(0);
    }

    /**
     * Commit for merging.
     *
     * @param givenBranchName   the given branch name.
     * @param mergeTree         the blob tree after merging.
     */
    void commitForMerge(String givenBranchName, BlobTree mergeTree) {
        String blobTreeId = updateExecutor.saveContent(
                Utils.serialize(mergeTree), "");
        String message = "Merged " + givenBranchName + " into " + curBranchName
                + ".";
        CommitTree commitTree = new CommitTree();
        commitTree.setBlobTreeId(blobTreeId);
        commitTree.setPreCommitTreeId(curCommitId);
        commitTree.setMessage(message);
        commitTree.setMergeCommitIdOne(curCommitId);
        commitTree.setMergeCommitIdTwo(givenCommitId);
        commitTree.setDate(Instant.now().atZone(ZoneId.of("US/Pacific")));

        updateExecutor.saveCommit(commitTree, curBranchName);
    }

    /**
     * Find the ancestor commit of current branch and given branch.
     *
     * @return the ancestor commit id.
     */
    private String getAncestor() {

        if (curCommitId.equals(givenCommitId)) {
            return curCommitId;
        }

        List<String> curCommits = collectCommitId(curCommitId);
        String tmpCommitId = givenCommitId;
        if (curCommits.contains(tmpCommitId)) {
            return tmpCommitId;
        }
        while (null != tmpCommitId) {
            CommitTree commitTree = queryExecutor.getCommitById(tmpCommitId);
            if (null == commitTree) {
                break;
            }
            tmpCommitId = commitTree.getPreCommitTreeId();
            if (curCommits.contains(tmpCommitId)) {
                return tmpCommitId;
            }
            String mergeId = commitTree.getMergeCommitIdTwo();
            if (curCommits.contains(mergeId)) {
                return mergeId;
            }
        }
        return null;
    }

    /**
     * Get the chain of the commit id from the latest commit id.
     *
     * @param commitId the latest commit id.
     * @return the list of commit id.
     */
    private List<String> collectCommitId(String commitId) {
        List<String> commitIds = new ArrayList<>();
        while (null != commitId) {
            commitIds.add(commitId);
            CommitTree commitTree = queryExecutor.getCommitById(commitId);
            if (null != commitTree) {
                commitId = commitTree.getPreCommitTreeId();
                if (commitTree.getMergeCommitIdTwo() != null) {
                    commitIds.add(commitTree.getMergeCommitIdTwo());
                }
            } else {
                break;
            }
        }
        return commitIds;
    }

    /**
     * Merge blobs from curBlobs, workBlobs, givenBlobs.
     *
     * @param map   the map of merge-blobs,
     *              key-filepath,
     *              value-[ancestorState, curStage, givenStage].
     * @param blobs blobs which need to be merged.
     * @param idx   the idx in blob id in Stage[], only 0, 1, 2.
     */
    private void mergeBlob(Map<String, Stage[]> map, Set<Stage> blobs,
                           int idx) {
        for (Stage stage : blobs) {
            Stage[] stages = map.get(stage.getFileName());
            if (null == stages) {
                stages = new Stage[3];
            }
            stages[idx] = stage;
            map.put(stage.getFileName(), stages);
        }
    }

    /**
     * Compare and merge the blob.
     *
     * @param map the map of merge-blobs,
     *            key-filepath,
     *            value-[ancestorState, curStage, givenStage].
     * @return the blob tree of merged
     */
    private BlobTree compareAndMerge(Map<String, Stage[]> map) {
        BlobTree mergeTree = new BlobTree();
        List<Stage> stages = new ArrayList<>();

        for (Map.Entry<String, Stage[]> blobs : map.entrySet()) {
            Stage[] value = blobs.getValue();
            Stage ancestorStage = value[0];
            Stage curStage = value[1];
            Stage givenStage = value[2];

            if (null == ancestorStage) {
                stages.add(compareAncestorIsNull(curStage, givenStage));
            } else if ((ancestorStage.equals(curStage) && null == givenStage)
                    || (ancestorStage.equals(givenStage) && null == curStage)
                    || (null == givenStage && null == curStage)) {
                continue;
            } else if (ancestorStage.equals(curStage)
                    && !ancestorStage.equals(givenStage)) {
                stages.add(givenStage);
            } else if (!ancestorStage.equals(curStage)
                        && ancestorStage.equals(givenStage)) {
                stages.add(curStage);
            } else if (null != curStage && curStage.equals(givenStage)) {
                stages.add(curStage);
            } else {
                stages.add(compareStage(curStage, givenStage));
            }
        }

        stages.removeIf(Objects::isNull);
        mergeTree.setStages(stages);

        return mergeTree;
    }

    /**
     * Get the stage when ancestor-stage is null.
     *
     * @param curStage   current stage.
     * @param givenStage stage of given branch.
     * @return stage.
     */
    private Stage compareAncestorIsNull(Stage curStage, Stage givenStage) {
        if (null == curStage && null != givenStage) {
            return givenStage;
        } else if (null != curStage && null == givenStage) {
            return curStage;
        } else if (null != curStage && curStage.equals(givenStage)) {
            return curStage;
        } else {
            return compareStage(curStage, givenStage);
        }
    }

    /**
     * Compare tow stage.
     *
     * @param stage         the stage of current branch.
     * @param comparedStage the stage of given branch.
     * @return stage which has been merged.
     */
    private Stage compareStage(Stage stage, Stage comparedStage) {
        String[] curContent = null;
        String[] givenContent = null;
        if (stage != null) {
            curContent = queryExecutor.readBlobLine(stage.getBlobId());
        }
        if (comparedStage != null) {
            givenContent = queryExecutor.readBlobLine(
                    comparedStage.getBlobId());
        }
        if (curContent == null) {
            curContent = new String[1];
        }
        if (givenContent == null) {
            givenContent = new String[1];
        }
        List<String> mergeContents = new ArrayList<>();
        int curIdx = 0;
        int givenIdx = 0;
        boolean isConflict = false;
        while (curIdx < curContent.length) {
            String curText = curContent[curIdx];
            while (givenIdx < givenContent.length) {
                if (null != curText && curText.equals(givenContent[givenIdx])) {
                    mergeContents.add(curText);
                    curIdx++;
                    givenIdx++;
                    break;
                } else {
                    int[] idx = getNextSameLine(curContent, givenContent,
                            curIdx, givenIdx);
                    mergeContents.add("<<<<<<< HEAD");
                    addContent(mergeContents, curIdx, curContent, idx[0]);
                    mergeContents.add("=======");
                    addContent(mergeContents, givenIdx, givenContent, idx[1]);
                    mergeContents.add(">>>>>>>");
                    curIdx = idx[0];
                    givenIdx = idx[1];
                    isConflict = true;
                }
            }
        }
        Stage mergeStage = new Stage();
        StringBuilder content = new StringBuilder();
        mergeContents.forEach(s -> content.append(s).append("\n"));
        String blobId = updateExecutor.saveContent(content.toString().getBytes(
                StandardCharsets.UTF_8), stage.getFileName());
        mergeStage.setFileName(stage.getFileName());
        mergeStage.setStatus("0");
        mergeStage.setBlobId(blobId);
        if (isConflict) {
            conflict.add(mergeStage);
        }
        return mergeStage;
    }

    /**
     * Add conflict content.
     * @param mergeContents merged contents.
     * @param curIdx    current index.
     * @param content   content.
     * @param endIdx    end index.
     */
    private void addContent(List<String> mergeContents, int curIdx,
                                    String[] content, int endIdx) {
        if (content.length == 1 && content[0] == null) {
            return;
        }
        mergeContents.addAll(Arrays.asList(content).subList(curIdx, endIdx));
    }

    /**
     * Get next idx which is same again.
     * @param curContent    blob of current branch.
     * @param givenContent  blob of given branch.
     * @param curIdx        the traversal index of current branch.
     * @param givenIdx      the traversal index of given branch.
     * @return the index of current and given branch which is same again.
     */
    private int[] getNextSameLine(String[] curContent, String[] givenContent,
                                  int curIdx, int givenIdx) {
        int[] idx = {curContent.length, givenContent.length};
        for (int i = curIdx + 1; i < curContent.length; i++) {
            for (int j = givenIdx; j < givenContent.length; j++) {
                if (curContent[i].equals(givenContent[j])) {
                    idx[0] = i;
                    idx[1] = j;
                    return idx;
                }
            }
        }
        return idx;
    }

    /**
     * Rewrite the blob to work directory.
     * @param mergeTree the blobs of merged.
     * @param workBlobs the blobs of work directory.
     */
    private void resetWorkDirectory(BlobTree mergeTree, Set<Stage> workBlobs) {
        Map<String, Stage> merges = queryExecutor.convertStage(
                new HashSet<>(mergeTree.getStages()));
        Map<String, Stage> works = queryExecutor.convertStage(workBlobs);

        for (Map.Entry<String, Stage> merge : merges.entrySet()) {
            Stage mergeStage = merge.getValue();
            Stage workStage = works.get(merge.getKey());
            if (!mergeStage.equals(workStage)) {
                byte[] content = queryExecutor.readBlob(mergeStage.getBlobId());
                File file = new File(mergeStage.getFileName());
                Utils.writeContents(file, content);
            }
        }

        for (Map.Entry<String, Stage> work : works.entrySet()) {
            if (null == merges.get(work.getKey())) {
                Utils.restrictedDelete(work.getKey());
            }
        }
        updateExecutor.saveStage(mergeTree);
    }
}

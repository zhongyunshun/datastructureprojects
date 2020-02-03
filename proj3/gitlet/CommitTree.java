package gitlet;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * The structure of content when gitlet commit.
 * @author Yunshun Zhong
 */
public class CommitTree implements Serializable {

    /**
     * SerialVersionUID.
     */
    private static final long serialVersionUID = -589563105937417500L;

    /**
     * The ID of current blob tree.
     */
    private String blobTreeId;

    /**
     * The ID of previous commit.
     */
    private String preCommitTreeId;

    /**
     * The Date of commit.
     */
    private ZonedDateTime date;

    /**
     * The first commitId of merge.
     */
    private String mergeCommitIdOne;

    /**
     * The second commitId of merge.
     */
    private String mergeCommitIdTwo;

    /**
     * Commit Message.
     */
    private String message;

    /**
     * Get blobTreeId.
     * @return blobTreeId.
     */
    public String getBlobTreeId() {
        return blobTreeId;
    }

    /**
     * Set blobTreeId.
     * @param blobTreeId1 blobTreeId.
     */
    public void setBlobTreeId(String blobTreeId1) {
        this.blobTreeId = blobTreeId1;
    }

    /**
     * Get preCommitTreeId.
     * @return preCommitTreeId.
     */
    public String getPreCommitTreeId() {
        return preCommitTreeId;
    }

    /**
     * Set preCommitTreeId.
     * @param preCommitTreeId1 preCommitTreeId.
     */
    public void setPreCommitTreeId(String preCommitTreeId1) {
        this.preCommitTreeId = preCommitTreeId1;
    }

    /**
     * Get date.
     * @return date.
     */
    public ZonedDateTime getDate() {
        return date;
    }

    /**
     * Set date.
     * @param date1 date.
     */
    public void setDate(ZonedDateTime date1) {
        this.date = date1;
    }

    /**
     * Get mergeCommitIdOne.
     * @return mergeCommitIdOne.
     */
    public String getMergeCommitIdOne() {
        return mergeCommitIdOne;
    }

    /**
     * Set mergeCommitIdOne.
     * @param mergeCommitIdOne1 mergeCommitIdOne.
     */
    public void setMergeCommitIdOne(String mergeCommitIdOne1) {
        this.mergeCommitIdOne = mergeCommitIdOne1;
    }

    /**
     * Get mergeCommitIdTwo.
     * @return mergeCommitIdTwo.
     */
    public String getMergeCommitIdTwo() {
        return mergeCommitIdTwo;
    }

    /**
     * Set mergeCommitIdTwo.
     * @param mergeCommitIdTwo1 mergeCommitIdTwo.
     */
    public void setMergeCommitIdTwo(String mergeCommitIdTwo1) {
        this.mergeCommitIdTwo = mergeCommitIdTwo1;
    }

    /**
     * Get message.
     * @return message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set message.
     * @param message1 message.
     */
    public void setMessage(String message1) {
        this.message = message1;
    }
}

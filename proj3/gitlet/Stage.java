package gitlet;

import java.io.Serializable;

/**
 * The stage model.
 * @author Yunshun Zhong
 */
public class Stage implements Serializable {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 5129187111758439262L;

    /**
     * BlobId, the sha1 hash.
     */
    private String blobId;

    /**
     * Status, 0-add, 1-remove.
     */
    private String status;

    /**
     * FileName, include directory.
     */
    private String fileName;

    /**
     * Get the blobId.
     * @return blobId.
     */
    public String getBlobId() {
        return blobId;
    }

    /**
     * Set blobId.
     * @param blobId1 blobId.
     */
    public void setBlobId(String blobId1) {
        this.blobId = blobId1;
    }

    /**
     * Get status.
     * @return status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Set status.
     * @param status1 status.
     */
    public void setStatus(String status1) {
        this.status = status1;
    }

    /**
     * Get fileName.
     * @return fileName.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Set fileName.
     * @param fileName1 fileName.
     */
    public void setFileName(String fileName1) {
        this.fileName = fileName1;
    }

    /**
     * Compare the BlogTree.
     * @param comparedStage stage which be compared.
     * @return true if blobId and status both equal, else false.
     */
    public boolean equals(Stage comparedStage) {
        if (null == comparedStage) {
            return false;
        } else {
            return blobId.equals(comparedStage.getBlobId())
                    && status.equals(comparedStage.getStatus());
        }
    }
}

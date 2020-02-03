package gitlet;

import java.io.Serializable;
import java.util.List;

/**
 * The List of Stage.
 * @author Yunshun Zhong
 */
public class BlobTree implements Serializable {

    /**
     * SerialVersionUID.
     */
    private static final long serialVersionUID = 5813822694105090675L;

    /**
     * The set of stages.
     */
    private List<Stage> stages;

    /**
     * Get stages.
     * @return stages.
     */
    public List<Stage> getStages() {
        return stages;
    }

    /**
     * Set stages.
     * @param stages1 stages.
     */
    public void setStages(List<Stage> stages1) {
        this.stages = stages1;
    }
}

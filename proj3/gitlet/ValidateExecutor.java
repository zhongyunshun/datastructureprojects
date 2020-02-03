package gitlet;

import java.io.File;
import java.util.Map;
import java.util.Set;

import gitlet.GitletConstant.GitletFile;

/**
 * The executor of validating.
 * @author Yunshun Zhong
 */
public class ValidateExecutor {

    /**
     * Get the commitId by the six digits.
     * @param inputCommitId the commit id which user inputs.
     * @return commit id if exist, else null.
     */
    public String getCommitBySix(String inputCommitId) {
        if (inputCommitId == null || inputCommitId.length() < 6) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        } else if (inputCommitId.length() == GitletConstant.SHA_HASH_SIZE) {
            return inputCommitId;
        }
        String path = GitletConstant.GITLET_PATH + "/"
                + GitletFile.OBJECTS.getPath() + "/"
                + inputCommitId.substring(0, 2);
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        String commitId = inputCommitId.substring(2);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        for (File file : files) {
            if (file.isFile() && file.getName().startsWith(commitId)) {
                return dir.getName() + file.getName();
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
        return null;
    }

    /**
     * Check the number of args should be 2.
     *
     * @param args the inputs.
     */
    public void checkArgsShouldBeTwo(String[] args) {
        if (null == args || args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    /**
     * Check the number of args should be 1.
     *
     * @param args the inputs.
     */
    public void checkArgsShouldBeOne(String[] args) {
        if (null == args || args.length != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    /**
     * Check and get the file from args.
     *
     * @param args     the inputs of args.
     * @param workPath the work directory.
     * @return the file for args.
     */
    public File checkFile(String[] args, String workPath) {
        checkArgsShouldBeTwo(args);
        String fileName = args[1];
        File file = new File(workPath + "/" + fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        return file;
    }

    /**
     * Check if the branch can be checkout.
     * @param curBlobs the current branch stages, not null.
     * @param workBlobs work files in current work, not null.
     */
    void canBeCheckout(Set<Stage> curBlobs, Set<Stage> workBlobs) {
        for (Stage work : workBlobs) {
            boolean exist = false;
            for (Stage stage : curBlobs) {
                if (stage.equals(work)) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it or add it first.");
                System.exit(0);
            }
        }
    }

    /**
     * Check if the branch can be checkout.
     * @param curBlobs the current branch stages, not null.
     * @param workBlobs work files in current work, not null.
     * @param branchBlobs files in checkout branch, not null.
     */
    void canBeCheckout(Map<String, Stage> curBlobs,
            Map<String, Stage> workBlobs, Map<String, Stage> branchBlobs) {

        for (Map.Entry<String, Stage> branch : branchBlobs.entrySet()) {
            Stage branchBlob = branch.getValue();
            Stage workBlob = workBlobs.get(branch.getKey());
            Stage commitBlob = curBlobs.get(branch.getKey());
            if (workBlob != null && !workBlob.equals(branchBlob)
                    && commitBlob == null) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it or add it first.");
                System.exit(0);
            }
        }
    }

    /**
     * Print the error message and exit.
     */
    public void exitByFile() {
        System.out.println("File does not exist in that commit.");
        System.exit(0);
    }

    /**
     * Check the args length should be three.
     * @param args args.
     */
    void checkArgsShouldBeThree(String[] args) {
        if (null == args || args.length != 3) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    /**
     * Check the add-remote command.
     * @param args args.
     */
    void checkAddRemoteCommand(String[] args) {
        checkArgsShouldBeThree(args);
        String remotePath = args[2];
        if (remotePath.indexOf(".gitlet") <= 0) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        File file = new File(GitletConstant.GITLET_PATH
                + GitletFile.REF_REMOTES.getPath() + "/" + args[1]);
        if (file.exists()) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }
    }

    /**
     * Check the remote name if exist, if no, exit.
     * @param remoteName remote name.
     */
    void checkRemoteNameExist(String remoteName) {
        File file = new File(GitletConstant.GITLET_PATH
                + GitletFile.REF_REMOTES.getPath() + "/" + remoteName);
        if (!file.exists()) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }
    }

    /**
     * Check the remote path if exist, if no exit.
     * @param remotePath remote path.
     */
    void checkRemoteExist(String remotePath) {
        File file = new File(remotePath);
        if (!file.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
    }
}

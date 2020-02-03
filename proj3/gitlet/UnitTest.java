package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import ucb.junit.textui;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The suite of all JUnit tests for the gitlet package.
 *
 * @author Yunshun Zhong
 */
public class UnitTest {

    /**
     * The controller of gitlet.
     */
    private Controller controller = new Controller();

    /**
     * Run the JUnit tests in the loa package. Add xxxTest.class entries to
     * the arguments of runClasses to run other JUnit tests.
     */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /**
     * FileTest the command matcher.
     */
    @Test
    public void matcherTest() {
        String initOk = "init";
        String initFail = "init add";
        String addOk = "add base.txt";
        String addFail = "add";

        assertTrue("the matcher of init is wrong",
                Pattern.matches(Command.INIT.getPattern(), initOk));
        assertFalse("the matcher of init is wrong",
                Pattern.matches(Command.INIT.getPattern(), initFail));

        assertTrue("the matcher of add is wrong",
                Pattern.matches(Command.ADD.getPattern(), addOk));
        assertFalse("the matcher of add is wrong",
                Pattern.matches(Command.ADD.getPattern(), addFail));
    }

    @Test
    public void compareBlobTree() {
        List<Stage> list1 = new ArrayList<>();
        List<Stage> list2 = new ArrayList<>();
        BlobTree blobTree1 = new BlobTree();
        blobTree1.setStages(list1);
        BlobTree blobTree2 = new BlobTree();
        blobTree2.setStages(list2);
        String sha1 = Utils.sha1(Utils.serialize(blobTree1));
        String sha2 = Utils.sha1(Utils.serialize(blobTree2));

        assertTrue("BlobTree1 and BlobTree2 is not same", sha1.equals(sha2));
    }

    @Test
    public void doInit() {
        String path = GitletConstant.GITLET_PATH;
        File file = new File(path);
        if (file.exists()) {
            deleteFile(file);
        }
        controller.doInit();
        file = new File(path);
        assertTrue(file.exists());
    }

    /**
     * A dummy base to avoid complaint.
     */
    @Test
    public void placeholderTest() {
    }

    /**
     * Delete all file in file
     * @param file the given file.
     */
    void deleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteFile(f);
                    } else {
                        f.delete();
                    }
                }
            }
        }
        file.delete();
    }

}



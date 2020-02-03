package gitlet;

/**
 * Define ths constant for gitlet, contains the files in .gitlet directory.
 * @author Yunshun Zhong.
 */
public class GitletConstant {

    /**
     * The base path for gitlet.
     */
    public static final String GITLET_PATH = System.getProperty("user.dir")
            + "/.gitlet";

    /**
     * The default branch.
     */
    public static final String DEFAULT_BRANCH = "master";

    /**
     * The length of sha hash.
     */
    public static final int SHA_HASH_SIZE = 40;

    /**
     * The path for files in .gitlet directory.
     */
    enum GitletFile {
        LOGS("/logs", "directory", "logs directory"),
        LOG_HEADS("/logs/heads", "directory", "local logs directory"),
        LOG_REFS("/logs/refs", "directory", "remote logs directory"),
        OBJECTS("/objects", "directory", "logs directory"),
        REFS("/refs", "directory", "refs directory"),
        REF_HEADS("/refs/heads", "directory", "local refs directory"),
        REF_REMOTES("/refs/remotes", "directory", "remote refs directory"),

        LOG_HEAD_FILE("/logs/HEAD", "file", "logs head file"),
        CONFIG_FILE("/config", "file", "config file"),
        HEAD_FILE("/HEAD", "file", "HEAD file"),
        INDEX_FILE("/index", "file", "index file");

        /**
         * The file path.
         */
        private String path;

        /**
         * The file type, file or directory.
         */
        private String type;

        /**
         * Describe the file.
         */
        private String description;

        /**
         * Constructor method for GitletFile.
         * @param path1 file path.
         * @param type1 file type.
         * @param description1 describe the file.
         */
        GitletFile(String path1, String type1, String description1) {
            this.path = path1;
            this.type = type1;
            this.description = description1;
        }

        /**
         * Get the path.
         * @return path.
         */
        public String getPath() {
            return path;
        }

        /**
         * Get the type.
         * @return type.
         */
        public String getType() {
            return type;
        }
    }
}

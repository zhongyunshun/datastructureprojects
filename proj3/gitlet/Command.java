package gitlet;

import java.util.regex.Pattern;

/** A Command is pair (<pattern>, <processor>), where <pattern> is a
 *  Matcher that matches instances of a particular command, and
 *  <processor> is a functional object whose .accept method takes a
 *  successfully matched Matcher and performs some operation.
 *
 * @author Yunshun Zhong
 */
public enum Command {

    PRINT("print", "print\\s+([\\s\\S]+)$", "doPrint"),
    INIT("init", "init$", "doInit"),
    ADD("add", "add\\s+([\\s\\S]+)", "doAdd"),
    COMMIT("commit", "commit\\s+([\\s\\S]*)", "doCommit"),
    RM("rm", "rm\\s+([\\s\\S]+)", "doRemove"),
    LOG("log", "log$", "doLog"),
    GLOBAL_LOG("global-log", "global-log$", "doGlobalLog"),
    FIND("find", "find\\s+([\\s\\S]+)", "doFind"),
    STATUS("status", "status$", "doStatus"),
    CHECKOUT("checkout", "checkout\\s+([\\s\\S]+)", "doCheckout"),
    BRANCH("branch", "branch\\s+([\\s\\S]+)", "doBranch"),
    RM_BRANCH("rm-branch", "rm-branch\\s+([\\s\\S]+)", "doRemoveBranch"),
    RESET("reset", "reset\\s+([\\s\\S]+)", "doReset"),
    MERGE("merge", "merge\\s+([\\s\\S]+)", "doMerge"),
    ADD_REMOTE("add-remote", "add-remote\\s+([\\s\\S]+)(\\.)gitlet$",
            "doAddRemote"),
    RM_REMOTE("rm-remote", "rm-remote\\s+([\\s\\S]+)", "doRemoveRemote"),
    PUSH("push", "push\\s+([\\s\\S]+)", "doPush"),
    FETCH("fetch", "fetch\\s+([\\s\\S]+)", "doFetch"),
    PULL("pull", "pull\\s+([\\s\\S]+)", "doPull");

    /**
     * Command Name.
     */
    private String commandName;

    /**
     * A matcher, like 'init$'.
     */
    private String pattern;

    /**
     * A functional name, like 'doInit'.
     */
    private String processor;

    /**
     * Constructor of Command.
     * @param commandName1 commandName.
     * @param pattern1 pattern.
     * @param processor1 processor.
     */
    Command(String commandName1, String pattern1, String processor1) {
        this.commandName = commandName1;
        this.pattern = pattern1;
        this.processor = processor1;
    }

    /**
     * Get commandName.
     * @return commandName
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * Get pattern.
     * @return pattern.
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Get processor.
     * @return processor.
     */
    public String getProcessor() {
        return processor;
    }

    /**
     * Get Command by commandName.
     * @param commandName the name of command.
     * @return Command if exist, else null.
     */
    public static Command getCommandByName(String commandName) {
        for (Command command : Command.values()) {
            if (command.getCommandName().equals(commandName)) {
                return command;
            }
        }
        return null;
    }

    /**
     * Check if the command matches.
     * @param args    The inputs of user's command.
     * @return true if the command is correct, else false.
     */
    public boolean checkCommand(String[] args) {
        if (args == null || args.length == 0) {
            return false;
        }
        StringBuilder sb = new StringBuilder("");
        for (String arg : args) {
            sb.append(arg).append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        return Pattern.matches(this.getPattern(), sb.toString());
    }
}

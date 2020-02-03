package gitlet;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Yunshun Zhong
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {
        Command command = checkCommand(args);

        boolean gitletExist = checkExistGitlet();

        if (Command.INIT.getCommandName().equals(command.getCommandName())) {
            if (gitletExist) {
                System.out.println("A Gitlet version-control system already "
                        + "exists in the current directory.");
            } else {
                Controller controller = new Controller();
                controller.doInit();
            }
        } else if (gitletExist) {
            Controller controller = new Controller();
            try {
                Method method = controller.getClass().getMethod(
                        command.getProcessor(), String[].class);
                method.invoke(controller, (Object) args);
            } catch (NoSuchMethodException | IllegalAccessException
                    | InvocationTargetException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        } else {
            System.out.println("Not in an initialized Gitlet directory.");
        }
    }

    /**
     * Check if the inputs legal.
     *
     * @param args the inputs.
     * @return command if the inputs is legal, else exit.
     */
    private static Command checkCommand(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String commandName = args[0];

        Command command = Command.getCommandByName(commandName);
        if (null == command) {
            System.out.println("No command with that name exists.");
            System.exit(0);
        }

        boolean commandLegal = command.checkCommand(args);
        if (!commandLegal) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }

        return command;
    }

    /**
     * Check if exist '.gitlet' in the executing path.
     * @return true if exist .gitlet directory, else false.
     */
    private static boolean checkExistGitlet() {
        File directory = new File(System.getProperty("user.dir"));
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (".gitlet".equals(file.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

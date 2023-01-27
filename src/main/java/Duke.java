import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Predicate;

public class Duke {
    private static final int INDENT_LEVEL = 4;

    private TaskList tasks;

    public Duke() {
        try {
            this.tasks = TaskList.load();
        } catch(DukeLoadException e) {
            this.say(e.getDukeMessage());
            this.tasks = new TaskList();
        }
    }

    public void run() {
        String logo = " ____        _        \n"
                + "|  _ \\ _   _| | _____ \n"
                + "| | | | | | | |/ / _ \\\n"
                + "| |_| | |_| |   <  __/\n"
                + "|____/ \\__,_|_|\\_\\___|\n";
        this.say("Hello from\n" + logo);

        Scanner scanner = new Scanner(System.in);

        whileLoop:
        while (true) {
            if (!scanner.hasNextLine()) {
                continue;
            }
            
            String input = scanner.nextLine();
            Command command = new Command(input);
        
            try {
                switch (command.baseCommand) {
                    case "todo":
                        Duke.addTodo(command, tasks);
                        break;
                    case "deadline":
                        Duke.addDeadline(command, tasks);
                        break;
                    case "event":
                        Duke.addEvent(command, tasks);
                        break;
                    case "mark":
                        Duke.mark(command, tasks);
                        break;
                    case "unmark":
                        Duke.unmark(command, tasks);
                        break;
                    case "delete":
                        Duke.delete(command, tasks);
                        break;
                    case "list":
                        Duke.list(command, tasks);
                        break;
                    case "quit":
                    case "exit":
                    case "bye":
                        this.say("Bye. Hope to see you again soon!");
                        break whileLoop;
                    default:
                        throw new DukeInvalidCommandException();
                }

                tasks.save();
            } catch (DukeException e) {
                this.say(e.getDukeMessage());
            }
        }
        
        scanner.close();
    }

    public static void main(String[] args) {
        new Duke().run();
    }

    private void say(String whatToSay) {
        String indentation = " ".repeat(Duke.INDENT_LEVEL);
        String horizontalLine = "_".repeat(60);
        String indentedInput = whatToSay.replaceAll("(?<=^|\n)", indentation);
        
        System.out.println(indentation + horizontalLine);
        System.out.println(indentedInput);
        System.out.println(indentation + horizontalLine + '\n');
    }

    private static void addTodo(Command command, TaskList tasks) throws DukeInvalidArgumentException {
        if (command.hasEmptyBody()) {
            throw new DukeInvalidArgumentException("The description of a todo cannot be empty.");
        }
        
        String description = command.body;
        Task task = new TaskTodo(description);
        tasks.add(task);
        this.say(
            "Got it. I've added this task:\n"
                + "  " + task.toString() + "\n"
                + tasks.getStatus()
        );
    }

    private static void addDeadline(Command command, TaskList tasks) throws DukeInvalidArgumentException {
        if (command.hasEmptyBody()) {
            throw new DukeInvalidArgumentException("The description of a deadline cannot be empty.");
        }
        if (!command.namedParameters.containsKey("by")) {
            throw new DukeInvalidArgumentException("The \"/by\" parameter of a deadline is missing.");
        }
        if (command.namedParameters.get("by").isEmpty()) {
            throw new DukeInvalidArgumentException("The \"/by\" parameter of a deadline cannot be empty.");
        }
        
        try {
            String description = command.body;
            Task task = new TaskDeadline(description, command.namedParameters.get("by"));
            tasks.add(task);
            this.say(
                "Got it. I've added this task:\n"
                    + "  " + task.toString() + "\n"
                    + tasks.getStatus()
            );
        } catch (DateTimeParseException e) {
            throw new DukeInvalidArgumentException(
                "The \"/by\" value must be in the form \"yyyy-mm-dd\" (eg. 2019-10-15)."
            );
        }
    }

    private static void addEvent(Command command, TaskList tasks) throws DukeInvalidArgumentException {
        if (command.hasEmptyBody()) {
            throw new DukeInvalidArgumentException("The description of an event cannot be empty.");
        }
        if (!command.namedParameters.containsKey("from")) {
            throw new DukeInvalidArgumentException("The \"/from\" parameter of an event is missing.");
        }
        if (command.namedParameters.get("from").isEmpty()) {
            throw new DukeInvalidArgumentException("The \"/from\" parameter of an event cannot be empty.");
        }
        if (!command.namedParameters.containsKey("to")) {
            throw new DukeInvalidArgumentException("The \"/to\" parameter of an event is missing.");
        }
        if (command.namedParameters.get("to").isEmpty()) {
            throw new DukeInvalidArgumentException("The \"/to\" parameter of an event cannot be empty.");
        }

        try {
            String description = command.body;
            Task task = new TaskEvent(
                description, 
                command.namedParameters.get("from"), 
                command.namedParameters.get("to")
            );
            tasks.add(task);
            this.say(
                "Got it. I've added this task:\n"
                    + "  " + task.toString() + "\n"
                    + tasks.getStatus()
            );
        } catch (DateTimeParseException e) {
            throw new DukeInvalidArgumentException(
                "The \"/from\" and \"/to\" values must be in the form \"yyyy-mm-dd\" (eg. 2019-10-15)."
            );
        }
    }

    private static void mark(Command command, TaskList tasks) throws DukeInvalidArgumentException {
        if (command.hasEmptyBody()) {
            throw new DukeInvalidArgumentException("No task index given.");
        }
        
        Predicate<String> isNumeric = str -> str.matches("^-?\\d+$");
        int taskIndex = Optional.of(command.body)
            .filter(isNumeric)
            .map(body -> Integer.parseInt(body) - 1)
            .filter(i -> i >= 0)
            .orElseThrow(() -> new DukeInvalidArgumentException(
                "Invalid task index. Index needs to be a positive integer."
            ));
        Task task = Optional.of(taskIndex)
            .filter(index -> index < tasks.size())
            .map(index -> tasks.get(index))
            .orElseThrow(() -> new DukeInvalidArgumentException(
                "Task index is beyond the range of the task list."
            ));
        task.markAsDone();
        this.say(
            "Nice! I've marked this task as done:\n"
                + "  " + task.toString()
        );
    }

    private static void unmark(Command command, TaskList tasks) throws DukeInvalidArgumentException {
        if (command.hasEmptyBody()) {
            throw new DukeInvalidArgumentException("No task index given.");
        }
        
        Predicate<String> isNumeric = str -> str.matches("^-?\\d+$");
        int taskIndex = Optional.of(command.body)
            .filter(isNumeric)
            .map(body -> Integer.parseInt(body) - 1)
            .filter(i -> i >= 0)
            .orElseThrow(() -> new DukeInvalidArgumentException(
                "Invalid task index. Index needs to be a positive integer."
            ));
        Task task = Optional.of(taskIndex)
            .filter(index -> index < tasks.size())
            .map(index -> tasks.get(index))
            .orElseThrow(() -> new DukeInvalidArgumentException(
                "Task index is beyond the range of the task list."
            ));
        task.markAsNotDone();
        this.say(
            "OK, I've marked this task as not done yet:\n"
                + "  " + task.toString()
        );
    }

    private static void delete(Command command, TaskList tasks) throws DukeInvalidArgumentException {
        if (command.hasEmptyBody()) {
            throw new DukeInvalidArgumentException("No task index given.");
        }
        
        Predicate<String> isNumeric = str -> str.matches("^-?\\d+$");
        int taskIndex = Optional.of(command.body)
            .filter(isNumeric)
            .map(body -> Integer.parseInt(body) - 1)
            .filter(i -> i >= 0)
            .orElseThrow(() -> new DukeInvalidArgumentException(
                "Invalid task index. Index needs to be a positive integer."
            ));
        Task task = Optional.of(taskIndex)
            .filter(index -> index < tasks.size())
            .map(index -> tasks.get(index))
            .orElseThrow(() -> new DukeInvalidArgumentException(
                "Task index is beyond the range of the task list."
            ));
        tasks.remove(taskIndex);
        this.say(
            "Noted. I've removed this task:\n"
                + "  " + task.toString()
        );
    }

    private static void list(Command command, TaskList tasks) {
        this.say(tasks.toString());
    }
}

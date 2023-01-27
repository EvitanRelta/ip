import java.util.HashMap;

public class Parser {
    private final String fullCommand;
    public final String baseCommand;
    public final String body;
    public final HashMap<String, String> namedParameters = new HashMap<>();

    public Parser(String command) {
        this.fullCommand = command;
        String[] temp = command.split(" +", 2);
        this.baseCommand = temp[0];
        String rawBody = temp.length > 1 ? temp[1].trim() : "";
        String body = "";

        boolean isFirstElement = true;
        for (String str : rawBody.split("\\s+/")) {
            if (isFirstElement) {
                isFirstElement = false;
                body = str;
                continue;
            }
            String[] temp2 = str.split(" +", 2);
            namedParameters.put(temp2[0], temp2.length > 1 ? temp2[1] : "");
        }
        this.body = body;
    }

    public boolean hasEmptyBody() {
        return this.body.isEmpty();
    }

    @Override
    public String toString() {
        return fullCommand;
    }
}

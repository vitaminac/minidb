import java.io.Serializable;

public class Command implements Serializable {
    public enum CommandType implements Serializable {
        GET, SET, DEL, EXISTS, PING, QUIT
    }

    private final CommandType type;
    private final Object extras;

    private Command(CommandType type, Object extras) {
        this.type = type;
        this.extras = extras;
    }

    public CommandType getType() {
        return type;
    }

    public Object getExtras() {
        return extras;
    }

    public static Command createPingCommand() {
        return new Command(CommandType.PING, null);
    }

    public static Command createGetCommand(Object key) {
        return new Command(CommandType.GET, key);
    }

    public static Command createSetCommand(Object key, Object value) {
        return new Command(CommandType.SET, new DictEntry(key, value));
    }

    public static Command createDelCommand(Object key) {
        return new Command(CommandType.DEL, key);
    }

    public static Command createExistsCommand(Object key) {
        return new Command(CommandType.EXISTS, key);
    }

    public static Command createQuitCommand() {
        return new Command(CommandType.QUIT, null);
    }
}

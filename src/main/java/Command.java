import java.io.Serializable;
import java.util.AbstractMap;

public class Command implements Serializable {
    public enum CommandType implements Serializable {
        PING,
        KEYS,
        GET,
        SET,
        EXISTS,
        DEL,
        EXPIRE,
        LEN,
        FIRST,
        LAST,
        LPUSH,
        LPOP,
        RPUSH,
        RPOP,
        QUIT
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

    public static Command createKeysCommand(String pattern) {
        return new Command(CommandType.KEYS, pattern);
    }

    public static Command createGetCommand(Object key) {
        return new Command(CommandType.GET, key);
    }

    public static Command createSetCommand(Object key, Object value) {
        return new Command(CommandType.SET, new AbstractMap.SimpleEntry<>(key, value));
    }

    public static Command createDelCommand(Object key) {
        return new Command(CommandType.DEL, key);
    }

    public static Command createExistsCommand(Object key) {
        return new Command(CommandType.EXISTS, key);
    }

    public static Command createLengthCommand(Object key) {
        return new Command(CommandType.LEN, key);
    }

    public static Command createExpireCommand(Object key, long milliseconds) {
        return new Command(CommandType.EXPIRE, new AbstractMap.SimpleEntry<>(key, milliseconds));
    }

    public static Command createLeftPushCommand(Object key, Object value) {
        return new Command(CommandType.LPUSH, new AbstractMap.SimpleEntry<>(key, value));
    }

    public static Command createLeftPopCommand(Object key) {
        return new Command(CommandType.LPOP, key);
    }

    public static Command createRightPushCommand(Object key, Object value) {
        return new Command(CommandType.RPUSH, new AbstractMap.SimpleEntry<>(key, value));
    }

    public static Command createRightPopCommand(Object key) {
        return new Command(CommandType.RPOP, key);
    }

    public static Command createQuitCommand() {
        return new Command(CommandType.QUIT, null);
    }
}

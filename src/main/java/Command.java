import java.io.Serializable;

public class Command implements Serializable {
    public enum CommandType implements Serializable {
        PING,
        SELECT,
        KEYS,
        GET,
        SET,
        EXISTS,
        DEL,
        EXPIRE,
        HKEYS,
        HGET,
        HSET,
        HEXISTS,
        HDEL,
        LEN,
        FIRST,
        LAST,
        LPUSH,
        LPOP,
        RPUSH,
        RPOP,
        TYPE,
        QUIT
    }

    private final CommandType type;
    private final Object extras;

    public Command(CommandType type, Object extras) {
        this.type = type;
        this.extras = extras;
    }

    public CommandType getType() {
        return type;
    }

    public Object getExtras() {
        return extras;
    }

    public static final Command PING = new Command(CommandType.PING, null);
    public static final Command QUIT = new Command(CommandType.QUIT, null);
}

import java.io.Serializable;

public class Reply implements Serializable {
    public enum ReplyType implements Serializable {
        OK, ERROR
    }

    private final ReplyType type;
    private final Object extras;

    private Reply(ReplyType type, Object extras) {
        this.type = type;
        this.extras = extras;
    }

    public boolean isOk() {
        return this.type == ReplyType.OK;
    }

    public Object getExtras() {
        return this.extras;
    }

    public static Reply fail(String msg) {
        return new Reply(ReplyType.ERROR, msg);
    }

    public static Reply ok(Object extras) {
        return new Reply(ReplyType.OK, extras);
    }

    public static final Reply EXPIRE_SYNTAX_ERROR = Reply.fail("Syntax error: EXPIRE key milliseconds");
    public static final Reply UNKNOWN_COMMAND = Reply.fail("ERROR: Unknown Command");
    public static final Reply QUITTING = Reply.ok("QUITTING");
    public static final Reply OK = Reply.ok("OK");
    public static final Reply YES = Reply.ok("YES");
    public static final Reply NO = Reply.ok("NO");
    public static final Reply PONG = Reply.ok("PONG");
}

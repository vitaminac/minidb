import java.io.Serializable;

public class Result implements Serializable {
    public enum ResultType implements Serializable {
        OK, ERROR
    }

    private final ResultType type;
    private final Object extras;

    private Result(ResultType type, Object extras) {
        this.type = type;
        this.extras = extras;
    }

    public boolean isOk() {
        return this.type == ResultType.OK;
    }

    public Object getExtras() {
        return this.extras;
    }

    public static Result fail(String msg) {
        return new Result(ResultType.ERROR, msg);
    }

    public static Result ok(Object extras) {
        return new Result(ResultType.OK, extras);
    }

    public static final Result EXPIRE_SYNTAX_ERROR = Result.fail("Syntax error: EXPIRE key milliseconds");
}

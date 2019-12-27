package scheduler;

public class TestException extends Exception {
    private final Object value;

    public TestException(String message) {
        super(message);
        this.value = message;
    }

    public TestException(Integer value) {
        super(String.valueOf(value));
        this.value = value;
    }

    public Object getValue() {
        return this.value;
    }
}

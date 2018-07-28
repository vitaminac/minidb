package util;

public class Logger {
    private final Class<?> clazz;

    public Logger(Class<?> clazz) {
        this.clazz = clazz;
    }

    public void error(Throwable e) {
        e.printStackTrace();
    }
}

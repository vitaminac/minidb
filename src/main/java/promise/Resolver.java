package promise;

public interface Resolver<T> {
    void resolve(T result);
}
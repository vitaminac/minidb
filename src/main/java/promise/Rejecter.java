package promise;

public interface Rejecter {
    void reject(Throwable reason);
}
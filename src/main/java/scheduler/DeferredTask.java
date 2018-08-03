package scheduler;

import promise.Promise;

public interface DeferredTask<T> {
    void start(Promise<T> promise);
}
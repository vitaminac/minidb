package scheduler;

import promise.Rejecter;
import promise.Resolver;

public interface DeferredTask<T> {
    void start(Resolver<T> resolver, Rejecter rejecter);
}
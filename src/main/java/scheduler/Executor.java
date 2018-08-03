package scheduler;

import promise.Rejecter;
import promise.Resolver;

public interface Executor<T> {
    void execute(Resolver<T> resolver, Rejecter rejecter);
}
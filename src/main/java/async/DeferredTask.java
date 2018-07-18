package async;

import async.promise.Rejecter;
import async.promise.Resolver;

public interface DeferredTask<T> {
    void start(Resolver<T> resolver, Rejecter rejecter);
}
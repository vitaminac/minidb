package promise;

import scheduler.Delegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface Promise<Thing> {
    boolean isDone();

    boolean isFailed();

    boolean isPending();

    Optional<Thing> getResult() throws Exception;

    void resolve(Thing thing);

    void reject(Throwable reason);

    <R> Promise<R> onFulfilled(FulfilledHandler<? super Thing, R> handler);

    <R> Promise<R> onRejected(FailureHandler<R> handler);

    <R> Promise<R> then(FulfilledHandler<? super Thing, R> fulfilledHandler, FailureHandler<R> failureHandler);

    <R> Promise<R> then(Delegate<Thing, Promise<R>, Exception> delegate);

    void onFinally(DoneCallback callback);

    static <T> Promise<List<T>> all(List<Promise<T>> promises) {
        return DeferredPromise.from(promise -> {
            List<T> results = new ArrayList<>(promises.size());
            Promise<Void> chain = DeferredPromise.from(p -> p.resolve(null));
            for (Promise<T> next : promises) {
                chain = chain.then(none -> DeferredPromise.from(p -> {
                    next.onFulfilled(r -> {
                        results.add(r);
                        p.resolve(null);
                        return null;
                    });
                }));
            }
            chain.onFinally(() -> {
                promise.resolve(results);
            });
        });
    }
}

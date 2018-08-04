package promise;

import scheduler.Delegate;

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
}

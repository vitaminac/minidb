package async.promise;

import async.DeferredTask;
import async.Task;

import java.util.ArrayList;
import java.util.List;

public class Promise<T> implements Resolver<T>, Rejecter {

    private static class NextPromise<P, R> extends Promise<R> {
        private final OnFulfilledHandler<P, R> onFulfilledHandler;
        private final OnRejectedHandler<R> onRejectedHandler;

        private NextPromise(OnFulfilledHandler<P, R> onFulfilledHandler, OnRejectedHandler<R> onRejectedHandler) {
            this.onFulfilledHandler = onFulfilledHandler;
            this.onRejectedHandler = onRejectedHandler;
        }

        private void propagate(P value) {
            if (this.onFulfilledHandler != null) {
                try {
                    this.resolve(onFulfilledHandler.pass(value));
                } catch (Exception e) {
                    this.reject(e);
                }
            } else {
                this.resolve(null);
            }
        }

        private void propagate(Throwable reason) {
            if (this.onRejectedHandler != null) {
                try {
                    this.resolve(this.onRejectedHandler.pass(reason));
                } catch (Exception e) {
                    this.reject(e);
                }
            } else {
                this.reject(reason);
            }
        }
    }

    public static <T> Promise<T> create(DeferredTask<T> callBack) {
        final Promise<T> promise = new Promise<>();
        callBack.start(promise, promise);
        return promise;
    }

    public enum State {
        Pending, // initial state, neither fulfilled nor rejected.
        Fulfilled, // meaning that the operation completed successfully.
        Rejected // meaning that the operation failed.
    }

    private State state;
    private Object result;
    private List<NextPromise<? super T, ?>> chains = new ArrayList<>();

    private Promise() {
        this.state = State.Pending;
    }

    public void onFinally(Task task) {
        this.then(new OnFulfilledHandler<T, Object>() {
            @Override
            public final Object pass(Object result) {
                task.doTask();
                return null;
            }
        }, new OnRejectedHandler<Object>() {
            @Override
            public final Object pass(Throwable e) {
                task.doTask();
                return null;
            }
        });
    }

    public <R> Promise<R> onFulfilled(OnFulfilledHandler<? super T, R> handler) {
        return this.then(handler, null);
    }

    public <R> Promise<R> onRejected(OnRejectedHandler<R> handler) {
        return this.then(null, handler);
    }

    @Override
    public void reject(Throwable reason) {
        this.state = State.Rejected;
        this.result = reason;
        for (NextPromise<? super T, ?> next : this.chains) {
            next.propagate(reason);
        }
    }

    @Override
    public void resolve(T result) {
        this.state = State.Fulfilled;
        this.result = result;
        for (NextPromise<? super T, ?> next : this.chains) {
            next.propagate(result);
        }
    }

    public <R> Promise<R> then(OnFulfilledHandler<? super T, R> onFulfilledHandler, OnRejectedHandler<R> onRejectedHandler) {
        final NextPromise<? super T, R> promise = new NextPromise<>(onFulfilledHandler, onRejectedHandler);
        this.chains.add(promise);
        return promise;
    }
}

package promise;

import scheduler.Delegate;
import scheduler.Executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeferredPromise<D> implements Promise<D> {
    public static <D> DeferredPromise<D> from(Executor<Promise<D>> promiser) {
        final DeferredPromise<D> promise = new DeferredPromise<>();
        try {
            promiser.execute(promise);
        } catch (Exception e) {
            promise.reject(e);
        }
        return promise;
    }

    private State state;
    private Object result;
    private List<PipedPromise<? super D, ?>> chains = new ArrayList<>();

    public DeferredPromise() {
        this.state = State.Pending;
    }

    @Override
    public void onFinally(DoneCallback callback) {
        this.then(result -> {
            callback.doCallback();
            return null;
        }, (FailureHandler<Void>) e -> {
            callback.doCallback();
            return null;
        });
    }

    @Override
    public <R> Promise<R> onFulfilled(FulfilledHandler<? super D, R> handler) {
        return this.then(handler, null);
    }

    @Override
    public <R> Promise<R> onRejected(FailureHandler<R> handler) {
        return this.then(null, handler);
    }

    @Override
    public void reject(Throwable reason) {
        this.state = State.Rejected;
        this.result = reason;
        for (PipedPromise<? super D, ?> next : this.chains) {
            next.pipe(reason);
        }
    }

    @Override
    public boolean isDone() {
        return this.state == State.Fulfilled;
    }

    @Override
    public boolean isFailed() {
        return this.state == State.Rejected;
    }

    @Override
    public boolean isPending() {
        return this.state == State.Pending;
    }

    @Override
    public Optional<D> getResult() throws Exception {
        if (this.result == null) {
            return Optional.empty();
        }
        switch (this.state) {
            case Fulfilled:
                return Optional.of((D) this.result);
            case Rejected:
                throw (Exception) this.result;
            default:
                return Optional.empty();
        }
    }

    @Override
    public void resolve(D result) {
        this.state = State.Fulfilled;
        this.result = result;
        for (PipedPromise<? super D, ?> next : this.chains) {
            next.pipe(result);
        }
    }

    @Override
    public <R> Promise<R> then(FulfilledHandler<? super D, R> fulfilledHandler, FailureHandler<R> failureHandler) {
        if (this.isPending()) {
            final PipedValuePromise<? super D, R> promise = new PipedValuePromise<>(fulfilledHandler, failureHandler);
            this.chains.add(promise);
            return promise;
        } else {
            final DeferredPromise<R> ret = new DeferredPromise<>();
            try {
                final Optional<D> result = this.getResult();
                R nextResult;
                if (result.isPresent()) {
                    nextResult = fulfilledHandler.doNext(result.get());
                } else {
                    nextResult = fulfilledHandler.doNext(null);
                }
                ret.resolve(nextResult);
            } catch (Exception e) {
                ret.reject(e);
            }
            return ret;
        }
    }

    @Override
    public <R> Promise<R> then(Delegate<D, Promise<R>, Exception> delegate) {
        if (this.isPending()) {
            final DelegatingPromise<D, R> promise = DelegatingPromise.from(delegate);
            this.chains.add(promise);
            return promise;
        } else {
            try {
                final Optional<D> result = this.getResult();
                if (result.isPresent()) {
                    return delegate.delegate(result.get());
                } else {
                    return delegate.delegate(null);
                }
            } catch (Exception e) {
                final DeferredPromise<R> ret = new DeferredPromise<>();
                ret.reject(e);
                return ret;
            }
        }
    }
}

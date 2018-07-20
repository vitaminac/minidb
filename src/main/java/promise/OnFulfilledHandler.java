package promise;

public interface OnFulfilledHandler<P, R> {
    R doNext(P result) throws Exception;
}
package async.promise;

public interface OnFulfilledHandler<P, R> {
    R pass(P result) throws Exception;
}
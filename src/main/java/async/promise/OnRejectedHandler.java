package async.promise;

public interface OnRejectedHandler<R> {
    R pass(Throwable e) throws Exception;
}
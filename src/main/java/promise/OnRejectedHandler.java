package promise;

public interface OnRejectedHandler<R> {
    R doCatch(Throwable e) throws Exception;
}
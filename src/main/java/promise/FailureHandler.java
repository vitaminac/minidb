package promise;

public interface FailureHandler<R> {
    R doCatch(Throwable e) throws Exception;
}
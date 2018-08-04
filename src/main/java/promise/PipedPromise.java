package promise;

interface PipedPromise<P, R> extends Promise<R> {
    void pipe(P value);

    void pipe(Throwable reason);
}

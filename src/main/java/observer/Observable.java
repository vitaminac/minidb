package observer;

public interface Observable<Event> extends Publisher<Event> {
    void subscribe(Subscriber<? super Event> subscriber);
}

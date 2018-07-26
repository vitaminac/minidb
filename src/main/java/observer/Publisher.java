package observer;

public interface Publisher<Event> {
    void publish(Event event);

    void subscribe(Subscriber<? super Event> subscriber);
}

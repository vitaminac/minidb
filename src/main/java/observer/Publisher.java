package observer;

public interface Publisher<E extends Event> {
    void publish(Event event);

    void subscribe(Subscriber<? extends E> subscriber);
}

package observer;

import event.Event;

public interface Publisher<E extends Event> {
    void publish(E event);

    void subscribe(Subscriber<? super E> subscriber);
}

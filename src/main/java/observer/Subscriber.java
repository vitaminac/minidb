package observer;

import event.Event;

public interface Subscriber<E extends Event> {
    void notify(E event) throws Exception;
}

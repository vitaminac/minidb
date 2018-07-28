package observer;

public interface Publisher<Event> {
    void publish(Event event);
}

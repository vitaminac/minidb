package observer;

public interface Subscriber<Event> {
    void notify(Event event) throws Exception;
}

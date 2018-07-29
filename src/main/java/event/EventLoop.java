package event;

import nio.IOEvent;
import nio.NIOSelectable;
import util.Logger;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class EventLoop {
    public static final EventLoop DEFAULT_EVENT_LOOP;

    static {
        EventLoop deferTemp;
        try {
            final Selector selector = Selector.open();
            deferTemp = new EventLoop(selector);
        } catch (IOException e) {
            deferTemp = null;
            e.printStackTrace();
        }
        DEFAULT_EVENT_LOOP = deferTemp;
    }

    private final Logger logger = new Logger(this.getClass());
    private final HashMap<SelectionKey, NIOSelectable> handlers;
    private final Selector selector;

    public EventLoop(Selector selector) {
        this.selector = selector;
        this.handlers = new HashMap<>();
    }

    public void poll(long timeout) {
        // Processing events in the poll queue
        try {
            int num;
            if (timeout > 0) {
                num = selector.select(timeout);
            } else {
                num = selector.selectNow();
            }
            if (num > 0) {
                final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                final Iterator<SelectionKey> it = selectedKeys.iterator();
                while (it.hasNext()) {
                    final SelectionKey key = it.next();
                    if (key.isValid() && key.channel().isOpen()) {
                        final NIOSelectable handler = this.handlers.get(key);
                        if (handler != null) {
                            try {
                                handler.onSelect(new IOEvent<>(key, key.readyOps()));
                            } catch (Exception e) {
                                this.unregister(key);
                                logger.error(e);
                            }
                        }
                    } else {
                        this.unregister(key);
                    }
                    // remove the processed SelectionKey
                    it.remove();
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public void register(NIOSelectable handler, IOEvent<SelectableChannel> event) throws ClosedChannelException {
        final SelectionKey key = event.getSource().register(selector, event.getOpCode());
        this.handlers.put(key, handler);
    }

    public void unregister(SelectionKey key) {
        key.cancel();
        this.handlers.remove(key);
    }

    public boolean isIdle() {
        return this.handlers.isEmpty();
    }
}

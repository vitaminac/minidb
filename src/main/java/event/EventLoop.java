package event;

import nio.SelectHandler;
import util.Logger;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;

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
    private final HashMap<SelectionKey, SelectHandler> handlers;
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
                // we must use iterator and remove already processed key
                final Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    final SelectionKey key = it.next();
                    if (key.isValid() && key.channel().isOpen()) {
                        final SelectHandler handler = this.handlers.get(key);
                        if (handler != null) {
                            try {
                                handler.select();
                            } catch (IOException e) {
                                handler.close();
                                this.unregister(key);
                                logger.error(e);
                            } catch (Exception e) {
                                this.unregister(key);
                                logger.error(e);
                            }
                        } else {
                            this.unregister(key);
                        }
                    } else {
                        this.unregister(key);
                    }
                    it.remove();
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public SelectionKey register(SelectHandler handler, SelectableChannel channel, int... ops) throws ClosedChannelException {
        int watch = 0;
        for (int op : ops) {
            watch |= op;
        }
        final SelectionKey key = channel.register(selector, watch);
        this.handlers.put(key, handler);
        return key;
    }

    public void unregister(SelectionKey key) {
        key.cancel();
        this.handlers.remove(key);
    }

    public boolean isIdle() {
        return this.handlers.isEmpty();
    }
}

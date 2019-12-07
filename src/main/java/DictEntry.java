import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class DictEntry implements Map.Entry<Object, Object>, Serializable {
    private final Object key;
    private Object value;

    public DictEntry(Object key, Object value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public Object getKey() {
        return this.key;
    }

    @Override
    public Object getValue() {
        return this.value;
    }

    @Override
    public Object setValue(Object value) {
        Object old = this.value;
        this.value = value;
        return old;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DictEntry entry = (DictEntry) o;
        return key.equals(entry.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return this.getKey().toString() + " -> " + this.getValue().toString();
    }
}

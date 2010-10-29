package polyglot.dispatch;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TinyMap<K,V> extends AbstractMap<K,V> {
    
    Object[] keys;
    Object[] values;
    int len;
    
public    TinyMap() {
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
	if (len == 0)
	    return Collections.emptySet();

	return new AbstractSet<Entry<K,V>>() {
	    @Override
	    public Iterator<java.util.Map.Entry<K, V>> iterator() {
		return new Iterator<Entry<K,V>>() {
		    int i = 0;
		    public boolean hasNext() {
			return i < len;
		    }

		    public java.util.Map.Entry<K, V> next() {
			final int i = this.i++;
			return new Map.Entry<K, V>() {
			    public K getKey() {
				return (K) keys[i];
			    }

			    public V getValue() {
				return (V) values[i];
			    }

			    public V setValue(V value) {
				V old = (V) values[i];
				values[i] = value;
				return old;
			    }
			};
		    }

		    public void remove() {
			throw new UnsupportedOperationException();
		    }
		    
		};
	    }

	    @Override
	    public int size() {
		return len;
	    }
	};
    }
    
    @Override
    public int size() {
	return len;
    }
    
    public V put(K key, V value) {
	if (len == 0) {
	    keys = new Object[1];
	    values = new Object[1];
	    keys[0] = key;
	    values[0] = value;
	    len = 1;
	    return null;
	}
	
	for (int i = 0; i < len; i++) {
	    Object k = keys[i];
	    if (k == key || k.equals(key)) {
		V old = (V) values[i];
		values[i] = value;
		return old;
	    }
	}

	if (len >= keys.length) {
	    Object[] ks = new Object[len * 2];
	    Object[] vs = new Object[len * 2];
	    System.arraycopy(keys, 0, ks, 0, len);
	    System.arraycopy(values, 0, vs, 0, len);
	    keys = ks;
	    values = vs;
	}

	keys[len] = key;
	values[len] = value;
	len++;
	return null;
    }
}

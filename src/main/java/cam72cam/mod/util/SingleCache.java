package cam72cam.mod.util;

import java.util.Objects;
import java.util.function.Function;

public class SingleCache<K, V> {
    private final Function<K, V> provider;
    private K cacheK;
    private V cacheV;

    public SingleCache(Function<K, V> provider) {
        this.provider = provider;
    }

    public V get(K key) {
        if (!(Objects.equals(cacheK, key)) || cacheV == null) {
            cacheK = key;
            cacheV = provider.apply(cacheK);
        }
        return cacheV;
    }
}

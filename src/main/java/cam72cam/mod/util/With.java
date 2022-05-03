package cam72cam.mod.util;

@FunctionalInterface
public interface With extends AutoCloseable {
    default void close() {
        restore();
    }

    void restore();

    default With and(With other) {
        return () -> {
            this.close();
            other.close();
        };
    }
}

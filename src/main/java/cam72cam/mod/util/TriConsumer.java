package cam72cam.mod.util;

@FunctionalInterface
public interface TriConsumer<T, T1, T2> {
    public void accept(T a, T1 b, T2 c);
}

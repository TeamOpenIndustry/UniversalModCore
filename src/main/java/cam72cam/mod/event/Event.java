package cam72cam.mod.event;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class Event<T> {
    private final Set<Runnable> pre = new LinkedHashSet<>();
    private final Set<T> callbacks = new LinkedHashSet<>();
    private final Set<Runnable> post = new LinkedHashSet<>();

    public void pre(Runnable callback) {
        pre.add(callback);
    }
    public void subscribe(T callback) {
        callbacks.add(callback);
    }
    public void post(Runnable callback) {
        post.add(callback);
    }
    public void execute(Consumer<T> handler) {
        pre.forEach(Runnable::run);
        callbacks.forEach(handler);
        post.forEach(Runnable::run);
    }

    boolean executeCancellable(Function<T, Boolean> handler) {
        pre.forEach(Runnable::run);
        for (T callback : callbacks) {
            if (!handler.apply(callback)) {
                return false;
            }
        }
        post.forEach(Runnable::run);
        return true;
    }
}

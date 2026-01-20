package cam72cam.mod.event;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class Event<T> {
    private final Set<Runnable> pre = new LinkedHashSet<>();
    private final Set<T> callbacks = new LinkedHashSet<>();
    private final Set<T> flushableCallbacks = new LinkedHashSet<>();
    private final Set<Runnable> post = new LinkedHashSet<>();

    public void pre(Runnable callback) {
        pre.add(callback);
    }

    //If this event is fired only 1 time per game launch or should be handled indifferently
    public void subscribe(T callback) {
        callbacks.add(callback);
    }

    //If this event is fired multiple times per game launch and should be handled separately
    public void subscribeFlushable(T callback) {
        subscribe(callback);
        flushableCallbacks.add(callback);
    }

    public void post(Runnable callback) {
        post.add(callback);
    }

    void execute(Consumer<T> handler) {
        pre.forEach(Runnable::run);
        callbacks.forEach(handler);
        callbacks.removeAll(flushableCallbacks);

        post.forEach(Runnable::run);
    }

    boolean executeCancellable(Function<T, Boolean> handler) {
        pre.forEach(Runnable::run);
        for (T callback : new LinkedHashSet<>(callbacks)) {
            if (!handler.apply(callback)) {
                callbacks.removeAll(flushableCallbacks);
                return false;
            }
        }
        post.forEach(Runnable::run);
        callbacks.removeAll(flushableCallbacks);
        return true;
    }
}

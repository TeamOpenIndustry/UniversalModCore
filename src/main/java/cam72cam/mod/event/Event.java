package cam72cam.mod.event;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class Event<T> {
    final Set<Runnable> pre = new LinkedHashSet<>();
    final Set<T> callbacks = new LinkedHashSet<>();
    final Set<Runnable> post = new LinkedHashSet<>();

    public void pre(Runnable callback) {
        pre.add(callback);
    }

    public void subscribe(T callback) {
        callbacks.add(callback);
    }

    public void post(Runnable callback) {
        post.add(callback);
    }

    void execute(Consumer<T> handler) {
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

    /**
     * For those events fired multiple times and should be handled respectively
     */
    static class TransientEvent<T> extends Event<T> {
        @Override
        void execute(Consumer<T> handler) {
            super.execute(handler);
            callbacks.clear();
        }

        boolean executeCancellable(Function<T, Boolean> handler) {
            boolean result = super.executeCancellable(handler);
            callbacks.clear();
            return result;
        }
    }
}

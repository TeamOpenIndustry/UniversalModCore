package cam72cam.mod.util;

import com.mojang.blaze3d.systems.RenderPass;

@FunctionalInterface
public interface With extends AutoCloseable {
    default void close() {
        restore();
    }

    void restore();

    default RenderPass getContent() {
        return null;
    }

    default With and(With other) {
        return () -> {
            this.close();
            other.close();
        };
    }
}

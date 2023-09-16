package cam72cam.mod.gui;

import cam72cam.mod.ModCore;
import net.minecraftforge.fml.StartupMessageManager;

/** Wrapper around game loading bar, only functional on certain MC versions / loaders */
public class Progress {
    /** Add a new bar to the stack */
    public static Bar push(String name, int steps) {
        return new Bar(name, steps);
    }

    /** Remove a particular bar from the stack */
    public static void pop(Bar bar) {
    }

    /** Remove the current bar from the stack */
    public static void pop() {
    }

    /** Get from push() and step() to increment progress */
    public static class Bar {
        private final String name;
        private final int steps;
        private int at;

        public Bar(String name, int steps) {
            this.name = name;
            this.steps = steps;
            this.at = 0;
            StartupMessageManager.addModMessage(name + " 0%");
        }

        public void step(String name) {
            at += 1;
            StartupMessageManager.addModMessage(this.name + " " + (at * 100 / steps) + "% : " + name);
            ModCore.info(this.name + " " + (at * 100 / steps) + "%% : " + name);
        }
    }
}

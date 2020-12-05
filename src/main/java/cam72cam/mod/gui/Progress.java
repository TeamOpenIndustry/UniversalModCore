package cam72cam.mod.gui;

import cam72cam.mod.ModCore;
import cpw.mods.fml.common.ProgressManager;
import cpw.mods.fml.common.ProgressManager.ProgressBar;

import java.util.Iterator;

/** Wrapper around game loading bar, only functional on certain MC versions / loaders */
public class Progress {
    /** Add a new bar to the stack */
    public static Bar push(String name, int steps) {
        return new Bar(ProgressManager.push(name, steps));
    }

    /** Remove a particular bar from the stack */
    public static void pop(Bar bar) {
        ProgressManager.pop(bar.bar);
    }

    /** Remove the current bar from the stack */
    public static void pop() {
        ProgressBar origBar = null;
        Iterator<ProgressBar> itr = ProgressManager.barIterator();
        while (itr.hasNext()) {
            origBar = itr.next();
        }

        //This is terrible, I am sorry
        if (origBar != null) {
            ProgressManager.pop(origBar);
        }
    }

    /** Get from push() and step() to increment progress */
    public static class Bar {
        private final ProgressBar bar;

        public Bar(ProgressBar bar) {
            this.bar = bar;
        }

        public void step(String name) {
            bar.step(name);
            ModCore.info(name);
        }
    }
}

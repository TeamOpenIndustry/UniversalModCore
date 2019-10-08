package cam72cam.mod.gui;

public class Progress {
    public static Bar push(String name, int steps) {
        //return new Bar(ProgressManager.push(name, steps));
        return new Bar(name, steps);
    }

    public static void pop(Bar bar) {
        //ProgressManager.pop(bar.bar);
    }

    public static void pop() {
        /*
        ProgressBar origBar = null;
        Iterator<ProgressBar> itr = ProgressManager.barIterator();
        while (itr.hasNext()) {
            origBar = itr.next();
        }

        //This is terrible, I am sorry
        ProgressManager.pop(origBar);
        */
    }

    public static class Bar {
        private final String name;
        private final int steps;
        private int at;

        public Bar(String name, int steps) {
            this.name = name;
            this.steps = steps;
            this.at=0;
            System.out.println(name+ " 0%");
        }

        /*
                private final ProgressBar bar;

                public Bar(ProgressBar bar) {
                    this.bar = bar;
                }
                */
        public void step(String name) {
            at+=1;
            System.out.println(this.name + " " + (at*100/steps) + "% : " + name);
            //bar.step(name);
        }
    }
}

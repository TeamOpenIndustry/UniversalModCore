package cam72cam.mod.automate;

import javax.swing.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Action {
    private static final Map<String, Function<String[], Action>> ctrs = new HashMap<>();
    private static final Map<String, Supplier<List<Action>>> potentials = new HashMap<>();
    private final String type;

    public static void register(String type, Function<String[], Action> ctr, Supplier<List<Action>> potential) {
        ctrs.put(type, ctr);
        potentials.put(type, potential);
    }

    public static Set<String> getTypes() {
        return potentials.keySet();
    }

    public static List<Action> getPotential(String type) {
        return potentials.get(type).get();
    }

    public static Action deserialize(String line) {
        String[] params = line.split("\\|\\|\\|");
        String clsName = params[0];
        params = Arrays.copyOfRange(params, 1, params.length);

        return ctrs.get(clsName).apply(params);
    }

    protected Action(String type) {
        this.type = type;
    }

    public abstract List<String> getParams();
    public abstract boolean tick();

    public abstract void renderEditor(JComponent panel);

    public abstract void renderSummary(JComponent panel);

    public final String serialize() {
        return type + "|||" + String.join("|||", getParams());
    }
}

package cam72cam.umc.api.entity.custom;

public interface ITickable {
    ITickable NOP = () -> {

    };

    static ITickable get(Object o) {
        if (o instanceof ITickable) {
            return (ITickable) o;
        }
        return NOP;
    }

    /** onUpdate */
    void onTick();
}

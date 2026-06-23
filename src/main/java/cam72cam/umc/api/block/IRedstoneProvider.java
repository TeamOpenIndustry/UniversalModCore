package cam72cam.umc.api.block;

import cam72cam.umc.api.util.Facing;

public interface IRedstoneProvider {
    int getStrongPower(Facing from);

    int getWeakPower(Facing from);
}

package cam72cam.mod.mixin.accessor;

import net.minecraft.world.item.CreativeModeTab;

public interface ACreativeModeTab {
    void setUMCTab();

    static ACreativeModeTab from(CreativeModeTab tab) {
        return (ACreativeModeTab)tab;
    }
}

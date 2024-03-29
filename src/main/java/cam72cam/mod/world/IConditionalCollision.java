package cam72cam.mod.world;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blocks that implement this interface can decide whether to allow collision,
 * depending on certain conditions.
 */
public interface IConditionalCollision {

    /**
     * Return whether or not a block at the given position with the given state can collide
     * with the given damage source.
     *
     * @param world        World the block is in.
     * @param pos          Position of the block.
     * @param state        Block state of the block.
     * @param damageSource Damage source that would be used to collide with the block.
     * @return Whether or not to calculate actual collision.
     */
    boolean canCollide(Level world, BlockPos pos, BlockState state, DamageSource damageSource);

}

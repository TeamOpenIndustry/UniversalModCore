package cam72cam.mod.entity.boundingbox;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CustomVoxelShape extends VoxelShape {
    private final BoundingBox bb;

    public CustomVoxelShape(BoundingBox boundingBox) {
        super(Shapes.block().shape);
        this.bb = boundingBox;
    }

    @Override
    public AABB bounds() {
        return this.bb;
    }

    @Override
    public List<AABB> toAabbs() {
        return Collections.singletonList(bb);
    }

//    @Override
//    protected boolean isFullWide(double p_211542_1_, double p_211542_3_, double p_211542_5_) {
//        return bb.contains(p_211542_1_, p_211542_3_, p_211542_5_);
//    }

    @Override
    protected double collideX(AxisCycle movementAxis, AABB collisionBox, double desiredOffset) {
        if (this.isEmpty()) {
            return desiredOffset;
        } else if (Math.abs(desiredOffset) < 1.0E-7D) {
            return 0.0D;
        } else {
            boolean colliding = bb.intersects(collisionBox.minX, collisionBox.minY, collisionBox.minZ,
                                              collisionBox.maxX, collisionBox.maxY, collisionBox.maxZ);
            switch (movementAxis) {
                case FORWARD: //Z
                    boolean willZCollide = !colliding
                            && bb.intersects(collisionBox.minX, collisionBox.minY, collisionBox.minZ + desiredOffset,
                                             collisionBox.maxX, collisionBox.maxY, collisionBox.maxZ + desiredOffset);

                    if (willZCollide) {
                        return bb.calculateZOffset(collisionBox, desiredOffset);
                    } else {
                        return desiredOffset;
                    }
                case NONE: //X
                    boolean willXCollide = !colliding
                            && bb.intersects(collisionBox.minX + desiredOffset, collisionBox.minY, collisionBox.minZ,
                                             collisionBox.maxX + desiredOffset, collisionBox.maxY, collisionBox.maxZ);
                    if (willXCollide) {
                        return bb.calculateXOffset(collisionBox, desiredOffset);
                    } else {
                        return desiredOffset;
                    }
                case BACKWARD: //Y
                default:
                    //Add a small offset so jump won't get blocked
                    return bb.calculateYOffset(collisionBox, desiredOffset) + 0.01;
            }
        }
    }

    @Override
    protected DoubleList getCoords(Direction.Axis axis) {
        return switch (axis) {
            case X -> DoubleArrayList.wrap(
                    Arrays.copyOf(new double[]{bb.minX, bb.maxX}, shape.getSize(Direction.Axis.X) + 1));
            case Y -> DoubleArrayList.wrap(
                    Arrays.copyOf(new double[]{bb.minY, bb.maxY}, shape.getSize(Direction.Axis.Y) + 1));
            case Z -> DoubleArrayList.wrap(
                    Arrays.copyOf(new double[]{bb.minZ, bb.maxZ}, shape.getSize(Direction.Axis.Z) + 1));
            default -> throw new IllegalArgumentException();
        };
    }
}

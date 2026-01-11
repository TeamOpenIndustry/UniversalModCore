package cam72cam.mod.entity.boundingbox;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.AxisRotation;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CustomVoxelShape extends VoxelShape {
    private final BoundingBox bb;

    public CustomVoxelShape(BoundingBox boundingBox) {
        super(VoxelShapes.fullCube().part);
        this.bb = boundingBox;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return this.bb;
    }

    @Override
    public List<AxisAlignedBB> toBoundingBoxList() {
        return Collections.singletonList(bb);
    }

    @Override
    protected boolean contains(double p_211542_1_, double p_211542_3_, double p_211542_5_) {
        return bb.contains(p_211542_1_, p_211542_3_, p_211542_5_);
    }

    @Override
    protected double getAllowedOffset(AxisRotation movementAxis, AxisAlignedBB collisionBox, double desiredOffset) {
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
    protected DoubleList getValues(Direction.Axis axis) {
        switch(axis) {
            case X:
                return DoubleArrayList.wrap(Arrays.copyOf(new double[]{bb.minX, bb.maxX}, part.getXSize() + 1));
            case Y:
                return DoubleArrayList.wrap(Arrays.copyOf(new double[]{bb.minY, bb.maxY}, part.getYSize() + 1));
            case Z:
                return DoubleArrayList.wrap(Arrays.copyOf(new double[]{bb.minZ, bb.maxZ}, part.getZSize() + 1));
            default:
                throw new IllegalArgumentException();
        }
    }
}

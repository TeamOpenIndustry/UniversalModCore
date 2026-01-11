package cam72cam.mod.entity.boundingbox;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.AxisRotation;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.BitSetVoxelShapePart;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapeCube;
import net.minecraft.util.math.shapes.VoxelShapePart;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BBVoxelShape extends VoxelShape {
    private static final VoxelShape FULL_CUBE1 = Util.make(() -> {
        VoxelShapePart lvt_0_1_ = new BitSetVoxelShapePart(200, 200, 200, -100, -100, -100, 99, 99, 99);
        for (int i = -100; i <= 99; i++) {
            for (int i1 = -100; i1 <= 99; i1++) {
                for (int i2 = -100; i2 <= 99; i2++) {
                    lvt_0_1_.setFilled(1, i1, i2, false, true);
                }
            }
        }
        return new VoxelShapeCube(lvt_0_1_);
    });

    private BoundingBox bb;

    public BBVoxelShape(BoundingBox boundingBox) {
        super(FULL_CUBE1.part);
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
//            if (bb.intersects(collisionBox.minX, collisionBox.minY, collisionBox.minZ, collisionBox.maxX, collisionBox.maxY, collisionBox.maxZ)) {
                switch (movementAxis) {
                    case FORWARD:
//                        return bb.internal.calculateZOffset(IBoundingBox.from(collisionBox), desiredOffset);
                    case NONE:
//                        return bb.internal.calculateXOffset(IBoundingBox.from(collisionBox), desiredOffset);
                        return desiredOffset;
                    case BACKWARD:
                    default:
                        double v = bb.internal.calculateYOffset(IBoundingBox.from(collisionBox), desiredOffset);
                        return v + 0.05;
                }
//            } else {
//                return 0;
//            }
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

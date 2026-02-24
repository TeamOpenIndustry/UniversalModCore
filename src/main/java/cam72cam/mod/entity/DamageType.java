package cam72cam.mod.entity;

import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

import java.util.HashMap;
import java.util.Objects;

/**
 * Damage type wrapper
 * <p>
 * Note that in order to make this work on 1.21.1 and upper, <code>DamageType</code>s should be treated as constants and created as soon as possible (like CONSTRUCT phase)
 */
public final class DamageType {
    //DamageType become a vanilla registry in 1.19, but not a exposed registry till 1.21
    public static final DamageType FIRE = new DamageType(DamageTypes.IN_FIRE);
    public static final DamageType PROJECTILE = new DamageType(DamageTypes.ARROW);
    public static final DamageType EXPLOSION = new DamageType(DamageTypes.EXPLOSION);
    public static final DamageType MAGIC = new DamageType(DamageTypes.MAGIC);
    public static final DamageType OTHER = new DamageType(DamageTypes.BAD_RESPAWN_POINT); //i.e. Intentional game design

    private static final HashMap<Identifier, DamageType> registered = new HashMap<>();

    public final Identifier id;
    public final ResourceKey<net.minecraft.world.damagesource.DamageType> internal;

    public static DamageType getOrCreate(String cause) {
        return getOrCreate(new Identifier(cause));
    }

    public static DamageType getOrCreate(Identifier cause) {
        return registered.computeIfAbsent(cause, DamageType::new);
    }

    private DamageType(String cause) {
        this(new Identifier(cause));
    }

    private DamageType(Identifier cause) {
        this.id = cause;
        this.internal = ResourceKey.create(Registries.DAMAGE_TYPE, cause.internal);
    }

    private DamageType(ResourceKey<net.minecraft.world.damagesource.DamageType> key) {
        this.id = new Identifier(key.location());
        this.internal = key;
    }

    public DamageSource getDamageSource() {
        try {
            Holder.Reference<net.minecraft.world.damagesource.DamageType> type =
                    RegistryUtil.getRegistry().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(internal);
            return new DamageSource(type);
        } catch (IllegalStateException e) {
            //Meaning requested isn't registered in vanilla, use our fallback
            return new DamageSource(Holder.direct(new net.minecraft.world.damagesource.DamageType(id.toString(), DamageScaling.NEVER, 0f)));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DamageType other = (DamageType) o;
        return Objects.equals(id, other.id);
    }
}

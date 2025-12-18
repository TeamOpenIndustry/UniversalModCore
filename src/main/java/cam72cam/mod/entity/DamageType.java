package cam72cam.mod.entity;

import cam72cam.mod.resource.Identifier;
import net.minecraft.util.DamageSource;

import java.util.HashMap;
import java.util.Objects;

/**
 * Damage type wrapper
 * <p>
 * Note that in order to make this work on 1.21.1 and upper, <code>DamageType</code>s should be treated as constants and created as soon as possible (like CONSTRUCT phase)
 */
public class DamageType {
    public static final DamageType FIRE = new DamageType("fire");
    public static final DamageType PROJECTILE = new DamageType("projectile");
    public static final DamageType EXPLOSION = new DamageType("explosion");
    public static final DamageType MAGIC = new DamageType("magic");
    public static final DamageType OTHER = new DamageType("other");

    private static final HashMap<Identifier, DamageType> registered = new HashMap<>();

    public Identifier damageType;
    public DamageSource internal;

    public static DamageType getOrCreate(String cause) {
        return getOrCreate(new Identifier(cause));
    }

    public static DamageType getOrCreate(Identifier cause) {
        return registered.computeIfAbsent(cause, DamageType::new);
    }

    private DamageType(String cause) {
        this(new Identifier(cause));
    }

    private DamageType(DamageSource source) {
        this(new Identifier("minecraft", source.damageType));
    }

    private DamageType(Identifier cause) {
        this.damageType = cause;
        this.internal = new DamageSource(damageType.internal.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DamageType other = (DamageType) o;
        return Objects.equals(damageType, other.damageType);
    }
}

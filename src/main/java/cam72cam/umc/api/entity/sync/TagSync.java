package cam72cam.umc.api.entity.sync;

import cam72cam.umc.api.serialization.TagField;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation that this @TagField should be auto-synchronized every tick between server and client
 *
 * @see TagField
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TagSync {
    /**
     * Minimum float precision change required for synchronization. Default is 0.001(10 ^ -3), max is 1, min is 10^-8
     */
    int floatPrecision() default 3;

    /**
     * Minimum double precision change required for synchronization. Default is 0.00001(10 ^ -5), max is 1, min is 10^-8
     */
    int doublePrecision() default 5;
}

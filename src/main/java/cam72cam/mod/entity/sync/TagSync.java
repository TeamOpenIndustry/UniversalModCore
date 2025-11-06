package cam72cam.mod.entity.sync;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation that this @TagField should be auto-synchronized every tick between server and client
 *
 * @see cam72cam.mod.serialization.TagField
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TagSync {
    /**
     * Should a float change less than 0.001 or a double change less than 0.00001 be synchronized?
     */
    boolean forceSync() default false;
}

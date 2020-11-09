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
}

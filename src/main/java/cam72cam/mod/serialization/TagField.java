package cam72cam.mod.serialization;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TagField {
    /**
     * @return Override tag name
     */
    String value() default "";

    /**
     * @return Custom tag mapper
     */
    Class<? extends TagMapper> mapper() default DefaultTagMapper.class;

    /**
     * @return Type hint for non-generic lists (Required for enums)
     */
    Class<?> typeHint() default Object.class;
}

package cam72cam.mod.loading;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

//TODO Move into umc.json
@Retention(RetentionPolicy.RUNTIME)
public @interface UMCMod {
    String modid();

    String name();

    String version();

    String dependencies();
}

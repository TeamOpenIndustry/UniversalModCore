package cam72cam.mod.resource;

import cam72cam.mod.ModCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Internal, don't use
 * <p>
 * Keep in mind that some of the methods are side-only
 * */
public class BuiltinPacks {
    @SideOnly(Side.CLIENT)
    public static void loadModResource(ModCore.Mod mod) {
        List<IResourcePack> packs = Minecraft.getMinecraft().defaultResourcePacks;

        String configDir = Loader.instance().getConfigDir().toString();
        new File(configDir).mkdirs();

        File folder = new File(configDir + File.separator + mod.modID());
        if (folder.exists()) {
            if (folder.isDirectory()) {
                File[] files = folder.listFiles(file -> file.getName().endsWith(".zip"));
                for (File file : files) {
                    packs.add(BuiltinPacks.asResource(file));
                }

                File[] folders = folder.listFiles(File::isDirectory);
                for (File dir : folders) {
                    packs.add(BuiltinPacks.asResource(dir));
                }
            }
        } else {
            folder.mkdirs();
        }

        IResourcePack modPack = BuiltinPacks.asResource(Loader.instance().activeModContainer().getSource());
        // Force first and last (and inject mod time) BUG: sounds can still be overridden by resource packs
        packs.add(1, modPack);
        packs.add(modPack);
    }

    @SideOnly(Side.CLIENT)
    public static IResourcePack asResource(File path) {
        if (path.isDirectory()) {
            return new FolderResourcePack(path) {
                @Override
                protected InputStream getInputStreamByName(String name) throws IOException {
                    InputStream stream = super.getInputStreamByName(name);
                    File file = this.getFile(name);
                    return new Identifier.InputStreamMod(stream, file.lastModified());
                }
            };
        } else {
            return new FileResourcePack(path) {
                @Override
                protected InputStream getInputStreamByName(String name) throws IOException {
                    return new Identifier.InputStreamMod(super.getInputStreamByName(name), resourcePackFile.lastModified());
                }
            };
        }
    }
}

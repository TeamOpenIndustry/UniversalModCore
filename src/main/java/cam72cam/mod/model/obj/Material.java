package cam72cam.mod.model.obj;

public class Material {
    public final String name;

    public final String texKd;
    public final String texBump;
    public final String texNs;

    public final float KdR;
    public final float KdG;
    public final float KdB;
    public final float KdA;
    public boolean used;

    public int copiesU;
    public int copiesV;

    public Material(String name, String texKd, String texBump, String texNs, Float kdR, Float kdG, Float kdB, Float kdA) {
        this.name = name;
        this.texKd = texKd;
        this.texBump = texBump;
        this.texNs = texNs;
        KdR = kdR == null ? 1 : kdR;
        KdG = kdG == null ? 1 : kdG;
        KdB = kdB == null ? 1 : kdB;
        KdA = kdA == null ? 1 : kdA;
        copiesU = 1;
        copiesV = 1;
        used = false;
    }

    public boolean hasTexture() {
        return texKd != null;
    }
}

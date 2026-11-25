package cam72cam.mod.entity;

import java.util.UUID;

/** Input intent only; the server computes the actual movement. */
public final class PassengerInputState {
    public static final int LEFT=1, RIGHT=2, FORWARD=4, BACK=8, SPRINT=16;
    private UUID owner;
    private long tick;
    private int mask;
    public void update(UUID owner, long tick, int mask) {
        this.owner=owner;
        this.tick=tick;
        this.mask=mask & 31;
    }
    public void clear() { owner=null; mask=0; }
    public int sample(UUID player, long now) {
        return player.equals(owner) && now >= tick && now-tick <= 10 ? mask : 0;
    }
    public record Motion(double x, double z) {}
    public static Motion movement(int mask) {
        int x=((mask&LEFT)!=0?1:0)-((mask&RIGHT)!=0?1:0);
        int z=((mask&FORWARD)!=0?1:0)-((mask&BACK)!=0?1:0);
        double len=Math.hypot(x,z);
        if (len==0) return new Motion(0,0);
        double speed=(mask&SPRINT)!=0?0.4:0.2;
        return new Motion(x*speed/len,z*speed/len);
    }
}

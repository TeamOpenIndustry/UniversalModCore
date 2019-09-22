package cam72cam.mod.sound;

public enum StandardSound {
    // Partial list only
    BLOCK_ANVIL_PLACE("random.anvil_land"),
    BLOCK_FIRE_EXTINGUISH("random.fizz");

    final String event;

    StandardSound(String event) {
        this.event = event;
    }
}

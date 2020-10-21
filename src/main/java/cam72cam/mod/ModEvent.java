package cam72cam.mod;

public enum ModEvent {
    CONSTRUCT, // Mod constructor is called, do your registration (Block, Item, etc...) here
    INITIALIZE, // First chance to interact with resource pack data
    SETUP, // Say Hi to other mods here
    FINALIZE, // Any final stuff to fire after registrations and setup stuff
    START, // Server start
    RELOAD // Resources reloaded
}

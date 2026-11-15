package dev.hachimi.standalone;

public final class ElytraFlags {
    private ElytraFlags() {}
    public static byte retainGlide(byte flags){return (byte)(flags|0x80);}
}

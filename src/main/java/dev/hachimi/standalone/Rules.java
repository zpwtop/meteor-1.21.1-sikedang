package dev.hachimi.standalone;

/** Pure decisions shared by modules, tested without loading Minecraft. */
public final class Rules {
    private Rules() {}
    public static boolean safeDamage(double enemy, double self, double min, double max, double health) {
        return Double.isFinite(enemy) && Double.isFinite(self) && enemy >= min && self >= 0
            && self <= max && self + 0.5 < health;
    }
    public static boolean refreshEffect(boolean enabled, int amplifier, int duration, int threshold, int minimumAmplifier) {
        return enabled && (amplifier < minimumAmplifier || duration <= threshold);
    }
    public static int screenSlot(int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot > 35) throw new IllegalArgumentException("Not a main inventory slot");
        return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
    }
    public static double[] motion(double yawDegrees, double forward, double sideways, double speed) {
        double length = Math.hypot(forward, sideways);
        if (length == 0) return new double[] {0, 0};
        forward /= length; sideways /= length;
        double yaw = Math.toRadians(yawDegrees);
        return new double[] {(-Math.sin(yaw) * forward + Math.cos(yaw) * sideways) * speed,
            (Math.cos(yaw) * forward + Math.sin(yaw) * sideways) * speed};
    }
}

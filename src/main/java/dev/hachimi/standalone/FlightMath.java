package dev.hachimi.standalone;

/** Coordinate conversions used by Slime's CONTROL and ROTATION flight modes. */
public final class FlightMath {
    private FlightMath() {}
    public static double[] direction(double yaw, double pitch, double forward, double strafe, double up, boolean rotation, double speed) {
        double y=Math.toRadians(yaw),p=Math.toRadians(pitch);
        double x=-Math.sin(y)*forward*(rotation?Math.cos(p):1)+Math.cos(y)*strafe;
        double z=Math.cos(y)*forward*(rotation?Math.cos(p):1)+Math.sin(y)*strafe;
        double vertical=up-(rotation?Math.sin(p)*forward:0);
        double length=Math.sqrt(x*x+vertical*vertical+z*z);
        if(length<1e-10)return new double[]{0,0,0};
        return new double[]{x/length*speed,vertical/length*speed,z/length*speed};
    }
}

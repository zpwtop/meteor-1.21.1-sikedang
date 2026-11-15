package dev.hachimi.standalone;

/** Monotonic timing of the original reset interval (one configured tick = 50 ms). */
public final class ElytraResetClock {
    private boolean used;
    private long last;
    public void reset(){used=false;last=0;}
    public boolean due(long now,double delayTicks){
        if(!Double.isFinite(delayTicks)||delayTicks<0)throw new IllegalArgumentException("Invalid reset delay");
        return !used||now-last>=(long)(delayTicks*50_000_000L);
    }
    public void sent(long now){used=true;last=now;}
}

package dev.hachimi.standalone;

/** Mining progress and bounded acknowledgement retries; never edits client block state. */
public final class MiningCycle {
    private final double threshold;
    private double progress;
    private int waitTicks,retries;
    private boolean waiting;
    public MiningCycle(double threshold){
        if(!Double.isFinite(threshold)||threshold<=0)throw new IllegalArgumentException("Invalid mining threshold");
        this.threshold=threshold;
    }
    public boolean advance(double delta){
        if(!waiting&&Double.isFinite(delta)&&delta>0)progress+=delta;
        return !waiting&&progress>=threshold;
    }
    public void stopSent(){waiting=true;waitTicks=0;}
    public boolean acknowledgementExpired(int delay){return waiting&&++waitTicks>=delay;}
    public boolean retry(int maximum){
        if(retries>=maximum)return false;
        retries++;progress=0;waitTicks=0;waiting=false;return true;
    }
    public boolean waiting(){return waiting;}
    public double fraction(){return Math.min(1,progress/threshold);}
}

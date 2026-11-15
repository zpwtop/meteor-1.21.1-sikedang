package dev.hachimi.standalone;

import java.util.function.ToDoubleFunction;

/** Reference angle convention: snap only near multiples of 45; prefer current/positive yaw on ties. */
public final class JumpDirection {
    private JumpDirection() {}
    public static float wrap(float yaw){float value=yaw%360;return value>=180?value-360:value< -180?value+360:value;}
    public static float snap(float yaw){float value=wrap(yaw),nearest=Math.round(value/45)*45;return Math.abs(value-nearest)<=10?wrap(nearest):value;}
    public static float choose(float yaw,int range,ToDoubleFunction<Float> clearance){
        float best=yaw;double score=clearance.applyAsDouble(yaw);
        for(int offset=1;offset<=range;offset++)for(int sign:new int[]{1,-1}){
            float candidate=wrap(yaw+sign*offset);double next=clearance.applyAsDouble(candidate);
            if(next>score){score=next;best=candidate;}
        }
        return best;
    }
}

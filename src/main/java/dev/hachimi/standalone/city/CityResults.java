package dev.hachimi.standalone.city;

import java.util.*;

public final class CityResults {
    private CityResults() {}
    public static long seed(String value){
        if(value==null||value.isBlank())throw new IllegalArgumentException("请填写正确的数字世界种子。");
        try{return Long.parseLong(value.trim());}catch(NumberFormatException e){throw new IllegalArgumentException("种子必须是有效的有符号 64 位整数。",e);}
    }
    public static List<CityLocation> nearest(Collection<CityLocation> input,CityArea area,int limit){
        if(limit<1)throw new IllegalArgumentException("Invalid result limit");
        return input.stream().filter(p->area.contains(p.x(),p.z())).distinct()
            .sorted(Comparator.comparingLong((CityLocation p)->area.distanceSquared(p.x(),p.z())).thenComparingInt(CityLocation::x).thenComparingInt(CityLocation::z))
            .limit(limit).toList();
    }
}

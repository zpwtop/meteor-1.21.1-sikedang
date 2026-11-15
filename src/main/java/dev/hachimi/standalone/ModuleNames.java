package dev.hachimi.standalone;

import java.util.Locale;
import java.util.Map;

/** Exact old names are accepted for config loading, but never used as displayed names. */
public final class ModuleNames {
    private ModuleNames() {}
    private static final Map<String,String> OLD=Map.of(
        "hachimi自动水晶","自动水晶","hachimi自动围住","自动围住",
        "hachimi自动围脚","自动围脚","hachimi重锤秒切","重锤秒切",
        "hachimi假飞","假飞","hachimi自动药水","自动药水",
        "slime甲飞","甲飞","slime简易鞘翅控制","简易鞘翅控制",
        "hachimi发包挖掘","发包挖掘","hachimi着色器","着色器");
    public static String canonical(String name){return name==null?null:OLD.getOrDefault(name.toLowerCase(Locale.ROOT),name);}
}

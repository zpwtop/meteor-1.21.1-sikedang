package dev.hachimi.standalone;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

public final class HachimiAddon extends MeteorAddon {
    public static final Category CATEGORY = new Category("死氪党");
    public static final Category SLIME_CATEGORY = CATEGORY;
    @Override public void onRegisterCategories() { Modules.registerCategory(CATEGORY); }
    @Override public String getPackage() { return "dev.hachimi.standalone"; }
    @Override public void onInitialize() {
        Modules.get().add(new HachimiCrystal());
        Modules.get().add(new HachimiSelfTrap());
        Modules.get().add(new HachimiSurround());
        Modules.get().add(new HachimiMace());
        Modules.get().add(new HachimiFakeFly());
        Modules.get().add(new HachimiPotion());
        Modules.get().add(new SlimeArmorFly());
        Modules.get().add(new SlimeElytraControl());
        Modules.get().add(new HachimiPacketMine());
        Modules.get().add(new HachimiShaders());
        Modules.get().add(new DurableElytra());
        Modules.get().add(new AncientCitySearch());
        Modules.get().add(new JumpingTiger());
    }
}

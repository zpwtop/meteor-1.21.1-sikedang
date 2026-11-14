package dev.hachimi.standalone;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.screen.slot.SlotActionType;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;

abstract class HachimiModule extends Module {
    protected final SettingGroup general = settings.getDefaultGroup();
    protected int generation;
    HachimiModule(String name, String description) { this(HachimiAddon.CATEGORY,name,description); }
    HachimiModule(meteordevelopment.meteorclient.systems.modules.Category category,String name,String description) { super(category,name,description); }
    protected Setting<Boolean> bool(String name, String description, boolean value) {
        return general.add(new BoolSetting.Builder().name(name).description(description).defaultValue(value).build());
    }
    protected Setting<Integer> integer(String name, String description, int value, int min, int max) {
        return general.add(new IntSetting.Builder().name(name).description(description).defaultValue(value).range(min,max).sliderRange(min,max).build());
    }
    protected Setting<Double> number(String name, String description, double value, double min, double max) {
        return general.add(new DoubleSetting.Builder().name(name).description(description).defaultValue(value).range(min,max).sliderRange(min,max).build());
    }
    protected boolean ready() { return mc.player != null && mc.world != null && mc.interactionManager != null && mc.player.isAlive(); }
    protected boolean inventoryReady() {
        return ready() && mc.currentScreen == null && mc.player.currentScreenHandler == mc.player.playerScreenHandler
            && mc.player.currentScreenHandler.getCursorStack().isEmpty();
    }
    @Override public void onActivate() { generation++; }
    @Override public void onDeactivate() { generation++; }
    private void selectSlot(int slot) {
        mc.player.getInventory().selectedSlot = slot;
        ((IClientPlayerInteractionManager)mc.interactionManager).meteor$syncSelected();
    }
    /** Swap to a main inventory slot and restore it, including failures in the action. */
    protected void withSlot(int slot, Runnable action) {
        if (!inventoryReady()) return;
        var player = mc.player;
        var world = mc.world;
        int selected = player.getInventory().selectedSlot;
        Transactions.withSlot(slot, selected, this::selectSlot,
            (source, hotbar) -> {
                if (mc.player == player && mc.world == world)
                    mc.interactionManager.clickSlot(player.playerScreenHandler.syncId, Rules.screenSlot(source), hotbar, SlotActionType.SWAP, player);
            }, action);
    }
}

package dev.hachimi.standalone;

import java.util.function.IntConsumer;
import java.util.function.BiConsumer;

/** Inventory operations with an explicit restoration path, independent of Minecraft. */
public final class Transactions {
    private Transactions() {}
    public static void withSlot(int source, int selected, IntConsumer select, BiConsumer<Integer,Integer> swap, Runnable action) {
        Rules.screenSlot(source); // Validate before the first mutation.
        if (selected < 0 || selected > 8) throw new IllegalArgumentException("Not a hotbar slot");
        if (source == selected) { action.run(); return; }
        if (source < 9) {
            select.accept(source);
            try { action.run(); } finally { select.accept(selected); }
        } else {
            swap.accept(source, selected);
            try { action.run(); } finally { swap.accept(source, selected); }
        }
    }
    public static void withElytra(int source, IntConsumer chestSwap, BiConsumer<Integer,Integer> swap, Runnable action) {
        if (source == -1) { action.run(); return; }
        Rules.screenSlot(source);
        if (source < 9) {
            chestSwap.accept(source);
            try { action.run(); } finally { chestSwap.accept(source); }
        } else {
            swap.accept(source,8);
            try {
                chestSwap.accept(8);
                try { action.run(); } finally { chestSwap.accept(8); }
            } finally { swap.accept(source,8); }
        }
    }
}

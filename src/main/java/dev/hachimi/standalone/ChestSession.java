package dev.hachimi.standalone;

import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/** Keeps an elytra equipped across a player tick and reverses all swaps exactly once. */
public final class ChestSession implements AutoCloseable {
    private final int source;
    private final IntConsumer chest;
    private final BiConsumer<Integer,Integer> inventory;
    private boolean closed;
    private ChestSession(int source, IntConsumer chest, BiConsumer<Integer,Integer> inventory) {
        Rules.screenSlot(source);
        this.source=source;this.chest=chest;this.inventory=inventory;
        if(source>=9) {
            inventory.accept(source,8);
            try { chest.accept(8); }
            catch(RuntimeException e) { inventory.accept(source,8);throw e; }
        } else chest.accept(source);
    }
    public static ChestSession open(int source, IntConsumer chest, BiConsumer<Integer,Integer> inventory) {
        return new ChestSession(source,chest,inventory);
    }
    @Override public void close() {
        if(closed)return;
        closed=true;
        if(source>=9) {
            try { chest.accept(8); }
            finally { inventory.accept(source,8); }
        } else chest.accept(source);
    }
}

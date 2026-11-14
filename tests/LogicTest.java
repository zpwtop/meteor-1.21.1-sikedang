package dev.hachimi.standalone;
import java.util.Arrays;
public final class LogicTest {
    static int checks;
    static void check(boolean b) { checks++; if(!b) throw new AssertionError("Check " + checks); }
    static void close(double a,double b) { check(Math.abs(a-b)<1e-8); }
    public static void main(String[] args) {
        check(Rules.safeDamage(8,4,6,6,20));
        check(!Rules.safeDamage(5.9,4,6,6,20));
        check(!Rules.safeDamage(8,6.1,6,6,20));
        check(!Rules.safeDamage(8,5.5,6,6,6));
        check(!Rules.safeDamage(Double.NaN,1,6,6,20));
        check(!Rules.safeDamage(8,Double.POSITIVE_INFINITY,6,6,20));
        check(!Rules.safeDamage(8,-1,6,6,20));
        check(Rules.refreshEffect(true,-1,0,0,0));
        check(!Rules.refreshEffect(false,-1,0,0,0));
        check(!Rules.refreshEffect(true,0,100,0,0));
        check(Rules.refreshEffect(true,0,100,0,1));
        check(Rules.refreshEffect(true,1,19,20,1));
        check(!Rules.refreshEffect(true,1,Integer.MAX_VALUE,20,1));
        check(Rules.screenSlot(0)==36 && Rules.screenSlot(8)==44 && Rules.screenSlot(9)==9 && Rules.screenSlot(35)==35);
        try { Rules.screenSlot(36); throw new AssertionError(); } catch(IllegalArgumentException expected) { checks++; }
        double[] v=Rules.motion(0,1,0,0.6);close(v[0],0);close(v[1],0.6);
        v=Rules.motion(90,1,0,0.6);close(v[0],-0.6);close(v[1],0);
        v=Rules.motion(0,1,1,0.6);close(Math.hypot(v[0],v[1]),0.6);
        v=Rules.motion(15,0,0,1);close(v[0],0);close(v[1],0);
        // Use the same transaction implementation as the modules, with an inventory simulation.
        for (int source : new int[]{0,4,8,9,20,35}) for (boolean fail : new boolean[]{false,true}) {
            Inventory inv = new Inventory();String[] before=inv.items.clone();int previous=inv.selected;
            try {
                Transactions.withSlot(source,inv.selected, s -> inv.selected=s, inv::swap, () -> {
                    check(inv.items[inv.selected].equals(before[source]));
                    if(fail) throw new TestFailure();
                });
                check(!fail);
            } catch(TestFailure expected) { check(fail); }
            check(inv.selected==previous && Arrays.equals(inv.items,before));
        }
        for (int source : new int[]{-1,0,8,9,24,35}) for(boolean fail : new boolean[]{false,true}) {
            Inventory inv=new Inventory();String[] before=inv.items.clone();String oldChest=inv.chest;
            try {
                Transactions.withElytra(source,inv::chestSwap,inv::swap, () -> {
                    check(inv.chest.equals(source==-1?oldChest:before[source]));
                    if(fail) throw new TestFailure();
                });
                check(!fail);
            } catch(TestFailure expected) { check(fail); }
            check(Arrays.equals(inv.items,before) && inv.chest.equals(oldChest));
        }
        extras();
        System.out.println("PASS: " + checks + " checks: damage thresholds, effects, movement, slot bounds, hotbar/inventory/chest restoration on success and failure.");
    }
    static void extras() {
        for(int source:new int[]{0,8,9,24,35}) {
            Inventory inv=new Inventory();String[] before=inv.items.clone();String chest=inv.chest;
            ChestSession session=ChestSession.open(source,inv::chestSwap,inv::swap);
            check(inv.chest.equals(before[source]));
            Transactions.withSlot(1,inv.selected,slot->inv.selected=slot,inv::swap,()->check(inv.items[inv.selected].equals(before[1])));
            session.close();check(Arrays.equals(inv.items,before)&&inv.chest.equals(chest));
            session.close();check(Arrays.equals(inv.items,before)&&inv.chest.equals(chest));
        }
        Inventory failed=new Inventory();String[] before=failed.items.clone();
        try { ChestSession.open(24,slot->{throw new TestFailure();},failed::swap);throw new AssertionError(); }
        catch(TestFailure expected){check(Arrays.equals(before,failed.items)&&failed.chest.equals("armor"));}
        double[] v=FlightMath.direction(0,70,1,0,0,false,0.05);close(v[0],0);close(v[1],0);close(v[2],0.05);
        v=FlightMath.direction(0,90,1,0,0,true,1);close(v[0],0);close(v[1],-1);close(v[2],0);
        v=FlightMath.direction(90,0,1,1,1,false,0.6);close(Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]),0.6);
        v=FlightMath.direction(20,30,0,0,0,true,1);close(v[0],0);close(v[1],0);close(v[2],0);
        MiningCycle cycle=new MiningCycle(0.99);
        for(int i=0;i<9;i++)check(!cycle.advance(0.1));
        check(cycle.advance(0.1));close(cycle.fraction(),1);
        cycle.stopSent();check(cycle.waiting());check(!cycle.advance(2));
        for(int i=0;i<9;i++)check(!cycle.acknowledgementExpired(10));
        check(cycle.acknowledgementExpired(10));check(cycle.retry(2));check(!cycle.waiting());close(cycle.fraction(),0);
        check(!cycle.advance(Double.NaN));check(!cycle.advance(Double.POSITIVE_INFINITY));check(!cycle.advance(-1));
        check(cycle.advance(1));cycle.stopSent();check(cycle.retry(2));
        check(cycle.advance(1));cycle.stopSent();check(!cycle.retry(2));
        try{new MiningCycle(Double.NaN);throw new AssertionError();}catch(IllegalArgumentException expected){checks++;}
    }
    static class TestFailure extends RuntimeException { private static final long serialVersionUID=1; }
    static class Inventory {
        String[] items=new String[36];int selected=4;String chest="armor";
        Inventory() { for(int i=0;i<36;i++)items[i]="item"+i; }
        void swap(int source,int hotbar) { String a=items[source];items[source]=items[hotbar];items[hotbar]=a; }
        void chestSwap(int slot) { String a=chest;chest=items[slot];items[slot]=a; }
    }
}

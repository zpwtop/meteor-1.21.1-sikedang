package dev.hachimi.standalone;
import java.util.Arrays;
public final class LogicTest {
    static int checks;
    static void check(boolean b) { checks++; if(!b) throw new AssertionError("Check " + checks); }
    static void close(double a,double b) { check(Math.abs(a-b)<1e-8); }
    public static void main(String[] args) {
        close(JumpDirection.snap(9),0);close(JumpDirection.snap(10),0);close(JumpDirection.snap(11),11);
        close(JumpDirection.snap(-36),-45);close(JumpDirection.snap(179),-180);close(JumpDirection.snap(350),0);
        close(JumpDirection.wrap(540),-180);close(JumpDirection.wrap(-541),179);
        close(JumpDirection.choose(0,0,yaw->100),0);
        close(JumpDirection.choose(0,50,yaw->Math.abs(yaw)),50);
        close(JumpDirection.choose(0,50,yaw->-Math.abs(yaw+17)), -17);
        close(JumpDirection.choose(179,4,yaw->yaw< -179?10:0),-180);
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
        durableAndNames();
        citySearchRules();
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
    static void durableAndNames() {
        for(int flags=0;flags<256;flags++){
            int retained=ElytraFlags.retainGlide((byte)flags)&255;
            check((retained&127)==(flags&127));check((retained&128)!=0);
        }
        ElytraResetClock clock=new ElytraResetClock();
        check(clock.due(0,3));clock.sent(0);
        check(!clock.due(149_999_999L,3));check(clock.due(150_000_000L,3));
        clock.sent(150_000_000L);check(!clock.due(299_999_999L,3));check(clock.due(300_000_000L,3));
        check(clock.due(150_000_000L,0));
        check(!clock.due(174_999_999L,0.5));check(clock.due(175_000_000L,0.5));
        clock.reset();check(clock.due(0,20));
        try{clock.due(0,Double.NaN);throw new AssertionError();}catch(IllegalArgumentException expected){checks++;}
        for(int slot:new int[]{0,7,8})for(boolean fail:new boolean[]{false,true}){
            Inventory inv=new Inventory();String[] items=inv.items.clone();String chest=inv.chest;
            try{Transactions.withChestSwap(slot,inv::chestSwap,()->{
                check(inv.chest.equals(items[slot]));
                if(fail)throw new TestFailure();
            });check(!fail);}catch(TestFailure expected){check(fail);}
            check(Arrays.equals(items,inv.items)&&chest.equals(inv.chest));
        }
        Inventory inv=new Inventory();String[] items=inv.items.clone();
        try{Transactions.withChestSwap(9,inv::chestSwap,()->{});throw new AssertionError();}catch(IllegalArgumentException expected){check(Arrays.equals(items,inv.items));}
        String[] names={"自动水晶","自动围住","自动围脚","重锤秒切","假飞","自动药水","甲飞","简易鞘翅控制","发包挖掘","着色器"};
        for(String name:names){
            String prefix=name.equals("甲飞")||name.equals("简易鞘翅控制")?"slime":"hachimi";
            check(ModuleNames.canonical(prefix+name).equals(name));
            check(ModuleNames.canonical(prefix.toUpperCase(java.util.Locale.ROOT)+name).equals(name));
            check(ModuleNames.canonical(name).equals(name));
        }
        check(ModuleNames.canonical("unrelated-addon").equals("unrelated-addon"));
        check(ModuleNames.canonical("hachimiUnknown").equals("hachimiUnknown"));
        check(ModuleNames.canonical(null)==null);
    }
    static void citySearchRules(){
        var area=new dev.hachimi.standalone.city.CityArea(0,0,384);
        check(area.contains(384,0));check(area.contains(-384,0));check(!area.contains(384,1));check(!area.contains(385,0));
        check(area.regions(384).minX()==-2&&area.regions(384).maxX()==1);
        check(area.regions(384).minZ()==-2&&area.regions(384).maxZ()==1);check(area.regions(384).size()==16);
        var negative=new dev.hachimi.standalone.city.CityArea(-385,-385,1);check(negative.regions(384).minX()==-2);check(negative.contains(-386,-385));
        int border=dev.hachimi.standalone.city.CityArea.WORLD_LIMIT;
        for(int center:new int[]{border,-border}){
            var bounded=new dev.hachimi.standalone.city.CityArea(center,center,32768);
            check(bounded.contains(center,center));check(!bounded.contains(center>0?center+1:center-1,center));
            check(bounded.regions(384).size()>0);
        }
        check(dev.hachimi.standalone.city.CityResults.seed("0")==0);
        check(dev.hachimi.standalone.city.CityResults.seed("  -9223372036854775808 ")==Long.MIN_VALUE);
        check(dev.hachimi.standalone.city.CityResults.seed("9223372036854775807")==Long.MAX_VALUE);
        for(String invalid:new String[]{"","   ","unknown","9223372036854775808"}){
            try{dev.hachimi.standalone.city.CityResults.seed(invalid);throw new AssertionError();}catch(IllegalArgumentException expected){checks++;}
        }
        var near=new dev.hachimi.standalone.city.CityLocation(-2,-27,0,-1,0);
        var tie=new dev.hachimi.standalone.city.CityLocation(2,-27,0,0,0);
        var far=new dev.hachimi.standalone.city.CityLocation(100,-27,0,6,0);
        var outside=new dev.hachimi.standalone.city.CityLocation(385,-27,0,24,0);
        var result=dev.hachimi.standalone.city.CityResults.nearest(java.util.List.of(far,outside,tie,near,near),area,2);
        check(result.equals(java.util.List.of(near,tie)));
        check(near.coordinates().equals("-2 -27 0"));
        try{result.clear();throw new AssertionError();}catch(UnsupportedOperationException expected){checks++;}
        try{new dev.hachimi.standalone.city.CityArea(0,0,0);throw new AssertionError();}catch(IllegalArgumentException expected){checks++;}
        try{area.regions(0);throw new AssertionError();}catch(IllegalArgumentException expected){checks++;}
        try{dev.hachimi.standalone.city.CityResults.nearest(java.util.List.of(),area,0);throw new AssertionError();}catch(IllegalArgumentException expected){checks++;}
    }
    static class TestFailure extends RuntimeException { private static final long serialVersionUID=1; }
    static class Inventory {
        String[] items=new String[36];int selected=4;String chest="armor";
        Inventory() { for(int i=0;i<36;i++)items[i]="item"+i; }
        void swap(int source,int hotbar) { String a=items[source];items[source]=items[hotbar];items[hotbar]=a; }
        void chestSwap(int slot) { String a=chest;chest=items[slot];items[slot]=a; }
    }
}

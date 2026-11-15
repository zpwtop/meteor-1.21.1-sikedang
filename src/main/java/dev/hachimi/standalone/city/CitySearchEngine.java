package dev.hachimi.standalone.city;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** A single worker owns vanilla RNG/noise/template caches. Cancellation never interrupts JAR reads. */
public final class CitySearchEngine implements AutoCloseable {
    public record Progress(long completed,long total,int cities) {}
    private final ExecutorService worker=Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"sikedang-ancient-city");thread.setDaemon(true);return thread;});
    private final AtomicLong epoch=new AtomicLong();
    private Future<?> future;
    private volatile Progress progress=new Progress(0,0,0);
    private volatile boolean running;
    public Progress progress(){return progress;}
    public boolean running(){return running;}
    public synchronized void cancel(){epoch.incrementAndGet();if(future!=null)future.cancel(false);future=null;running=false;progress=new Progress(0,0,0);}
    public synchronized void search(long seed,CityArea area,int limit,Consumer<List<CityLocation>> done,Consumer<Throwable> error){
        cancel();long id=epoch.get();running=true;
        future=worker.submit(()->{
            try(var context=new CityPredictionWorld(seed)){
                var bounds=area.regions(context.regionSizeBlocks());
                List<CityLocation> found=new ArrayList<>();long completed=0;
                for(int x=bounds.minX();x<=bounds.maxX();x++)for(int z=bounds.minZ();z<=bounds.maxZ();z++){
                    if(epoch.get()!=id)return;
                    var chunk=context.candidate(x,z);
                    var position=context.locate(chunk);
                    if(position.isPresent()){
                        var pos=position.get();
                        if(area.contains(pos.getX(),pos.getZ()))found.add(new CityLocation(pos.getX(),pos.getY(),pos.getZ(),chunk.x,chunk.z));
                    }
                    if(epoch.get()==id)progress=new Progress(++completed,bounds.size(),found.size());
                }
                if(epoch.get()==id){running=false;done.accept(CityResults.nearest(found,area,limit));}
            }catch(Throwable failure){if(epoch.get()==id){running=false;error.accept(failure);}}
        });
    }
    @Override public synchronized void close(){cancel();worker.shutdown();}
}

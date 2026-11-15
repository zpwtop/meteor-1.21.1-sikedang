package dev.hachimi.standalone.city;

/** Circular block radius; expand only the candidate enumeration, never the returned results. */
public record CityArea(int x,int z,int radius) {
    public static final int WORLD_LIMIT=29_999_984;
    public CityArea {
        if(Math.abs((long)x)>WORLD_LIMIT||Math.abs((long)z)>WORLD_LIMIT||radius<1||radius>1_000_000)
            throw new IllegalArgumentException("Invalid search center or radius");
    }
    public boolean contains(int px,int pz){return distanceSquared(px,pz)<=(long)radius*radius&&Math.abs((long)px)<=WORLD_LIMIT&&Math.abs((long)pz)<=WORLD_LIMIT;}
    public long distanceSquared(int px,int pz){long dx=(long)px-x,dz=(long)pz-z;return dx*dx+dz*dz;}
    public Bounds regions(int regionSize){
        if(regionSize<1)throw new IllegalArgumentException("Invalid region size");
        // Vanilla city-center templates can extend away from their starting chunk.
        long reach=(long)radius+128;
        return new Bounds((int)Math.floorDiv(Math.max(-WORLD_LIMIT,(long)x-reach),regionSize),
            (int)Math.floorDiv(Math.min(WORLD_LIMIT,(long)x+reach),regionSize),
            (int)Math.floorDiv(Math.max(-WORLD_LIMIT,(long)z-reach),regionSize),
            (int)Math.floorDiv(Math.min(WORLD_LIMIT,(long)z+reach),regionSize));
    }
    public record Bounds(int minX,int maxX,int minZ,int maxZ){public long size(){return ((long)maxX-minX+1)*((long)maxZ-minZ+1);}}
}

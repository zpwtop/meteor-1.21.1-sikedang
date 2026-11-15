package dev.hachimi.standalone.city;

public record CityLocation(int x,int y,int z,int chunkX,int chunkZ) {
    public String coordinates(){return x+" "+y+" "+z;}
}

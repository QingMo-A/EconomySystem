package com.mo.economy_system.core.world_wrap_system;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public class WorldWrapTransformer {
    private static final int CHUNK_SIZE = 16;

    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;
    private final double width;
    private final double height;
    private final int minChunkX;
    private final int minChunkZ;
    private final int widthChunks;
    private final int heightChunks;

    public WorldWrapTransformer(WorldWrapConfig.WorldWrapConfigData config) {
        this.minX = config.getMinX();
        this.maxX = config.getMaxX();
        this.minZ = config.getMinZ();
        this.maxZ = config.getMaxZ();
        this.width = config.getWidth();
        this.height = config.getHeight();
        this.minChunkX = floorChunk(minX);
        this.minChunkZ = floorChunk(minZ);
        this.widthChunks = Math.max(1, (int) Math.round(width / CHUNK_SIZE));
        this.heightChunks = Math.max(1, (int) Math.round(height / CHUNK_SIZE));
    }

    public double wrapX(double x) {
        return wrapCoordinate(x, minX, maxX, width);
    }

    public double wrapZ(double z) {
        return wrapCoordinate(z, minZ, maxZ, height);
    }

    public int wrapChunkX(int chunkX) {
        return wrapChunk(chunkX, minChunkX, widthChunks);
    }

    public int wrapChunkZ(int chunkZ) {
        return wrapChunk(chunkZ, minChunkZ, heightChunks);
    }

    public ChunkPos wrapChunk(ChunkPos chunkPos) {
        return new ChunkPos(wrapChunkX(chunkPos.x), wrapChunkZ(chunkPos.z));
    }

    public boolean isChunkInside(ChunkPos chunkPos) {
        return isChunkInside(chunkPos.x, chunkPos.z);
    }

    public boolean isChunkInside(int chunkX, int chunkZ) {
        return isChunkXInside(chunkX) && isChunkZInside(chunkZ);
    }

    public boolean isChunkXInside(int chunkX) {
        return chunkX >= minChunkX && chunkX < minChunkX + widthChunks;
    }

    public boolean isChunkZInside(int chunkZ) {
        return chunkZ >= minChunkZ && chunkZ < minChunkZ + heightChunks;
    }

    public ChunkPos unwrapChunkFromServerToClient(ChunkPos clientReference, ChunkPos serverChunk) {
        return new ChunkPos(
                unwrapChunk(clientReference.x, serverChunk.x, minChunkX, widthChunks),
                unwrapChunk(clientReference.z, serverChunk.z, minChunkZ, heightChunks)
        );
    }

    public BlockPos unwrapBlockFromServerToClient(BlockPos clientReference, BlockPos serverBlock) {
        return new BlockPos(
                unwrapBlock(clientReference.getX(), serverBlock.getX(), minX, maxX, width),
                serverBlock.getY(),
                unwrapBlock(clientReference.getZ(), serverBlock.getZ(), minZ, maxZ, height)
        );
    }

    public double unwrapXFromServerToClient(double clientReference, double serverX) {
        return unwrapCoordinate(clientReference, serverX, minX, maxX, width);
    }

    public double unwrapZFromServerToClient(double clientReference, double serverZ) {
        return unwrapCoordinate(clientReference, serverZ, minZ, maxZ, height);
    }

    public double shortestDeltaX(double delta) {
        return shortestDelta(delta, width);
    }

    public double shortestDeltaZ(double delta) {
        return shortestDelta(delta, height);
    }

    public boolean isChunkAligned() {
        return isWholeChunk(minX) && isWholeChunk(maxX) && isWholeChunk(minZ) && isWholeChunk(maxZ)
                && isWholeChunk(width) && isWholeChunk(height);
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public int getMinChunkX() {
        return minChunkX;
    }

    public int getMinChunkZ() {
        return minChunkZ;
    }

    public int getMaxChunkXExclusive() {
        return minChunkX + widthChunks;
    }

    public int getMaxChunkZExclusive() {
        return minChunkZ + heightChunks;
    }

    private static double wrapCoordinate(double value, double min, double max, double size) {
        double wrapped = min + positiveModulo(value - min, size);
        if (wrapped >= max) {
            wrapped -= size;
        }
        return wrapped;
    }

    private static int wrapChunk(int chunk, int minChunk, int sizeChunks) {
        return minChunk + positiveModulo(chunk - minChunk, sizeChunks);
    }

    private static int unwrapChunk(int clientReference, int serverChunk, int minChunk, int sizeChunks) {
        int wrappedClientReference = wrapChunk(clientReference, minChunk, sizeChunks);
        int diff = serverChunk - wrappedClientReference;
        int unwrapped = clientReference + diff;
        int half = sizeChunks / 2;
        while (unwrapped < clientReference - half) {
            unwrapped += sizeChunks;
        }
        while (unwrapped > clientReference + half) {
            unwrapped -= sizeChunks;
        }
        return unwrapped;
    }

    private static int unwrapBlock(int clientReference, int serverBlock, double min, double max, double size) {
        return (int) Math.floor(unwrapCoordinate(clientReference, serverBlock, min, max, size));
    }

    private static int floorChunk(double blockCoordinate) {
        return (int) Math.floor(blockCoordinate / CHUNK_SIZE);
    }

    private static double shortestDelta(double delta, double size) {
        double half = size / 2.0D;
        while (delta > half) {
            delta -= size;
        }
        while (delta < -half) {
            delta += size;
        }
        return delta;
    }

    private static double unwrapCoordinate(double clientReference, double serverCoordinate, double min, double max, double size) {
        double wrappedClientReference = wrapCoordinate(clientReference, min, max, size);
        double diff = serverCoordinate - wrappedClientReference;
        double unwrapped = clientReference + diff;
        double half = size / 2.0D;
        while (unwrapped < clientReference - half) {
            unwrapped += size;
        }
        while (unwrapped > clientReference + half) {
            unwrapped -= size;
        }
        return unwrapped;
    }

    private static boolean isWholeChunk(double value) {
        double chunks = value / CHUNK_SIZE;
        return Math.abs(chunks - Math.rint(chunks)) < 0.0001D;
    }

    private static int positiveModulo(int value, int modulo) {
        return Math.floorMod(value, modulo);
    }

    private static double positiveModulo(double value, double modulo) {
        return (value % modulo + modulo) % modulo;
    }
}

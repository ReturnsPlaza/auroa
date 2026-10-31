package dev.aurora.module.impl;

import dev.aurora.hud.EspTarget;
import dev.aurora.module.Category;
import dev.aurora.setting.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Finds budding-amethyst locations only in chunks already loaded by the client. */
public final class AmethystChunkFinder extends EspModule {
    private final NumberSetting chunkRange = add(new NumberSetting("Chunk range", 3, 1, 8, 1));

    private volatile List<EspTarget> targets = Collections.emptyList();
    private final List<ChunkPos> scanQueue = new ArrayList<>();
    private final List<EspTarget> found = new ArrayList<>();
    private int scanCursor;
    private int refreshDelay;
    private ChunkPos scanCenter;

    public AmethystChunkFinder() {
        super("Amethyst Chunk Finder", "Marks loaded chunks containing budding amethyst", Category.WORLD);
    }

    @Override
    public List<EspTarget> targets() {
        return targets;
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) {
            clearScan();
            return;
        }

        ChunkPos playerChunk = mc.player.getChunkPos();
        if (scanCenter == null || Math.abs(playerChunk.x - scanCenter.x) > 1
                || Math.abs(playerChunk.z - scanCenter.z) > 1) {
            beginScan(playerChunk);
        } else if (scanCursor >= scanQueue.size()) {
            if (refreshDelay-- <= 0) {
                beginScan(playerChunk);
            }
            return;
        }

        // One already-loaded chunk per tick keeps the search work spread out.
        if (scanCursor < scanQueue.size()) {
            scanChunk(scanQueue.get(scanCursor++));
        }
        if (scanCursor >= scanQueue.size()) {
            BlockPos playerPos = mc.player.getBlockPos();
            found.sort(Comparator.comparingInt(t -> distanceSquared(t.pos(), playerPos)));
            targets = List.copyOf(found.subList(0, Math.min(found.size(), 36)));
            refreshDelay = 40;
        }
    }

    private void beginScan(ChunkPos center) {
        scanCenter = center;
        scanQueue.clear();
        found.clear();
        scanCursor = 0;
        int r = chunkRange.asInt();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                scanQueue.add(new ChunkPos(center.x + dx, center.z + dz));
            }
        }
    }

    private void scanChunk(ChunkPos pos) {
        WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(pos.x, pos.z);
        if (chunk == null) return;

        ChunkSection[] sections = chunk.getSectionArray();
        searchChunk:
        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section == null || section.isEmpty()
                    || !section.hasAny(state -> state.isOf(Blocks.BUDDING_AMETHYST))) {
                continue;
            }

            int baseY = chunk.sectionIndexToCoord(i) << 4;
            for (int lx = 0; lx < 16; lx++) {
                for (int ly = 0; ly < 16; ly++) {
                    for (int lz = 0; lz < 16; lz++) {
                        if (section.getBlockState(lx, ly, lz).isOf(Blocks.BUDDING_AMETHYST)) {
                            BlockPos block = new BlockPos(pos.getStartX() + lx, baseY + ly, pos.getStartZ() + lz);
                            found.add(new EspTarget(block, 0xFF67E6FF));
                            // One marker per chunk locates the geode without drawing a dense cluster.
                            break searchChunk;
                        }
                    }
                }
            }
        }
    }

    private void clearScan() {
        targets = Collections.emptyList();
        scanQueue.clear();
        found.clear();
        scanCenter = null;
        scanCursor = 0;
        refreshDelay = 0;
    }

    private static int distanceSquared(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        int dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    protected void onDisable() {
        clearScan();
    }
}


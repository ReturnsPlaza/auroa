package dev.aurora.module.impl;

import dev.aurora.hud.EspTarget;
import dev.aurora.module.Category;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Outlines containers and other storage-type blocks through walls. */
public class StorageEsp extends EspModule {
    private final BooleanSetting chests = add(new BooleanSetting("Chests", true));
    private final BooleanSetting enderChests = add(new BooleanSetting("Ender chests", true));
    private final BooleanSetting spawners = add(new BooleanSetting("Spawners", true));
    private final BooleanSetting shulkers = add(new BooleanSetting("Shulker boxes", true));
    private final BooleanSetting furnaces = add(new BooleanSetting("Furnaces", true));
    private final BooleanSetting barrels = add(new BooleanSetting("Barrels", true));
    private final BooleanSetting enchantingTables = add(new BooleanSetting("Enchanting tables", true));
    private final BooleanSetting droppers = add(new BooleanSetting("Droppers", true));
    private final BooleanSetting trialSpawners = add(new BooleanSetting("Trial spawners", true));
    private final BooleanSetting pistons = add(new BooleanSetting("Pistons", false));
    private final NumberSetting chunks = add(new NumberSetting("Chunks", 3, 2, 8, 1));

    private volatile List<EspTarget> targets = Collections.emptyList();
    private final List<ChunkPos> scanQueue = new ArrayList<>();
    private final List<EspTarget> scanFound = new ArrayList<>();
    private ChunkPos scanCenter;
    private Object scanWorld;
    private int scanRange = -1;
    private int scanCursor;
    private int refreshDelay;

    public StorageEsp() {
        super("Storage ESP", "Outlines chests, barrels, spawners and more through walls", Category.WORLD);
    }

    @Override
    public List<EspTarget> targets() {
        return targets;
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) {
            targets = Collections.emptyList();
            scanQueue.clear();
            scanFound.clear();
            scanCenter = null;
            scanWorld = null;
            scanCursor = 0;
            return;
        }
        ChunkPos center = mc.player.getChunkPos();
        int r = chunks.asInt();
        if (scanWorld != mc.world) {
            scanWorld = mc.world;
            targets = Collections.emptyList();
            beginScan(center, r);
        } else if (scanCenter == null || scanRange != r || Math.abs(center.x - scanCenter.x) > 1
                || Math.abs(center.z - scanCenter.z) > 1) {
            beginScan(center, r);
        }
        if (scanCursor >= scanQueue.size()) {
            if (refreshDelay-- <= 0) beginScan(center, r);
            return;
        }

        // Scan just one already-loaded chunk per tick, matching Donut's finder
        // strategy and avoiding large, periodic frame-time spikes.
        ChunkPos chunkPos = scanQueue.get(scanCursor++);
        WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z);
        if (chunk != null) {
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                int color = colorOf(be);
                if (color != 0) scanFound.add(new EspTarget(be.getPos(), color));
            }
            if (pistons.on()) scanPistons(chunk, scanFound);
        }
        if (scanCursor >= scanQueue.size()) {
            BlockPos playerPos = mc.player.getBlockPos();
            scanFound.sort(Comparator.comparingInt(t -> distanceSquared(t.pos(), playerPos)));
            targets = List.copyOf(scanFound.subList(0, Math.min(MAX_BOXES, scanFound.size())));
            refreshDelay = 40;
        }
    }

    private void beginScan(ChunkPos center, int range) {
        scanCenter = center;
        scanRange = range;
        scanCursor = 0;
        refreshDelay = 0;
        scanQueue.clear();
        scanFound.clear();
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                scanQueue.add(new ChunkPos(center.x + dx, center.z + dz));
            }
        }
    }

    private static int distanceSquared(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        int dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static final int MAX_BOXES = 36;

    private void scanPistons(WorldChunk chunk, List<EspTarget> found) {
        ChunkSection[] sections = chunk.getSectionArray();
        ChunkPos cp = chunk.getPos();
        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section == null || section.isEmpty()) {
                continue;
            }
            if (!section.hasAny(s -> s.isOf(Blocks.PISTON) || s.isOf(Blocks.STICKY_PISTON))) {
                continue;
            }
            int baseY = chunk.sectionIndexToCoord(i) << 4;
            for (int lx = 0; lx < 16; lx++) {
                for (int ly = 0; ly < 16; ly++) {
                    for (int lz = 0; lz < 16; lz++) {
                        BlockState st = section.getBlockState(lx, ly, lz);
                        if (st.isOf(Blocks.PISTON) || st.isOf(Blocks.STICKY_PISTON)) {
                            found.add(new EspTarget(
                                    new BlockPos(cp.getStartX() + lx, baseY + ly, cp.getStartZ() + lz), 0xFF8FD16B));
                            if (found.size() > 3000) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private int colorOf(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) {
            return chests.on() ? 0xFFFF4040 : 0;
        }
        if (be instanceof ChestBlockEntity) {
            return chests.on() ? 0xFFFFA52E : 0;
        }
        if (be instanceof BarrelBlockEntity) {
            return barrels.on() ? 0xFFE0B35A : 0;
        }
        if (be instanceof EnderChestBlockEntity) {
            return enderChests.on() ? 0xFFB05CFF : 0;
        }
        if (be instanceof ShulkerBoxBlockEntity) {
            return shulkers.on() ? 0xFF6ED3FF : 0;
        }
        if (be instanceof MobSpawnerBlockEntity) {
            return spawners.on() ? 0xFFFF4DFF : 0;
        }
        if (be instanceof TrialSpawnerBlockEntity) {
            return trialSpawners.on() ? 0xFFFF80E1 : 0;
        }
        if (be instanceof DropperBlockEntity) {
            return droppers.on() ? 0xFF6DDAC5 : 0;
        }
        if (be instanceof AbstractFurnaceBlockEntity) {
            return furnaces.on() ? 0xFFBFC5CC : 0;
        }
        if (be instanceof EnchantingTableBlockEntity) {
            return enchantingTables.on() ? 0xFF4DEBFF : 0;
        }
        return 0;
    }

    @Override
    protected void onDisable() {
        targets = Collections.emptyList();
        scanQueue.clear();
        scanFound.clear();
        scanCenter = null;
        scanWorld = null;
        scanCursor = 0;
        refreshDelay = 0;
    }
}


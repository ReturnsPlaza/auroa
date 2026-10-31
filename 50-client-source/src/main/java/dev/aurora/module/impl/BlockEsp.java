package dev.aurora.module.impl;

import dev.aurora.gui.Palette;
import dev.aurora.hud.EspTarget;
import dev.aurora.setting.ListSetting;
import dev.aurora.setting.ModeSetting;
import dev.aurora.setting.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Outlines blocks near you through walls. Pick a preset (Ores, Diamond, Debris,
 * Spawners) or use Custom with any blocks you like, added with commands:
 * /aurora blocks add minecraft:chest
 */
public class BlockEsp extends EspModule {
    private final ModeSetting target = add(new ModeSetting("Target", "Ores",
            "Ores", "Diamond", "Debris", "Spawners", "Custom"));
    private final ModeSetting color = add(new ModeSetting("Color", "Purple",
            concat("Auto", Palette.NAMES)));
    private final NumberSetting range = add(new NumberSetting("Range", 14, 6, 28, 2));
    private final ListSetting blocks = addHidden(new ListSetting("blocks"));

    private volatile List<EspTarget> targets = Collections.emptyList();
    private BlockPos scanOrigin;
    private BlockPos lastScanCenter;
    private int scanRange = -1;
    private int scanCursor;
    private int scanSide;
    private int refreshDelay;
    private Set<Block> scanCustom = Set.of();
    private final List<EspTarget> scanFound = new ArrayList<>();
    private static final int BLOCKS_PER_TICK = 4096;

    public BlockEsp() {
        super("Block ESP", "Outlines ores or any blocks you choose through walls");
    }

    private static String[] concat(String first, String[] rest) {
        String[] out = new String[rest.length + 1];
        out[0] = first;
        System.arraycopy(rest, 0, out, 1, rest.length);
        return out;
    }

    // ---- custom block list (used by the /aurora blocks commands)

    /** True when the Target setting is Custom, meaning the custom list is what gets highlighted. */
    public boolean customMode() {
        return target.is("Custom");
    }

    public List<String> customBlocks() {
        return blocks.get();
    }

    public boolean addBlock(Identifier id) {
        if (!Registries.BLOCK.containsId(id)) {
            return false;
        }
        String s = id.toString();
        if (!blocks.get().contains(s)) {
            blocks.get().add(s);
        }
        restartScan();
        return true;
    }

    public boolean removeBlock(Identifier id) {
        restartScan();
        return blocks.get().remove(id.toString());
    }

    public void clearBlocks() {
        blocks.get().clear();
        restartScan();
    }

    @Override
    public List<EspTarget> targets() {
        return targets;
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) {
            return;
        }
        BlockPos playerPos = mc.player.getBlockPos();
        int r = range.asInt();
        if (scanOrigin == null || scanRange != r || dist2(scanOrigin, playerPos) > 4) {
            if (refreshDelay > 0 && scanRange == r && scanOrigin == null && dist2(lastScanCenter, playerPos) <= 4) {
                refreshDelay--;
                return;
            }
            beginScan(playerPos, r);
        }

        BlockPos.Mutable pos = new BlockPos.Mutable();
        int end = Math.min(scanSide * scanSide * scanSide, scanCursor + BLOCKS_PER_TICK);
        for (; scanCursor < end; scanCursor++) {
            int x = scanCursor / (scanSide * scanSide) - scanRange;
            int y = (scanCursor / scanSide) % scanSide - scanRange;
            int z = scanCursor % scanSide - scanRange;
            pos.set(scanOrigin.getX() + x, scanOrigin.getY() + y, scanOrigin.getZ() + z);
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir()) continue;
            int c = colorOf(state, scanCustom);
            if (c != 0 && scanFound.size() < 1500) scanFound.add(new EspTarget(pos.toImmutable(), c));
        }
        if (scanCursor >= scanSide * scanSide * scanSide) {
            targets = List.copyOf(nearest(scanFound, scanOrigin));
            lastScanCenter = scanOrigin;
            scanOrigin = null;
            refreshDelay = 15;
        }
    }

    private void beginScan(BlockPos origin, int r) {
        scanOrigin = origin.toImmutable();
        scanRange = r;
        scanSide = r * 2 + 1;
        scanCursor = 0;
        scanFound.clear();
        Set<Block> custom = new HashSet<>();
        if (target.is("Custom")) {
            for (String s : blocks.get()) {
                Identifier id = Identifier.tryParse(s);
                if (id != null && Registries.BLOCK.containsId(id)) {
                    custom.add(Registries.BLOCK.get(id));
                }
            }
            if (custom.isEmpty()) {
                targets = Collections.emptyList();
                scanCursor = scanSide * scanSide * scanSide;
            }
        }
        scanCustom = custom;
    }

    private void restartScan() {
        scanOrigin = null;
        scanCursor = 0;
        refreshDelay = 0;
        scanFound.clear();
    }

    /** Keeps only the closest blocks, so the outline renderer never draws more than it can run smoothly. */
    private static List<EspTarget> nearest(List<EspTarget> found, BlockPos origin) {
        found.sort(Comparator.comparingInt(t -> dist2(t.pos(), origin)));
        return found.size() > MAX_BOXES ? new ArrayList<>(found.subList(0, MAX_BOXES)) : new ArrayList<>(found);
    }

    private static int dist2(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        int dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public static final int MAX_BOXES = 120;

    private int colorOf(BlockState s, Set<Block> custom) {
        int natural = naturalColor(s, custom);
        if (natural == 0) {
            return 0;
        }
        return color.is("Auto") ? natural : Palette.of(color.get());
    }

    private int naturalColor(BlockState s, Set<Block> custom) {
        if (target.is("Custom")) {
            if (!custom.contains(s.getBlock())) {
                return 0;
            }
            return Palette.hash(Registries.BLOCK.getId(s.getBlock()).toString());
        }
        if (target.is("Diamond")) {
            return s.isIn(BlockTags.DIAMOND_ORES) ? 0xFF4DEBFF : 0;
        }
        if (target.is("Debris")) {
            return s.isOf(Blocks.ANCIENT_DEBRIS) ? 0xFFC47A3A : 0;
        }
        if (target.is("Spawners")) {
            return s.isOf(Blocks.SPAWNER) ? 0xFFFF4DFF : 0;
        }
        // Ores
        if (s.isIn(BlockTags.DIAMOND_ORES)) {
            return 0xFF4DEBFF;
        }
        if (s.isIn(BlockTags.EMERALD_ORES)) {
            return 0xFF35E06A;
        }
        if (s.isIn(BlockTags.GOLD_ORES) || s.isOf(Blocks.NETHER_GOLD_ORE)) {
            return 0xFFFFD83D;
        }
        if (s.isIn(BlockTags.IRON_ORES)) {
            return 0xFFD8AF93;
        }
        if (s.isIn(BlockTags.COPPER_ORES)) {
            return 0xFFE07B3C;
        }
        if (s.isIn(BlockTags.REDSTONE_ORES)) {
            return 0xFFFF3B3B;
        }
        if (s.isIn(BlockTags.LAPIS_ORES)) {
            return 0xFF3D6BFF;
        }
        if (s.isIn(BlockTags.COAL_ORES)) {
            return 0xFF9AA0A6;
        }
        if (s.isOf(Blocks.NETHER_QUARTZ_ORE)) {
            return 0xFFF2EBDD;
        }
        if (s.isOf(Blocks.ANCIENT_DEBRIS)) {
            return 0xFFC47A3A;
        }
        return 0;
    }

    @Override
    protected void onDisable() {
        targets = Collections.emptyList();
        timer = 0;
    }
}

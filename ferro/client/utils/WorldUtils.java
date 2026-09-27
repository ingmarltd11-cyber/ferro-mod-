package dev.ferro.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * World queries: entity lookups, block searches and the hole/pillar checks that PvP modules need.
 */
public final class WorldUtils {

    private WorldUtils() {
    }

    /**
     * @return the local player, or {@code null}
     */
    public static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    /**
     * @return the client level, or {@code null}
     */
    public static ClientLevel level() {
        return Minecraft.getInstance().level;
    }

    /**
     * @param range the maximum distance
     * @return every entity within range of the player, sorted by distance
     */
    public static List<Entity> getEntities(double range) {
        return getEntities(range, entity -> true);
    }

    /**
     * @param range  the maximum distance
     * @param filter an extra predicate
     * @return every entity within range, sorted by distance
     */
    public static List<Entity> getEntities(double range, Predicate<Entity> filter) {
        List<Entity> result = new ArrayList<>();
        LocalPlayer player = player();
        ClientLevel level = level();
        if (player == null || level == null) {
            return result;
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == player || entity == player.getVehicle()) {
                continue;
            }
            if (entity.distanceTo(player) > range) {
                continue;
            }
            if (!filter.test(entity)) {
                continue;
            }
            result.add(entity);
        }
        result.sort(Comparator.comparingDouble(entity -> entity.distanceTo(player)));
        return result;
    }

    /**
     * @param range the maximum distance
     * @return every other player within range, sorted by distance
     */
    public static List<Player> getPlayers(double range) {
        List<Player> result = new ArrayList<>();
        for (Entity entity : getEntities(range, candidate -> candidate instanceof Player)) {
            result.add((Player) entity);
        }
        return result;
    }

    /**
     * @param range the maximum distance
     * @return every living entity within range, sorted by distance
     */
    public static List<LivingEntity> getLivingEntities(double range) {
        List<LivingEntity> result = new ArrayList<>();
        for (Entity entity : getEntities(range, candidate -> candidate instanceof LivingEntity)) {
            result.add((LivingEntity) entity);
        }
        return result;
    }

    /**
     * @param name the player name
     * @return {@code true} when the name belongs to a friend
     */
    public static boolean isFriend(String name) {
        return dev.ferro.client.Argon.get() != null
                && dev.ferro.client.Argon.get().getFriendManager().isFriend(name);
    }

    /**
     * @param player the player to test
     * @return {@code true} when the player is a friend
     */
    public static boolean isFriend(Player player) {
        return player != null && isFriend(player.getName().getString());
    }

    /**
     * @param entity the entity to test
     * @return {@code true} when the entity is another player that is alive and not a friend
     */
    public static boolean isValidTarget(Entity entity, double range) {
        if (!(entity instanceof Player other) || other == player()) {
            return false;
        }
        if (other.isDeadOrDying() || other.isSpectator() || other.isCreative()) {
            return false;
        }
        if (isFriend(other)) {
            return false;
        }
        return other.distanceTo(player()) <= range;
    }

    /**
     * The shared target filter of the combat modules: a living entity in range that is not the player, not
     * dead, not a spectator, not creative, not a friend and not invisible.
     *
     * <p>There is deliberately no bot filter here. Bots are players too, and a tab list check would quietly
     * make every combat module ignore them (and every real player on a server that hides its tab list).</p>
     *
     * @param entity the entity to test
     * @param range  the maximum distance in blocks
     * @return {@code true} when the entity can be attacked
     */
    public static boolean isCombatTarget(Entity entity, double range) {
        LocalPlayer self = player();
        if (self == null || !(entity instanceof LivingEntity living) || living == self) {
            return false;
        }
        if (living.isDeadOrDying() || living.isSpectator() || living.isInvisible()) {
            return false;
        }
        if (entity instanceof Player other) {
            if (other.getAbilities().instabuild || isFriend(other)) {
                return false;
            }
        }
        return entity.distanceTo(self) <= range;
    }

    /**
     * @param pos the block position
     * @return {@code true} when the block has a collision shape
     */
    public static boolean isSolid(BlockPos pos) {
        ClientLevel level = level();
        if (level == null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * @param pos the block position
     * @return {@code true} when the position is air
     */
    public static boolean isAir(BlockPos pos) {
        ClientLevel level = level();
        return level != null && level.getBlockState(pos).isAir();
    }

    /**
     * @param entity the entity whose feet to test
     * @return {@code true} when the entity stands in a one by one hole with all four sides blocked
     */
    public static boolean isInHole(Entity entity) {
        BlockPos feet = entity.blockPosition();
        boolean blocked = isSolid(feet.north()) && isSolid(feet.south()) && isSolid(feet.east()) && isSolid(feet.west());
        return blocked && isAir(feet) && isAir(feet.above());
    }

    /**
     * @param pos the position to test
     * @return {@code true} when the position is a valid hole
     */
    public static boolean isHole(BlockPos pos) {
        boolean blocked = isSolid(pos.north()) && isSolid(pos.south()) && isSolid(pos.east()) && isSolid(pos.west());
        return blocked && isSolid(pos.below()) && isAir(pos) && isAir(pos.above());
    }

    /**
     * @param center the centre position
     * @param range  the search radius
     * @param blocks the blocks to look for
     * @return the nearest matching block, or {@code null}
     */
    public static BlockPos findBlock(BlockPos center, int range, Block... blocks) {
        List<Block> wanted = List.of(blocks);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    ClientLevel level = level();
                    if (level == null) {
                        continue;
                    }
                    if (!wanted.contains(level.getBlockState(pos).getBlock())) {
                        continue;
                    }
                    double distance = pos.distSqr(center);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    /**
     * @param pos the block position
     * @return the centre of the block, at the top face
     */
    public static Vec3 topCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
    }

    /**
     * @param pos the block position
     * @return the centre of the block
     */
    public static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    /**
     * @param entity the entity
     * @return the bounding box of the entity
     */
    public static AABB box(Entity entity) {
        return entity.getBoundingBox();
    }

    /**
     * @param other the entity to test
     * @return {@code true} when the player has line of sight to the entity
     */
    public static boolean canSee(Entity other) {
        LocalPlayer player = player();
        return player != null && other != null && player.hasLineOfSight(other);
    }

    /**
     * @param pos the block position
     * @return {@code true} when the block can be replaced by a placed block
     */
    public static boolean isReplaceable(BlockPos pos) {
        ClientLevel level = level();
        if (level == null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || state.getBlock() == Blocks.FIRE;
    }

    /**
     * @param pos the block position
     * @return {@code true} when either block next to {@code pos} is obsidian
     */
    public static boolean isNextToObsidian(BlockPos pos) {
        ClientLevel level = level();
        if (level == null) {
            return false;
        }
        for (var direction : net.minecraft.core.Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).getBlock() == Blocks.OBSIDIAN) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param entity the entity
     * @return the health plus absorption of the entity
     */
    public static float getTotalHealth(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    /**
     * @param entity the entity
     * @return the armour value of the entity, {@code 0} for non players
     */
    public static int getArmor(Entity entity) {
        return entity instanceof LivingEntity living ? living.getArmorValue() : 0;
    }

    /**
     * Every block FERRO can look up by name. Registry lookups are deliberately avoided: this table
     * keeps block list settings working no matter how the registry API is renamed.
     *
     * @return a map from block path name to block
     */
    public static Map<String, Block> commonBlocks() {
        Map<String, Block> blocks = new LinkedHashMap<>();
        blocks.put("chest", Blocks.CHEST);
        blocks.put("trapped_chest", Blocks.TRAPPED_CHEST);
        blocks.put("ender_chest", Blocks.ENDER_CHEST);
        blocks.put("barrel", Blocks.BARREL);
        blocks.put("shulker_box", Blocks.SHULKER_BOX);
        blocks.put("obsidian", Blocks.OBSIDIAN);
        blocks.put("crying_obsidian", Blocks.CRYING_OBSIDIAN);
        blocks.put("respawn_anchor", Blocks.RESPAWN_ANCHOR);
        blocks.put("glowstone", Blocks.GLOWSTONE);
        blocks.put("water", Blocks.WATER);
        blocks.put("lava", Blocks.LAVA);
        blocks.put("farmland", Blocks.FARMLAND);
        blocks.put("wheat", Blocks.WHEAT);
        blocks.put("spawner", Blocks.SPAWNER);
        blocks.put("crafting_table", Blocks.CRAFTING_TABLE);
        blocks.put("furnace", Blocks.FURNACE);
        blocks.put("hopper", Blocks.HOPPER);
        blocks.put("dispenser", Blocks.DISPENSER);
        blocks.put("dropper", Blocks.DROPPER);
        blocks.put("bedrock", Blocks.BEDROCK);
        blocks.put("dragon_egg", Blocks.DRAGON_EGG);
        blocks.put("netherite_block", Blocks.NETHERITE_BLOCK);
        blocks.put("ancient_debris", Blocks.ANCIENT_DEBRIS);
        blocks.put("diamond_ore", Blocks.DIAMOND_ORE);
        blocks.put("deepslate_diamond_ore", Blocks.DEEPSLATE_DIAMOND_ORE);
        blocks.put("gold_ore", Blocks.GOLD_ORE);
        blocks.put("deepslate_gold_ore", Blocks.DEEPSLATE_GOLD_ORE);
        blocks.put("iron_ore", Blocks.IRON_ORE);
        blocks.put("deepslate_iron_ore", Blocks.DEEPSLATE_IRON_ORE);
        blocks.put("coal_ore", Blocks.COAL_ORE);
        blocks.put("deepslate_coal_ore", Blocks.DEEPSLATE_COAL_ORE);
        blocks.put("redstone_ore", Blocks.REDSTONE_ORE);
        blocks.put("deepslate_redstone_ore", Blocks.DEEPSLATE_REDSTONE_ORE);
        blocks.put("lapis_ore", Blocks.LAPIS_ORE);
        blocks.put("deepslate_lapis_ore", Blocks.DEEPSLATE_LAPIS_ORE);
        blocks.put("emerald_ore", Blocks.EMERALD_ORE);
        blocks.put("deepslate_emerald_ore", Blocks.DEEPSLATE_EMERALD_ORE);
        blocks.put("copper_ore", Blocks.COPPER_ORE);
        blocks.put("deepslate_copper_ore", Blocks.DEEPSLATE_COPPER_ORE);
        blocks.put("nether_quartz_ore", Blocks.NETHER_QUARTZ_ORE);
        return blocks;
    }

    /**
     * @param block the block
     * @return the path name of the block, for example {@code diamond_ore}
     */
    public static String blockName(Block block) {
        for (Map.Entry<String, Block> entry : commonBlocks().entrySet()) {
            if (entry.getValue() == block) {
                return entry.getKey();
            }
        }
        return "";
    }

    /**
     * Parses a comma separated block list into blocks.
     *
     * @param names the list, for example {@code "diamond_ore, ancient_debris"}
     * @return the resolved blocks, unknown names are skipped
     */
    public static List<Block> parseBlocks(String names) {
        List<Block> result = new ArrayList<>();
        if (names == null || names.isBlank()) {
            return result;
        }
        Map<String, Block> lookup = commonBlocks();
        for (String name : names.split(",")) {
            Block block = lookup.get(name.trim().toLowerCase(java.util.Locale.ROOT));
            if (block != null && !result.contains(block)) {
                result.add(block);
            }
        }
        return result;
    }

    /**
     * @param states the block states to test
     * @param blocks the blocks to look for
     * @return {@code true} when any of the states is one of the blocks
     */
    public static boolean matches(BlockState state, List<Block> blocks) {
        return state != null && blocks.contains(state.getBlock());
    }

    /**
     * Places the held block against a support block.
     *
     * @param player  the local player
     * @param support the block the new block is placed against
     * @param face    the face of the support block to place on
     * @return {@code true} when an interaction was sent
     */
    public static boolean placeOn(LocalPlayer player, BlockPos support, Direction face) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft.gameMode == null) {
            return false;
        }
        if (!isSolid(support)) {
            return false;
        }
        Vec3 hit = center(support).add(face.getStepX() * 0.5D, face.getStepY() * 0.5D, face.getStepZ() * 0.5D);
        // Look at the spot with the real camera: the client has no silent rotation to fall back on.
        float[] look = RotationUtils.calculate(player.getEyePosition(), hit);
        player.setYRot(look[0]);
        player.setXRot((float) dev.ferro.client.utils.MathUtils.clamp(look[1], -90.0D, 90.0D));
        minecraft.gameMode.useItemOn(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(hit, face, support, false));
        return true;
    }

    /**
     * Places the held block at an empty position by finding a solid neighbour to place against.
     *
     * @param player the local player
     * @param target the position that should receive the new block
     * @return {@code true} when an interaction was sent
     */
    public static boolean placeAt(LocalPlayer player, BlockPos target) {
        if (!isReplaceable(target)) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            BlockPos support = target.relative(direction);
            if (isSolid(support)) {
                return placeOn(player, support, direction.getOpposite());
            }
        }
        return false;
    }

    /**
     * @param from the position to start from
     * @param to   the position to look at
     * @return the direction that points from {@code from} to {@code to}
     */
    public static Direction directionTo(BlockPos from, Vec3 to) {
        double dx = to.x - (from.getX() + 0.5D);
        double dy = to.y - (from.getY() + 0.5D);
        double dz = to.z - (from.getZ() + 0.5D);
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        if (ay >= ax && ay >= az) {
            return dy > 0.0D ? Direction.UP : Direction.DOWN;
        }
        if (ax >= az) {
            return dx > 0.0D ? Direction.EAST : Direction.WEST;
        }
        return dz > 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    /**
     * Scans a cube around a position for matching blocks.
     *
     * <p>Expensive by nature: callers should scan on an interval and cache the result instead of doing
     * this every frame.</p>
     *
     * @param center     the centre of the scan
     * @param radius     the radius in blocks
     * @param predicate  the filter
     * @param maxResults the maximum amount of results
     * @return the matching positions
     */
    public static List<BlockPos> scanBlocks(BlockPos center, int radius,
                                            java.util.function.Predicate<BlockState> predicate, int maxResults) {
        List<BlockPos> result = new ArrayList<>();
        ClientLevel level = level();
        if (level == null) {
            return result;
        }
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!predicate.test(level.getBlockState(pos))) {
                        continue;
                    }
                    result.add(pos);
                    if (result.size() >= maxResults) {
                        return result;
                    }
                }
            }
        }
        return result;
    }
}

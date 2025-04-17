package com.karasu256.teamUtils.utils;

import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import com.karasu256.teamUtils.TeamUtils;

public class LocationUtils {

    private static final Random random = new Random();
    private static final int MAX_ATTEMPTS = 50; // 安全な場所を探す最大試行回数
    private static final int MIN_SAFE_DISTANCE = 30; // 指定範囲から最低限離れるべき距離
    private static final Logger LOGGER = TeamUtils.LOGGER;

    /**
     * 指定されたx, z座標で安全なy座標を見つけます。
     * ビーコンのビームが出る条件（上空に遮るものがない）を利用して安全性を判断します。
     * 
     * @param world 対象のワールド
     * @param x     X座標
     * @param z     Z座標
     * @return 安全な場所のLocation、見つからない場合はnull
     */
    private static Location findSafeY(World world, double x, double z) {
        // 地表から探索を開始
        int startY = world.getHighestBlockYAt((int) x, (int) z);

        // 高すぎる場合は適正な高さから始める
        if (startY > 256)
            startY = 256;
        else if (startY < 60)
            startY = 100; // 地下が深い場合は空中から探す

        // 上から下に探索
        for (int y = startY; y > 0; y--) {
            Block block = world.getBlockAt((int) x, y, (int) z);
            Block below = world.getBlockAt((int) x, y - 1, (int) z);

            // 1. 現在地が通過可能
            // 2. 足元が固体でかつ危険でない
            if (isAirOrSafe(block) && isSolid(below) && !isHazardous(below)) {
                // ビーコンの条件：上空が遮られていないことを確認
                if (hasDirectSkyAccess(world, (int) x, y, (int) z)) {
                    // プレイヤーの頭上のスペースを確認（2ブロック分）
                    if (isAirOrSafe(world.getBlockAt((int) x, y + 1, (int) z)) &&
                            isAirOrSafe(world.getBlockAt((int) x, y + 2, (int) z))) {
                        return new Location(world, x + 0.5, y, z + 0.5);
                    }
                }
            }
        }

        // 安全な場所が見つからなかった場合、空中に生成（ただし天井がない場所を探す）
        for (int y = Math.min(startY, 200); y > 100; y -= 5) {
            if (hasDirectSkyAccess(world, (int) x, y, (int) z) &&
                    isAirOrSafe(world.getBlockAt((int) x, y, (int) z)) &&
                    isAirOrSafe(world.getBlockAt((int) x, y + 1, (int) z)) &&
                    isAirOrSafe(world.getBlockAt((int) x, y + 2, (int) z))) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }

        // 最終的にはワールドの高い位置に配置
        return new Location(world, x + 0.5, 150, z + 0.5);
    }

    /**
     * 指定された位置から空までの直接のアクセスがあるかを確認します。
     * ビーコンのビームが出る条件と同様に、上空に遮るものがないかチェックします。
     * 
     * @param world 対象のワールド
     * @param x     X座標
     * @param y     Y座標
     * @param z     Z座標
     * @return 空に直接アクセスできるならtrue
     */
    private static boolean hasDirectSkyAccess(World world, int x, int y, int z) {
        // 現在の位置から最大建築高度まで確認
        int maxHeight = world.getMaxHeight();

        for (int checkY = y + 1; checkY < maxHeight; checkY++) {
            Block blockAbove = world.getBlockAt(x, checkY, z);
            if (!isAirOrSafe(blockAbove) || blockAbove.getType().isOccluding()) {
                return false; // 遮るブロックがある
            }
        }

        return true; // 空までの直接アクセスがある
    }

    /**
     * ブロックが空気または安全なブロック（プレイヤーが通過できる）かどうかを判定します。
     */
    private static boolean isAirOrSafe(Block block) {
        Material type = block.getType();
        return type == Material.AIR ||
                type == Material.CAVE_AIR ||
                type == Material.VOID_AIR ||
                type == Material.WATER ||
                !type.isSolid();
    }

    /**
     * ブロックが固体かどうかを判定します。
     */
    private static boolean isSolid(Block block) {
        return block.getType().isSolid();
    }

    /**
     * ブロックが危険かどうかを判定します。
     */
    private static boolean isHazardous(Block block) {
        Material type = block.getType();
        return type == Material.LAVA ||
                type == Material.FIRE ||
                type == Material.MAGMA_BLOCK ||
                type == Material.CACTUS ||
                type == Material.CAMPFIRE ||
                type == Material.SOUL_CAMPFIRE;
    }

    /**
     * 全チームのリーダーを安全な場所にランダムにテレポートさせます。
     * チームリーダー同士が近すぎる場所にテレポートしないように調整します。
     * 
     * @return テレポートに成功したリーダーの数
     */
    public static int teleportTeamLeadersToSafeLocations() {
        final int[] successCount = { 0 };

        // すでにテレポートしたリーダーの位置を記録して、他のリーダーが近くにテレポートしないようにする
        final List<Location> leaderLocations = new ArrayList<>();

        // チームリーダーごとに処理
        TeamUtility.forEachTeamLeader((leader, team) -> {
            LOGGER.info("Teleporting team leader " + leader.getName() + "...");

            // このリーダーを他のリーダーから離れた安全な場所にテレポート
            boolean success = teleportLeaderToSafeLocation(leader, team, leaderLocations);

            if (success) {
                successCount[0]++;
                leaderLocations.add(leader.getLocation()); // 成功したらリーダーの新しい位置を記録
                LOGGER.info("Successfully teleported team leader " + leader.getName() + ": " +
                        leader.getLocation().getBlockX() + ", " +
                        leader.getLocation().getBlockY() + ", " +
                        leader.getLocation().getBlockZ());
            } else {
                LOGGER.warning("Failed to teleport team leader " + leader.getName());
            }
        });

        return successCount[0];
    }

    /**
     * チームリーダーではないチームに属したプレイヤーをそのチームのリーダー付近にスポーンさせます。
     * リーダーの周辺の安全な場所を探して配置します。
     * 
     * @return テレポートに成功したプレイヤーの数
     */
    public static int teleportTeamMembersNearLeaders() {
        final int[] successCount = { 0 };

        // チームごとのリーダー位置を記録
        final Map<Team, List<Location>> teamLeaderLocations = new HashMap<>();

        // まずリーダーの位置情報を収集
        TeamUtility.forEachTeamLeader((leader, team) -> {
            if (!teamLeaderLocations.containsKey(team)) {
                teamLeaderLocations.put(team, new ArrayList<>());
            }
            teamLeaderLocations.get(team).add(leader.getLocation());
        });

        // チームメンバー（リーダー以外）を処理
        TeamUtility.forEachTeamPlayer(player -> {
            // プレイヤーのチームを取得
            Team playerTeam = player.getScoreboard().getEntryTeam(player.getName());

            // リーダーでない場合のみ処理
            if (playerTeam != null && !TeamUtility.isTeamLeader(player)) {
                // プレイヤーのチームのリーダー位置を取得
                List<Location> leaderLocations = teamLeaderLocations.get(playerTeam);

                if (leaderLocations != null && !leaderLocations.isEmpty()) {
                    // リーダーのランダムな位置を選択
                    Location leaderLocation = leaderLocations.get(random.nextInt(leaderLocations.size()));

                    // リーダー付近の安全な場所にテレポート
                    boolean success = teleportPlayerNearLocation(player, leaderLocation, 5, 20);

                    if (success) {
                        successCount[0]++;
                        LOGGER.info("Teleported player " + player.getName() + " near team leader: " +
                                player.getLocation().getBlockX() + ", " +
                                player.getLocation().getBlockY() + ", " +
                                player.getLocation().getBlockZ());
                    } else {
                        LOGGER.warning("Failed to teleport player " + player.getName());
                    }
                } else {
                    LOGGER.warning("No leader found for player " + player.getName() + "'s team");
                }
            }
        });

        return successCount[0];
    }

    /**
     * テレポート後にプレイヤーの下に地面がない場合、安全な地面を見つけて再テレポートします。
     * また、プレイヤーが水中にいる場合は、上方向に空気を探して再テレポートします。
     * 
     * @param player   テレポートするプレイヤー
     * @param location テレポート先の位置
     * @return テレポートに成功したかどうか
     */
    private static boolean teleportAndEnsureGround(Player player, Location location) {
        if (player == null || location == null) {
            return false;
        }

        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        // テレポート前にチャンクをロード
        world.getChunkAt(location).load(true);

        // テレポート前に溶岩チェック
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // プレイヤーの位置と足元のブロックを確認
        Block targetBlock = world.getBlockAt(x, y, z);
        Block belowBlock = world.getBlockAt(x, y - 1, z);

        // 溶岩チェック - プレイヤーの位置や足元が溶岩の場合は別の場所を探す
        if (targetBlock.getType() == Material.LAVA || belowBlock.getType() == Material.LAVA) {
            LOGGER.warning("Target location for player " + player.getName() + " contains lava. Finding alternative...");

            // 周囲の安全な場所を探す (半径5ブロック内)
            for (int offsetX = -5; offsetX <= 5; offsetX++) {
                for (int offsetZ = -5; offsetZ <= 5; offsetZ++) {
                    // 中心から遠い順に確認（溶岩から離れるため）
                    if (offsetX * offsetX + offsetZ * offsetZ < 4)
                        continue; // 中心近くはスキップ

                    for (int offsetY = 0; offsetY <= 3; offsetY++) {
                        Block checkBlock = world.getBlockAt(x + offsetX, y + offsetY, z + offsetZ);
                        Block checkBelow = world.getBlockAt(x + offsetX, y + offsetY - 1, z + offsetZ);

                        if (isAirOrSafe(checkBlock) && !checkBlock.getType().equals(Material.LAVA) &&
                                isSolid(checkBelow) && !checkBelow.getType().equals(Material.LAVA)) {
                            // 安全な場所が見つかった
                            location = new Location(world, x + offsetX + 0.5, y + offsetY, z + offsetZ + 0.5,
                                    location.getYaw(), location.getPitch());
                            LOGGER.info("Found safe alternative location away from lava");
                            break;
                        }
                    }
                }
            }

            // それでも安全な場所が見つからない場合、元の位置より高い場所を試す
            if (targetBlock.getType() == Material.LAVA || belowBlock.getType() == Material.LAVA) {
                LOGGER.info("Trying location above lava for player " + player.getName());
                location = new Location(world, x + 0.5, y + 5, z + 0.5, location.getYaw(), location.getPitch());
            }
        }

        // テレポート実行
        player.teleport(location);

        // プレイヤーの現在位置を取得
        Location playerLoc = player.getLocation();
        x = playerLoc.getBlockX();
        y = playerLoc.getBlockY();
        z = playerLoc.getBlockZ();

        // プレイヤーの位置または頭上が水かどうか確認
        Block playerBlock = world.getBlockAt(x, y, z);
        Block headBlock = world.getBlockAt(x, y + 1, z);

        // 念のためテレポート後も溶岩チェック
        if (playerBlock.getType() == Material.LAVA) {
            LOGGER.warning("Player " + player.getName() + " teleported into lava! Emergency teleport...");
            // 緊急避難 - 高い位置に再テレポート
            Location emergencyLoc = new Location(world, x + 0.5, y + 10, z + 0.5,
                    playerLoc.getYaw(), playerLoc.getPitch());
            player.teleport(emergencyLoc);
            return true; // 緊急避難したので処理終了
        }

        // 昆布や海藻が存在する可能性があるため、それらのブロックがあるか確認
        boolean isPlayerInSeaweed = isSeaweed(playerBlock);
        boolean isHeadInSeaweed = isSeaweed(headBlock);

        // プレイヤーが水中または昆布/海藻内にいる場合、上方向に空気を探す
        if (playerBlock.getType() == Material.WATER || headBlock.getType() == Material.WATER ||
                isPlayerInSeaweed || isHeadInSeaweed) {
            LOGGER.info("Player " + player.getName() + " is underwater or in seaweed. Searching for surface...");

            // 上方向に空気を探す（最大50ブロック）
            for (int checkY = y + 1; checkY < Math.min(world.getMaxHeight(), y + 50); checkY++) {
                Block block = world.getBlockAt(x, checkY, z);
                Block aboveBlock = world.getBlockAt(x, checkY + 1, z);

                // 水の表面（その上が空気）を見つけた場合、または昆布/海藻の上が空気の場合
                if ((block.getType() == Material.WATER || isSeaweed(block)) &&
                        (aboveBlock.getType() == Material.AIR ||
                                aboveBlock.getType() == Material.CAVE_AIR ||
                                aboveBlock.getType() == Material.VOID_AIR)) {

                    // プレイヤーを水面の上にテレポート
                    Location surfaceLoc = new Location(world, x + 0.5, checkY + 1, z + 0.5,
                            playerLoc.getYaw(), playerLoc.getPitch());

                    LOGGER.info("Teleporting player " + player.getName() + " to water surface: " +
                            surfaceLoc.getBlockX() + ", " + surfaceLoc.getBlockY() + ", " + surfaceLoc.getBlockZ());
                    return player.teleport(surfaceLoc);
                }

                // すでに空気を見つけた場合
                if ((block.getType() == Material.AIR ||
                        block.getType() == Material.CAVE_AIR ||
                        block.getType() == Material.VOID_AIR) &&
                        (aboveBlock.getType() == Material.AIR ||
                                aboveBlock.getType() == Material.CAVE_AIR ||
                                aboveBlock.getType() == Material.VOID_AIR)) {

                    // プレイヤーをその位置にテレポート
                    Location airLoc = new Location(world, x + 0.5, checkY, z + 0.5,
                            playerLoc.getYaw(), playerLoc.getPitch());

                    LOGGER.info("Teleporting player " + player.getName() + " to air location: " +
                            airLoc.getBlockX() + ", " + airLoc.getBlockY() + ", " + airLoc.getBlockZ());
                    return player.teleport(airLoc);
                }
            }

            LOGGER.warning("No safe air found above player " + player.getName());
        }

        // プレイヤーの足元が空気かどうか確認
        Block blockBelow = world.getBlockAt(x, y - 1, z);

        // 足元が空気または通過可能なブロックの場合、地面を探す
        if (isAirOrSafe(blockBelow) && !isSolid(blockBelow)) {
            LOGGER.info("Player " + player.getName() + " has no ground below. Searching for ground...");

            // 下方向に安全な地面を探す（最大100ブロック）
            for (int checkY = y - 1; checkY > Math.max(0, y - 100); checkY--) {
                Block block = world.getBlockAt(x, checkY, z);

                // 固体のブロックを見つけた場合
                if (isSolid(block) && !isHazardous(block)) {
                    // プレイヤーを地面の上にテレポート
                    Location groundLoc = new Location(world, x + 0.5, checkY + 1, z + 0.5,
                            playerLoc.getYaw(), playerLoc.getPitch());

                    // 頭上のスペースを確認
                    if (isAirOrSafe(world.getBlockAt(x, checkY + 1, z)) &&
                            isAirOrSafe(world.getBlockAt(x, checkY + 2, z))) {
                        LOGGER.info("Teleporting player " + player.getName() + " to ground: " +
                                groundLoc.getBlockX() + ", " + groundLoc.getBlockY() + ", " + groundLoc.getBlockZ());
                        return player.teleport(groundLoc);
                    }
                }
            }

            LOGGER.warning("No safe ground found below player " + player.getName());
        }

        // 元々足元に地面があるか、地面が見つからない場合は現状維持
        return true;
    }

    /**
     * プレイヤーを指定された位置の周辺の安全な場所にテレポートさせます。
     * 
     * @param player      プレイヤー
     * @param center      中心位置
     * @param minDistance 最小距離（これより近くには配置しない）
     * @param maxDistance 最大距離（これより遠くには配置しない）
     * @return テレポートに成功したかどうか
     */
    public static boolean teleportPlayerNearLocation(Player player, Location center, double minDistance,
            double maxDistance) {
        if (player == null || center == null) {
            return false;
        }

        World world = center.getWorld();
        if (world == null) {
            return false;
        }

        // 最大試行回数
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // 中心位置からランダムな角度と距離を選択
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);

            // 新しい位置を計算
            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;

            // ワールドボーダー内かどうか確認
            WorldBorder border = world.getWorldBorder();
            Location borderCenter = border.getCenter();
            double borderRadius = border.getSize() / 2;

            if (Math.abs(x - borderCenter.getX()) < borderRadius &&
                    Math.abs(z - borderCenter.getZ()) < borderRadius) {

                // 安全な高さを見つける
                Location safeLoc = findSafeY(world, x, z);
                if (safeLoc != null) {
                    return teleportAndEnsureGround(player, safeLoc);
                }
            }
        }

        // 安全な場所が見つからなかった場合、直接中心位置の近くにテレポート
        Location fallbackLocation = center.clone();
        fallbackLocation.add((random.nextDouble() * 2 - 1) * 5, 1, (random.nextDouble() * 2 - 1) * 5);
        return teleportAndEnsureGround(player, fallbackLocation);
    }

    /**
     * ワールドボーダーのサイズを500に設定し、指定されたプレイヤーの位置をボーダーの中心に設定します。
     * 
     * @param player ボーダーの中心に設定するプレイヤー
     */
    public static void setWorldBorderCenteredOnPlayer(Player player) {
        // ...existing code...
    }

    /**
     * ワールドボーダー内の安全な場所にプレイヤーをスポーンさせます。
     * 
     * @param player スポーンさせるプレイヤー
     * @return スポーンに成功したかどうか
     */
    public static boolean spawnPlayerInSafeLocation(Player player) {
        return spawnPlayerInSafeLocation(player, null, 0, 0, 0);
    }

    /**
     * ワールドボーダー内の安全な場所にプレイヤーをスポーンさせます。
     * 指定された範囲から一定距離離れた場所にスポーンします。
     * 
     * @param player      スポーンさせるプレイヤー
     * @param avoidCenter 避けるべき中心位置（nullの場合は考慮しない）
     * @param avoidDx     避けるべきX方向の範囲
     * @param avoidDy     避けるべきY方向の範囲
     * @param avoidDz     避けるべきZ方向の範囲
     * @return スポーンに成功したかどうか
     */
    public static boolean spawnPlayerInSafeLocation(Player player, Location avoidCenter, double avoidDx, double avoidDy,
            double avoidDz) {
        if (player == null) {
            return false;
        }

        World world = player.getWorld();
        WorldBorder border = world.getWorldBorder();
        Location borderCenter = border.getCenter();
        double borderSize = border.getSize() / 2; // 中心からの距離

        // ボーダーの有効範囲を少し小さくして、ボーダーぎりぎりにスポーンしないようにする
        double effectiveBorderSize = borderSize * 0.9;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // ランダムな座標を生成
            double x = borderCenter.getX() + (random.nextDouble() * 2 - 1) * effectiveBorderSize;
            double z = borderCenter.getZ() + (random.nextDouble() * 2 - 1) * effectiveBorderSize;

            // 避けるべき範囲が指定されている場合、その範囲から十分離れているか確認
            if (avoidCenter != null) {
                double distanceX = Math.abs(x - avoidCenter.getX());
                double distanceZ = Math.abs(z - avoidCenter.getZ());

                // X軸またはZ軸の距離が小さい場合、この位置は避ける
                if (distanceX < avoidDx + MIN_SAFE_DISTANCE || distanceZ < avoidDz + MIN_SAFE_DISTANCE) {
                    continue; // 次の候補地を試す
                }
            }

            // 安全な高さを見つける
            Location safeLoc = findSafeY(world, x, z);
            if (safeLoc != null) {
                return teleportAndEnsureGround(player, safeLoc);
            }
        }

        return false; // 安全な場所が見つからなかった
    }

    /**
     * 個別のチームリーダーを安全な場所にテレポートさせます。
     * 他のリーダーから離れた場所に配置します。
     * 
     * @param leader               テレポートさせるリーダー
     * @param team                 リーダーのチーム
     * @param otherLeaderLocations 他のリーダーの位置のリスト
     * @return テレポートに成功したかどうか
     */
    private static boolean teleportLeaderToSafeLocation(Player leader, Team team,
            List<Location> otherLeaderLocations) {
        // 他のリーダーから十分離れた場所を見つける
        // リーダーに最も近い他のリーダーの位置を取得
        Location nearestLeaderLoc = findNearestLocation(leader.getLocation(), otherLeaderLocations);

        if (nearestLeaderLoc == null) {
            // 他のリーダーがまだいない場合は、単純に安全な場所へテレポート
            return spawnPlayerInSafeLocation(leader);
        } else {
            // 他のリーダーの位置から離れた場所にテレポート
            double avoidDistance = MIN_SAFE_DISTANCE * 2; // リーダー間の距離は通常の2倍確保
            return spawnPlayerInSafeLocation(leader, nearestLeaderLoc, avoidDistance, avoidDistance, avoidDistance);
        }
    }

    /**
     * 基準位置から最も近い位置を見つけます
     * 
     * @param baseLocation 基準位置
     * @param locations    検索対象の位置のリスト
     * @return 最も近い位置、リストが空の場合はnull
     */
    private static Location findNearestLocation(Location baseLocation, List<Location> locations) {
        if (locations.isEmpty()) {
            return null;
        }

        Location nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Location loc : locations) {
            if (loc.getWorld().equals(baseLocation.getWorld())) {
                double distance = loc.distanceSquared(baseLocation);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = loc;
                }
            }
        }

        return nearest;
    }

    /**
     * ブロックが昆布や海藻かどうかを判定します。
     * これらのブロックは通常水中に存在し、プレイヤーが埋まっていると水中として扱う必要があります。
     * 
     * @param block 確認するブロック
     * @return 昆布や海藻ならtrue
     */
    private static boolean isSeaweed(Block block) {
        Material type = block.getType();
        return type == Material.KELP ||
                type == Material.KELP_PLANT ||
                type == Material.SEAGRASS ||
                type == Material.TALL_SEAGRASS;
    }
}

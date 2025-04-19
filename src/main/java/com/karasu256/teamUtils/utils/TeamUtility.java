package com.karasu256.teamUtils.utils;

import com.karasu256.teamUtils.TeamUtils;
import com.karasu256.teamUtils.config.GameConfig;
import com.karasu256.teamUtils.exception.TeamUtilityException;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TeamUtility {
    private static final Logger LOGGER = TeamUtils.LOGGER;
    private static List<Team> teams;
    private static Team spectatorTeam;
    private static final String LEADER_OBJECTIVE = "teamLeader";
    private static final String TEAM_SIZE_OBJECTIVE = "teamSize";
    private static List<String> lastTeamMembers;
    private static List<String> lastSpectatorMembers;
    private static boolean isGameRunning = false;
    private static final Map<Player, Boolean> teamLeaderMap = new HashMap<>();

    // プレイヤーのチーム状態をキャッシュするマップ
    private static final Map<UUID, String> playerTeamCache = new HashMap<>();

    // リーダーリスポーン待機関連
    private static final Map<UUID, Consumer<Player>> respawnCallbacks = new HashMap<>();
    private static final Map<UUID, Team> deadLeaders = new HashMap<>();

    // ゲームモード待機関連
    private static final Map<UUID, Consumer<Player>> gameModeCallbacks = new HashMap<>();
    private static final Map<UUID, org.bukkit.GameMode> targetGameModes = new HashMap<>();

    private static Scoreboard getScoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    private static Objective getObjective(String name) {
        return getScoreboard().getObjective(name);
    }

    private static void getOrCreateObjective(String name, Component displayName) {
        Objective objective = getObjective(name);
        if (objective == null) {
            getScoreboard().registerNewObjective(name, Criteria.DUMMY, displayName);
        }
    }

    public static void reloadIsGameRunning() {
        TeamUtils.getPlugin().reloadConfig("game_config.json", GameConfig.class);
    }

    /**
     * チームのキャッシュをリロードします
     */
    public static void reloadTeamCache() {
        teams = getScoreboard().getTeams().stream()
                .filter(team -> !team.getName().equals("spectator"))
                .toList();

        spectatorTeam = getScoreboard().getTeam("spectator");
        if (spectatorTeam == null) {
            spectatorTeam = getScoreboard().registerNewTeam("spectator");
            spectatorTeam.displayName(Component.text("観戦"));
            spectatorTeam.color(NamedTextColor.GRAY);
        }

        // プレイヤーチームキャッシュを更新
        updatePlayerTeamCache();
    }

    /**
     * プレイヤーのチーム所属状態をキャッシュに更新します
     */
    private static void updatePlayerTeamCache() {
        // キャッシュをクリア
        playerTeamCache.clear();

        // 全オンラインプレイヤーのチーム情報をキャッシュに追加
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = getScoreboard().getEntryTeam(player.getName());
            if (team != null) {
                playerTeamCache.put(player.getUniqueId(), team.getName());
            }
        }

        // オフラインのエントリも処理
        getScoreboard().getTeams().forEach(team -> {
            team.getEntries().forEach(entry -> {
                Player player = Bukkit.getPlayer(entry);
                if (player != null) {
                    playerTeamCache.put(player.getUniqueId(), team.getName());
                }
            });
        });

        LOGGER.info("Updated team cache: " + playerTeamCache.size() + "entries");
    }

    /**
     * プレイヤーのチーム所属をキャッシュから取得します
     * 
     * @param player プレイヤー
     * @return チーム名、所属していない場合はnull
     */
    public static String getCachedPlayerTeam(Player player) {
        if (player == null) {
            return null;
        }
        return playerTeamCache.get(player.getUniqueId());
    }

    /**
     * チーム初期化時の処理を行います
     */
    public static void initialize() {
        reloadTeamCache();

        // 各チームにランダムな色を設定し、色と名前を合わせる
        teams.forEach(team -> {
            team.color(ColorUtils.getRandomNamedTextColor());
            updateTeamDisplayName(team);
        });

        // スコアボード初期化
        getOrCreateObjective(LEADER_OBJECTIVE, Component.text("チームリーダー"));
        getOrCreateObjective(TEAM_SIZE_OBJECTIVE, Component.text("チームサイズ"));
        lastTeamMembers = new ArrayList<>();
        lastSpectatorMembers = new ArrayList<>();
        updateLastTeamState();

        reloadIsGameRunning();
    }

    /**
     * チームの表示名を更新します
     * チームリーダーがいる場合はリーダーの名前に、いない場合は色に基づいた名前にします
     * 
     * @param team 更新するチーム
     */
    private static void updateTeamDisplayName(Team team) {
        if (team == null || team == spectatorTeam) {
            return;
        }

        // チームリーダーを探す
        List<Player> leaders = team.getEntries().stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline() && isTeamLeader(player))
                .toList();
        if (!leaders.isEmpty()) {
            // リーダーがいる場合は、リーダーの名前を表示名に設定
            team.displayName(Component.text(leaders.getFirst().getName()));
        } else {
            // リーダーがいない場合は、チームの色に基づいた名前を設定
            team.displayName(Component.text(team.color().examinableName()));
        }
        try {
            ChatColor teamColor = textColorToChatColor(team.color());
            TextColor color = ColorUtils.convert(teamColor);
            team.displayName().color(color);
        } catch (IllegalStateException e) {
            LOGGER.warning("Could not set the team(" + team.getName() + ") color.Is the team color modified?");
        }
    }

    private static void updateLastTeamState() {
        lastTeamMembers = teams.stream()
                .flatMap(team -> team.getEntries().stream())
                .toList();
        lastSpectatorMembers = new ArrayList<>(spectatorTeam.getEntries());
    }

    public static void update() {
        if (!isGameRunning())
            return;

        boolean needsUpdate = false;

        // オフラインプレイヤーの確認と処理
        for (Team team : teams) {
            for (String entry : new ArrayList<>(team.getEntries())) {
                Player player = Bukkit.getPlayer(entry);
                if (player == null || !player.isOnline()) {
                    handlePlayerQuit(player);
                    needsUpdate = true;
                }
            }
        }

        // チームメンバーの変更検知
        List<String> currentTeamMembers = teams.stream()
                .flatMap(team -> team.getEntries().stream())
                .toList();
        List<String> currentSpectatorMembers = new ArrayList<>(spectatorTeam.getEntries());

        if (!currentTeamMembers.equals(lastTeamMembers) ||
                !currentSpectatorMembers.equals(lastSpectatorMembers)) {
            needsUpdate = true;
        }

        // 変更があった場合のみ更新処理を実行
        if (needsUpdate) {
            updateTeamSize();
            updateLastTeamState();
        }
    }

    public static void updateTeamSize() {
        var objective = getObjective(TEAM_SIZE_OBJECTIVE);

        // 各チームのサイズを計算して設定
        teams.forEach(team -> {
            int size = team.getEntries().size();
            objective.getScore(team.getName()).setScore(size);
        });

        // 観戦チームのサイズを設定
        objective.getScore(spectatorTeam.getName()).setScore(spectatorTeam.getEntries().size());
    }

    /**
     * チームに所属する全プレイヤー（観戦者を除く）に対して処理を実行します
     * 
     * @param consumer プレイヤーに対して実行する処理
     */
    public static void forEachTeamPlayer(Consumer<Player> consumer) {
        if (teams == null) {
            initialize();
        } else {
            reloadTeamCache();
        }

        getValidTeamPlayers().forEach(consumer);
    }

    /**
     * チームに所属している有効なプレイヤー（オンラインで観戦者でない）のStreamを返します
     * 
     * @return 有効なチームプレイヤーのStream
     */
    private static Stream<Player> getValidTeamPlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.isOnline() && !spectatorTeam.hasEntry(player.getName()) &&
                        teams.stream().anyMatch(team -> team.hasEntry(player.getName())))
                .map(player -> player);
    }

    /**
     * 特定のチームに所属する有効なプレイヤー（オンラインで観戦者でない）のStreamを返します
     * 
     * @param team 対象チーム
     * @return 有効なチームプレイヤーのStream
     */
    private static Stream<Player> getValidTeamPlayers(Team team) {
        if (team == null || team == spectatorTeam) {
            return Stream.empty();
        }

        return team.getEntries().stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline() && !spectatorTeam.hasEntry(player.getName()));
    }

    /**
     * 特定のチームに所属するプレイヤーに対して処理を実行します
     * 
     * @param teamName チーム名
     * @param consumer プレイヤーに対して実行する処理
     */
    public static void forEachTeamPlayer(String teamName, Consumer<Player> consumer) {
        reloadTeamCache();
        Team team = getScoreboard().getTeam(teamName);
        if (team == null || team == spectatorTeam) {
            return;
        }

        getValidTeamPlayers(team).forEach(consumer);
    }

    /**
     * チームリーダーの条件に合うプレイヤーのStreamを返します
     * 
     * @return チームリーダーのStream
     */
    private static Stream<Player> getTeamLeadersStream() {
        return teams.stream()
                .flatMap(team -> team.getEntries().stream())
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline() && isTeamLeader(player));
    }

    /**
     * 特定のチームのリーダーのStreamを返します
     * 
     * @param team 対象チーム
     * @return チームリーダーのStream
     */
    private static Stream<Player> getTeamLeadersStream(Team team) {
        if (team == null || team == spectatorTeam) {
            return Stream.empty();
        }

        return team.getEntries().stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline() && isTeamLeader(player));
    }

    /**
     * 全チームのリーダーに対して処理を実行します
     * 
     * @param consumer リーダーに対して実行する処理
     */
    public static void forEachTeamLeader(Consumer<Player> consumer) {
        if (teams == null) {
            initialize();
        } else {
            reloadTeamCache();
        }

        getTeamLeadersStream().forEach(consumer);
    }

    /**
     * 全チームのリーダーに対してチーム情報と共に処理を実行します
     * 
     * @param biConsumer リーダーとそのチームに対して実行する処理
     */
    public static void forEachTeamLeader(BiConsumer<Player, Team> biConsumer) {
        if (teams == null) {
            initialize();
        } else {
            reloadTeamCache();
        }

        teams.forEach(team -> {
            getTeamLeadersStream(team).forEach(player -> biConsumer.accept(player, team));
        });
    }

    /**
     * 特定のチームのリーダーに対して処理を実行します
     * 
     * @param teamName チーム名
     * @param consumer リーダーに対して実行する処理
     */
    public static void forEachTeamLeader(String teamName, Consumer<Player> consumer) {
        reloadTeamCache();
        Team team = getScoreboard().getTeam(teamName);
        if (team == null || team == spectatorTeam) {
            return;
        }

        getTeamLeadersStream(team).forEach(consumer);
    }

    /**
     * 特定のチームのリーダーに対してチーム情報と共に処理を実行します
     * 
     * @param teamName   チーム名
     * @param biConsumer リーダーとそのチームに対して実行する処理
     */
    public static void forEachTeamLeader(String teamName, BiConsumer<Player, Team> biConsumer) {
        reloadTeamCache();
        Team team = getScoreboard().getTeam(teamName);
        if (team == null || team == spectatorTeam) {
            return;
        }

        getTeamLeadersStream(team).forEach(player -> biConsumer.accept(player, team));
    }

    /**
     * 指定されたチームを検証し、有効なチームであるか確認します
     * 
     * @param team 検証するチーム
     * @return 有効なチームの場合true、観戦者チームまたはnullの場合false
     */
    private static boolean isValidTeam(Team team) {
        return team != null && team != spectatorTeam;
    }

    /**
     * 指定されたチーム名から有効なチームを取得します
     * 
     * @param teamName チーム名
     * @return 有効なチーム、無効な場合はnull
     */
    private static Team getValidTeam(String teamName) {
        Team team = getScoreboard().getTeam(teamName);
        return isValidTeam(team) ? team : null;
    }

    /**
     * 指定されたチームのリーダーを取得します
     * 
     * @param teamName チーム名
     * @return リーダーのリスト（チームが存在しない場合は空のリスト）
     */
    public static List<Player> getTeamLeaders(String teamName) {
        reloadTeamCache();
        Team team = getValidTeam(teamName);
        if (team == null) {
            return new ArrayList<>();
        }

        return getTeamLeadersStream(team).collect(Collectors.toList());
    }

    /**
     * 全チームのリーダー情報を含む文字列を取得します
     * 
     * @return 全チームのリーダー情報を含む文字列
     */
    public static String getAllTeamLeadersInfo() {
        if (teams == null) {
            initialize();
        } else {
            reloadTeamCache();
        }

        StringBuilder info = new StringBuilder("チームリーダー情報:\n");

        for (Team team : teams) {
            List<Player> leaders = team.getEntries().stream()
                    .map(Bukkit::getPlayer)
                    .filter(player -> player != null && player.isOnline() && isTeamLeader(player))
                    .toList();

            String teamName = team.getName();
            ChatColor teamColor = textColorToChatColor(team.color());

            info.append(teamColor).append(teamName).append(ChatColor.RESET).append(": ");

            if (leaders.isEmpty()) {
                info.append("リーダーなし");
            } else {
                info.append(String.join(", ", leaders.stream().map(Player::getName).toList()));
            }

            info.append(" (メンバー数: ").append(team.getEntries().size()).append(")\n");
        }

        return info.toString();
    }

    /**
     * 指定されたチームのリーダー情報を含む文字列を取得します
     * 
     * @param teamName チーム名
     * @return チームのリーダー情報を含む文字列（チームが存在しない場合はその旨のメッセージ）
     */
    public static String getTeamLeadersInfo(String teamName) {
        Team team = getScoreboard().getTeam(teamName);
        if (team == null) {
            return "指定されたチーム「" + teamName + "」は存在しません。";
        }

        List<Player> leaders = getTeamLeaders(teamName);
        ChatColor teamColor = textColorToChatColor(team.color());

        StringBuilder info = new StringBuilder(teamColor + teamName + ChatColor.RESET + "のリーダー: ");

        if (leaders.isEmpty()) {
            info.append("リーダーは設定されていません");
        } else {
            info.append(String.join(", ", leaders.stream().map(Player::getName).toList()));
        }

        info.append(" (メンバー数: ").append(team.getEntries().size()).append(")");

        return info.toString();
    }

    /**
     * TextColorからChatColorへの変換
     * 
     * @param color 変換するTextColor
     * @return 対応するChatColor
     */
    private static ChatColor textColorToChatColor(net.kyori.adventure.text.format.TextColor color) {
        if (color == null)
            return ChatColor.WHITE;

        // color.toString()は "NamedTextColor[red]" などの形式で返すため、純粋な色名を抽出する必要がある
        String colorName = color.toString().toLowerCase();

        // NamedTextColorの場合は[]内の名前を抽出
        if (colorName.contains("[") && colorName.contains("]")) {
            colorName = colorName.substring(colorName.indexOf("[") + 1, colorName.indexOf("]"));
        }

        return switch (colorName) {
            case "red" -> ChatColor.RED;
            case "dark_red" -> ChatColor.DARK_RED;
            case "blue" -> ChatColor.BLUE;
            case "dark_blue" -> ChatColor.DARK_BLUE;
            case "green" -> ChatColor.GREEN;
            case "dark_green" -> ChatColor.DARK_GREEN;
            case "yellow" -> ChatColor.YELLOW;
            case "gold" -> ChatColor.GOLD;
            case "aqua" -> ChatColor.AQUA;
            case "dark_aqua" -> ChatColor.DARK_AQUA;
            case "light_purple" -> ChatColor.LIGHT_PURPLE;
            case "dark_purple" -> ChatColor.DARK_PURPLE;
            case "gray" -> ChatColor.GRAY;
            case "dark_gray" -> ChatColor.DARK_GRAY;
            case "black" -> ChatColor.BLACK;
            default -> ChatColor.WHITE;
        };
    }

    public static void shuffle(int maxMember, int teamLeaders) {
        // スコアボードの取得
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // チームの初期化またはキャッシュ更新
        if (teams == null || teams.isEmpty()) {
            initialize();
        } else {
            reloadTeamCache();
        }

        // スペクテイターチーム以外のチームを削除
        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            if (!team.equals(spectatorTeam)) {
                team.unregister();
                try{
                    teams.remove(team);
                }
                catch (Exception ignored) {

                }
            }
        }

        try{
            if(teams != null){
                teams.clear();
            }
        }
        catch (Exception e){
            teams = new ArrayList<>();
        }

        // スコアボードオブジェクティブの用意
        getOrCreateObjective(LEADER_OBJECTIVE, Component.text("チームリーダー"));
        getOrCreateObjective(TEAM_SIZE_OBJECTIVE, Component.text("チームサイズ"));

        // プレイヤー収集 & シャッフル
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.removeIf(player -> spectatorTeam.hasEntry(player.getName()));
        Collections.shuffle(players);

        if (players.isEmpty()) {
            LOGGER.warning("No players available for team shuffle");
            return;
        }

        int playerCount = players.size();
        int requiredTeams = (maxMember == 1) ? playerCount : Math.max(1, (int) Math.ceil((double) playerCount / maxMember));

        // プレイヤーをチームごとに分割
        int basePlayersPerTeam = (maxMember == 1) ? 1 : playerCount / requiredTeams;
        int remainingPlayers = (maxMember == 1) ? 0 : playerCount % requiredTeams;

        int currentIndex = 0;
        for (int i = 0; i < requiredTeams; i++) {
            int teamSize = basePlayersPerTeam + (i < remainingPlayers ? 1 : 0);
            List<Player> teamPlayers = new ArrayList<>();

            for (int j = 0; j < teamSize && currentIndex < players.size(); j++) {
                teamPlayers.add(players.get(currentIndex));
                currentIndex++;
            }

            if (teamPlayers.isEmpty()) continue;

            // チーム名の決定（ランダムなプレイヤー名を小文字化）
            Player representative = teamPlayers.get(new Random().nextInt(teamPlayers.size()));
            String teamName = "team_" + representative.getName().toLowerCase();

            Team newTeam = scoreboard.registerNewTeam(teamName);
            teams.add(newTeam);

            // カラーをランダムに設定
            newTeam.color(ColorUtils.getRandomNamedTextColor());

            // チームにメンバー追加
            for (Player member : teamPlayers) {
                newTeam.addEntry(member.getName());
            }

            // リーダーの選出
            if (teamLeaders > 0) {
                getRandomTeamLeaders(newTeam, teamLeaders);
            } else {
                newTeam.getEntries().forEach(entry -> {
                    Player player = Bukkit.getPlayer(entry);
                    if (player != null && player.isOnline()) {
                        getObjective(LEADER_OBJECTIVE).getScore(player.getName()).setScore(0);
                    }
                });
            }

            updateTeamDisplayName(newTeam);
        }

        updateTeamSize();
        LOGGER.info("Completed team shuffle with " + requiredTeams + " teams and " + playerCount + " players");
    }

    public static void shuffle(int maxMember) {
        shuffle(maxMember, 0);
    }

    public static Player getRandomTeamLeader(Team team) {
        List<Player> leaders = getRandomTeamLeaders(team, 1);
        var leader = leaders.isEmpty() ? null : leaders.getFirst();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (leader == onlinePlayer) {
                teamLeaderMap.put(leader, true);
            } else {
                teamLeaderMap.put(leader, false);
            }
        }

        return leader;
    }

    public static List<Player> getRandomTeamLeaders(Team team, int leaders) {
        if (leaders > 2) {
            LOGGER.warning("Team leaders size must 0 or 1 or 2.");

            return new ArrayList<>();
        }

        teamLeaderMap.clear();

        try {
            List<Player> teamPlayers = new ArrayList<>(team.getEntries().stream()
                    .map(Bukkit::getPlayer)
                    .filter(player -> player != null && player.isOnline())
                    .toList());

            if (teamPlayers.isEmpty() || leaders <= 0 || leaders > teamPlayers.size()) {
                return new ArrayList<>();
            }

            // 全プレイヤーのリーダーフラグをリセット
            teamPlayers.forEach(player -> getObjective(LEADER_OBJECTIVE).getScore(player.getName()).setScore(0));

            List<Player> selectedLeaders = new ArrayList<>();
            for (int i = 0; i < Math.min(leaders, teamPlayers.size()); i++) {
                int randomIndex = (int) (Math.random() * teamPlayers.size());
                Player leader = teamPlayers.remove(randomIndex);
                selectedLeaders.add(leader);
                LOGGER.info("Setting team " + team.getName() + " leader: " + leader.getName());
                getObjective(LEADER_OBJECTIVE).getScore(leader.getName()).setScore(1);
            }

            // チームのリーダーが設定されたので表示名を更新
            if (!selectedLeaders.isEmpty()) {
                updateTeamDisplayName(team);
            }

            return selectedLeaders;
        } catch (TeamUtilityException e) {
            throw e;
        } catch (Exception e) {
            throw new TeamUtilityException("チームリーダーの設定中にエラーが発生しました: " + e.getMessage());
        }
    }

    public static boolean isTeamLeader(Player player) {
        return getObjective(LEADER_OBJECTIVE).getScore(player.getName()).getScore() == 1;
    }

    public static boolean isSameTeam(Player player1, Player player2) {
        if (player1 == null || player2 == null) {
            return false;
        }

        Team team1 = TeamUtility.getTeamByName(player1.getName());
        Team team2 = TeamUtility.getTeamByName(player2.getName());

        return team1 != null && team1.equals(team2);
    }

    public static boolean isSpectator(Player player){
        return spectatorTeam.getPlayers().contains(player);
    }

    public static Team getSpectatorTeam(){
        return spectatorTeam;
    }

    public static String getTeamInfo(String teamName) {
        Team team = getScoreboard().getTeam(teamName);
        if (team == null)
            return "";

        List<Player> leaders = team.getEntries().stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline() && isTeamLeader(player))
                .toList();

        int teamSize = getObjective(TEAM_SIZE_OBJECTIVE).getScore(teamName).getScore();

        if (leaders.isEmpty()) {
            return String.format("%sのリーダーは設定されていません。チームの人数は%dです", teamName, teamSize);
        }

        if (leaders.size() == 1) {
            return String.format("%sのリーダーは%sです。チームの人数は%dです",
                    teamName, leaders.getFirst().getName(), teamSize);
        }

        return String.format("%sのリーダーは%sと%sです。チームの人数は%dです",
                teamName, leaders.get(0).getName(), leaders.get(1).getName(), teamSize);
    }

    /**
     * ゲームの実行状態を設定します
     * 
     * @param running ゲームが実行中かどうか
     */
    public static void setGameRunning(boolean running) {
        // 設定ファイルに保存する
        TeamUtils.GAME_CONFIG.setGameRunning(running);
        TeamUtils.getPlugin().reloadConfig("game_config.json", GameConfig.class);
        LOGGER.info(running ? "Game started" : "Game ended");
    }

    /**
     * ゲームが実行中かどうかを取得します
     * 
     * @return ゲームが実行中の場合true
     */
    public static boolean isGameRunning() {
        return TeamUtils.GAME_CONFIG.isGameRunning();
    }

    /**
     * プレイヤーが退出した時の処理を行います
     * 
     * @param player 退出したプレイヤー
     */
    public static void handlePlayerQuit(Player player) {
        if (!isGameRunning())
            return;

        String playerName = player.getName();
        Team playerTeam = getScoreboard().getEntryTeam(playerName);
        if (playerTeam == null || playerTeam == spectatorTeam)
            return;

        // プレイヤーがチームリーダーだった場合
        boolean wasLeader = isTeamLeader(player);

        // チームからプレイヤーを削除
        playerTeam.removeEntry(playerName);

        // キャッシュからも削除
        playerTeamCache.remove(player.getUniqueId());

        // チームサイズが0になった場合はチームを削除対象としてマーク
        if (playerTeam.getEntries().isEmpty()) {
            LOGGER.info(String.format("Team %s is now empty", playerTeam.getName()));
            // 空のチームを削除する代わりに、チームをリセットしてキープ
            handleEmptyTeam(playerTeam);
            return;
        }

        // リーダーが抜けた場合は新しいリーダーを選出
        if (wasLeader) {
            LOGGER.info(String.format("Team leader of %s has left, selecting a new leader", playerTeam.getName()));
            getRandomTeamLeader(playerTeam);
            // チーム表示名を更新（getRandomTeamLeader内でも行われるが念のため）
            updateTeamDisplayName(playerTeam);
        }

        // チームサイズを更新
        updateTeamSize();
    }

    /**
     * 空になったチームを処理します
     * 
     * @param team 空のチーム
     */
    private static void handleEmptyTeam(Team team) {
        if (team == null || team == spectatorTeam) {
            return;
        }

        // チームリーダースコアをリセット
        Objective leaderObjective = getObjective(LEADER_OBJECTIVE);
        if (leaderObjective != null) {
            leaderObjective.getScore(team.getName()).setScore(0);
        }

        // チームサイズをリセット
        Objective sizeObjective = getObjective(TEAM_SIZE_OBJECTIVE);
        if (sizeObjective != null) {
            sizeObjective.getScore(team.getName()).setScore(0);
        }

        // チームカラーをリセット（オプション）
        team.color(NamedTextColor.WHITE);

        // チーム表示名を更新
        updateTeamDisplayName(team);

        // 変更を反映するためにチームリストを更新
        reloadTeamCache();
        updateTeamSize();
    }

    /**
     * チームリーダーが死亡した際に呼び出され、リスポーン時の処理を設定します
     * 
     * @param player   死亡したチームリーダー
     * @param callback リスポーン時に実行するコールバック
     * @return 待機設定に成功した場合はtrue
     */
    public static boolean waitForTeamLeaderRespawn(Player player, Consumer<Player> callback) {
        if (player == null || !isTeamLeader(player)) {
            return false;
        }

        Team team = getScoreboard().getEntryTeam(player.getName());
        if (team == null || team == spectatorTeam) {
            return false;
        }

        UUID playerUuid = player.getUniqueId();
        respawnCallbacks.put(playerUuid, callback);
        deadLeaders.put(playerUuid, team);

        LOGGER.info(String.format("Waiting for team leader %s to respawn", player.getName()));
        return true;
    }

    /**
     * プレイヤーが死亡した時の処理を行います
     * 
     * @param player 死亡したプレイヤー
     */
    public static void handlePlayerDeath(Player player) {
        if (!isGameRunning || player == null) {
            return;
        }

        // チームリーダーであれば記録
        if (isTeamLeader(player)) {
            Team team = getScoreboard().getEntryTeam(player.getName());
            if (team != null && team != spectatorTeam) {
                deadLeaders.put(player.getUniqueId(), team);
                LOGGER.info(String.format("Team leader %s has died", player.getName()));
            }
        }
    }

    /**
     * プレイヤーがリスポーンした時の処理を行います
     * 
     * @param player リスポーンしたプレイヤー
     */
    public static void handlePlayerRespawn(Player player) {
        if (!isGameRunning || player == null) {
            return;
        }
        UUID playerUuid = player.getUniqueId();
        // 待機中のリーダーのリスポーンを処理
        if (deadLeaders.containsKey(playerUuid)) {
            Team team = deadLeaders.get(playerUuid);
            // リスポーン後もまだチームリーダーかどうか確認
            if (isTeamLeader(player) && team.hasEntry(player.getName())) {
                LOGGER.info(String.format("Team leader %s has respawned", player.getName()));
                // 登録されているコールバックがあれば実行
                Consumer<Player> callback = respawnCallbacks.get(playerUuid);
                if (callback != null) {
                    callback.accept(player);
                    respawnCallbacks.remove(playerUuid);
                    LOGGER.info(String.format("Executed post-respawn process for team leader %s", player.getName()));
                }
            }
            // 死亡リーダーのリストから削除
            deadLeaders.remove(playerUuid);
        }
    }

    /**
     * リスポーン待機中のすべてのコールバックをクリアします
     */
    public static void clearRespawnCallbacks() {
        respawnCallbacks.clear();
        deadLeaders.clear();
        LOGGER.info("Cleared all respawn waiting processes");
    }

    /**
     * プレイヤー間の攻撃が可能かどうかを判定します
     * 同じチームのメンバー同士は攻撃できません
     * 
     * @param attacker 攻撃するプレイヤー
     * @param target   攻撃を受けるプレイヤー
     * @return 攻撃可能な場合はtrue、同じチームの場合はfalse
     */
    public static boolean canAttack(Player attacker, Player target) {
        if (attacker == null || target == null) {
            return true; // nullの場合は攻撃可能とする
        }
        // スコアボードから各プレイヤーのチームを取得
        Team attackerTeam = getScoreboard().getEntryTeam(attacker.getName());
        Team targetTeam = getScoreboard().getEntryTeam(target.getName());
        // 両方またはどちらかがチームに所属していない場合は攻撃可能
        if (attackerTeam == null || targetTeam == null) {
            return true;
        }
        // 観戦者チームは攻撃不可
        if (attackerTeam == spectatorTeam) {
            return false;
        }
        // 同じチームのメンバーは攻撃不可
        return !attackerTeam.equals(targetTeam);
    }

    /**
     * プレイヤーが特定のゲームモードに変更されるまで待機し、変更後にコールバックを実行します
     * 
     * @param player         プレイヤー
     * @param targetGameMode 待機する対象のゲームモード
     * @param callback       ゲームモード変更後に実行するコールバック
     * @return 待機設定に成功した場合はtrue
     */
    public static boolean waitForGameModeChange(Player player, org.bukkit.GameMode targetGameMode,
            Consumer<Player> callback) {
        if (player == null || targetGameMode == null) {
            return false;
        }
        // 既にターゲットのゲームモードである場合は即時実行
        if (player.getGameMode() == targetGameMode) {
            callback.accept(player);
            return true;
        }
        UUID playerUuid = player.getUniqueId();
        gameModeCallbacks.put(playerUuid, callback);
        targetGameModes.put(playerUuid, targetGameMode);
        LOGGER.info(String.format("Waiting for player %s to change gamemode to %s", player.getName(),
                targetGameMode.name()));
        return true;
    }

    /**
     * 特定のプレイヤーのゲームモード変更待機をクリアします
     * 
     * @param playerUuid クリアするプレイヤーのUUID
     */
    public static void removeGameModeCallback(UUID playerUuid) {
        if (playerUuid != null) {
            gameModeCallbacks.remove(playerUuid);
            targetGameModes.remove(playerUuid);
            LOGGER.info(String.format("Cleared gamemode change waiting for player %s", playerUuid));
        }
    }

    /**
     * プレイヤーのゲームモード変更を処理します
     * 
     * @param player      ゲームモードが変更されたプレイヤー
     * @param newGameMode 新しいゲームモード
     */
    public static void handleGameModeChange(Player player, org.bukkit.GameMode newGameMode) {
        if (!isGameRunning || player == null || newGameMode == null) {
            return;
        }
        UUID playerUuid = player.getUniqueId();
        // 待機中のゲームモード変更を処理
        if (targetGameModes.containsKey(playerUuid)) {
            org.bukkit.GameMode targetGameMode = targetGameModes.get(playerUuid);
            // 対象のゲームモードに変更されたか確認
            if (newGameMode == targetGameMode) {
                LOGGER.info(String.format("Player %s's gamemode changed to %s", player.getName(), newGameMode.name()));
                // 登録されているコールバックがあれば実行
                Consumer<Player> callback = gameModeCallbacks.get(playerUuid);
                if (callback != null) {
                    callback.accept(player);
                    gameModeCallbacks.remove(playerUuid);
                    LOGGER.info(String.format("Executed callback for gamemode change for player %s", player.getName()));
                }
                // 待機リストから削除
                targetGameModes.remove(playerUuid);
            }
        }
    }

    /**
     * ゲームモード変更待機中のすべてのコールバックをクリアします
     */
    public static void clearGameModeCallbacks() {
        gameModeCallbacks.clear();
        targetGameModes.clear();
        LOGGER.info("Cleared all gamemode change waiting processes");
    }

    /**
     * プレイヤーをチームに追加します
     * 
     * @param player   追加するプレイヤー
     * @param teamName 追加先のチーム名
     * @return 追加に成功した場合はtrue、失敗した場合はfalse
     */
    public static boolean addPlayerToTeam(Player player, String teamName) {
        if (player == null) {
            return false;
        }

        if (isTeamLeader(player)) {
            getRandomTeamLeader(TeamUtility.getPlayerTeam(player));
        }

        Team team = getScoreboard().getTeam(teamName);
        if (team == null) {
            LOGGER.warning("チーム '" + teamName + "' が見つかりません");
            return false;
        }

        // 既に所属しているチームがある場合は、そのチームから離脱させる
        Team currentTeam = getPlayerTeam(player);
        if (currentTeam != null && !currentTeam.getName().equals(teamName)) {
            currentTeam.removeEntry(player.getName());
            playerTeamCache.remove(player.getUniqueId());
        }

        // 指定したチームに追加
        team.addEntry(player.getName());

        // キャッシュを更新
        playerTeamCache.put(player.getUniqueId(), teamName);

        // チームサイズを更新
        updateTeamSize();

        // 表示名を更新
        updateTeamDisplayName(team);

        LOGGER.info(player.getName() + " を " + teamName + " チームに追加しました");
        return true;
    }

    /**
     * プレイヤーをチームから削除します
     * 
     * @param player プレイヤー
     * @return 削除に成功した場合はtrue、プレイヤーがチームに所属していない場合はfalse
     */
    public static boolean removePlayerFromTeam(Player player) {
        if (player == null) {
            return false;
        }

        Team team = getPlayerTeam(player);
        if (team == null) {
            return false;
        }

        boolean wasLeader = isTeamLeader(player);

        // チームからプレイヤーを削除
        team.removeEntry(player.getName());

        // キャッシュからも削除
        playerTeamCache.remove(player.getUniqueId());

        // リーダーならリーダーフラグをリセット
        if (wasLeader) {
            getObjective(LEADER_OBJECTIVE).getScore(player.getName()).setScore(0);

            // リーダーだった場合は、新しいリーダーを選出
            if (!team.equals(spectatorTeam) && !team.getEntries().isEmpty()) {
                getRandomTeamLeader(team);
            }
        }

        // チームサイズを更新
        updateTeamSize();

        // 空のチームの処理（観戦チームは消さない）
        if (!team.equals(spectatorTeam) && team.getEntries().isEmpty()) {
            handleEmptyTeam(team);
        } else {
            // 表示名を更新
            updateTeamDisplayName(team);
        }

        LOGGER.info(player.getName() + " を " + team.getName() + " チームから削除しました");
        return true;
    }

    /**
     * プレイヤーが所属しているチームを取得します（キャッシュも利用）
     * 
     * @param player プレイヤー
     * @return プレイヤーが所属しているチーム、所属していない場合はnull
     */
    public static Team getPlayerTeam(Player player) {
        if (player == null) {
            return null;
        }

        // まずキャッシュからチーム名を探す
        String teamName = playerTeamCache.get(player.getUniqueId());
        if (teamName != null) {
            Team cachedTeam = getScoreboard().getTeam(teamName);
            if (cachedTeam != null && cachedTeam.hasEntry(player.getName())) {
                return cachedTeam;
            } else {
                // キャッシュが古い場合は削除して再検索
                playerTeamCache.remove(player.getUniqueId());
            }
        }

        // キャッシュにない場合はScoreboardから取得して結果をキャッシュ
        Team team = getScoreboard().getEntryTeam(player.getName());
        if (team != null) {
            playerTeamCache.put(player.getUniqueId(), team.getName());
        }
        return team;
    }

    public static boolean isPlayerInAnyTeam(Player player) {
        return !playerTeamCache.containsKey(player.getUniqueId());
    }

    /**
     * プレイヤーを別のチームに移動させます
     * 
     * @param player   移動させるプレイヤー
     * @param teamName 移動先のチーム名
     * @return 移動に成功した場合はtrue、失敗した場合はfalse
     */
    public static boolean movePlayerToTeam(Player player, String teamName) {
        if (player == null) {
            return false;
        }

        Team team = getScoreboard().getTeam(teamName);
        if (team == null) {
            LOGGER.warning("チーム '" + teamName + "' が見つかりません");
            return false;
        }

        Team currentTeam = getPlayerTeam(player);
        if (currentTeam != null) {
            // 既に同じチームに所属している場合は何もしない
            if (currentTeam.equals(team)) {
                return true;
            }

            // リーダーであれば、リーダーフラグをリセット
            boolean wasLeader = isTeamLeader(player);
            if (wasLeader) {
                getObjective(LEADER_OBJECTIVE).getScore(player.getName()).setScore(0);
            }

            // 現在のチームから削除
            currentTeam.removeEntry(player.getName());

            // 前のチームでリーダーだった場合は、新しいリーダーを選出
            if (wasLeader && !currentTeam.equals(spectatorTeam) && !currentTeam.getEntries().isEmpty()) {
                getRandomTeamLeader(currentTeam);
            }

            // 空のチームの処理（観戦チームは消さない）
            if (!currentTeam.equals(spectatorTeam) && currentTeam.getEntries().isEmpty()) {
                handleEmptyTeam(currentTeam);
            } else {
                updateTeamDisplayName(currentTeam);
            }
        }

        // 新しいチームに追加
        team.addEntry(player.getName());
        playerTeamCache.remove(player.getUniqueId());
        playerTeamCache.put(player.getUniqueId(), teamName);

        // チームサイズを更新
        updateTeamSize();

        // 表示名を更新
        updateTeamDisplayName(team);

        LOGGER.info(player.getName() + " を " + (currentTeam != null ? currentTeam.getName() : "無所属") +
                " から " + teamName + " チームに移動しました");
        return true;
    }

    /**
     * プレイヤーを観戦チームに移動させます
     * 
     * @param player 観戦チームに移動させるプレイヤー
     * @return 移動に成功した場合はtrue、失敗した場合はfalse
     */
    public static boolean movePlayerToSpectator(Player player) {
        if (spectatorTeam == null) {
            reloadTeamCache(); // 観戦チームがnullの場合、再ロード
        }

        if (spectatorTeam == null) {
            LOGGER.warning("観戦チームが見つかりません");
            return false;
        }

        return movePlayerToTeam(player, spectatorTeam.getName());
    }

    public static List<Team> getTeams() {
        return teams;
    }

    /**
     * チーム名からチームを取得します
     * 
     * @param teamName 取得するチームの名前
     * @return 指定した名前のチーム、存在しない場合はnull
     */
    public static Team getTeamByName(String teamName) {
        if (teamName == null || teamName.isEmpty()) {
            return null;
        }

        // まずScoreboardから直接検索
        Team team = getScoreboard().getTeam(teamName);
        if (team != null) {
            return team;
        }

        // チームキャッシュが初期化されていない場合は再ロード
        if (teams == null || teams.isEmpty()) {
            reloadTeamCache();
        }

        // チームキャッシュから検索（大文字小文字を区別しない）
        return teams.stream()
                .filter(t -> t.getName().equalsIgnoreCase(teamName))
                .findFirst()
                .orElse(null);
    }

    /**
     * 観戦チーム以外のランダムなチームを1つ取得します
     * 
     * @return 観戦チーム以外のランダムなチーム、存在しない場合はnull
     */
    public static Team getRandomTeam() {
        if (teams == null || teams.isEmpty()) {
            reloadTeamCache();
        }

        // 観戦チーム以外のチームのリストを作成
        List<Team> availableTeams = teams.stream()
                .filter(team -> team != null && !team.equals(spectatorTeam))
                .collect(Collectors.toList());

        // 利用可能なチームがない場合はnullを返す
        if (availableTeams.isEmpty()) {
            return null;
        }

        // ランダムなインデックスを選択
        int randomIndex = (int) (Math.random() * availableTeams.size());

        // ランダムに選択されたチームを返す
        return availableTeams.get(randomIndex);
    }

    public static List<String> getTabCompletionsForJoinOrLeave(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 第一引数: チームのリストを提供
            List<String> teams = !TeamUtility.getTeams().isEmpty() ? TeamUtility.getTeams().stream()
                    .map(Team::getName)
                    .collect(Collectors.toList()) : new ArrayList<>();
            return filterCompletions(teams, args[0]);
        } else if (args.length == 2) {
            // 第二引数: プレイヤーのリストを提供
            List<String> players = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            return filterCompletions(players, args[1]);
        }

        return completions;
    }

    private static List<String> filterCompletions(List<String> options, String input) {
        if (input.isEmpty()) {
            return options;
        }

        String lowerInput = input.toLowerCase();
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lowerInput))
                .collect(Collectors.toList());
    }
}

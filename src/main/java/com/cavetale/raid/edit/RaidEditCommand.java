package com.cavetale.raid.edit;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.connect.NetworkServer;
import com.cavetale.raid.RaidPlugin;
import com.cavetale.raid.config.LocationConfig;
import com.cavetale.raid.config.MobSpawnConfig;
import com.cavetale.raid.config.RaidConfig;
import com.cavetale.raid.config.RaidConfigFolder;
import com.cavetale.raid.config.RaidConfigs;
import com.cavetale.raid.config.WaveConfig;
import com.cavetale.raid.config.WaveType;
import com.cavetale.raid.config.validate.ConfigIssue;
import com.cavetale.raid.config.validate.ConfigValidation;
import com.cavetale.raid.config.validate.ValidationContext;
import com.cavetale.raid.config.wave.WaveEditField;
import com.cavetale.raid.config.wave.WaveEditList;
import com.winthier.creative.BuildWorld;
import com.winthier.creative.BuildWorldPurpose;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public final class RaidEditCommand extends AbstractCommand<RaidPlugin> {
    /** Shared syntax for raid names, logical world names, and wave list names. */
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9_]+");
    private static final Pattern WAVE_INDEX_PATTERN = Pattern.compile("[0-9]+");

    public RaidEditCommand(final RaidPlugin plugin) {
        super(plugin, "raidedit");
    }

    @Override
    public void onEnable() {
        rootNode.addChild("create").arguments("<name>")
            .description("Create a new raid")
            .completers(CommandArgCompleter.supplyList(() -> RaidConfigs.getInstance().getAllRaidNames()))
            .playerCaller(this::create);
        rootNode.addChild("select").arguments("<raid>")
            .description("Select a raid for editing")
            .completers(CommandArgCompleter.supplyList(() -> RaidConfigs.getInstance().getAllRaidNames()))
            .playerCaller(this::select);
        rootNode.addChild("displayname").arguments("<name...>")
            .description("Set the selected raid's display name with MiniMessage formatting")
            .playerCaller(this::displayName);
        rootNode.addChild("setworld").arguments("<name> <path>")
            .description("Add or replace a world mapping")
            .completers(
                CommandArgCompleter.producePlayerList(this::getRaidWorldNames),
                CommandArgCompleter.supplyList(() -> getWorldPaths())
            )
            .playerCaller(this::setWorld);
        rootNode.addChild("rmworld").arguments("<name>")
            .description("Remove a world mapping")
            .completers(CommandArgCompleter.producePlayerList(this::getRaidWorldNames))
            .playerCaller(this::removeWorld);
        rootNode.addChild("info")
            .description("Show the selected raid")
            .playerCaller(this::info);
        rootNode.addChild("save")
            .description("Save the selected raid to disk")
            .playerCaller(this::save);
        rootNode.addChild("validate")
            .description("Check the selected raid for configuration issues")
            .playerCaller(this::validate);
        final CommandNode waveNode = rootNode.addChild("wave")
            .description("Wave commands");
        waveNode.addChild("addlocation").arguments("<list>")
            .description("Append your location to a wave list")
            .completers(CommandArgCompleter.producePlayerList(player -> editLists(player).stream()
                        .filter(list -> list.getType() == LocationConfig.class).map(WaveEditList::getName).toList()))
            .playerCaller(this::waveAddLocation);
        waveNode.addChild("addmob").arguments("<list> <mob>")
            .description("Add a mob spawn at your location")
            .completers(
                CommandArgCompleter.producePlayerList(player -> editLists(player).stream()
                    .filter(list -> list.getType() == MobSpawnConfig.class).map(WaveEditList::getName).toList()),
                CommandArgCompleter.supplyStream(() -> Registry.ENTITY_TYPE.stream()
                    .filter(type -> type.isSpawnable() && type.getEntityClass() != null && Mob.class.isAssignableFrom(type.getEntityClass()))
                    .map(type -> type.getKey().toString()))
            )
            .playerCaller(this::waveAddMob);
        waveNode.addChild("remove").arguments("<list> <index>")
            .description("Remove a list entry")
            .completers(
                CommandArgCompleter.producePlayerList(player -> editLists(player).stream().map(WaveEditList::getName).toList()),
                CommandArgCompleter.producePlayerList((player, args) -> {
                        for (final WaveEditList<?> list : editLists(player)) {
                            if (!list.getName().equals(args[0])) continue;
                            final List<?> entries = list.getGetter().get();
                            if (entries != null) return java.util.stream.IntStream.range(0, entries.size()).mapToObj(Integer::toString).toList();
                        }
                        return List.of();
                    })
            )
            .playerCaller(this::waveRemove);
        waveNode.addChild("setint").arguments("<field> <value>")
            .description("Set an integer field of the selected wave")
            .completers(
                CommandArgCompleter.producePlayerList(player -> getWaveFieldNames(player, Integer.class)),
                CommandArgCompleter.producePlayerList((player, args) -> getWaveFieldValue(player, args[0], Integer.class))
            )
            .playerCaller(this::waveSetInt);
        waveNode.addChild("setlocation").arguments("<field>")
            .description("Set a location field to your current position and rotation")
            .completers(CommandArgCompleter.producePlayerList(player -> getWaveFieldNames(player, LocationConfig.class)))
            .playerCaller(this::waveSetLocation);
        waveNode.addChild("info")
            .description("Show the selected wave and all its editable values")
            .playerCaller(this::waveInfo);
        waveNode.addChild("newlist").arguments("<name>")
            .description("Create and select a wave list")
            .playerCaller(this::waveNewList);
        waveNode.addChild("create").arguments("<type>")
            .completers(CommandArgCompleter.enumLowerList(WaveType.class))
            .description("Add a wave")
            .playerCaller(this::waveCreate);
        waveNode.addChild("select").arguments("<list> [index]")
            .completers(
                CommandArgCompleter.producePlayerList(player -> Editor.of(player).getCurrentWaveListNames()),
                CommandArgCompleter.producePlayerList((player, args) -> Editor.of(player).getWaveIndexNames(args[0]))
            )
            .description("Select a wave")
            .playerCaller(this::waveSelect);
    }

    private boolean create(final Player player, final String[] args) {
        if (args.length != 1) return false;
        final String raidName = args[0];
        if (!NAME_PATTERN.matcher(raidName).matches()) {
            throw new CommandWarn("Illegal raid name: '" + raidName + "', must be lower cased, alphanumeric, underscores allowed!");
        }
        if (RaidConfig.named(raidName) != null) {
            throw new CommandWarn("Raid already exists: " + raidName);
        }
        final RaidConfig raidConfig = new RaidConfig();
        final String mainWorld = player.getWorld().getName();
        raidConfig.setName(raidName);
        raidConfig.getBuildWorlds().put(RaidConfig.MAIN_WORLD, mainWorld);
        raidConfig.getWaves().put(RaidConfig.MAIN_WAVE, new ArrayList<>());
        RaidConfigs.getInstance().addRaidConfig(raidName, raidConfig);
        player.sendMessage(
            textOfChildren(
                text("Raid created: "),
                text(raidName, WHITE),
                text(". Main world: "),
                text(raidConfig.getBuildWorlds().get(RaidConfig.MAIN_WORLD), WHITE)
            ).color(GREEN)
        );
        Editor.of(player).selectRaid(raidName);
        return true;
    }

    private boolean select(final Player player, final String[] args) {
        if (args.length != 1) return false;
        final String raidName = args[0];
        if (RaidConfig.named(raidName) == null) {
            throw new CommandWarn("Raid not found: " + raidName);
        }
        Editor.of(player).selectRaid(raidName);
        player.sendMessage(
            textOfChildren(
                text("Raid selected: "),
                text(raidName, WHITE)
            ).color(GREEN)
        );
        return true;
    }

    private boolean displayName(final Player player, final String[] args) {
        if (args.length == 0) return false;
        final RaidConfig raidConfig = Editor.of(player).requireCurrentRaid();
        raidConfig.setDisplayName(String.join(" ", args));
        player.sendMessage(
            textOfChildren(
                text("Raid display name set: "),
                raidConfig.getDisplayNameComponent()
            ).color(GREEN)
        );
        return true;
    }

    private boolean setWorld(final Player player, final String[] args) {
        if (args.length != 2) return false;
        final RaidConfig raidConfig = Editor.of(player).requireCurrentRaid();
        final String name = args[0];
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new CommandWarn("Illegal world name: '" + name + "', must be lower cased, alphanumeric, underscores allowed!");
        }
        final String path = requireWorldPath(args[1]);
        raidConfig.getBuildWorlds().put(name, path);
        player.sendMessage(
            textOfChildren(
                text("World mapping set: "),
                text(name, WHITE),
                text(" -> "),
                text(path, WHITE)
            ).color(GREEN)
        );
        sendValidationIssues(player, raidConfig.validate());
        return true;
    }

    private String requireWorldPath(final String path) {
        if (path.isBlank()) throw new CommandWarn("World path is required!");
        if (!NetworkServer.CREATIVE.isThisServer()) return path;
        final BuildWorld buildWorld = BuildWorld.findWithPath(path);
        if (buildWorld == null) throw new CommandWarn("BuildWorld not found: " + path);
        if (buildWorld.getRow().parsePurpose() != BuildWorldPurpose.RAID) {
            throw new CommandWarn("BuildWorld purpose must be Raid: " + path);
        }
        return buildWorld.getPath();
    }

    private boolean removeWorld(final Player player, final String[] args) {
        if (args.length != 1) return false;
        final RaidConfig raidConfig = Editor.of(player).requireCurrentRaid();
        final String name = args[0];
        if (!raidConfig.getBuildWorlds().containsKey(name)) {
            throw new CommandWarn("World mapping not found: " + name);
        }
        raidConfig.getBuildWorlds().remove(name);
        player.sendMessage(
            textOfChildren(
                text("World mapping removed: "),
                text(name, WHITE)
            ).color(GREEN)
        );
        sendValidationIssues(player, raidConfig.validate());
        return true;
    }

    private List<String> getRaidWorldNames(final Player player) {
        final RaidConfig raidConfig = Editor.of(player).getCurrentRaid();
        if (raidConfig == null || raidConfig.getBuildWorlds() == null) return List.of();
        return List.copyOf(raidConfig.getBuildWorlds().keySet());
    }

    private List<String> getWorldPaths() {
        final List<String> result = new ArrayList<>();
        if (NetworkServer.CREATIVE.isThisServer()) {
            for (final BuildWorld buildWorld : BuildWorld.findPurposeWorlds(BuildWorldPurpose.RAID, false)) {
                result.add(buildWorld.getPath());
            }
        } else {
            for (final World world : Bukkit.getWorlds()) result.add(world.getName());
        }
        return result;
    }

    private boolean info(final Player player, final String[] args) {
        if (args.length != 0) return false;
        final Editor editor = Editor.of(player);
        final RaidConfig raidConfig = editor.requireCurrentRaid();
        player.sendMessage(
            textOfChildren(
                text("Raid: ", GREEN, BOLD),
                raidConfig.getDisplayNameComponent()
            ).color(WHITE)
        );
        player.sendMessage(text("World mappings (" + raidConfig.getBuildWorlds().size() + "):", GREEN));
        for (Map.Entry<String, String> entry : raidConfig.getBuildWorlds().entrySet()) {
            player.sendMessage(
                textOfChildren(
                    text(" | "),
                    text(entry.getKey()),
                    text(" -> "),
                    text(entry.getValue(), WHITE)
                ).color(GRAY)
            );
        }
        player.sendMessage(text("Wave lists (" + raidConfig.getWaves().size() + "):", GREEN));
        for (Map.Entry<String, ArrayList<WaveConfig>> entry : raidConfig.getWaves().entrySet()) {
            player.sendMessage(
                textOfChildren(
                    text(" | "),
                    text(entry.getKey()),
                    text(": "),
                    text(entry.getValue().size(), WHITE),
                    text(" waves")
                ).color(GRAY)
            );
        }
        player.sendMessage(
            textOfChildren(
                text("Areas: "),
                text(raidConfig.getAreas().size(), WHITE)
            ).color(GREEN)
        );
        return true;
    }

    private boolean save(final Player player, final String[] args) {
        if (args.length != 0) return false;
        final Editor editor = Editor.of(player);
        editor.requireCurrentRaid();
        final String raidName = editor.getCurrentRaidName();
        RaidConfigFolder.named(raidName).saveRaidConfig();
        player.sendMessage(
            textOfChildren(
                text("Raid saved: "),
                text(raidName, WHITE)
            ).color(GREEN)
        );
        return true;
    }

    private boolean validate(final Player player, final String[] args) {
        if (args.length != 0) return false;
        final List<ConfigIssue> issues = Editor.of(player).requireCurrentRaid().validate();
        if (issues.isEmpty()) {
            player.sendMessage(text("Raid validation passed.", GREEN));
        } else {
            player.sendMessage(text("Raid validation found " + issues.size() + " issue(s):", YELLOW));
            sendValidationIssues(player, issues);
        }
        return true;
    }

    /** Warn about local problems after applying an edit, without rejecting it. */
    private void validateWaveEdit(final Player player, final WaveConfig wave, final String fieldName, final Object value) {
        final Editor editor = Editor.of(player);
        final ValidationContext context = new ValidationContext(editor.requireCurrentRaid())
            .child("waves").child(editor.getCurrentWavesName()).index(editor.getCurrentWaveIndex())
            .child(wave.getType().name().toLowerCase(Locale.ROOT));
        wave.getWaveTypeConfig().validate(context);
        if (value instanceof ConfigValidation validation) {
            validation.validateDeep(context.child(fieldName));
        }
        sendValidationIssues(player, context.getIssues());
    }

    private void sendValidationIssues(final Player player, final List<ConfigIssue> issues) {
        for (final ConfigIssue issue : issues) {
            player.sendMessage(textOfChildren(
                    text("Warning: ", YELLOW), text(issue.getPath(), GRAY), text(": ", GRAY),
                    text(issue.getMessage(), YELLOW)
                ));
        }
    }

    private boolean waveSetInt(final Player player, final String[] args) {
        if (args.length != 2) return false;
        final WaveConfig wave = Editor.of(player).requireCurrentWave();
        final WaveEditField<?> field = requireWaveField(wave, args[0], Integer.class);
        final int value = CommandArgCompleter.requireInt(args[1]);
        setWaveField(field, value);
        sendWaveValue(player, field.getName(), value);
        validateWaveEdit(player, wave, field.getName(), value);
        return true;
    }

    private boolean waveSetLocation(final Player player, final String[] args) {
        if (args.length != 1) return false;
        final Editor editor = Editor.of(player);
        final WaveConfig wave = editor.requireCurrentWave();
        final WaveEditField<?> field = requireWaveField(wave, args[0], LocationConfig.class);
        final LocationConfig value = captureLocation(player, editor);
        setWaveField(field, value);
        sendWaveValue(player, field.getName(), value);
        validateWaveEdit(player, wave, field.getName(), value);
        return true;
    }

    private LocationConfig captureLocation(final Player player, final Editor editor) {
        final Location location = player.getLocation();
        final LocationConfig value = new LocationConfig();
        value.setWorldName(requireRaidWorldName(location.getWorld(), editor.requireCurrentRaid()));
        value.setX(location.getX());
        value.setY(location.getY());
        value.setZ(location.getZ());
        value.setYaw(location.getYaw());
        value.setPitch(location.getPitch());
        return value;
    }

    private List<WaveEditList<?>> editLists(final Player player) {
        final WaveConfig wave = Editor.of(player).getCurrentWave();
        return wave == null ? List.of() : wave.getWaveTypeConfig().getEditLists();
    }

    private WaveEditList<?> requireList(final WaveConfig wave, final String name) {
        for (final WaveEditList<?> list : wave.getWaveTypeConfig().getEditLists()) {
            if (list.getName().equals(name)) return list;
        }
        throw new CommandWarn("Wave list field not found: " + name);
    }

    private boolean waveAddLocation(final Player player, final String[] args) {
        if (args.length != 1) return false;
        final Editor editor = Editor.of(player);
        final WaveConfig wave = editor.requireCurrentWave();
        final WaveEditList<?> list = requireList(wave, args[0]);
        if (list.getType() != LocationConfig.class) throw new CommandWarn("Expected a location list");
        final LocationConfig location = captureLocation(player, editor);
        final int index = appendEntry(list, location);
        final String path = list.getName() + "[" + index + "]";
        sendWaveValue(player, path, location);
        validateWaveEdit(player, wave, path, location);
        return true;
    }

    private boolean waveAddMob(final Player player, final String[] args) {
        if (args.length != 2) return false;
        final Editor editor = Editor.of(player);
        final WaveConfig wave = editor.requireCurrentWave();
        final WaveEditList<?> list = requireList(wave, args[0]);
        if (list.getType() != MobSpawnConfig.class) throw new CommandWarn("Expected a mob spawn list");
        final MobSpawnConfig spawn = new MobSpawnConfig();
        spawn.setLocation(captureLocation(player, editor));
        spawn.setMob(args[1]);
        if (org.bukkit.NamespacedKey.fromString(args[1]) == null) throw new CommandWarn("Invalid mob key: " + args[1]);
        final int index = appendEntry(list, spawn);
        final String path = list.getName() + "[" + index + "]";
        sendWaveValue(player, path, spawn);
        validateWaveEdit(player, wave, path, spawn);
        return true;
    }

    private <T> int appendEntry(final WaveEditList<T> list, final Object value) {
        final List<T> current = list.getGetter().get();
        final List<T> entries = current == null ? new ArrayList<>() : new ArrayList<>(current);
        entries.add(list.getType().cast(value));
        list.getSetter().accept(entries);
        return entries.size() - 1;
    }

    private boolean waveRemove(final Player player, final String[] args) {
        if (args.length != 2) return false;
        final WaveConfig wave = Editor.of(player).requireCurrentWave();
        final WaveEditList<?> list = requireList(wave, args[0]);
        final int index = CommandArgCompleter.requireInt(args[1]);
        removeEntry(list, index);
        player.sendMessage(text("Removed " + list.getName() + "[" + index + "]", GREEN));
        validateWaveEdit(player, wave, list.getName(), null);
        return true;
    }

    private <T> void removeEntry(final WaveEditList<T> list, final int index) {
        final List<T> current = list.getGetter().get();
        if (current == null || index < 0 || index >= current.size()) throw new CommandWarn("Index out of range: " + index);
        final List<T> entries = new ArrayList<>(current);
        entries.remove(index);
        list.getSetter().accept(entries);
    }

    private String requireRaidWorldName(World world, RaidConfig raidConfig) {
        final List<String> worldNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : raidConfig.getBuildWorlds().entrySet()) {
            if (entry.getValue().equals(world.getName())) {
                worldNames.add(entry.getKey());
            }
        }
        if (worldNames.isEmpty()) {
            throw new CommandWarn("This world is not part of the selected raid!");
        }
        if (worldNames.size() != 1) {
            throw new CommandWarn("This world has multiple raid names: " + String.join(", ", worldNames));
        }
        return worldNames.get(0);
    }

    private WaveEditField<?> requireWaveField(final WaveConfig wave, final String name, final Class<?> type) {
        for (WaveEditField<?> field : wave.getWaveTypeConfig().getEditFields()) {
            if (!field.getName().equals(name)) continue;
            if (!field.getType().equals(type)) {
                throw new CommandWarn("Field '" + name + "' has type " + field.getType().getSimpleName()
                                      + ", expected " + type.getSimpleName() + "!");
            }
            return field;
        }
        throw new CommandWarn("Wave field not found: " + name);
    }

    private <T> void setWaveField(final WaveEditField<T> field, final Object value) {
        field.getSetter().accept(field.getType().cast(value));
    }

    private List<String> getWaveFieldNames(final Player player, final Class<?> type) {
        final WaveConfig wave = Editor.of(player).getCurrentWave();
        if (wave == null) return List.of();
        final List<String> result = new ArrayList<>();
        for (WaveEditField<?> field : wave.getWaveTypeConfig().getEditFields()) {
            if (field.getType().equals(type)) result.add(field.getName());
        }
        return result;
    }

    private List<String> getWaveFieldValue(final Player player, final String name, final Class<?> type) {
        final WaveConfig wave = Editor.of(player).getCurrentWave();
        if (wave == null) return List.of();
        for (WaveEditField<?> field : wave.getWaveTypeConfig().getEditFields()) {
            if (!field.getName().equals(name) || !field.getType().equals(type)) continue;
            final Object value = field.getGetter().get();
            return value != null ? List.of(value.toString()) : List.of();
        }
        return List.of();
    }

    private boolean waveInfo(final Player player, final String[] args) {
        if (args.length != 0) return false;
        final Editor editor = Editor.of(player);
        final WaveConfig wave = editor.requireCurrentWave();
        final List<WaveConfig> waveList = editor.requireCurrentWaveList();
        player.sendMessage(textOfChildren(
                text("Wave: "), text(editor.getCurrentWavesName(), WHITE), text(" "),
                text("[", GRAY),
                text(editor.getCurrentWaveIndex(), WHITE),
                text("/", GRAY),
                text(waveList.size() - 1, WHITE),
                text("]", GRAY)
            ).color(GREEN));
        sendWaveValue(player, "type", wave.getType());
        sendWaveValue(player, "label", wave.getLabel());
        for (WaveEditField<?> field : wave.getWaveTypeConfig().getEditFields()) {
            sendWaveValue(player, field.getName(), field.getGetter().get());
        }
        for (WaveEditList<?> list : wave.getWaveTypeConfig().getEditLists()) {
            final List<?> entries = list.getGetter().get();
            if (entries == null || entries.isEmpty()) {
                sendWaveValue(player, list.getName(), entries);
            } else {
                for (int i = 0; i < entries.size(); i++) {
                    sendWaveValue(player, list.getName() + "[" + i + "]", entries.get(i));
                }
            }
        }
        return true;
    }

    private void sendWaveValue(final Player player, final String name, final Object value) {
        player.sendMessage(
            textOfChildren(
                text(" | ", GRAY),
                text(name + ": "),
                formatWaveValue(value)
            ).color(GREEN)
        );
    }

    private Component formatWaveValue(final Object value) {
        if (value == null) return text("(unset)", DARK_GRAY);
        if (value instanceof MobSpawnConfig spawn) {
            return textOfChildren(
                formatWaveValue(spawn.getMob()),
                text(" at ", GRAY),
                formatWaveValue(spawn.getLocation())
            );
        }
        if (value instanceof LocationConfig location) {
            final DecimalFormat numberFormat = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance());
            return textOfChildren(
                formatWaveValue(location.getWorldName()),
                text(" "), text(numberFormat.format(location.getX()), WHITE),
                text(" "), text(numberFormat.format(location.getY()), WHITE),
                text(" "), text(numberFormat.format(location.getZ()), WHITE),
                text(" yaw="), text(numberFormat.format(location.getYaw()), WHITE),
                text(" pitch="), text(numberFormat.format(location.getPitch()), WHITE)
            ).color(GRAY).insertion(
                numberFormat.format(location.getX())
                + " " + numberFormat.format(location.getY())
                + " " + numberFormat.format(location.getZ())
                + " " + numberFormat.format(location.getYaw())
                + " " + numberFormat.format(location.getPitch())
            );
        }
        if (value instanceof List<?> list) {
            final TextComponent.Builder result = text().color(GRAY).append(text("["));
            boolean first = true;
            for (Object element : list) {
                if (!first) result.append(text(", "));
                result.append(formatWaveValue(element));
                first = false;
            }
            return result.append(text("]")).build();
        }
        return text(value.toString(), WHITE);
    }

    private boolean waveNewList(final Player player, final String[] args) {
        if (args.length != 1) return false;
        final Editor editor = Editor.of(player);
        final RaidConfig raidConfig = editor.requireCurrentRaid();
        final String wavesName = args[0];
        if (!NAME_PATTERN.matcher(wavesName).matches()) {
            throw new CommandWarn("Illegal wave list name: '" + wavesName + "', must be lower cased, alphanumeric, underscores allowed!");
        }
        if (raidConfig.getWaves().containsKey(wavesName)) {
            throw new CommandWarn("Wave list already exists: " + wavesName);
        }
        raidConfig.getWaves().put(wavesName, new ArrayList<>());
        editor.setCurrentWavesName(wavesName);
        editor.setCurrentWaveIndex(0);
        player.sendMessage(
            textOfChildren(
                text("Wave list created and selected: "),
                text(wavesName, WHITE)
            ).color(GREEN)
        );
        return true;
    }

    private boolean waveCreate(final Player player, final String[] args) {
        if (args.length != 1) return false;
        final WaveType waveType = CommandArgCompleter.requireEnum(WaveType.class, args[0]);
        final WaveConfig waveConfig = new WaveConfig();
        waveConfig.setType(waveType);
        waveConfig.getWaveTypeConfig();
        final Editor editor = Editor.of(player);
        final List<WaveConfig> waveList = editor.requireCurrentWaveList();
        waveList.add(waveConfig);
        editor.setCurrentWaveIndex(waveList.size() - 1);
        player.sendMessage(
            textOfChildren(
                text("Wave created and selected: "),
                text(waveType.name(), WHITE)
            ).color(GREEN)
        );
        return true;
    }

    private boolean waveSelect(final Player player, final String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        final String wavesName = args[0];
        final String waveIndexName = args.length == 2 ? args[1] : "0";
        final Editor editor = Editor.of(player);
        final RaidConfig raidConfig = editor.requireCurrentRaid();
        final List<WaveConfig> waveList = raidConfig.getWaves().get(wavesName);
        if (waveList == null) {
            throw new CommandWarn("Wave list not found: " + wavesName);
        }
        if (waveList.isEmpty() && args.length == 1) {
            editor.setCurrentWavesName(wavesName);
            editor.setCurrentWaveIndex(0);
            player.sendMessage(
                textOfChildren(
                    text("Empty wave list selected: "),
                    text(wavesName, WHITE)
                ).color(GREEN)
            );
            return true;
        }
        final int waveIndex;
        if (WAVE_INDEX_PATTERN.matcher(waveIndexName).matches()) {
            try {
                waveIndex = Integer.parseInt(waveIndexName);
            } catch (NumberFormatException exception) {
                throw new CommandWarn("Wave index out of range: " + waveIndexName);
            }
            if (waveIndex >= waveList.size()) {
                throw new CommandWarn("Wave index out of range: " + waveIndexName);
            }
        } else {
            waveIndex = raidConfig.findWaveIndex(wavesName, waveIndexName);
            if (waveIndex < 0) {
                throw new CommandWarn("Wave label not found: " + waveIndexName);
            }
        }
        final WaveConfig waveConfig = waveList.get(waveIndex);
        editor.setCurrentWavesName(wavesName);
        editor.setCurrentWaveIndex(waveIndex);
        player.sendMessage(
            textOfChildren(
                text("Wave selected: "),
                text(wavesName, WHITE),
                text(" "),
                text(waveIndex, WHITE),
                text(" ("),
                text(waveConfig.getType().name(), WHITE),
                text(")")
            ).color(GREEN)
        );
        return true;
    }
}

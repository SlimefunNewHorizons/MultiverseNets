package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * EN: Network Status Lights. A light sits on top of a network block and shows, from afar, whether
 * that block reaches a Controller: green when it does, red when it does not. It never joins a
 * network itself (see {@link Network#scan}), so it can not bridge two of them. Every light is kept
 * in {@code status-lights.yml} and checked once a second while its chunk is loaded.
 *
 * ES: Luces de estado de red. Una luz va encima de un bloque de red y muestra desde lejos si ese
 * bloque llega a un Controlador: verde si llega, roja si no. Nunca forma parte de una red (ver
 * {@link Network#scan}), así que no puede unir dos. Cada luz se guarda en {@code status-lights.yml}
 * y se revisa una vez por segundo mientras su chunk está cargado.
 */
public final class StatusLights {

    public static final Material ON = Material.LIME_STAINED_GLASS;
    public static final Material OFF = Material.RED_STAINED_GLASS;
    private static final long PERIOD_TICKS = 20L;

    private final MultiverseNets plugin;
    private final File file;
    private final Map<UUID, Set<Long>> lights = new HashMap<>();
    private BukkitTask task;

    public StatusLights(MultiverseNets plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "status-lights.yml");
        load();
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, PERIOD_TICKS, PERIOD_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public static boolean isLightMaterial(Material material) {
        return material == ON || material == OFF;
    }

    public void add(Block block) {
        if (lights.computeIfAbsent(block.getWorld().getUID(), id -> new HashSet<>()).add(pack(block))) {
            save();
        }
        update(block);
    }

    public void remove(Block block) {
        Set<Long> set = lights.get(block.getWorld().getUID());
        if (set != null && set.remove(pack(block))) {
            save();
        }
    }

    /** True when the block under the light belongs to a network / True si el bloque de abajo está en una red. */
    public boolean connected(Block light) {
        Block below = light.getRelative(BlockFace.DOWN);
        return plugin.networks().networkAt(below) != null;
    }

    /** Recolours one light; returns false when the block is no longer a light. */
    public boolean update(Block light) {
        if (!isLightMaterial(light.getType())) {
            return false;
        }
        Material wanted = connected(light) ? ON : OFF;
        if (light.getType() != wanted) {
            // Sin fisica: solo cambia el color del cristal, nada alrededor reacciona.
            light.setType(wanted, false);
        }
        return true;
    }

    private void tick() {
        boolean changed = false;
        for (Map.Entry<UUID, Set<Long>> entry : lights.entrySet()) {
            World world = plugin.getServer().getWorld(entry.getKey());
            if (world == null) {
                continue;
            }
            var it = entry.getValue().iterator();
            while (it.hasNext()) {
                long pos = it.next();
                int x = PosUtil.unpackX(pos);
                int z = PosUtil.unpackZ(pos);
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                Block block = world.getBlockAt(x, PosUtil.unpackY(pos), z);
                // Con el chunk cargado el material es fiable: si ya no es una luz (otro plugin o
                // WorldEdit la cambio), se olvida.
                if (!update(block) && NodeStore.getType(block) != DeviceType.MVN_STATUS_LIGHT) {
                    it.remove();
                    changed = true;
                }
            }
        }
        if (changed) {
            save();
        }
    }

    private static long pack(Block block) {
        return PosUtil.pack(block.getX(), block.getY(), block.getZ());
    }

    private void load() {
        if (!file.isFile()) {
            return;
        }
        ConfigurationSection root = YamlConfiguration.loadConfiguration(file).getConfigurationSection("lights");
        if (root == null) {
            return;
        }
        for (String worldId : root.getKeys(false)) {
            Set<Long> set = new HashSet<>();
            for (String coords : root.getStringList(worldId)) {
                String[] parts = coords.split(",");
                if (parts.length != 3) {
                    continue;
                }
                try {
                    set.add(PosUtil.pack(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                } catch (NumberFormatException ignored) {
                }
            }
            try {
                lights.put(UUID.fromString(worldId), set);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Set<Long>> entry : lights.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            List<String> coords = new ArrayList<>();
            for (long pos : entry.getValue()) {
                coords.add(PosUtil.unpackX(pos) + "," + PosUtil.unpackY(pos) + "," + PosUtil.unpackZ(pos));
            }
            yaml.set("lights." + entry.getKey(), coords);
        }
        try {
            yaml.save(file);
        } catch (IOException error) {
            plugin.getLogger().warning("Could not save status-lights.yml: " + error.getMessage());
        }
    }
}

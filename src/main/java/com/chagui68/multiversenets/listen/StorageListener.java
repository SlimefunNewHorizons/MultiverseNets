package com.chagui68.multiversenets.listen;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.persist.NodeStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * [EN] Keeps the node region files in step with the world: a chunk that loads gets its legacy data
 * migrated or its region read in the background, {@code /save-all} saves the networks too, and an
 * unloading world is saved before it goes.
 *
 * [ES] Mantiene los archivos de región de nodos al ritmo del mundo: un chunk que carga migra sus
 * datos antiguos o lee su región en segundo plano, {@code /save-all} también guarda las redes, y un
 * mundo que se descarga se guarda antes de irse.
 */
public class StorageListener implements Listener {

    private final MultiverseNets plugin;

    public StorageListener(MultiverseNets plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /** LOWEST: other plugins' handlers already see the chunk migrated / Los demás ya lo ven migrado. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        NodeStore.onChunkLoad(event.getChunk());
        // Las redes ya no se reescanean enteras cada segundo: la que llega a este chunk se marca
        // para reescanear pronto, porque puede crecer hacia el.
        var networks = plugin.networks();
        if (networks != null) {
            networks.chunkLoaded(event.getChunk());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldSave(WorldSaveEvent event) {
        NodeStore.flush(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        NodeStore.onWorldUnload(event.getWorld());
    }
}

package com.chagui68.multiversenets.persist;

import org.bukkit.World;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * [EN] The node regions of one world: which ones exist on disk, which ones are in memory and which
 * ones are being read in the background.
 * <p>
 * A region is read the first time one of its chunks loads (in the background, see
 * {@link #prefetch}) or, at the latest, the first time something asks about it. It stays in memory
 * while any of its chunks with nodes is loaded and is dropped by {@link #evictIdle} once it is
 * saved and none is. Main thread only.
 *
 * [ES] Las regiones de nodos de un mundo: cuáles existen en disco, cuáles están en memoria y cuáles
 * se están leyendo en segundo plano. Una región se lee la primera vez que carga uno de sus chunks
 * (en segundo plano) o, como muy tarde, la primera vez que algo pregunta por ella. Se queda en
 * memoria mientras alguno de sus chunks con nodos esté cargado y {@link #evictIdle} la suelta
 * cuando está guardada y ninguno lo está. Solo hilo principal.
 */
final class WorldNodes {

    private final Path folder;
    private final NodeIO io;
    private final Map<Long, NodeRegion> loaded = new HashMap<>();
    private final Map<Long, CompletableFuture<NodeRegion>> loading = new HashMap<>();
    private final Set<Long> onDisk = new HashSet<>();

    WorldNodes(Path folder, NodeIO io, Logger logger) {
        this.folder = folder;
        this.io = io;
        if (Files.isDirectory(folder)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "r.*" + RegionFile.EXTENSION)) {
                for (Path file : files) {
                    int[] coords = RegionFile.parseName(file.getFileName().toString());
                    if (coords != null) {
                        onDisk.add(NodeRegion.regionKey(coords[0], coords[1]));
                    }
                }
            } catch (IOException error) {
                logger.severe("Could not list node regions in " + folder + ": " + error.getMessage());
            }
        }
    }

    Path folder() {
        return folder;
    }

    /**
     * EN: Region that holds chunk {@code cx, cz}. Null when it has no nodes and {@code create} is
     * false, which is the common answer for chunks without devices and costs three hash lookups.
     *
     * ES: Región que contiene el chunk {@code cx, cz}. Null cuando no tiene nodos y {@code create} es
     * false, que es la respuesta habitual para chunks sin dispositivos y cuesta tres búsquedas.
     */
    NodeRegion region(int cx, int cz, boolean create) {
        int rx = cx >> NodeRegion.SHIFT;
        int rz = cz >> NodeRegion.SHIFT;
        long key = NodeRegion.regionKey(rx, rz);
        NodeRegion region = loaded.get(key);
        if (region != null) {
            return region;
        }
        CompletableFuture<NodeRegion> pending = loading.remove(key);
        if (pending == null && onDisk.contains(key)) {
            // Nadie la pidio por adelantado: se lee ya, por el mismo hilo de E/S para respetar el
            // orden con cualquier escritura que siga en cola.
            pending = io.read(file(rx, rz), rx, rz);
        }
        if (pending != null) {
            region = pending.join();
        } else if (create) {
            region = new NodeRegion(rx, rz);
        } else {
            return null;
        }
        loaded.put(key, region);
        return region;
    }

    /** Starts reading the region of a chunk that just loaded / Empieza a leer la región de un chunk recién cargado. */
    void prefetch(int cx, int cz) {
        int rx = cx >> NodeRegion.SHIFT;
        int rz = cz >> NodeRegion.SHIFT;
        long key = NodeRegion.regionKey(rx, rz);
        if (loaded.containsKey(key) || loading.containsKey(key) || !onDisk.contains(key)) {
            return;
        }
        loading.put(key, io.read(file(rx, rz), rx, rz));
    }

    /**
     * EN: Queues a write for every region with unsaved changes (a delete for the ones left empty)
     * and returns the pending writes.
     *
     * ES: Encola una escritura por cada región con cambios sin guardar (un borrado para las que
     * quedaron vacías) y devuelve las escrituras pendientes.
     */
    List<CompletableFuture<Void>> flush(Logger logger) {
        List<CompletableFuture<Void>> writes = new ArrayList<>();
        for (Map.Entry<Long, NodeRegion> entry : loaded.entrySet()) {
            NodeRegion region = entry.getValue();
            if (!region.dirty) {
                continue;
            }
            region.dirty = false;
            byte[] bytes = region.isEmpty() ? null : RegionFile.serialize(region);
            if (bytes == null) {
                onDisk.remove(entry.getKey());
            } else {
                onDisk.add(entry.getKey());
            }
            region.writesInFlight.incrementAndGet();
            Path file = file(region.rx, region.rz);
            writes.add(io.write(file, bytes).whenComplete((ok, error) -> {
                region.writesInFlight.decrementAndGet();
                if (error != null) {
                    // Se reintenta en el siguiente autoguardado.
                    region.dirty = true;
                    logger.severe("Could not save node region " + file + ": " + error.getMessage());
                }
            }));
        }
        return writes;
    }

    /**
     * EN: Drops saved regions none of whose chunks with nodes is loaded any more, and adopts
     * background reads that finished.
     *
     * ES: Suelta las regiones guardadas sin ningún chunk con nodos cargado y adopta las lecturas en
     * segundo plano que ya terminaron.
     */
    void evictIdle(World world) {
        Iterator<Map.Entry<Long, CompletableFuture<NodeRegion>>> reads = loading.entrySet().iterator();
        while (reads.hasNext()) {
            Map.Entry<Long, CompletableFuture<NodeRegion>> entry = reads.next();
            if (entry.getValue().isDone()) {
                loaded.put(entry.getKey(), entry.getValue().join());
                reads.remove();
            }
        }
        Iterator<NodeRegion> regions = loaded.values().iterator();
        while (regions.hasNext()) {
            NodeRegion region = regions.next();
            if (region.dirty || region.writesInFlight.get() > 0) {
                continue;
            }
            boolean inUse = false;
            for (long chunk : region.chunkKeys()) {
                if (world.isChunkLoaded(NodeRegion.chunkX(chunk), NodeRegion.chunkZ(chunk))) {
                    inUse = true;
                    break;
                }
            }
            if (!inUse) {
                regions.remove();
            }
        }
    }

    int loadedRegions() {
        return loaded.size();
    }

    int regionsOnDisk() {
        return onDisk.size();
    }

    int loadedNodes() {
        int total = 0;
        for (NodeRegion region : loaded.values()) {
            total += region.size();
        }
        return total;
    }

    int dirtyRegions() {
        int dirty = 0;
        for (NodeRegion region : loaded.values()) {
            if (region.dirty) {
                dirty++;
            }
        }
        return dirty;
    }

    private Path file(int rx, int rz) {
        return folder.resolve(RegionFile.fileName(rx, rz));
    }
}

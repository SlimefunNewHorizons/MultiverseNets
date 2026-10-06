package com.chagui68.multiversenets.persist;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.util.PosUtil;
import org.bukkit.NamespacedKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [EN] The nodes of one 32×32-chunk region, the unit that is read from and written to disk.
 * <p>
 * Lookups are one hash access by packed position. Every chunk keeps its own counters (all nodes
 * and ticking devices) so the placement check and {@code chunkHasNodes} never iterate anything.
 * Main thread only, except {@link #dirty} and {@link #writesInFlight}, which the I/O thread also
 * touches.
 *
 * [ES] Los nodos de una región de 32×32 chunks, la unidad que se lee y se escribe en disco.
 * <p>
 * Cada consulta es un acceso de hash por posición empaquetada. Cada chunk lleva sus propios
 * contadores (todos los nodos y los dispositivos que trabajan por tick), así la comprobación al
 * colocar y {@code chunkHasNodes} nunca recorren nada. Solo hilo principal, salvo {@link #dirty} y
 * {@link #writesInFlight}, que también toca el hilo de E/S.
 */
final class NodeRegion {

    /** Chunks per region side as a shift: 32 / Chunks por lado de región como desplazamiento: 32. */
    static final int SHIFT = 5;

    /** Per-chunk counters / Contadores por chunk. */
    static final class ChunkStats {
        int total;
        int ticking;
    }

    final int rx;
    final int rz;
    private final Map<Long, NodeRecord> nodes = new HashMap<>();
    private final Map<Long, ChunkStats> chunks = new HashMap<>();
    /** Changes not on disk yet / Cambios aún no escritos. */
    volatile boolean dirty;
    /** Writes queued or running for this region; it is never evicted while above 0 / Escrituras pendientes. */
    final AtomicInteger writesInFlight = new AtomicInteger();
    /**
     * EN: Chunks migrated from the PDC whose old keys are still there, by chunk key. An entry leaves
     * once a write that includes it is on disk and the keys are removed. Main thread only; the
     * region is never evicted while it is not empty.
     *
     * ES: Chunks migrados desde el PDC cuyas claves antiguas siguen ahí, por clave de chunk. Una
     * entrada sale cuando una escritura que la incluye está en disco y se borran las claves. Solo
     * hilo principal; la región no se suelta mientras no esté vacío.
     */
    final Map<Long, LegacyCleanup> legacyPending = new HashMap<>();

    /** PDC keys still to delete for one migrated chunk / Claves del PDC por borrar de un chunk migrado. */
    static final class LegacyCleanup {
        final List<NamespacedKey> keys;
        /** A write with these nodes is on disk / Ya hay en disco una escritura con estos nodos. */
        boolean written;

        LegacyCleanup(List<NamespacedKey> keys) {
            this.keys = keys;
        }
    }

    NodeRegion(int rx, int rz) {
        this.rx = rx;
        this.rz = rz;
    }

    static long regionKey(int rx, int rz) {
        return ((long) rx << 32) | (rz & 0xFFFFFFFFL);
    }

    static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    static int chunkX(long chunkKey) {
        return (int) (chunkKey >> 32);
    }

    static int chunkZ(long chunkKey) {
        return (int) chunkKey;
    }

    NodeRecord get(int x, int y, int z) {
        return nodes.get(PosUtil.pack(x, y, z));
    }

    /** Stores {@code record} and returns what was there / Guarda el registro y devuelve el anterior. */
    NodeRecord put(int x, int y, int z, NodeRecord record) {
        NodeRecord previous = nodes.put(PosUtil.pack(x, y, z), record);
        if (previous == null || !Objects.equals(previous.type, record.type)) {
            ChunkStats stats = chunks.computeIfAbsent(chunkKey(x >> 4, z >> 4), key -> new ChunkStats());
            if (previous == null) {
                stats.total++;
            } else if (isTicking(previous.type)) {
                stats.ticking--;
            }
            if (isTicking(record.type)) {
                stats.ticking++;
            }
        }
        dirty = true;
        return previous;
    }

    NodeRecord remove(int x, int y, int z) {
        NodeRecord previous = nodes.remove(PosUtil.pack(x, y, z));
        if (previous == null) {
            return null;
        }
        long key = chunkKey(x >> 4, z >> 4);
        ChunkStats stats = chunks.get(key);
        if (stats != null) {
            stats.total--;
            if (isTicking(previous.type)) {
                stats.ticking--;
            }
            if (stats.total <= 0) {
                chunks.remove(key);
            }
        }
        dirty = true;
        return previous;
    }

    ChunkStats stats(int cx, int cz) {
        return chunks.get(chunkKey(cx, cz));
    }

    /** Chunks that hold at least one node / Chunks con al menos un nodo. */
    Set<Long> chunkKeys() {
        return Collections.unmodifiableSet(chunks.keySet());
    }

    /** Read-only view keyed by packed position, for the file writer / Vista de solo lectura. */
    Map<Long, NodeRecord> nodes() {
        return Collections.unmodifiableMap(nodes);
    }

    int size() {
        return nodes.size();
    }

    boolean isEmpty() {
        return nodes.isEmpty();
    }

    private static boolean isTicking(String typeName) {
        DeviceType type = DeviceType.parse(typeName);
        return type != null && type.isTicking();
    }
}

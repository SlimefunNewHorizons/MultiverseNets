package com.chagui68.multiversenets.persist;

import com.chagui68.multiversenets.MultiverseNets;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * [EN] Node Storage & Network Registry
 * <p>
 * Every placed device lives in a region file inside the world folder
 * ({@code <world>/multiversenets/r.<rx>.<rz>.mvn}, 32×32 chunks per file), loaded into memory
 * while its chunks are loaded and saved in the background. Nothing is written to the chunk itself,
 * so there is no per-chunk ceiling: a chunk full of cables weighs what its positions weigh, and the
 * chunk's own save is untouched. Devices still in their default state (every cable, every device
 * nobody configured) store only their type.
 * <p>
 * The API is the one the rest of the plugin has always used: {@link #get} returns an independent
 * copy, {@link #canonical} the shared live instance, {@link #getType}/{@link #hasNode} answer
 * without decoding, and everything answers "no node" for a chunk that is not loaded. It also keeps
 * the persistent controller registry in {@code networks.yml}. Main thread only.
 *
 * [ES] Almacenamiento de Nodos y Registro de Redes
 * <p>
 * Cada dispositivo colocado vive en un archivo de región dentro de la carpeta del mundo
 * ({@code <mundo>/multiversenets/r.<rx>.<rz>.mvn}, 32×32 chunks por archivo), cargado en memoria
 * mientras sus chunks están cargados y guardado en segundo plano. No se escribe nada en el chunk,
 * así que no hay techo por chunk: un chunk lleno de cables pesa lo que pesan sus posiciones, y el
 * guardado del propio chunk no cambia. Los dispositivos en su estado por defecto (todos los cables,
 * todo dispositivo sin configurar) solo guardan su tipo.
 * <p>
 * La API es la de siempre: {@link #get} devuelve una copia independiente, {@link #canonical} la
 * instancia viva compartida, {@link #getType}/{@link #hasNode} responden sin decodificar, y todo
 * responde "no hay nodo" para un chunk sin cargar. También mantiene el registro de controladores en
 * {@code networks.yml}. Solo hilo principal.
 */
public final class NodeStore {

    /** Seconds the shutdown waits for pending writes / Segundos que el apagado espera a las escrituras. */
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30;

    private static MultiverseNets plugin;
    private static File registryFile;
    private static final Map<UUID, List<String>> CONTROLLERS = new HashMap<>();

    private static NodeIO io;
    private static final Map<UUID, WorldNodes> WORLDS = new HashMap<>();
    /** Encoded {@link NodeBlob#create} per type: a blob equal to it is stored as "default" / Blob por defecto. */
    private static final Map<String, byte[]> DEFAULT_ENCODINGS = new ConcurrentHashMap<>();
    /** A main-thread task to clean migrated PDC data is already queued / Ya hay una limpieza en cola. */
    private static final AtomicBoolean LEGACY_DRAIN_QUEUED = new AtomicBoolean();

    /** Numbers for {@code /mvnets stats} / Números para {@code /mvnets stats}. */
    public record StorageStats(int worlds, int loadedRegions, int regionsOnDisk, int loadedNodes, int unsavedRegions) {
    }

    private NodeStore() {
    }

    /**
     * EN: Opens the storage and loads the controller registry. Calling it again (a reload) first
     * saves and drops everything held in memory, so no pre-reload object survives.
     *
     * ES: Abre el almacenamiento y carga el registro de controladores. Llamarlo otra vez (una
     * recarga) primero guarda y suelta todo lo que hay en memoria, así ningún objeto previo
     * sobrevive.
     */
    public static void init(MultiverseNets pl) {
        if (io != null) {
            shutdown();
        }
        // Un autoguardado a medias de antes de una recarga murio con sus tareas: se empieza de cero.
        AUTOSAVE_QUEUE.clear();
        LIVE_EPOCH++;
        plugin = pl;
        io = new NodeIO(pl.getLogger());
        registryFile = new File(pl.getDataFolder(), "networks.yml");
        if (registryFile.isFile()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(registryFile);
            ConfigurationSection root = yaml.getConfigurationSection("controllers");
            if (root != null) {
                for (String worldId : root.getKeys(false)) {
                    CONTROLLERS.put(UUID.fromString(worldId), new ArrayList<>(root.getStringList(worldId)));
                }
            }
        }
    }

    /**
     * EN: Saves every region with changes, waits for the writes and closes the I/O thread.
     * ES: Guarda toda región con cambios, espera las escrituras y cierra el hilo de E/S.
     */
    public static void shutdown() {
        if (io == null) {
            return;
        }
        flushAll(true);
        io.shutdown(SHUTDOWN_TIMEOUT_SECONDS);
        io = null;
        WORLDS.clear();
        LIVE_EPOCH++;
    }

    /**
     * EN: Periodic task: saves what changed and drops regions that are no longer in use.
     * ES: Tarea periódica: guarda lo que cambió y suelta las regiones que ya no se usan.
     */
    public static void autosave() {
        if (plugin != null && plugin.isEnabled()) {
            // Repartido en ticks: cada tick codifica regiones hasta AUTOSAVE_BUDGET_NANOS y deja el
            // resto para el siguiente. Antes todo se codificaba en un solo tick cada 30 segundos.
            // Solo las regiones sucias al empezar: las que el ticker ensucie mientras tanto van al
            // siguiente autoguardado.
            if (AUTOSAVE_QUEUE.isEmpty()) {
                for (Map.Entry<UUID, WorldNodes> entry : WORLDS.entrySet()) {
                    Set<Long> dirty = entry.getValue().dirtyRegionKeys();
                    if (!dirty.isEmpty()) {
                        AUTOSAVE_QUEUE.put(entry.getKey(), dirty);
                    }
                }
                autosaveStep();
            }
            return;
        }
        flushAll(false);
        finishAutosave();
    }

    /**
     * Main-thread time one autosave tick may spend encoding; each tick saves at least one region
     * regardless / Tiempo por tick del autoguardado; cada tick guarda al menos una región.
     */
    static long autosaveBudgetNanos = 2_000_000L;
    /** Regions the running autosave still has to save, per world / Regiones que faltan por guardar. */
    private static final Map<UUID, Set<Long>> AUTOSAVE_QUEUE = new HashMap<>();

    /** Test hook: an autosave is still spreading its work over ticks / Gancho de test. */
    static boolean autosaveRunning() {
        return !AUTOSAVE_QUEUE.isEmpty();
    }

    private static void autosaveStep() {
        long deadline = System.nanoTime() + autosaveBudgetNanos;
        Iterator<Map.Entry<UUID, Set<Long>>> pending = AUTOSAVE_QUEUE.entrySet().iterator();
        boolean first = true;
        while (pending.hasNext() && (first || System.nanoTime() <= deadline)) {
            first = false;
            Map.Entry<UUID, Set<Long>> entry = pending.next();
            WorldNodes nodes = WORLDS.get(entry.getKey());
            if (nodes != null) {
                nodes.flush(logger(), deadline, entry.getValue());
            }
            if (nodes == null || entry.getValue().isEmpty()) {
                pending.remove();
            }
        }
        MultiverseNets owner = plugin;
        if (!AUTOSAVE_QUEUE.isEmpty() && owner != null && owner.isEnabled()) {
            try {
                Bukkit.getScheduler().runTask(owner, NodeStore::autosaveStep);
                return;
            } catch (RuntimeException disabled) {
                // Apagando: shutdown() guarda lo que quede.
            }
        }
        AUTOSAVE_QUEUE.clear();
        finishAutosave();
    }

    private static void finishAutosave() {
        drainLegacyWritten();
        Iterator<Map.Entry<UUID, WorldNodes>> worlds = WORLDS.entrySet().iterator();
        while (worlds.hasNext()) {
            Map.Entry<UUID, WorldNodes> entry = worlds.next();
            World world = Bukkit.getWorld(entry.getKey());
            if (world == null) {
                if (entry.getValue().dirtyRegions() == 0) {
                    worlds.remove();
                }
                continue;
            }
            entry.getValue().evictIdle(world);
        }
    }

    /**
     * EN: Saves one world now (on {@code /save-all} and world unload).
     * ES: Guarda un mundo ahora (con {@code /save-all} y al descargar el mundo).
     */
    public static void flush(World world) {
        WorldNodes nodes = WORLDS.get(world.getUID());
        if (nodes != null) {
            nodes.flush(logger());
        }
    }

    /**
     * EN: Saves every world. With {@code wait} it blocks until the files are written.
     * ES: Guarda todos los mundos. Con {@code wait} bloquea hasta que los archivos estén escritos.
     */
    public static void flushAll(boolean wait) {
        List<CompletableFuture<Void>> writes = new ArrayList<>();
        for (WorldNodes nodes : WORLDS.values()) {
            writes.addAll(nodes.flush(logger()));
        }
        if (wait && !writes.isEmpty()) {
            try {
                CompletableFuture.allOf(writes.toArray(new CompletableFuture[0])).join();
            } catch (RuntimeException alreadyLogged) {
                // Cada escritura fallida ya dejo su mensaje y su region marcada para reintentar.
            }
            drainLegacyWritten();
        }
    }

    /**
     * EN: Deletes the old PDC data of migrated chunks whose region file is now on disk. Main thread.
     * ES: Borra los datos antiguos del PDC de los chunks migrados cuya región ya está en disco.
     */
    static void drainLegacyWritten() {
        LEGACY_DRAIN_QUEUED.set(false);
        for (Map.Entry<UUID, WorldNodes> entry : WORLDS.entrySet()) {
            entry.getValue().drainLegacyWritten(Bukkit.getWorld(entry.getKey()), NodeStore::releaseChunk);
        }
    }

    /** I/O thread: asks the main thread to run {@link #drainLegacyWritten} / Pide la limpieza al hilo principal. */
    private static void queueLegacyDrain() {
        MultiverseNets owner = plugin;
        if (owner == null || !owner.isEnabled() || !LEGACY_DRAIN_QUEUED.compareAndSet(false, true)) {
            return;
        }
        try {
            Bukkit.getScheduler().runTask(owner, NodeStore::drainLegacyWritten);
        } catch (RuntimeException disabled) {
            // Apagando: flushAll(true) limpia al terminar de esperar; si no, el siguiente autoguardado.
            LEGACY_DRAIN_QUEUED.set(false);
        }
    }

    /** Keeps a migrated chunk loaded until its old data is deleted / Mantiene cargado el chunk migrado. */
    private static void holdChunk(Chunk chunk) {
        try {
            chunk.addPluginChunkTicket(plugin);
        } catch (RuntimeException unsupported) {
            // Sin tickets: si se descarga antes, se limpia la próxima vez que cargue.
        }
    }

    private static void releaseChunk(Chunk chunk) {
        try {
            chunk.removePluginChunkTicket(plugin);
        } catch (RuntimeException unsupported) {
            // Nada que soltar.
        }
    }

    /**
     * EN: A chunk just loaded: moves its legacy PDC data into the region files, or starts reading
     * its region in the background so the first lookup finds it in memory.
     *
     * ES: Un chunk acaba de cargar: pasa sus datos antiguos del PDC a los archivos de región, o
     * empieza a leer su región en segundo plano para que la primera consulta la encuentre en memoria.
     */
    public static void onChunkLoad(Chunk chunk) {
        if (migrateLegacy(chunk) == 0) {
            worldNodes(chunk.getWorld()).prefetch(chunk.getX(), chunk.getZ());
        }
    }

    /**
     * EN: The world is going away: save it and forget it.
     * ES: El mundo se descarga: se guarda y se olvida.
     */
    public static void onWorldUnload(World world) {
        WorldNodes nodes = WORLDS.remove(world.getUID());
        if (nodes != null) {
            nodes.flush(logger());
        }
    }

    /**
     * EN: Migrates every chunk that is already loaded (startup and {@code /reload}).
     * ES: Migra todos los chunks ya cargados (arranque y {@code /reload}).
     */
    public static int migrateLoadedChunks() {
        int migrated = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                migrated += migrateLegacy(chunk);
            }
        }
        if (migrated > 0) {
            logger().info("Moved " + migrated + " network node(s) from chunk data to region files.");
        }
        return migrated;
    }

    /**
     * EN: Moves the nodes version 5.2 or earlier left in this chunk's PDC into the region files.
     * The PDC copy is deleted only after a region write with those nodes succeeds (see
     * {@link #drainLegacyWritten}); until then the chunk is held loaded and a failed write or a crash
     * leaves the old data to migrate again. A node already in the region is newer and is kept.
     * Returns how many nodes were read from the PDC.
     *
     * ES: Pasa a los archivos de región los nodos que la versión 5.2 o anterior dejó en el PDC del
     * chunk. La copia del PDC solo se borra cuando una escritura de la región con esos nodos sale
     * bien; hasta entonces el chunk se mantiene cargado, y una escritura fallida o un crash dejan los
     * datos antiguos para migrarlos otra vez. Un nodo que ya está en la región es más nuevo y se
     * conserva. Devuelve cuántos nodos se leyeron del PDC.
     */
    public static int migrateLegacy(Chunk chunk) {
        if (plugin == null || !LegacyChunkData.present(chunk)) {
            return 0;
        }
        WorldNodes nodes = worldNodes(chunk.getWorld());
        long chunkKey = NodeRegion.chunkKey(chunk.getX(), chunk.getZ());
        NodeRegion known = nodes.region(chunk.getX(), chunk.getZ(), false);
        NodeRegion.LegacyCleanup pending = known == null ? null : known.legacyPending.get(chunkKey);
        if (pending != null) {
            // Ya migrado en esta sesión: si su región ya está en disco, se termina la limpieza.
            if (pending.written) {
                LegacyChunkData.clear(chunk, pending.keys);
                known.legacyPending.remove(chunkKey);
                releaseChunk(chunk);
            }
            return 0;
        }
        LegacyChunkData.Snapshot legacy = LegacyChunkData.read(chunk, plugin.getName());
        if (legacy.entries().isEmpty()) {
            // Solo claves vacías o ilegibles: no hay nada que perder.
            LegacyChunkData.clear(chunk, legacy.keys());
            return 0;
        }
        NodeRegion region = nodes.region(chunk.getX(), chunk.getZ(), true);
        for (LegacyChunkData.Entry entry : legacy.entries()) {
            if (region.get(entry.x(), entry.y(), entry.z()) != null) {
                continue;
            }
            byte[] data = entry.data();
            if (data != null && isDefault(entry.type(), data)) {
                data = null;
            }
            region.put(entry.x(), entry.y(), entry.z(), new NodeRecord(entry.type(), data));
            LIVE_EPOCH++;
        }
        // Aunque todo estuviera ya en la región, hace falta una escritura que confirme el borrado.
        region.dirty = true;
        region.legacyPending.put(chunkKey, new NodeRegion.LegacyCleanup(legacy.keys()));
        holdChunk(chunk);
        return legacy.entries().size();
    }

    public static StorageStats stats() {
        int loadedRegions = 0;
        int onDisk = 0;
        int nodes = 0;
        int unsaved = 0;
        for (WorldNodes world : WORLDS.values()) {
            loadedRegions += world.loadedRegions();
            onDisk += world.regionsOnDisk();
            nodes += world.loadedNodes();
            unsaved += world.dirtyRegions();
        }
        return new StorageStats(WORLDS.size(), loadedRegions, onDisk, nodes, unsaved);
    }

    // ------------------------------------------------------------------ node access

    public static NodeBlob get(Block block) {
        NodeRecord record = record(block);
        if (record == null) {
            return null;
        }
        byte[] data = record.data();
        return data == null ? defaultBlob(record.type) : decodeBytes(data);
    }

    /**
     * [EN] The shared live instance of this node's blob, decoded once and reused. Only safe for
     * callers that treat the object as the authoritative in-memory state and always write it back
     * with {@link #put}; everybody else wants {@link #get}. {@code NetworkStorage} asks about every
     * cell on every deposit and withdrawal, and decoding there was the plugin's heaviest recurring
     * cost.
     *
     * [ES] La instancia compartida y viva del blob de este nodo, decodificada una vez y reutilizada.
     * Solo es segura para quien trata el objeto como estado autoritativo en memoria y siempre lo
     * reescribe con {@link #put}; todos los demás quieren {@link #get}.
     */
    public static NodeBlob canonical(Block block) {
        NodeRecord record = record(block);
        if (record == null) {
            return null;
        }
        if (record.live == null) {
            byte[] data = record.data();
            record.live = data == null ? defaultBlob(record.type) : decodeBytes(data);
        }
        return record.live;
    }

    /**
     * EN: The DeviceType of a node without decoding its blob.
     * ES: El DeviceType de un nodo sin decodificar su blob.
     */
    public static com.chagui68.multiversenets.item.DeviceType getType(Block block) {
        NodeRecord record = record(block);
        return record == null ? null : com.chagui68.multiversenets.item.DeviceType.parse(record.type);
    }

    /**
     * EN: Whether a block is a registered network node, without decoding.
     * ES: Si un bloque es un nodo de red registrado, sin decodificar.
     */
    public static boolean hasNode(Block block) {
        return record(block) != null;
    }

    /**
     * EN: Stores the node. The caller's object becomes the live instance served by
     * {@link #canonical}, so a shared reader can never hold something older than the last write.
     * Encoding waits until the bytes are needed (see {@link NodeRecord#data()}); a blob that cannot
     * be serialized then keeps its last good bytes on disk and logs, instead of throwing here.
     *
     * ES: Guarda el nodo. El objeto del llamante pasa a ser la instancia viva que sirve
     * {@link #canonical}, así un lector compartido nunca tiene algo más viejo que la última
     * escritura. La codificación espera a que hagan falta los bytes; un blob no serializable
     * conserva en disco sus últimos bytes buenos y deja aviso, en vez de lanzar aquí.
     */
    public static void put(Block block, NodeBlob blob) {
        if (blob == null) {
            remove(block);
            return;
        }
        ensureLoaded(block);
        NodeRegion region = region(block, true);
        NodeRecord previous = region.get(block.getX(), block.getY(), block.getZ());
        if (previous == null || previous.live != blob) {
            LIVE_EPOCH++;
        }
        byte[] lastEncoded = previous == null ? null : previous.lastEncoded();
        region.put(block.getX(), block.getY(), block.getZ(),
                NodeRecord.unencoded(blob.typeName, blob, lastEncoded));
    }

    public static void remove(Block block) {
        NodeRegion region = region(block, false);
        if (region != null && region.remove(block.getX(), block.getY(), block.getZ()) != null) {
            LIVE_EPOCH++;
        }
    }

    /**
     * Bumped whenever the live instance behind a position changes: a new node, a removed one, or a
     * put of a different object (a menu writing back its copy). A put of the same live instance,
     * which is how the network loop saves what it mutated, leaves it alone. {@code NetworkStorage}
     * keeps its per-tick list of cells while this stays the same.
     */
    private static long LIVE_EPOCH;

    /** See {@link #LIVE_EPOCH} / Ver {@link #LIVE_EPOCH}. */
    public static long liveEpoch() {
        return LIVE_EPOCH;
    }

    /**
     * EN: Every node in the chunk, cables included. Constant time.
     * ES: Todos los nodos del chunk, cables incluidos. Tiempo constante.
     */
    public static int countNodesInChunk(Chunk chunk) {
        NodeRegion.ChunkStats stats = chunkStats(chunk);
        return stats == null ? 0 : stats.total;
    }

    public static boolean chunkHasNodes(Chunk chunk) {
        return countNodesInChunk(chunk) > 0;
    }

    /** Test hook: stores raw bytes as a node's blob / Gancho de test: guarda bytes crudos. */
    static void putRaw(Block block, String type, byte[] data) {
        LIVE_EPOCH++;
        ensureLoaded(block);
        region(block, true).put(block.getX(), block.getY(), block.getZ(), new NodeRecord(type, data));
    }

    /** Test hook: the stored bytes, null for a default blob / Gancho de test: los bytes guardados. */
    static byte[] rawData(Block block) {
        NodeRecord record = record(block);
        return record == null ? null : record.data();
    }

    /** Test hook: the loaded region holding a block / Gancho de test: la región cargada del bloque. */
    static NodeRegion regionForTest(Block block) {
        return region(block, false);
    }

    /** Test hook: where a world's region files go / Gancho de test: carpeta de regiones del mundo. */
    static Path folder(World world) {
        return worldNodes(world).folder();
    }

    private static NodeRecord record(Block block) {
        int x = block.getX();
        int z = block.getZ();
        World world = block.getWorld();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }
        NodeRegion region = worldNodes(world).region(x >> 4, z >> 4, false);
        return region == null ? null : region.get(x, block.getY(), z);
    }

    /**
     * Igual que antes (put pasaba por block.getChunk()): escribir un nodo deja su chunk cargado, o
     * el nodo recien guardado se leeria como ausente.
     */
    private static void ensureLoaded(Block block) {
        if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
            block.getChunk();
        }
    }

    private static NodeRegion region(Block block, boolean create) {
        return worldNodes(block.getWorld()).region(block.getX() >> 4, block.getZ() >> 4, create);
    }

    private static NodeRegion.ChunkStats chunkStats(Chunk chunk) {
        if (!chunk.isLoaded()) {
            return null;
        }
        NodeRegion region = worldNodes(chunk.getWorld()).region(chunk.getX(), chunk.getZ(), false);
        return region == null ? null : region.stats(chunk.getX(), chunk.getZ());
    }

    private static WorldNodes worldNodes(World world) {
        WorldNodes nodes = WORLDS.get(world.getUID());
        if (nodes == null) {
            if (io == null) {
                // Algo pidio datos despues del apagado (otro plugin en su onDisable): se sirve igual.
                io = new NodeIO(logger());
            }
            nodes = new WorldNodes(folderFor(world), io, logger(), NodeStore::queueLegacyDrain);
            WORLDS.put(world.getUID(), nodes);
        }
        return nodes;
    }

    /**
     * EN: Inside the world folder, so copying or backing up a world keeps its networks. Servers (or
     * test doubles) that do not expose the folder fall back to the plugin folder.
     *
     * ES: Dentro de la carpeta del mundo, así copiar o respaldar un mundo conserva sus redes. Si el
     * servidor (o un doble de test) no expone la carpeta, se usa la del plugin.
     */
    private static Path folderFor(World world) {
        try {
            File worldFolder = world.getWorldFolder();
            if (worldFolder != null) {
                return worldFolder.toPath().resolve("multiversenets");
            }
        } catch (RuntimeException unsupported) {
            // Cae al directorio del plugin.
        }
        Path base = plugin != null ? plugin.getDataFolder().toPath() : Path.of("plugins", "MultiverseNets");
        return base.resolve("nodes").resolve(world.getUID().toString());
    }

    private static Logger logger() {
        return plugin != null ? plugin.getLogger() : Logger.getLogger("MultiverseNets");
    }

    // ------------------------------------------------------------------ encoding

    public static String encode(NodeBlob blob) {
        return Base64.getEncoder().encodeToString(encodeBytes(blob));
    }

    public static NodeBlob decode(String data) {
        byte[] encoded;
        try {
            encoded = Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException notBase64) {
            return null;
        }
        return decodeBytes(encoded);
    }

    static byte[] encodeBytes(NodeBlob blob) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bytes);
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(gzip)) {
            out.writeObject(blob);
            out.flush();
            gzip.finish();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not serialize node", e);
        }
    }

    static NodeBlob decodeBytes(byte[] encoded) {
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(encoded);
             BukkitObjectInputStream in = new BukkitObjectInputStream(isGzip(encoded)
                     ? new GZIPInputStream(bytes)
                     : bytes)) {
            NodeBlob blob = (NodeBlob) in.readObject();
            normalize(blob);
            return blob;
        } catch (Throwable e) {
            // A truncated or misplaced stream means the stored entry got damaged (a crash mid-write
            // will do that). The block is not a node anymore, so the answer is null, and the world
            // moves on. printStackTrace here would spam the console on every tick for the rest of
            // the server session.
            if (plugin != null && com.chagui68.multiversenets.util.Settings.debug()) {
                plugin.getLogger().warning("Could not deserialize node data; the corrupt entry "
                        + "is still in place and can be inspected. " + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * EN: The bytes a record stores for {@code blob}: null for the type's default blob. If the blob
     * cannot be serialized, {@code fallback} (the last bytes that did) is kept.
     * ES: Los bytes que guarda un registro para {@code blob}: null si es el de por defecto. Si no se
     * puede serializar, se conserva {@code fallback} (los últimos bytes que sí).
     */
    static byte[] encodeRecord(String typeName, NodeBlob blob, byte[] fallback) {
        try {
            byte[] data = encodeBytes(blob);
            return isDefault(typeName, data) ? null : data;
        } catch (RuntimeException e) {
            if (plugin != null) {
                plugin.getLogger().warning("Could not serialize a " + typeName
                        + " node; keeping its last saved state. " + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
            return fallback;
        }
    }

    private static boolean isGzip(byte[] encoded) {
        return encoded.length >= 2 && encoded[0] == (byte) 0x1f && encoded[1] == (byte) 0x8b;
    }

    private static boolean isDefault(String typeName, byte[] encoded) {
        if (typeName == null) {
            return false;
        }
        byte[] fresh = DEFAULT_ENCODINGS.computeIfAbsent(typeName, type -> encodeBytes(NodeBlob.create(type)));
        return Arrays.equals(fresh, encoded);
    }

    private static NodeBlob defaultBlob(String typeName) {
        NodeBlob blob = NodeBlob.create(typeName);
        normalize(blob);
        return blob;
    }

    /**
     * Los campos anadidos despues de la primera version se deserializan a null en los blobs
     * viejos (la deserializacion de Java no ejecuta constructores). Aqui vuelven a su default.
     */
    private static void normalize(NodeBlob blob) {
        if (blob == null) {
            return;
        }
        if (blob.filterMaterials == null) {
            blob.filterMaterials = new ArrayList<>();
        }
        if (blob.filterItems == null) {
            blob.filterItems = new ArrayList<>();
        }
        if (blob.recipes == null) {
            blob.recipes = new ArrayList<>();
        }
        if (blob.blueprintData == null) {
            blob.blueprintData = new ArrayList<>();
        }
        if (blob.craftingMatrix == null) {
            blob.craftingMatrix = new org.bukkit.inventory.ItemStack[9];
        }
        if (blob.greedySamples == null) {
            blob.greedySamples = new ArrayList<>();
        }
        if (blob.greedyAmounts == null) {
            blob.greedyAmounts = new ArrayList<>();
        }
        if (blob.virtualSamples == null) {
            blob.virtualSamples = new ArrayList<>();
        }
        if (blob.virtualAmounts == null) {
            blob.virtualAmounts = new ArrayList<>();
        }
        if (blob.recoveredModules == null) {
            blob.recoveredModules = new ArrayList<>();
        }
        if (blob.dramFluids == null) {
            blob.dramFluids = new ArrayList<>();
        }
        if (blob.dramFluidAmounts == null) {
            blob.dramFluidAmounts = new ArrayList<>();
        }
        if (blob.chickenProducts == null) {
            blob.chickenProducts = new ArrayList<>();
        }
        // DRAM Bays de un solo modulo: el modulo pasa a la lista bayModules (hasta 18 por bay).
        blob.migrateSingleModuleBay();
        for (NodeBlob module : blob.bayModules) {
            normalize(module);
        }
        if (("GREEDY_CELL".equals(blob.typeName) || "MVN_GREEDY_CELL".equals(blob.typeName))
                && blob.cellSample != null && blob.cellAmount > 0) {
            if (blob.greedySamples.isEmpty()) {
                blob.greedySamples.add(blob.cellSample);
                blob.greedyAmounts.add(blob.cellAmount);
                blob.cellSample = null;
                blob.cellAmount = 0;
            }
        }
    }

    // ------------------------------------------------------------------ controller registry

    public static void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        synchronized (CONTROLLERS) {
            for (Map.Entry<UUID, List<String>> entry : CONTROLLERS.entrySet()) {
                yaml.set("controllers." + entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        if (plugin != null && plugin.isEnabled() && plugin.getServer() != null && plugin.getServer().isPrimaryThread()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    yaml.save(registryFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save networks.yml: " + e.getMessage());
                }
            });
        } else {
            try {
                yaml.save(registryFile);
            } catch (IOException e) {
                if (plugin != null) {
                    plugin.getLogger().severe("Could not save networks.yml: " + e.getMessage());
                }
            }
        }
    }

    public static List<long[]> controllers(UUID worldId) {
        List<String> raw = CONTROLLERS.get(worldId);
        if (raw == null) {
            return List.of();
        }
        List<long[]> result = new ArrayList<>();
        for (String entry : raw) {
            String[] parts = entry.split(",");
            result.add(new long[]{Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2])});
        }
        return result;
    }

    public static boolean isController(UUID worldId, int x, int y, int z) {
        List<String> raw = CONTROLLERS.get(worldId);
        if (raw == null) {
            return false;
        }
        return raw.contains(x + "," + y + "," + z);
    }

    public static void addController(UUID worldId, int x, int y, int z) {
        List<String> raw = CONTROLLERS.computeIfAbsent(worldId, k -> new ArrayList<>());
        String key = x + "," + y + "," + z;
        if (!raw.contains(key)) {
            raw.add(key);
            save();
        }
    }

    public static void removeController(UUID worldId, int x, int y, int z) {
        List<String> raw = CONTROLLERS.get(worldId);
        if (raw != null && raw.remove(x + "," + y + "," + z)) {
            save();
        }
    }
}

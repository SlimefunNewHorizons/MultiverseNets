package com.chagui68.multiversenets.persist;

import com.chagui68.multiversenets.util.Keys;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [EN] Reads (and removes) the node data that versions up to 5.2 kept in the chunk's
 * PersistentDataContainer: {@code n<x>_<y>_<z>} held the Base64 blob, {@code t<x>_<y>_<z>} the type,
 * and {@code chunk_has_nodes} marked the chunk. Migration is one-way: once a chunk is moved to the
 * region files its PDC entries are deleted, which is what frees the chunk from carrying them.
 *
 * [ES] Lee (y borra) los datos de nodos que las versiones hasta la 5.2 guardaban en el
 * PersistentDataContainer del chunk: {@code n<x>_<y>_<z>} tenía el blob en Base64,
 * {@code t<x>_<y>_<z>} el tipo, y {@code chunk_has_nodes} marcaba el chunk. La migración es de ida:
 * en cuanto un chunk pasa a los archivos de región se borran sus entradas del PDC, que es lo que
 * libera al chunk de cargarlas.
 */
final class LegacyChunkData {

    private static final Pattern KEY = Pattern.compile("([nt])(-?\\d+)_(-?\\d+)_(-?\\d+)");

    /** One legacy node; {@code data} is the decoded Base64 or null / Un nodo antiguo. */
    record Entry(int x, int y, int z, String type, byte[] data) {
    }

    private LegacyChunkData() {
    }

    static boolean present(Chunk chunk) {
        return Keys.CHUNK_HAS_NODES != null
                && chunk.getPersistentDataContainer().has(Keys.CHUNK_HAS_NODES, PersistentDataType.BYTE);
    }

    /**
     * EN: Every legacy node of the chunk, removing its keys and the marker as it goes.
     * ES: Todos los nodos antiguos del chunk, borrando sus claves y la marca por el camino.
     */
    static List<Entry> extract(Chunk chunk, String namespace) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        Map<String, String[]> byPos = new LinkedHashMap<>();
        List<NamespacedKey> consumed = new ArrayList<>();
        for (NamespacedKey key : pdc.getKeys()) {
            if (!key.getNamespace().equalsIgnoreCase(namespace)) {
                continue;
            }
            Matcher match = KEY.matcher(key.getKey());
            if (!match.matches()) {
                continue;
            }
            String value;
            try {
                value = pdc.get(key, PersistentDataType.STRING);
            } catch (IllegalArgumentException notAString) {
                value = null;
            }
            consumed.add(key);
            String pos = match.group(2) + "_" + match.group(3) + "_" + match.group(4);
            String[] slot = byPos.computeIfAbsent(pos, p -> new String[2]);
            slot["n".equals(match.group(1)) ? 0 : 1] = value;
        }
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, String[]> node : byPos.entrySet()) {
            String[] coords = node.getKey().split("_");
            String blob = node.getValue()[0];
            String type = node.getValue()[1];
            byte[] data = null;
            if (blob != null) {
                try {
                    data = Base64.getDecoder().decode(blob);
                } catch (IllegalArgumentException notBase64) {
                    // Se conserva tal cual: se leerá como corrupto (null), igual que antes.
                    data = blob.getBytes(StandardCharsets.UTF_8);
                }
            }
            if (type == null && data != null) {
                NodeBlob decoded = NodeStore.decodeBytes(data);
                type = decoded == null ? null : decoded.typeName;
            }
            if (type == null && data == null) {
                continue;
            }
            entries.add(new Entry(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]),
                    Integer.parseInt(coords[2]), type, data));
        }
        for (NamespacedKey key : consumed) {
            pdc.remove(key);
        }
        pdc.remove(Keys.CHUNK_HAS_NODES);
        return entries;
    }
}

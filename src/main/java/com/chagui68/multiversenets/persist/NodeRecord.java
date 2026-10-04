package com.chagui68.multiversenets.persist;

/**
 * [EN] One stored node: its type and, unless it is still in its default state, its encoded blob.
 * <p>
 * The type is kept apart so topology scans, piston/explosion guards and the per-chunk counters
 * never decode anything. {@link #data} is null when the blob equals {@link NodeBlob#create} for
 * that type, which is what every cable and every untouched device looks like: those nodes cost a
 * type name and a position, nothing else.
 * <p>
 * A record is replaced wholesale on every write ({@link NodeStore#put}), so {@link #live} can never
 * be older than {@link #data}.
 *
 * [ES] Un nodo guardado: su tipo y, salvo que siga en su estado por defecto, su blob codificado.
 * <p>
 * El tipo va aparte para que los escaneos de topología, las protecciones contra pistones y
 * explosiones y los contadores por chunk nunca decodifiquen nada. {@link #data} es null cuando el
 * blob es igual a {@link NodeBlob#create} de ese tipo, que es como se ve cada cable y cada
 * dispositivo sin configurar: esos nodos cuestan un nombre de tipo y una posición, nada más.
 * <p>
 * Cada escritura reemplaza el registro entero, así que {@link #live} nunca puede ser más viejo que
 * {@link #data}.
 */
final class NodeRecord {

    /** DeviceType name / Nombre del DeviceType. */
    final String type;
    /** Encoded blob, or null for the default blob of {@link #type} / Blob codificado, o null si es el de por defecto. */
    final byte[] data;
    /** Shared decoded instance served by {@link NodeStore#canonical}; null until asked / Instancia compartida. */
    NodeBlob live;

    NodeRecord(String type, byte[] data) {
        this.type = type;
        this.data = data;
    }
}

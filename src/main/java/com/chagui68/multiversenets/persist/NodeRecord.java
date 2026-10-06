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
 * be older than {@link #data}. A record written by {@link NodeStore#put} is not encoded until
 * something needs its bytes ({@link #data()}): the region writer on autosave, or a {@link
 * NodeStore#get} that wants an independent copy. A storage cell rewritten on every transfer used
 * to pay a GZIP + ObjectOutputStream encode each time; now it pays once per autosave. Main
 * thread only, like the region that holds it.
 *
 * [ES] Un nodo guardado: su tipo y, salvo que siga en su estado por defecto, su blob codificado.
 * <p>
 * El tipo va aparte para que los escaneos de topología, las protecciones contra pistones y
 * explosiones y los contadores por chunk nunca decodifiquen nada. {@link #data} es null cuando el
 * blob es igual a {@link NodeBlob#create} de ese tipo, que es como se ve cada cable y cada
 * dispositivo sin configurar: esos nodos cuestan un nombre de tipo y una posición, nada más.
 * <p>
 * Cada escritura reemplaza el registro entero, así que {@link #live} nunca puede ser más viejo que
 * {@link #data}. Lo que escribe {@link NodeStore#put} no se codifica hasta que alguien necesita los
 * bytes ({@link #data()}): el escritor de regiones en el autoguardado o un {@link NodeStore#get}
 * que quiere una copia independiente. Una celda reescrita en cada transferencia pagaba un GZIP +
 * ObjectOutputStream cada vez; ahora lo paga una vez por autoguardado. Solo hilo principal.
 */
final class NodeRecord {

    /** DeviceType name / Nombre del DeviceType. */
    final String type;
    /**
     * Encoded blob, or null for the default blob of {@link #type}; while {@link #stale}, the last
     * bytes that did encode / Blob codificado, o null si es el de por defecto; mientras
     * {@link #stale}, los últimos bytes que sí se codificaron.
     */
    private byte[] data;
    /** {@link #live} changed after {@link #data} was encoded / {@link #live} cambió tras codificar {@link #data}. */
    private boolean stale;
    /** Shared decoded instance served by {@link NodeStore#canonical}; null until asked / Instancia compartida. */
    NodeBlob live;

    NodeRecord(String type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    /**
     * EN: A record whose bytes are encoded from {@code live} on first use. {@code lastEncoded} is
     * what gets written if that encode ever fails, so one unserializable blob cannot sink the
     * whole region write.
     * ES: Un registro cuyos bytes se codifican desde {@code live} al primer uso. {@code lastEncoded}
     * es lo que se escribe si esa codificación falla, para que un blob no serializable no hunda la
     * escritura de toda la región.
     */
    static NodeRecord unencoded(String type, NodeBlob live, byte[] lastEncoded) {
        NodeRecord record = new NodeRecord(type, lastEncoded);
        record.live = live;
        record.stale = true;
        return record;
    }

    /** The last bytes that did encode, without encoding now / Los últimos bytes codificados, sin codificar ahora. */
    byte[] lastEncoded() {
        return data;
    }

    /** Encoded blob, or null for the default blob / Blob codificado, o null si es el de por defecto. */
    byte[] data() {
        if (stale) {
            stale = false;
            data = NodeStore.encodeRecord(type, live, data);
        }
        return data;
    }
}

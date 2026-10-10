package com.chagui68.multiversenets.persist;

import com.chagui68.multiversenets.util.PosUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * [EN] Binary format of a region file ({@code r.<rx>.<rz>.mvn}).
 * <pre>
 * int    magic "MVNR"
 * int    format version
 * int    rx, rz
 * int    type count, then one UTF string per distinct type
 * int    node count, then per node:
 *          int x, int y, int z       absolute block position
 *          int typeIndex             -1 = no type
 *          int length                -1 = default blob (nothing stored)
 *          byte[length]              encoded blob, exactly as {@link NodeStore} produced it
 * long   CRC32 of every byte above
 * </pre>
 * The blobs are already compressed, so the file is not compressed again; the type table keeps a
 * cable at 20 bytes. A file whose checksum does not match is reported as corrupt instead of being
 * half-read.
 *
 * [ES] Formato binario de un archivo de región. Los blobs ya van comprimidos, así que el archivo no
 * se vuelve a comprimir; la tabla de tipos deja un cable en 20 bytes. Un archivo cuya suma de
 * comprobación no coincide se informa como corrupto en vez de leerse a medias.
 */
final class RegionFile {

    static final int MAGIC = 0x4D564E52;
    static final int VERSION = 1;
    static final String EXTENSION = ".mvn";

    private RegionFile() {
    }

    static String fileName(int rx, int rz) {
        return "r." + rx + "." + rz + EXTENSION;
    }

    /**
     * EN: Parses {@code r.<rx>.<rz>.mvn}; null for any other name.
     * ES: Interpreta {@code r.<rx>.<rz>.mvn}; null para cualquier otro nombre.
     */
    static int[] parseName(String name) {
        if (!name.startsWith("r.") || !name.endsWith(EXTENSION)) {
            return null;
        }
        String[] parts = name.substring(2, name.length() - EXTENSION.length()).split("\\.");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (NumberFormatException notARegion) {
            return null;
        }
    }

    /**
     * Everything the file needs, frozen: positions, types and each record's encoded bytes (byte
     * arrays are never modified after encoding). Taking it is the only part that must run on the
     * main thread, because encoding reads live node objects; laying out the file and its checksum
     * can then run on the I/O thread.
     */
    record Snapshot(int rx, int rz, long[] positions, String[] types, byte[][] data) {
    }

    /** Main thread: freezes the region, encoding changed nodes / Hilo principal: congela la región. */
    static Snapshot snapshot(NodeRegion region) {
        Map<Long, NodeRecord> nodes = region.nodes();
        long[] positions = new long[nodes.size()];
        String[] types = new String[nodes.size()];
        byte[][] data = new byte[nodes.size()][];
        int i = 0;
        for (Map.Entry<Long, NodeRecord> entry : nodes.entrySet()) {
            positions[i] = entry.getKey();
            types[i] = entry.getValue().type;
            data[i] = entry.getValue().data();
            i++;
        }
        return new Snapshot(region.rx, region.rz, positions, types, data);
    }

    /** Main thread: a snapshot of the region as bytes / Hilo principal: instantánea en bytes. */
    static byte[] serialize(NodeRegion region) {
        return assemble(snapshot(region));
    }

    /** Any thread: the file bytes of a snapshot / Cualquier hilo: los bytes del archivo. */
    static byte[] assemble(Snapshot snapshot) {
        Map<String, Integer> typeIndex = new HashMap<>();
        List<String> types = new ArrayList<>();
        for (String type : snapshot.types()) {
            if (type != null && !typeIndex.containsKey(type)) {
                typeIndex.put(type, types.size());
                types.add(type);
            }
        }
        int count = snapshot.positions().length;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(64 + count * 24);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(snapshot.rx());
            out.writeInt(snapshot.rz());
            out.writeInt(types.size());
            for (String type : types) {
                out.writeUTF(type);
            }
            out.writeInt(count);
            for (int i = 0; i < count; i++) {
                long pos = snapshot.positions()[i];
                String type = snapshot.types()[i];
                out.writeInt(PosUtil.unpackX(pos));
                out.writeInt(PosUtil.unpackY(pos));
                out.writeInt(PosUtil.unpackZ(pos));
                out.writeInt(type == null ? -1 : typeIndex.get(type));
                byte[] data = snapshot.data()[i];
                if (data == null) {
                    out.writeInt(-1);
                } else {
                    out.writeInt(data.length);
                    out.write(data);
                }
            }
            CRC32 crc = new CRC32();
            crc.update(bytes.toByteArray());
            out.writeLong(crc.getValue());
        } catch (IOException impossible) {
            // ByteArrayOutputStream no lanza IOException.
            throw new UncheckedIOException(impossible);
        }
        return bytes.toByteArray();
    }

    /** Any thread: rebuilds a region from its file / Cualquier hilo: reconstruye la región. */
    static NodeRegion deserialize(int rx, int rz, byte[] bytes) throws IOException {
        if (bytes.length < 28) {
            throw new IOException("file too short (" + bytes.length + " bytes)");
        }
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, bytes.length - 8);
        long stored = 0;
        for (int i = bytes.length - 8; i < bytes.length; i++) {
            stored = (stored << 8) | (bytes[i] & 0xFFL);
        }
        if (crc.getValue() != stored) {
            throw new IOException("checksum mismatch");
        }
        NodeRegion region = new NodeRegion(rx, rz);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes, 0, bytes.length - 8))) {
            if (in.readInt() != MAGIC) {
                throw new IOException("not a MultiverseNets region file");
            }
            int version = in.readInt();
            if (version > VERSION) {
                throw new IOException("written by a newer MultiverseNets (format " + version + ")");
            }
            int fileRx = in.readInt();
            int fileRz = in.readInt();
            if (fileRx != rx || fileRz != rz) {
                throw new IOException("holds region " + fileRx + "," + fileRz + ", expected " + rx + "," + rz);
            }
            String[] types = new String[in.readInt()];
            for (int i = 0; i < types.length; i++) {
                types[i] = in.readUTF();
            }
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                int typeIndex = in.readInt();
                int length = in.readInt();
                byte[] data = null;
                if (length >= 0) {
                    data = new byte[length];
                    in.readFully(data);
                }
                String type = typeIndex >= 0 && typeIndex < types.length ? types[typeIndex] : null;
                region.put(x, y, z, new NodeRecord(type, data));
            }
        }
        region.dirty = false;
        return region;
    }
}

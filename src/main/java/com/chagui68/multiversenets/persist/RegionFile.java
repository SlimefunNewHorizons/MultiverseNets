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

    /** Main thread: a snapshot of the region as bytes / Hilo principal: instantánea en bytes. */
    static byte[] serialize(NodeRegion region) {
        Map<String, Integer> typeIndex = new HashMap<>();
        List<String> types = new ArrayList<>();
        for (NodeRecord record : region.nodes().values()) {
            if (record.type != null && !typeIndex.containsKey(record.type)) {
                typeIndex.put(record.type, types.size());
                types.add(record.type);
            }
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(64 + region.size() * 24);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(region.rx);
            out.writeInt(region.rz);
            out.writeInt(types.size());
            for (String type : types) {
                out.writeUTF(type);
            }
            out.writeInt(region.size());
            for (Map.Entry<Long, NodeRecord> entry : region.nodes().entrySet()) {
                long pos = entry.getKey();
                NodeRecord record = entry.getValue();
                out.writeInt(PosUtil.unpackX(pos));
                out.writeInt(PosUtil.unpackY(pos));
                out.writeInt(PosUtil.unpackZ(pos));
                out.writeInt(record.type == null ? -1 : typeIndex.get(record.type));
                if (record.data == null) {
                    out.writeInt(-1);
                } else {
                    out.writeInt(record.data.length);
                    out.write(record.data);
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

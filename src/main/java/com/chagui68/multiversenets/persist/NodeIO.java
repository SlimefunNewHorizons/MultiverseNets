package com.chagui68.multiversenets.persist;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * [EN] The single thread that touches region files.
 * <p>
 * One thread on purpose: reads and writes of the same file run in the order they were queued, so a
 * read can never see the file from before a write that was asked first. Writes go to a temporary
 * file that then replaces the real one in one move, so a crash leaves either the old file or the
 * new one, never half of each. A file that cannot be read is renamed to {@code .corrupt-<time>} and
 * kept for inspection; the region then starts empty instead of stopping the server.
 *
 * [ES] El único hilo que toca los archivos de región. Uno solo a propósito: lecturas y escrituras
 * del mismo archivo corren en el orden en que se pidieron. Las escrituras van a un archivo
 * temporal que luego reemplaza al real de un solo movimiento, así un crash deja el archivo viejo o
 * el nuevo, nunca mitad y mitad. Un archivo ilegible se renombra a {@code .corrupt-<hora>} y se
 * conserva para inspeccionarlo; la región empieza vacía en vez de detener el servidor.
 */
final class NodeIO {

    private final Logger logger;
    private final ExecutorService executor;

    NodeIO(Logger logger) {
        this.logger = logger;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "MultiverseNets-NodeIO");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Reads a region; never fails, a broken file yields an empty region / Nunca falla. */
    CompletableFuture<NodeRegion> read(Path file, int rx, int rz) {
        return submit(() -> {
            try {
                return RegionFile.deserialize(rx, rz, Files.readAllBytes(file));
            } catch (NoSuchFileException gone) {
                return new NodeRegion(rx, rz);
            } catch (IOException | RuntimeException broken) {
                quarantine(file, broken);
                return new NodeRegion(rx, rz);
            }
        });
    }

    /** Writes {@code bytes}, or deletes the file when null / Escribe, o borra si es null. */
    CompletableFuture<Void> write(Path file, byte[] bytes) {
        return write(file, () -> bytes);
    }

    /**
     * EN: Like {@link #write(Path, byte[])}, but the bytes are produced on the I/O thread (a region
     * snapshot is laid out and checksummed there, not on the main thread). A null result deletes.
     * ES: Igual, pero los bytes se producen en el hilo de E/S. Null borra el archivo.
     */
    CompletableFuture<Void> write(Path file, java.util.function.Supplier<byte[]> producer) {
        return submit(() -> {
            byte[] bytes = producer.get();
            try {
                if (bytes == null) {
                    Files.deleteIfExists(file);
                    return null;
                }
                Files.createDirectories(file.getParent());
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.write(tmp, bytes);
                try {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException noAtomicMove) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException error) {
                throw new java.io.UncheckedIOException(error);
            }
            return null;
        });
    }

    /**
     * EN: Lets every queued task finish, then stops the thread.
     * ES: Deja terminar todo lo encolado y detiene el hilo.
     */
    void shutdown(long timeoutSeconds) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                logger.severe("Node storage did not finish writing within " + timeoutSeconds
                        + " s; some network data may not be saved.");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /** After shutdown the work runs on the caller, so a late flush is never dropped / Tras apagar, en el llamante. */
    private <T> CompletableFuture<T> submit(Supplier<T> task) {
        try {
            return CompletableFuture.supplyAsync(task, executor);
        } catch (RejectedExecutionException stopped) {
            try {
                return CompletableFuture.completedFuture(task.get());
            } catch (RuntimeException error) {
                return CompletableFuture.failedFuture(error);
            }
        }
    }

    private void quarantine(Path file, Exception cause) {
        Path aside = file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis());
        try {
            Files.move(file, aside, StandardCopyOption.REPLACE_EXISTING);
            logger.log(Level.SEVERE, "Region file " + file + " could not be read (" + cause.getMessage()
                    + "). It was kept as " + aside.getFileName() + " and the region starts empty.");
        } catch (IOException moveFailed) {
            logger.log(Level.SEVERE, "Region file " + file + " could not be read (" + cause.getMessage()
                    + ") nor moved aside (" + moveFailed.getMessage() + ").");
        }
    }
}

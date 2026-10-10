package com.chagui68.multiversenets.compat;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] The network loop asks {@link ProtectionBridge#mayActorUse(World, int, int, int, UUID)} for
 * every block it touches, thousands of times per second. That path used to skip every cache and
 * call the protection plugin each time. These tests pin that repeated questions are answered from
 * memory, with the same answers as the uncached rule, until the cache is dropped.
 *
 * [ES] El bucle de red pregunta por cada bloque que toca. Ese camino se saltaba todas las cachés y
 * llamaba al plugin de protección cada vez. Estos tests fijan que las preguntas repetidas se
 * contestan de memoria, con las mismas respuestas que la regla sin caché.
 */
class ProtectionCacheTest {

    private static final int CLAIM_X = 100;
    private static final int CLAIM_Y = 64;
    private static final int CLAIM_Z = 200;

    private final AtomicInteger tests = new AtomicInteger();
    private final AtomicInteger ownerChecks = new AtomicInteger();
    private final AtomicInteger supportChecks = new AtomicInteger();

    @AfterEach
    void restore() {
        ProtectionBridge.useProvidersForTest(List.of());
    }

    private static World world(String name) {
        UUID uid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return (World) java.lang.reflect.Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getUID" -> uid;
                    case "toString" -> "World[" + name + "]";
                    case "hashCode" -> name.hashCode();
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(
                            "WorldStub." + method.getName() + " is not stubbed");
                });
    }

    private ProtectionBridge.Provider countingClaimOf(UUID owner) {
        return new ProtectionBridge.Provider() {
            @Override
            public String id() {
                return "COUNTING";
            }

            @Override
            public boolean setup(java.util.logging.Logger logger) {
                return true;
            }

            @Override
            public boolean supports(World world) {
                supportChecks.incrementAndGet();
                return true;
            }

            @Override
            public boolean test(Location loc) {
                tests.incrementAndGet();
                return isClaim(loc);
            }

            @Override
            public Boolean allowsActor(UUID who, Location loc) {
                ownerChecks.incrementAndGet();
                return who != null && who.equals(owner) && isClaim(loc) ? Boolean.TRUE : null;
            }
        };
    }

    private static boolean isClaim(Location loc) {
        return loc.getBlockX() == CLAIM_X && loc.getBlockY() == CLAIM_Y && loc.getBlockZ() == CLAIM_Z;
    }

    @Test
    @DisplayName("repeated checks of unclaimed land ask the protection plugin once")
    void unclaimedLandIsAskedOnce() {
        ProtectionBridge.useProvidersForTest(List.of(countingClaimOf(UUID.randomUUID())));
        World world = world("world");
        UUID stranger = UUID.randomUUID();

        for (int i = 0; i < 100; i++) {
            assertTrue(ProtectionBridge.mayActorUse(world, 0, 64, 0, stranger));
        }

        assertEquals(1, tests.get(), "the anonymous answer must come from the cache after the first call");
        assertEquals(0, ownerChecks.get(), "unclaimed land needs no ownership check at all");
        assertEquals(1, supportChecks.get(), "whether the provider manages the world is asked once");
    }

    @Test
    @DisplayName("owner and stranger keep their own answers inside a claim, both cached")
    void claimAnswersAreCachedPerActor() {
        UUID owner = UUID.randomUUID();
        ProtectionBridge.useProvidersForTest(List.of(countingClaimOf(owner)));
        World world = world("world");
        UUID stranger = UUID.randomUUID();

        for (int i = 0; i < 50; i++) {
            assertTrue(ProtectionBridge.mayActorUse(world, CLAIM_X, CLAIM_Y, CLAIM_Z, owner));
            assertFalse(ProtectionBridge.mayActorUse(world, CLAIM_X, CLAIM_Y, CLAIM_Z, stranger));
            assertFalse(ProtectionBridge.mayActorUse(world, CLAIM_X, CLAIM_Y, CLAIM_Z, null),
                    "a network without an owner is a stranger");
        }

        assertEquals(1, tests.get());
        assertEquals(2, ownerChecks.get(), "one ownership lookup per actor, then memory");
    }

    @Test
    @DisplayName("the periodic sweep keeps answers that have not expired yet")
    void sweepKeepsLiveAnswers() {
        ProtectionBridge.useProvidersForTest(List.of(countingClaimOf(UUID.randomUUID())));
        World world = world("world");

        ProtectionBridge.mayActorUse(world, 0, 64, 0, null);
        ProtectionBridge.sweepExpired();
        ProtectionBridge.mayActorUse(world, 0, 64, 0, null);

        assertEquals(1, tests.get(), "a sweep must not drop the whole cache at once any more");
    }

    @Test
    @DisplayName("dropping the cache asks the plugin again, so claim changes are honoured")
    void invalidateAsksAgain() {
        ProtectionBridge.useProvidersForTest(List.of(countingClaimOf(UUID.randomUUID())));
        World world = world("world");

        ProtectionBridge.mayActorUse(world, 0, 64, 0, null);
        ProtectionBridge.invalidate();
        ProtectionBridge.mayActorUse(world, 0, 64, 0, null);

        assertEquals(2, tests.get());
    }
}

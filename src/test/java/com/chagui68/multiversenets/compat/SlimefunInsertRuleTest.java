package com.chagui68.multiversenets.compat;

import com.chagui68.multiversenets.MultiverseNets;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * [EN] The insert rule for Slimefun machines ({@link SlimefunBridge#fillInputSlots}): top up the
 * slots that already hold the item, then open at most one new slot per insert, like NetworksV6. A
 * fake Electric Smeltery answers the transport question exactly as Slimefun's does, so each
 * ingredient ends up in a single stack; an ordinary machine still fills every input slot, one per
 * cycle.
 *
 * [ES] La regla de inserción en máquinas de Slimefun: rellenar los huecos que ya tienen el ítem y
 * abrir como mucho un hueco nuevo por inserción, como NetworksV6. Una Electric Smeltery falsa responde
 * a la pregunta de transporte igual que la de Slimefun, así cada ingrediente queda en un solo stack;
 * una máquina normal sigue llenando todos sus huecos, uno por ciclo.
 */
class SlimefunInsertRuleTest {

    private static final int[] INPUTS = {19, 20, 21, 28, 29, 30};

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        MockBukkit.load(MultiverseNets.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** An array-backed machine menu / Menú de máquina sobre un array. */
    private static class FakeMenu implements SlimefunBridge.SlotAccess {
        final ItemStack[] slots = new ItemStack[54];

        @Override
        public ItemStack get(int slot) {
            return slots[slot];
        }

        @Override
        public void set(int slot, ItemStack item) {
            slots[slot] = item;
        }

        int amountOf(Material type) {
            int total = 0;
            for (int slot : INPUTS) {
                if (slots[slot] != null && slots[slot].getType() == type) {
                    total += slots[slot].getAmount();
                }
            }
            return total;
        }

        int freeInputs() {
            int free = 0;
            for (int slot : INPUTS) {
                if (slots[slot] == null) {
                    free++;
                }
            }
            return free;
        }
    }

    /** What ElectricSmeltery#getSlotsAccessedByItemTransport answers for INSERT. */
    private static int[] smelterySlots(FakeMenu menu, ItemStack item) {
        List<Integer> holding = new ArrayList<>();
        int full = 0;
        for (int slot : INPUTS) {
            ItemStack stack = menu.slots[slot];
            if (stack != null && stack.isSimilar(item)) {
                if (stack.getAmount() >= stack.getMaxStackSize()) {
                    full++;
                }
                holding.add(slot);
            }
        }
        if (holding.isEmpty()) {
            return INPUTS;
        }
        if (full == holding.size()) {
            return new int[0];
        }
        return holding.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int insertIntoSmeltery(FakeMenu menu, Material type, int amount) {
        ItemStack stack = new ItemStack(type, amount);
        return SlimefunBridge.fillInputSlots(menu, smelterySlots(menu, stack), stack, 0, error -> {
            throw error;
        });
    }

    @Test
    void aSmelteryKeepsEachIngredientInOneStack() {
        FakeMenu smeltery = new FakeMenu();

        assertEquals(1024 - 64, insertIntoSmeltery(smeltery, Material.IRON_INGOT, 1024),
                "an Advanced Pusher's 1,024 units: only one stack goes in");
        assertEquals(64, smeltery.amountOf(Material.IRON_INGOT));
        assertEquals(1024, insertIntoSmeltery(smeltery, Material.IRON_INGOT, 1024),
                "with its stack full the smeltery takes no more iron");

        assertEquals(1024 - 64, insertIntoSmeltery(smeltery, Material.GOLD_INGOT, 1024));
        assertEquals(1024 - 64, insertIntoSmeltery(smeltery, Material.COPPER_INGOT, 1024));
        assertEquals(64, smeltery.amountOf(Material.GOLD_INGOT));
        assertEquals(3, smeltery.freeInputs(), "the other slots stay free for other ingredients");
    }

    @Test
    void aSmelteryStackIsToppedUpWithoutOpeningAnotherSlot() {
        FakeMenu smeltery = new FakeMenu();
        smeltery.slots[INPUTS[2]] = new ItemStack(Material.IRON_INGOT, 10);

        assertEquals(46, insertIntoSmeltery(smeltery, Material.IRON_INGOT, 100));
        assertEquals(64, smeltery.slots[INPUTS[2]].getAmount());
        assertEquals(5, smeltery.freeInputs(), "no second iron slot");
    }

    @Test
    void anOrdinaryMachineStillFillsEverySlotOnePerCycle() {
        FakeMenu machine = new FakeMenu();
        ItemStack batch = new ItemStack(Material.COBBLESTONE, 1024);
        for (int cycle = 1; cycle <= INPUTS.length; cycle++) {
            int left = SlimefunBridge.fillInputSlots(machine, INPUTS, batch, 0, error -> {
                throw error;
            });
            assertEquals(1024 - 64, left, "cycle " + cycle + " moves one stack");
            assertEquals(64 * cycle, machine.amountOf(Material.COBBLESTONE));
        }
        assertEquals(0, machine.freeInputs(), "after six cycles every slot is used");
        assertEquals(1024, SlimefunBridge.fillInputSlots(machine, INPUTS, batch, 0, error -> {
            throw error;
        }), "a full machine takes nothing");
    }

    @Test
    void aFailingMenuNeverReportsMoreThanWhatReallyStayedOut() {
        FakeMenu broken = new FakeMenu() {
            @Override
            public void set(int slot, ItemStack item) {
                if (slot == INPUTS[0]) {
                    throw new IllegalStateException("menu gone");
                }
                super.set(slot, item);
            }
        };
        broken.slots[INPUTS[1]] = new ItemStack(Material.SAND, 54);
        RuntimeException[] seen = new RuntimeException[1];

        // Rellena el hueco con arena (+10) y falla al abrir uno nuevo: devuelve 90, no 100.
        int left = SlimefunBridge.fillInputSlots(broken, new int[]{INPUTS[1], INPUTS[0]},
                new ItemStack(Material.SAND, 100), 0, error -> seen[0] = error);

        assertEquals(64, broken.slots[INPUTS[1]].getAmount());
        assertEquals(90, left, "what went in is not returned again");
        assertEquals("menu gone", seen[0].getMessage(), "the failure is reported");
    }
}

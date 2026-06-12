package org.mesdag.particlestorm.api;

import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.mesdag.particlestorm.ParticleStorm;

public class IntAllocator {
    private final IntPriorityQueue availableIds;
    private final IntSet usedIds;
    private int nextId;

    public IntAllocator() {
        this.availableIds = new IntHeapPriorityQueue();
        this.usedIds = new IntOpenHashSet();
        this.nextId = 0;
    }

    public int allocate() {
        int id = availableIds.isEmpty() ? nextId++ : availableIds.dequeueInt();
        usedIds.add(id);
        return id;
    }

    public void release(int id) {
        if (usedIds.contains(id)) {
            usedIds.remove(id);
            availableIds.enqueue(id);
        } else {
            ParticleStorm.LOGGER.warn("ID {} is not currently allocated.", id);
        }
    }

    public boolean isAllocated(int id) {
        return usedIds.contains(id);
    }

    public boolean forceAllocate(int id) {
        return usedIds.add(id);
    }

    public void clear() {
        availableIds.clear();
        usedIds.clear();
        this.nextId = 0;
    }
}

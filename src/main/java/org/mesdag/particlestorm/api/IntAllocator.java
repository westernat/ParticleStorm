package org.mesdag.particlestorm.api;

import org.mesdag.particlestorm.ParticleStorm;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

public class IntAllocator {
    private final PriorityQueue<Integer> availableIds;
    private final Set<Integer> usedIds;
    private int nextId;

    public IntAllocator() {
        this.availableIds = new PriorityQueue<>();
        this.usedIds = new HashSet<>();
        this.nextId = 0;
    }

    public int allocate() {
        while (!availableIds.isEmpty()) {
            int id = availableIds.poll();
            if (usedIds.add(id)) {
                return id;
            }
        }

        while (usedIds.contains(nextId)) {
            nextId++;
        }
        int id = nextId++;
        usedIds.add(id);
        return id;
    }

    public void release(int id) {
        if (usedIds.contains(id)) {
            usedIds.remove(id);
            availableIds.offer(id);
        } else {
            ParticleStorm.LOGGER.warn("ID {} is not currently allocated.", id);
        }
    }

    public boolean isAllocated(int id) {
        return usedIds.contains(id);
    }

    public boolean forceAllocate(int id) {
        boolean wasAllocated = usedIds.contains(id);
        usedIds.add(id);
        availableIds.remove(id);
        if (id >= nextId) {
            nextId = id + 1;
        }
        return wasAllocated;
    }

    public void clear() {
        availableIds.clear();
        usedIds.clear();
        this.nextId = 0;
    }
}

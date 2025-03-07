package org.mesdag.particlestorm.api;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

public class IntAllocator {
    private final PriorityQueue<Integer> availableIds; // 用于存储可用的ID
    private final Set<Integer> usedIds; // 用于存储已使用的ID
    private int nextId; // 下一个可用的ID

    public IntAllocator() {
        this.availableIds = new PriorityQueue<>();
        this.usedIds = new HashSet<>();
        this.nextId = 0; // 从0开始分配
    }

    // 分配一个不重复的ID
    public int allocate() {
        int id;// 标记为已使用
        if (availableIds.isEmpty()) {
            id = nextId++;
        } else {
            id = availableIds.poll();
        }
        usedIds.add(id); // 标记为已使用
        return id;
    }

    // 释放一个ID，使其可以被复用
    public void release(int id) {
        if (usedIds.contains(id)) {
            usedIds.remove(id); // 从已使用集合中移除
            availableIds.offer(id); // 添加到可用ID队列中
        } else {
            throw new IllegalArgumentException("ID " + id + " is not currently allocated.");
        }
    }

    // 检查某个ID是否已被分配
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

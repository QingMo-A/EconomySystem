package com.mo.economy_system.core.storybook_system;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 玩家随记本数据。
 * 只保存玩家自己的收集、阅读、章节解锁和排序状态。
 */
public class StoryBookData {
    private boolean hasStoryBook;
    private boolean journeyStarted;
    private int fragmentPageUseCount;
    private Set<Integer> unlockedFragmentIds = new LinkedHashSet<>();
    private Set<Integer> readFragmentIds = new HashSet<>();
    private Set<Integer> unlockedChapterIds = new LinkedHashSet<>();
    private List<Integer> obtainedOrder = new ArrayList<>();

    public StoryBookData() {
    }

    public boolean hasStoryBook() {
        return hasStoryBook;
    }

    public void setHasStoryBook(boolean hasStoryBook) {
        this.hasStoryBook = hasStoryBook;
    }

    public boolean isJourneyStarted() {
        return journeyStarted;
    }

    public void setJourneyStarted(boolean journeyStarted) {
        this.journeyStarted = journeyStarted;
    }

    public int getFragmentPageUseCount() {
        return fragmentPageUseCount;
    }

    public void setFragmentPageUseCount(int fragmentPageUseCount) {
        this.fragmentPageUseCount = Math.max(0, fragmentPageUseCount);
    }

    public void incrementFragmentPageUseCount() {
        this.fragmentPageUseCount++;
    }

    public Set<Integer> getUnlockedFragmentIds() {
        return unlockedFragmentIds;
    }

    public void setUnlockedFragmentIds(Set<Integer> unlockedFragmentIds) {
        this.unlockedFragmentIds = unlockedFragmentIds != null
                ? new LinkedHashSet<>(unlockedFragmentIds)
                : new LinkedHashSet<>();
        rebuildObtainedOrder();
    }

    public Set<Integer> getReadFragmentIds() {
        return readFragmentIds;
    }

    public void setReadFragmentIds(Set<Integer> readFragmentIds) {
        this.readFragmentIds = readFragmentIds != null
                ? new HashSet<>(readFragmentIds)
                : new HashSet<>();
    }

    public Set<Integer> getUnlockedChapterIds() {
        return unlockedChapterIds;
    }

    public void setUnlockedChapterIds(Set<Integer> unlockedChapterIds) {
        this.unlockedChapterIds = unlockedChapterIds != null
                ? new LinkedHashSet<>(unlockedChapterIds)
                : new LinkedHashSet<>();
    }

    public List<Integer> getObtainedOrder() {
        return obtainedOrder;
    }

    public void setObtainedOrder(List<Integer> obtainedOrder) {
        this.obtainedOrder = obtainedOrder != null
                ? new ArrayList<>(obtainedOrder)
                : new ArrayList<>();
        normalizeObtainedOrder();
    }

    /**
     * 获取排序后的片段ID列表（玩家自定义顺序）
     */
    public List<Integer> getSortedFragmentIds() {
        return new ArrayList<>(obtainedOrder);
    }

    /**
     * 移动片段到指定位置
     * @param fragmentId 片段ID
     * @param newPosition 目标位置（0-based）
     * @return 是否移动成功
     */
    public boolean moveFragmentTo(int fragmentId, int newPosition) {
        if (!unlockedFragmentIds.contains(fragmentId)) {
            return false;
        }

        int currentIndex = obtainedOrder.indexOf(fragmentId);
        if (currentIndex < 0) {
            return false;
        }

        // 确保新位置在有效范围内
        newPosition = Math.max(0, Math.min(newPosition, obtainedOrder.size() - 1));

        obtainedOrder.remove(currentIndex);
        obtainedOrder.add(newPosition, fragmentId);
        return true;
    }

    /**
     * 交换两个片段的位置
     * @param fragmentId1 第一个片段ID
     * @param fragmentId2 第二个片段ID
     * @return 是否交换成功
     */
    public boolean swapFragments(int fragmentId1, int fragmentId2) {
        if (!unlockedFragmentIds.contains(fragmentId1) || !unlockedFragmentIds.contains(fragmentId2)) {
            return false;
        }

        int index1 = obtainedOrder.indexOf(fragmentId1);
        int index2 = obtainedOrder.indexOf(fragmentId2);

        if (index1 < 0 || index2 < 0) {
            return false;
        }

        obtainedOrder.set(index1, fragmentId2);
        obtainedOrder.set(index2, fragmentId1);
        return true;
    }

    /**
     * 将片段移动到最前面
     */
    public boolean moveFragmentToFront(int fragmentId) {
        return moveFragmentTo(fragmentId, 0);
    }

    /**
     * 将片段移动到最后面
     */
    public boolean moveFragmentToBack(int fragmentId) {
        return moveFragmentTo(fragmentId, obtainedOrder.size() - 1);
    }

    /**
     * 向前移动一位
     */
    public boolean moveFragmentUp(int fragmentId) {
        int currentIndex = obtainedOrder.indexOf(fragmentId);
        if (currentIndex <= 0) {
            return false;
        }
        return moveFragmentTo(fragmentId, currentIndex - 1);
    }

    /**
     * 向后移动一位
     */
    public boolean moveFragmentDown(int fragmentId) {
        int currentIndex = obtainedOrder.indexOf(fragmentId);
        if (currentIndex < 0 || currentIndex >= obtainedOrder.size() - 1) {
            return false;
        }
        return moveFragmentTo(fragmentId, currentIndex + 1);
    }

    public boolean unlockFragment(int fragmentId) {
        if (fragmentId <= 0) {
            return false;
        }
        boolean added = unlockedFragmentIds.add(fragmentId);
        if (added && !obtainedOrder.contains(fragmentId)) {
            obtainedOrder.add(fragmentId);
        }
        return added;
    }

    public boolean hasUnlockedFragment(int fragmentId) {
        return unlockedFragmentIds.contains(fragmentId);
    }

    public void markFragmentRead(int fragmentId) {
        if (fragmentId > 0) {
            readFragmentIds.add(fragmentId);
        }
    }

    public boolean hasReadFragment(int fragmentId) {
        return readFragmentIds.contains(fragmentId);
    }

    public boolean unlockChapter(int chapterId) {
        if (chapterId <= 0) {
            return false;
        }
        return unlockedChapterIds.add(chapterId);
    }

    public boolean hasUnlockedChapter(int chapterId) {
        return unlockedChapterIds.contains(chapterId);
    }

    public int getUnlockedFragmentCount() {
        return unlockedFragmentIds.size();
    }

    public int getReadFragmentCount() {
        return readFragmentIds.size();
    }

    public void clearAllFragments() {
        unlockedFragmentIds.clear();
        readFragmentIds.clear();
        obtainedOrder.clear();
    }

    private void rebuildObtainedOrder() {
        if (obtainedOrder == null) {
            obtainedOrder = new ArrayList<>();
        }
        if (obtainedOrder.isEmpty()) {
            obtainedOrder.addAll(unlockedFragmentIds);
            return;
        }
        normalizeObtainedOrder();
    }

    private void normalizeObtainedOrder() {
        LinkedHashSet<Integer> normalized = new LinkedHashSet<>();
        for (Integer fragmentId : obtainedOrder) {
            if (fragmentId != null && unlockedFragmentIds.contains(fragmentId)) {
                normalized.add(fragmentId);
            }
        }
        for (Integer fragmentId : unlockedFragmentIds) {
            if (fragmentId != null) {
                normalized.add(fragmentId);
            }
        }
        obtainedOrder = new ArrayList<>(normalized);
    }
}

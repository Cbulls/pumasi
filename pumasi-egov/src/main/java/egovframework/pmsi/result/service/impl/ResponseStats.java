package egovframework.pmsi.result.service.impl;

/** 폼 응답 통계(KPI). MyBatis 매핑. */
public class ResponseStats {
    private int totalResponses;
    private int passCount;
    private int holdCount;
    private int rejectCount;
    private int unlockedCount;
    private int unlockedPassCount;
    private int lockedCount;

    public int getTotalResponses() { return totalResponses; }
    public void setTotalResponses(int totalResponses) { this.totalResponses = totalResponses; }
    public int getPassCount() { return passCount; }
    public void setPassCount(int passCount) { this.passCount = passCount; }
    public int getHoldCount() { return holdCount; }
    public void setHoldCount(int holdCount) { this.holdCount = holdCount; }
    public int getRejectCount() { return rejectCount; }
    public void setRejectCount(int rejectCount) { this.rejectCount = rejectCount; }
    public int getUnlockedCount() { return unlockedCount; }
    public void setUnlockedCount(int unlockedCount) { this.unlockedCount = unlockedCount; }
    public int getUnlockedPassCount() { return unlockedPassCount; }
    public void setUnlockedPassCount(int unlockedPassCount) { this.unlockedPassCount = unlockedPassCount; }
    public int getLockedCount() { return lockedCount; }
    public void setLockedCount(int lockedCount) { this.lockedCount = lockedCount; }
}

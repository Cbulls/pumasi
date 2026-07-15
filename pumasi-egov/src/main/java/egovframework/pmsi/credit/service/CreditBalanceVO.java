package egovframework.pmsi.credit.service;

/** 크레딧 잔액 조회/전달용 VO (available=가용, escrow=예치잠금) */
public class CreditBalanceVO {
    private String userId;
    private long available;
    private long escrow;
    private long version;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public long getAvailable() { return available; }
    public void setAvailable(long available) { this.available = available; }
    public long getEscrow() { return escrow; }
    public void setEscrow(long escrow) { this.escrow = escrow; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}

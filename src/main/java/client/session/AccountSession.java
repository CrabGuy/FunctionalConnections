package client.session;

public final class AccountSession {
    private volatile String accountToken;

    public String accountToken() {
        return accountToken;
    }

    public void setAccountToken(String accountToken) {
        this.accountToken = accountToken;
    }

    public void clear() {
        this.accountToken = null;
    }
}

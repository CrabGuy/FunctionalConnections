package server.account;
import server.account.exceptions.InvalidTokenException;
import server.dto.AccountPrincipal;
public interface TokenSigner {
    String sign(String username, long expiresAt);
    AccountPrincipal verify(String token) throws InvalidTokenException;
}

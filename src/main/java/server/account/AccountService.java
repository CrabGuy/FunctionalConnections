package server.account;
import server.account.exceptions.IncorrectPasswordException;
import server.account.exceptions.InvalidTokenException;
import server.account.exceptions.NewUsernameAlreadyTakenException;
import server.account.exceptions.UsernameAlreadyRegisteredException;
import server.dto.AccountPrincipal;
import shared.dto.LoginData;
import shared.dto.RegisterData;
import shared.dto.UpdateCredentialsData;
public interface AccountService {
    RegisterData register(String username, String password) throws UsernameAlreadyRegisteredException;
    LoginData login(String username, String password, int udpPort, String remoteAddress) throws IncorrectPasswordException;
    void logout(String accountToken) throws InvalidTokenException;
    UpdateCredentialsData updateCredentials(String oldUsername, String newUsername,
                                             String oldPassword, String newPassword) throws IncorrectPasswordException, NewUsernameAlreadyTakenException;
    AccountPrincipal resolve(String accountToken) throws InvalidTokenException;
}

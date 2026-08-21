package shared;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "operation"
)
public sealed interface Request {

    String operation();

    record Signup(String operation, String username, String psw) implements Request {}
    record UpdateCredentials(String operation, String oldUsername, String oldPsw,
                             String newUsername, String newPsw) implements Request {}
    record Login(String operation, String username, String psw) implements Request {}
    record Logout(String operation) implements Request {}
    record SendAnswer(String operation, List<String> words) implements Request {}
    record RequestGameState(String operation, Long gameId) implements Request {}
    record RequestGameStatistics(String operation, Long gameId) implements Request {}
    record RequestLeaderboardInfo(String operation, String playerName, Integer topPlayers) implements Request {}
    record RequestPersonalStats(String operation) implements Request {}
}
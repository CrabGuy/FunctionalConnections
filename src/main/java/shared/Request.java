package shared;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "operation")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Request.Register.class, name = "register"),
    @JsonSubTypes.Type(value = Request.UpdateCredentials.class, name = "updateCredentials"),
    @JsonSubTypes.Type(value = Request.Login.class, name = "login"),
    @JsonSubTypes.Type(value = Request.Logout.class, name = "logout"),
    @JsonSubTypes.Type(value = Request.SubmitProposal.class, name = "submitProposal"),
    @JsonSubTypes.Type(value = Request.RequestGameInfo.class, name = "requestGameInfo"),
    @JsonSubTypes.Type(value = Request.RequestGameStats.class, name = "requestGameStats"),
    @JsonSubTypes.Type(value = Request.RequestLeaderboard.class, name = "requestLeaderboard"),
    @JsonSubTypes.Type(value = Request.RequestPlayerStats.class, name = "requestPlayerStats")
})
public sealed interface Request {
    record Register(String username, String psw) implements Request {}
    record UpdateCredentials(String oldUsername, String oldPsw, String newUsername, String newPsw) implements Request {}
    record Login(String username, String psw) implements Request {}
    record Logout() implements Request {}
    record SubmitProposal(List<String> words) implements Request {}
    record RequestGameInfo(Long gameId) implements Request {}
    record RequestGameStats(Long gameId) implements Request {}
    record RequestLeaderboard(String playerName, Integer topPlayers) implements Request {}
    record RequestPlayerStats() implements Request {}
}
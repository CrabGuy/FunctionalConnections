package server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record User(
        String username,
        String passwordHash
) {
    @JsonCreator
    public User(
            @JsonProperty("username") String username,
            @JsonProperty("passwordHash") String passwordHash
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public User withUsername(String newUsername) {
        return new User(newUsername, passwordHash);
    }

    public User withPasswordHash(String newPasswordHash) {
        return new User(username, newPasswordHash);
    }
}
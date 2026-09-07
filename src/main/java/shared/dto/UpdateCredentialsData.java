package shared.dto;

/**
 * Response data for a successful credential update.
 *
 * @param newUsername the new username (may be unchanged)
 */
public record UpdateCredentialsData(String newUsername) {}

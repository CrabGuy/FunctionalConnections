package server.handlers;

import server.ServiceContext;
import server.UserManager;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

public final class UpdateCredentialsHandler implements RequestHandler {
    @Override
    public String operation() {
        return "updateCredentials";
    }

    @Override
    public Response handle(Request request, ServiceContext ctx, String currentUser) {
        if (!(request instanceof Request.UpdateCredentials update)) {
            return Response.error(ErrorCode.UNKNOWN_REQUEST);
        }

        if (update.oldUsername() == null || update.oldUsername().isBlank()) {
            return Response.error(ErrorCode.INVALID_USERNAME);
        }

        if (currentUser != null && !currentUser.equals(update.oldUsername())) {
            return Response.error(ErrorCode.UNAUTHORIZED_OR_USER_MISMATCH);
        }

        UserManager.UpdateResult result = ctx.userManager().updateCredentials(
                update.oldUsername(),
                update.oldPsw(),
                update.newUsername(),
                update.newPsw()
        );

        return switch (result) {
            case SUCCESS -> Response.success("Credentials updated successfully");
            case INVALID_CREDENTIALS -> Response.error(ErrorCode.INVALID_CREDENTIALS);
            case TARGET_USERNAME_TAKEN -> Response.error(ErrorCode.TARGET_USERNAME_TAKEN);
        };
    }
}
package server.handlers;

import server.ServiceContext;
import server.UserManager;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

public final class UpdateCredentialsHandler implements RequestHandler<Request.UpdateCredentials> {
    @Override
    public Response handle(Request.UpdateCredentials update, ServiceContext ctx, String currentUser) {
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
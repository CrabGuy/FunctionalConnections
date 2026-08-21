package server.handlers;

import server.ServiceContext;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

public final class LogoutHandler implements RequestHandler {
    @Override
    public String operation() {
        return "logout";
    }

    @Override
    public Response handle(Request request, ServiceContext ctx, String currentUser) {
        if (!(request instanceof Request.Logout)) {
            return Response.error(ErrorCode.UNKNOWN_REQUEST);
        }

        if (currentUser == null) {
            return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        }

        return Response.success("Logout successful");
    }
}
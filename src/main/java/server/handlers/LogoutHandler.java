package server.handlers;

import server.ServiceContext;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

public final class LogoutHandler implements RequestHandler<Request.Logout> {
    @Override
    public Response handle(Request.Logout request, ServiceContext ctx, String currentUser) {
        if (currentUser == null) {
            return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        }
        return Response.success("Logout successful");
    }
}
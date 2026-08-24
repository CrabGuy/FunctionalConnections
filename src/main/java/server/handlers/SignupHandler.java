package server.handlers;

import server.ServiceContext;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

public final class SignupHandler implements RequestHandler<Request.Signup> {
    @Override
    public Response handle(Request.Signup request, ServiceContext ctx, String currentUser) {
        if (request.username() == null || request.username().isBlank()
                || request.psw() == null || request.psw().isBlank()) {
            return Response.error(ErrorCode.INVALID_CREDENTIALS_FORMAT);
        }
        return ctx.userManager().register(request.username(), request.psw())
                ? Response.success("User registered successfully")
                : Response.error(ErrorCode.USERNAME_ALREADY_EXISTS);
    }
}
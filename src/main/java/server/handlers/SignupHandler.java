package server.handlers;

import server.ServiceContext;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

public final class SignupHandler implements RequestHandler {
    @Override
    public String operation() {
        return "signup";
    }

    @Override
    public Response handle(Request request, ServiceContext ctx, String currentUser) {
        if (!(request instanceof Request.Signup signup)) {
            return Response.error(ErrorCode.UNKNOWN_REQUEST);
        }

        if (signup.username() == null || signup.username().isBlank()
                || signup.psw() == null || signup.psw().isBlank()) {
            return Response.error(ErrorCode.INVALID_CREDENTIALS_FORMAT);
        }

        return ctx.userManager().register(signup.username(), signup.psw())
                ? Response.success("User registered successfully")
                : Response.error(ErrorCode.USERNAME_ALREADY_EXISTS);
    }
}
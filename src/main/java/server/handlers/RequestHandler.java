package server.handlers;

import server.ServiceContext;
import shared.Request;
import shared.Response;

public interface RequestHandler {
    String operation();
    Response handle(Request request, ServiceContext ctx, String currentUser);
}
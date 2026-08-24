package server.handlers;

import server.ServiceContext;
import shared.Request;
import shared.Response;

public interface RequestHandler<T extends Request> {
    Response handle(T request, ServiceContext ctx, String currentUser);
}
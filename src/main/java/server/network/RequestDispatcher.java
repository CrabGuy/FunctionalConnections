package server.network;

import shared.dto.ApiRequest;
import shared.dto.ApiResponse;
import java.net.InetSocketAddress;

public interface RequestDispatcher {
    ApiResponse<?> dispatch(ApiRequest request, InetSocketAddress remoteAddress);
}
package server.network;

import java.net.InetSocketAddress;
import shared.dto.ApiRequest;
import shared.dto.ApiResponse;

public interface RequestDispatcher {
  ApiResponse<?> dispatch(ApiRequest request, InetSocketAddress remoteAddress);
}

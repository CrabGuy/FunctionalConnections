package server.network;

import java.net.InetSocketAddress;
import shared.dto.ApiRequest;
import shared.dto.ApiResponse;

/** Dispatches API requests to the appropriate services and returns responses. */
public interface RequestDispatcher {

  /**
   * Dispatches a request and returns the response.
   *
   * @param request the request to dispatch
   * @param remoteAddress the remote address of the client
   * @return the API response
   */
  ApiResponse<?> dispatch(ApiRequest request, InetSocketAddress remoteAddress);
}

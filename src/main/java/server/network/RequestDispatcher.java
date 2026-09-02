package server.network;

import shared.dto.ApiRequest;
import shared.dto.ApiResponse;

/**
 * Component responsible for protocol dispatch.
 * Depends on: all service slices (A–D), for their public interfaces only.
 */
public interface RequestDispatcher {
    
    /**
     * Dispatches an incoming request to the appropriate service logic.
     * 
     * @param request the deserialized request to handle.
     * @return an ApiResponse containing the success status, potential errors, and payload.
     */
    ApiResponse<?> dispatch(ApiRequest request);
}
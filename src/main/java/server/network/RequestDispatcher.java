package server.network;
import shared.dto.ApiRequest;
import shared.dto.ApiResponse;
public interface RequestDispatcher {
    ApiResponse<?> dispatch(ApiRequest request);
}

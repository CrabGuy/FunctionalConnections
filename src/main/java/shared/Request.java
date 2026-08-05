package shared;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "operation"
)

public sealed interface Request {

    String operation();

    //TODO: Add parameters to handle to also include GameManager and UserManager
    Response handle();

    record Register(String operation, String username, String psw) implements Request {
        @Override
        public Response handle() {
            return new Response(true, "User " + username + " registered", null);
        }
    }

    record SubmitProposal(String operation, List<String> words) implements Request {
        @Override
        public Response handle() {
            return new Response(true, "Received " + words.size() + " words", null);
        }
    }
}
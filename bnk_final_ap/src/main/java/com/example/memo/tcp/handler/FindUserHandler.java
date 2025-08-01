package com.example.memo.tcp.handler;

@Service
@RequiredArgsConstructor
public class FindUserHandler implements TcpMessageHandler {

    private final UserService userService;
    private final ObjectMapper om;

    @Override
    public Command supports() { return Command.FIND_USER; }

    @Override
    public JsonNode handle(Message msg) {
        String userId = msg.getPayload().get("id").asText();
        UserDto dto   = userService.findById(userId);

        return om.valueToTree(dto);
    }
}
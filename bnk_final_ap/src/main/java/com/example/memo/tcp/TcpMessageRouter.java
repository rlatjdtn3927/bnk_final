package com.example.memo.tcp;

@Component
public class TcpMessageRouter {

    private final Map<Command, TcpMessageHandler> handlerMap;

    @Autowired
    public TcpMessageRouter(List<TcpMessageHandler> handlers) {
        // Command → Handler 해시맵 구성
        this.handlerMap = handlers.stream()
                                  .collect(Collectors.toMap(TcpMessageHandler::supports, h -> h));
    }

    public JsonNode route(Message msg) throws Exception {
        TcpMessageHandler h = handlerMap.get(msg.getCmd());
        if (h == null) throw new UnsupportedOperationException("지원하지 않는 Command");
        return h.handle(msg);
    }
}

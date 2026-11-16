package com.zc.cart.listener;

import com.zc.cart.service.ICartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class listenerMq {
    private final ICartService cartService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "cart.clear.queue", durable = "true"),
            exchange = @Exchange(value = "trade.topic"),
            key = "order.create"
    ))
    public void listenerDelCart(Map<String, Object> message){
        Object userIdObj = message.get("userId");
        Object itemIdsObj = message.get("itemIds");
        Long userId = ((Number) userIdObj).longValue();

        List<Long> itemIds = ((Collection<?>) itemIdsObj).stream()
                .map(id -> ((Number) id).longValue())
                .collect(Collectors.toList());

        log.info("准备清空购物车: userId={}, itemIds={}", userId, itemIds);
        int count = cartService.removeByItemIds(userId, itemIds);
    }
}

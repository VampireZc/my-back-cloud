package com.zc.trade.listener;

import com.zc.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class payListenerMq {

    private final IOrderService orderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "trade.pay.success.queue"),
            exchange = @Exchange(value = "pay.direct", type = ExchangeTypes.DIRECT),
            key = "pay.success"
    ))
    public void listenerPaySuccess(Long id){
        log.info("订单支付成功，开始更新订单状态 {}", id);
        orderService.markOrderPaySuccess(id);
    }
}

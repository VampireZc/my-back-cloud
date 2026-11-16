package com.zc.trade.listener;

import client.PayClient;
import com.zc.trade.constants.MqConstants;
import com.zc.trade.domain.po.Order;
import com.zc.trade.service.IOrderService;
import dto.PayOrderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderDelayMessageListener {

    private final RabbitListener rabbitListener;
    private final IOrderService iOrderService;
    private final PayClient payClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.DELAY_QUEUE_NAME),
            exchange = @Exchange(value = MqConstants.DELAY_EXCHANGE_NAME, delayed = "true"),
            key = MqConstants.DELAY_QUEUE_ROUTING_KEY
    ))
    public void listenerDelayMessage(Long orderId){
        // 查询订单
        Order order = iOrderService.getById(orderId);

        // 检查订单状态是否已经支付
        if (order == null || order.getStatus() != 1){
            return;
        }
        //未支付，查询支付流水状态
        PayOrderDTO payOrder = payClient.queryPayOrderByBizOrderNo(orderId);

        if (payOrder == null || payOrder.getStatus() == 3){
            // 已支付，修改订单
            iOrderService.markOrderPaySuccess(orderId);
        }else {
            // TODO 4.2.未支付，取消订单，回复库存
            // iOrderService.cancelOrder(orderId);
        }
    }
}

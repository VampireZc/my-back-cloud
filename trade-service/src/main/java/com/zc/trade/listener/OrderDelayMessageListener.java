package com.zc.trade.listener;

import client.PayClient;
import com.zc.trade.constants.MqConstants;
import com.zc.trade.domain.po.Order;
import com.zc.trade.service.IOrderService;
import dto.PayOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDelayMessageListener {

    private final IOrderService iOrderService;
    private final PayClient payClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.DELAY_QUEUE_NAME),
            exchange = @Exchange(value = MqConstants.DELAY_EXCHANGE_NAME, delayed = "true"),
            key = MqConstants.DELAY_QUEUE_ROUTING_KEY
    ))
    public void listenerDelayMessage(Long orderId){
        log.info("订单支付超时，开始关闭订单，orderId：{}", orderId);
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
            // TODO 4.2.未支付，取消订单，恢复库存
            iOrderService.cancelOrder(orderId);
            log.info("订单支付超时未支付，开始取消订单");
        }
    }
}

package com.zc.trade.service.Impl;

import client.CartClient;
import client.ItemClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.UserContext;
import com.zc.trade.constants.MqConstants;
import com.zc.trade.domain.dto.OrderFormDTO;
import com.zc.trade.domain.po.Order;
import com.zc.trade.domain.po.OrderDetail;
import com.zc.trade.mapper.OrderMapper;
import com.zc.trade.service.IOrderDetailService;
import com.zc.trade.service.IOrderService;
import dto.ItemDTO;
import dto.OrderDetailDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemClient itemService;
    private final IOrderDetailService detailService;
    private final CartClient cartService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemService.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        // 3.清理购物车商品
        // cartService.deleteCartItemByIds(itemIds);
        // 用 HashMap 而不是 Dict/KeySet：Jackson 转换器默认只信任 java.util/java.lang 下的类型
        Map<String, Object> cartClearMsg = new HashMap<>();
        cartClearMsg.put("userId", order.getUserId());
        cartClearMsg.put("itemIds", new ArrayList<>(itemIds));
        rabbitTemplate.convertAndSend("trade.topic", "order.create", cartClearMsg);

        // 4.扣减库存
        try {
            itemService.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }

        // 发送延时消息
        log.info("发送延时消息，订单超时未支付，将关闭订单 orderId={}", order.getId());
        rabbitTemplate.convertAndSend(
                MqConstants.DELAY_EXCHANGE_NAME,
                MqConstants.DELAY_QUEUE_ROUTING_KEY,
                order.getId(),
                message -> {
                    message.getMessageProperties().setDelay(5000);
                    return message;
                }

        );

        return order.getId();
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
        Order old = getById(orderId);
        // 订单不存在或者订单状态不是1，放弃处理
        if (old == null || old.getStatus() != 1){
            return;
        }
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(2);
        order.setPayTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    public void cancelOrder(Long orderId) {
        // 根据订单id查询相关的商品恢复库存
        List<OrderDetail> details = detailService.list(
                new QueryWrapper<OrderDetail>()
                        .eq("order_id", orderId)
        );

        Map<Long, Integer> collect = details.stream().collect(Collectors.toMap(
                OrderDetail::getItemId,
                OrderDetail::getNum
        ));
        Set<Long> ids = collect.keySet();
        List<ItemDTO> itemList = itemService.queryItemByIds(ids);
        for (ItemDTO item: itemList){
            // 计算需要恢复的库存
            item.setStock(item.getStock() + collect.get(item.getId()));
            itemService.updateItem(item);
        }
        // 删除这个订单
        removeById(orderId);
        detailService.remove(
                new QueryWrapper<OrderDetail>()
                        .eq("order_id", orderId)
        );

        // 清空购物车
        cartService.deleteCartItemByIds(ids);
    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}

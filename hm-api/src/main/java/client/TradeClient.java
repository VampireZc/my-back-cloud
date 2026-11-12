package client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient("trade-service")
public interface TradeClient {

    /**
     * 标记订单支付成功
     *
     * @param orderId 业务订单id
     */
    @PutMapping("/api/orders/{orderId}")
    void markOrderPaySuccess(@PathVariable("orderId") Long orderId);
}

package client;

import dto.ItemDTO;
import dto.OrderDetailDTO;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

//声明服务名称
@FeignClient("item-service")
public interface ItemClient {

    // 声明请求方式
    @GetMapping("/api/items")
    // 声明请求参数与返回值类型
    List<ItemDTO> queryItemByIds(@RequestParam("ids") Collection<Long> ids);

    @PutMapping("/api/items/stock/deduct")
    void deductStock(@RequestBody List<OrderDetailDTO> items);
}
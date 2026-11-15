package fallback;

import client.ItemClient;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.CollUtils;
import dto.ItemDTO;
import dto.OrderDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.List;

@Slf4j
public class ItemClientFallback implements FallbackFactory<ItemClient> {
    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient() {
            @Override
            public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
                // 查询购物车允许失败，查询失败，返回空集合
                log.info("查询商品列表失败，原因：{}", cause.getMessage());
                return CollUtils.emptyList();
            }

            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                // 库存扣减业务需要触发事务回滚，查询失败，抛出异常
                log.info("库存扣减失败，原因：{}", cause.getMessage());
                throw new BizIllegalException(cause);
            }
        };
    }
}

package com.zc.search.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.domain.PageDTO;
import com.zc.item.domain.po.Item;
import com.zc.item.domain.query.ItemPageQuery;
import com.zc.item.service.IItemService;
import com.zc.search.config.clientConfig;
import dto.ItemDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final IItemService itemService;
    private final RestHighLevelClient client;

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) throws IOException {
        // 分页查询
//        Page<Item> result = itemService.lambdaQuery()
//                .like(StrUtil.isNotBlank(query.getKey()), Item::getName, query.getKey())
//                .eq(StrUtil.isNotBlank(query.getBrand()), Item::getBrand, query.getBrand())
//                .eq(StrUtil.isNotBlank(query.getCategory()), Item::getCategory, query.getCategory())
//                .eq(Item::getStatus, 1)
//                .between(query.getMaxPrice() != null, Item::getPrice, query.getMinPrice(), query.getMaxPrice())
//                .page(query.toMpPage("update_time", false));
        // 封装并返回
        int pageNo = query.getPageNo();
        int pageSize = query.getPageSize();
        SearchRequest request = new SearchRequest("items");

        // 用 bool 组合各条件：有值才加入对应子句，全空时等价于 match_all
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        // 关键字：影响相关性打分，放 must
        if (StrUtil.isNotBlank(query.getKey())) {
            bool.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        // 分类、品牌：精确过滤，不参与打分，放 filter
        if (StrUtil.isNotBlank(query.getCategory())) {
            bool.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        if (StrUtil.isNotBlank(query.getBrand())) {
            bool.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        // 价格区间：min/max 各自可能为空，rangeQuery.gte/lte 传 null 会报错，需分别判空
        if (query.getMinPrice() != null || query.getMaxPrice() != null) {
            RangeQueryBuilder priceRange = QueryBuilders.rangeQuery("price");
            if (query.getMinPrice() != null) {
                priceRange.gte(query.getMinPrice());
            }
            if (query.getMaxPrice() != null) {
                priceRange.lte(query.getMaxPrice());
            }
            bool.filter(priceRange);
        }
        request.source().query(bool);
        // 默认按更新时间倒序，无关键字时保证结果稳定
        request.source().sort("updateTime", SortOrder.DESC);
        request.source().from((pageNo - 1) * pageSize).size(pageSize);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        return clientConfig.handleResponse(response, ItemDTO.class, pageNo, pageSize);
    }
}

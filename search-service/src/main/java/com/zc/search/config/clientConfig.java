package com.zc.search.config;

import cn.hutool.json.JSONUtil;
import com.hmall.common.domain.PageDTO;
import com.zc.item.domain.dto.ItemDoc;
import lombok.Data;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "es")
@Component
@Data
public class clientConfig {
    private String esHost;
    private Integer esPort;
    private String esIndex;

    @Bean
    public RestHighLevelClient restHighLevelClient() {
       return new RestHighLevelClient(RestClient.builder(
                HttpHost.create(esHost + ":" + esPort)
        ));
    }

    public static <T> PageDTO<T> handleResponse(SearchResponse response, Class<T> clazz, int pageNo, int pageSize) {
        SearchHits searchHits = response.getHits();
        // 1.获取总条数
        long total = searchHits.getTotalHits().value;
        long pages = (total + pageSize - 1) / pageSize;

        System.out.println("共搜索到" + total + "条数据");
        // 2.遍历结果数组
        List<T> beanList = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            // 3.得到_source，也就是原始json文档
            String source = hit.getSourceAsString();
            // 4.反序列化并打印
            beanList.add(JSONUtil.toBean(source, clazz));

        }
        return new PageDTO<>(total, pages, beanList);
    }


}

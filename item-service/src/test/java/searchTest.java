import cn.hutool.json.JSONUtil;
import com.zc.item.domain.dto.ItemDoc;
import org.apache.http.HttpHost;
import org.aspectj.lang.annotation.Before;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
public class searchTest {

    // request.source() 包含排序、分页、查询
    // QueryBuilders 查询构建器

    private RestHighLevelClient client;

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.64.4:9200")
        ));
    }

    // 查询全部数据
    @Test
    void testMatchAll() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().query(
                QueryBuilders.matchAllQuery()
        );
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    // 叶子查询
    @Test
    void testMultiMatch() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().query(
                QueryBuilders.multiMatchQuery("蒙牛牛奶", "name", "category")
        );
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    // 范围查询
    @Test
    void testRange() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().query(
                QueryBuilders.rangeQuery("price").from(10).to(100)
        );
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    // term查询
    @Test
    void testTerm() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().query(
                QueryBuilders.termQuery("category", "牛奶")
        );
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    // 复合查询
    @Test
    void testBool() throws IOException {
        SearchRequest request = new SearchRequest("items");
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(
                QueryBuilders.termQuery("category", "牛奶")
        );
        boolQuery.filter(
                QueryBuilders.rangeQuery("price").from(1000).to(10000)
        );
        boolQuery.mustNot(
                QueryBuilders.termQuery("name", "蒙牛")
        );
        request.source().query(boolQuery);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    // 排序和分页
    @Test
    void testSortAndPage() throws IOException {
        int pageNo = 1;
        int pageSize = 100;
        SearchRequest request = new SearchRequest("items");
        request.source().query(
                QueryBuilders.matchQuery("name", "牛奶")
        );
        request.source().sort("price", SortOrder.ASC);
        request.source().from((pageNo - 1) * pageSize).size(pageSize);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    // 聚合查询
    @Test
    void testAggregation() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().aggregation(
                AggregationBuilders
                .terms("brand_agg") // 聚合名称
                .field("category") // 聚合字段
                .size(10) // 聚合结果数量
        );
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 5.解析聚合结果
        Aggregations aggregations = response.getAggregations();
        // 5.1.获取品牌聚合
        Terms brandTerms = aggregations.get("brand_agg");
        // 5.2.获取聚合中的桶
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        // 5.3.遍历桶内数据
        for (Terms.Bucket bucket : buckets) {
            // 5.4.获取桶内key
            String brand = bucket.getKeyAsString();
            System.out.print("brand = " + brand);
            long count = bucket.getDocCount();
            System.out.println("; count = " + count);
        }
    }


    private void handleResponse(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        // 1.获取总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("共搜索到" + total + "条数据");
        // 2.遍历结果数组
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            // 3.得到_source，也就是原始json文档
            String source = hit.getSourceAsString();
            // 4.反序列化并打印
            ItemDoc item = JSONUtil.toBean(source, ItemDoc.class);
            System.out.println(item);
        }
    }
}

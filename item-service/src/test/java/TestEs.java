import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.zc.item.domain.dto.ItemDoc;
import com.zc.item.domain.po.Item;
import com.zc.item.itemApplication;
import com.zc.item.service.impl.ItemServiceImpl;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest(classes = itemApplication.class)
@Slf4j
public class TestEs {

    private RestHighLevelClient client;

    @Autowired
    private ItemServiceImpl itemService;

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.64.4:9200")
        ));
    }
    @Test
    void TestConnection() {
        System.out.println("Connection successful" + client);
    }

    // 测试创建索引
    @Test
    void testCreatIndex() throws IOException {
        // 创建request对象
        CreateIndexRequest request = new CreateIndexRequest("items");

        // 设置映射
        request.source(MAPPING_TEMPLATE, XContentType.JSON);

        // 发送请求
        client.indices().create(request, RequestOptions.DEFAULT);

    }

    // 测试新增文档
    @Test
    void testInsertDocument() throws IOException {

        Item item = itemService.getById(584382L);
        ItemDoc itemDoc = BeanUtils.copyBean(item, ItemDoc.class);
        String jsonStr = JSONUtil.toJsonStr(itemDoc);

        // 创建request对象
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        // 准备json文件
        request.source(jsonStr, XContentType.JSON);
        // 发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    // 测试获取文档
    @Test
    void getDocument() throws IOException {
        // 创建request对象
        GetRequest items = new GetRequest("items", "584382");
        // 发送请求
        GetResponse response = client.get(items, RequestOptions.DEFAULT);
        String string = response.getSourceAsString();
        System.out.println(string);
    }

    // 删除文档
    @Test
    void deleteDocument() throws IOException {
        // 创建request对象
        DeleteRequest request = new DeleteRequest("items", "584382");
        // 发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }

    // 把数据库中的商品数据导入到es中
    @Test
    void BatchImport() throws IOException {
        log.info("开始导入数据");
        // 获取数据
        int pageNo = 1;
        int pageSize = 2000;

        while (true){
            // 获取数据（批量导入不需要总数，关闭 count 查询）
            Page<Item> page = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(new Page<>(pageNo, pageSize, false));

            // 没数据了，结束循环
            List<Item> list = page.getRecords();
            if (CollUtils.isEmpty(list)) break;
            log.info("加载第{}页数据，共{}条", pageNo, list.size());

            // 每页创建一个 BulkRequest，分页批量提交
            BulkRequest request = new BulkRequest();
            // 构建数据
            for(Item item : list){
                ItemDoc itemDoc = BeanUtils.copyBean(item, ItemDoc.class);
                String jsonStr = JSONUtil.toJsonStr(itemDoc);
                // 每条文档必须新建 IndexRequest，不能复用同一个对象
                request.add(
                        new IndexRequest("items").id(itemDoc.getId()).source(jsonStr, XContentType.JSON)
                );
            }
            // 发送请求
            client.bulk(request, RequestOptions.DEFAULT);
            pageNo++;
        }


    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    static final String MAPPING_TEMPLATE = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"name\":{\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_max_word\"\n" +
            "      },\n" +
            "      \"price\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"stock\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"image\":{\n" +
            "        \"type\": \"keyword\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"category\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"sold\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"commentCount\":{\n" +
            "        \"type\": \"integer\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"isAD\":{\n" +
            "        \"type\": \"boolean\"\n" +
            "      },\n" +
            "      \"updateTime\":{\n" +
            "        \"type\": \"date\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";


}

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.Properties;

public class NacosProbe {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put("serverAddr", "192.168.64.4:8848");
        props.put("namingLoadCacheAtStart", "false");
        long t0 = System.currentTimeMillis();
        NamingService naming = NacosFactory.createNamingService(props);
        System.out.println("namingService created in " + (System.currentTimeMillis() - t0) + "ms");

        Instance instance = new Instance();
        instance.setIp("192.168.31.202");
        instance.setPort(19999);
        instance.setServiceName("probe-service");
        t0 = System.currentTimeMillis();
        naming.registerInstance("probe-service", "DEFAULT_GROUP", instance);
        System.out.println("REGISTER OK in " + (System.currentTimeMillis() - t0) + "ms");
        System.out.println("instances: " + naming.getAllInstances("probe-service"));
        naming.deregisterInstance("probe-service", "DEFAULT_GROUP", instance);
        System.out.println("DEREGISTER OK");
        naming.shutDown();
        System.exit(0);
    }
}

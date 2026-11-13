## nacos 配置流程
1. 在docker上配置nacos并部署，nacos相当于通讯录，用于服务注册与发现。
2. 浏览器访问nacos后台可以直接看到服务列表
3. 项目里面引入nacos-discovery与loadbalancer进行服务注册与负载均衡算法
4. 在配置文件中配置nacos的地址端口和各种详细配置
5. 在启动类上加上注解@EnableDiscoveryClient
6. 启动项目会自动把地址和端口注册到nacos后台，服务端和nacos通过长连接一直通信状态，假设接收不到服务端心跳，就会标记这个服务器状态为不健康，nacos会进行服务下线操作。
7. 当服务端需要调用其他服务时，会通过nacos获取服务列表，然后进行负载均衡，最后调用。
8. 使用RestTemplate可以原始方法调用，也可以使用OpenFeign快速调用，推荐使用@FeignClien注解可以直接获取服务端列表。

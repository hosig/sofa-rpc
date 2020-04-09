[TOC]

## 背景：

重要笔记存于:

```
K:\OneDrive-Seik2013\kb\phpAndHtml\java-sofarpc-QA.docx
```

但所有细节，不宜放入其中，特写此文。

## QA集

### sofa基本

#### Q: 最基本的compile

##### A：compile过程如下：

1. intellij中专门做了一个K:\g\Hg\java\sofa\sofa-rpc 的module，里面的pom是最根部的pom。每次拿到新代码，先执行此pom的*install* lifecycle。

2. 修改某个subModule的代码后，在该subModule的pom中，执行 该pom的*compile* lifecycle。

执行方法：

```
（执行http2的server端的sample）：在intellij中对 K:\g\Hg\java\sofa\sofa-rpc\example\src\test\java\com\alipay\sofa\rpc\http2\Http2ServerMain.java 中执行其main

（执行http2的client端的sample）：同上，针对同目录下的Http2ClientMain.java 执行其main。
```

这样，就实现了 基本的 web Server的搭建 和调用了。

#### Q：K:\g\Hg\java\sofa\sofa-rpc\example\pom.xml 的下处 Line4，intellij报错：**Properties in parent definition are prohibited**。

```xml
    <parent>
        <groupId>com.alipay.sofa</groupId>
        <artifactId>sofa-rpc-parent</artifactId>
        <version>${revision}</version>
    </parent>

```

A: [sof](https://stackoverflow.com/questions/45598007/properties-in-parent-definition-are-prohibited-in-the-intellij-maven-on-my-mac-o/50448898) 说maven 版本太低（>=3.5.2 即可）。但intellij的bundled 就是那个低版本。所以只好按sof介绍，装了maven3.6.3, 然后invalid cache and restart intellij, 

但，然后试图 reimport pom, intellij 说 ``` Unable to import maven project: See logs for details ```, 然后在“**Help ==》 Show Log in Explorer**" 查看具体的报错日志.  一团雾水，网上查得  idea2019.1 与高版本的 maven （apache-maven-3.6.2 ）不太兼容，我用的是 idea2017.3，所以估计得用很早期的maven 版本

於是下载了maven3.5.2（发现**不用在os中设置Path**，intellij也能使用该maven）。 尽管如此，该intellij报错还是存在，但是pom的re-compile之后, 原本无法执行的“K:\g\Hg\java\sofa\sofa-rpc\example\src\test\java\com\alipay\sofa\rpc\http2\Http2ServerMain.java”，终于可以执行了。

##### 结论：maven版本低。但高版本的maven和低版本的intellij不兼容。当前的idea2017.3 可以使用 maven3.5.2来改善此问题。

### 关于Http2的例子（test\java\com\alipay\sofa\rpc\http2\Http2ServerMain.java）

#### Q: Http2ServerMain.java中，没看到HelloService和ProtoService对应的provider做了bind。内部是如何做到的呢？

##### A:  关联了同一个application （Line17，Line25）, 关联了同一个serverConfig（Line19，Line27），

```java
public class Http2ServerMain {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Http2ServerMain.class);

    public static void main(String[] args) {
        ApplicationConfig application = new ApplicationConfig().setAppName("test-server");

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("h2c")
            .setPort(12300)
            .setDaemon(false);

        ProviderConfig<ProtoService> providerConfig = new ProviderConfig<ProtoService>()
            .setInterfaceId(ProtoService.class.getName())
            .setApplication(application)
            .setRef(new ProtoServiceImpl())
            .setServer(serverConfig);

        providerConfig.export();
        // 就算不执行下面两句，Http2ClientMain也可正常调用，执行的是 ProtoServiceImpl.echoObj() 方法。--为啥执行的是echoObj()方法呢？
        ProviderConfig<HelloService> providerConfig2 = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setApplication(application)
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig)
            .setRegister(false);
        providerConfig2.export();
        // 访问“http://127.0.0.1:12300/com.alipay.sofa.rpc.test.HelloService/sayHello?name=Bob&age=2”才行(前提：不能屏蔽providerConfig2的代码)
        

```

#### Q: 可不可以只关联HelloService，而不关联ProtoService呢？ 

##### A： 不能。但可以仅关联ProtoService，而不关联HelloService。

#### Q：Line28的ProviderConfig的 .setRegister(false) 是啥作用呢?

##### A：

#### Q:为何http2不能被实现（不管用chrome还是firefox访问url，server端出现下行Warning）?

```perl
# 访问下url，尽管可以得到正确resp的Text，但Server端提示并没有使用http2，而是降级成了http1.1. 
#     chrome的插件“HTTP/2 and SPDY indicator” 也提示没有使用http2
# url： http://127.0.0.1:12300/com.alipay.sofa.rpc.test.HelloService/sayHello?name=Bob&age=2
2020-04-08 10:49:39,386 SOFA-SEV-H2C-BIZ-12300-4-T1  WARN [com.alipay.sofa.rpc.transport.http.Http2ServerChannelInitializer:warn:142] - Directly talking: HTTP/1.1 (no upgrade was attempted) from 127.0.0.1:50226
```

##### A: 尽管http2是可以通过非tls方式调用的，但 普通的browser（chrome/ff之类）仅支持https下使用http2. 

##### 既然一般不需使用 非tls方式的http2，就不去深究了。--> 反正 可以通过 \sofa\rpc\http2\Http2ClientMain.java 如下访问

```java
    public static void main(String[] args) {
        ApplicationConfig application = new ApplicationConfig().setAppName("test-client");

        ConsumerConfig<ProtoService> consumerConfig = new ConsumerConfig<ProtoService>()
            .setApplication(application)
            .setInterfaceId(ProtoService.class.getName())
            .setProtocol("h2c")
            .setDirectUrl("h2c://127.0.0.1:12300")
            .setSerialization("protobuf")
            .setRegister(false)
            .setTimeout(1000);
        ProtoService helloService = consumerConfig.refer();

        LOGGER.info("started at pid {}", RpcRuntimeContext.PID);

        while (true) {
            try {
                EchoRequest request = EchoRequest.newBuilder().setGroup(Group.A).setName("xxx").build();
                EchoResponse s = helloService.echoObj(request);
                LOGGER.info("【P】{}", s);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
        }
    }
```

###### Q：那如何做 tls版的http2的Server端？


/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.http2.sk;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.protobuf.ProtoService;
import com.alipay.sofa.rpc.protobuf.ProtoServiceImpl;
import com.alipay.sofa.rpc.server.http.Http2ClearTextServer;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * 【C端】 Http2ClientWithAndNoProtobufMain 【S端】  Http2ServerWithAndNoProtobufMain
 */
public class Http2ServerWithAndNoProtobufMain {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Http2ServerWithAndNoProtobufMain.class);

    public static void main(String[] args) {
        ApplicationConfig application = new ApplicationConfig().setAppName("test-server");

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("h2c")
            .setPort(12300)
            .setDaemon(false);

        // 下面是ProtoBuf服务（C端req过来的话，执行 ProtoServiceImpl.echoObj() 方法。）
        //  Q： 为啥执行的是echoObj()方法呢？
        //  A： ProtoService.proto 中 写了 “service ProtoService {rpc echoObj (EchoRequest) returns (EchoResponse) {}}”
        ProviderConfig<ProtoService> providerConfig = new ProviderConfig<ProtoService>()
            .setInterfaceId(ProtoService.class.getName())
            .setApplication(application)
            .setRef(new ProtoServiceImpl())
            .setServer(serverConfig);

        providerConfig.export();

        // 下面是非ProtoBuf服务
        // 注：chrome中 访问“http://127.0.0.1:12300/com.alipay.sofa.rpc.test.HelloService/sayHello?name=Bob&age=2” 也可得到结果但不是h2c协议（而是被降级到了http1.1协议）
        ProviderConfig<HelloService> providerConfig2 = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setApplication(application)
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig)
            .setRegister(false);
        providerConfig2.export();

        LOGGER.info("started at pid {}", RpcRuntimeContext.PID);

        // 下面这块代码 只是为了每隔1s，print tps和queue数而已。
        final AtomicInteger cnt = ((ProtoServiceImpl) providerConfig.getRef()).getCounter();
        final ThreadPoolExecutor executor = ((Http2ClearTextServer) serverConfig.getServer()).getBizThreadPool();
        Thread thread = new Thread(new Runnable() {
            private long last = 0;

            @Override
            public void run() {
                while (true) {
                    long count = cnt.get();
                    long tps = count - last;
                    LOGGER.info("【B】last 1s invoke: {}, queue: {}", tps, executor.getQueue().size());
                    last = count;

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }, "Print-tps-THREAD");
        thread.start();
    }

}

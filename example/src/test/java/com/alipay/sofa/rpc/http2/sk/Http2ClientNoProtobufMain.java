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
package com.alipay.sofa.rpc.http2;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.protobuf.EchoRequest;
import com.alipay.sofa.rpc.protobuf.EchoResponse;
import com.alipay.sofa.rpc.protobuf.Group;
import com.alipay.sofa.rpc.protobuf.ProtoService;
import com.alipay.sofa.rpc.test.HelloService;

/**
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * 【C端】 Http2ClientNoProtobufMain 【S端】  Http2ServerNoProtobufMain
 */
public class Http2ClientNoProtobufMain {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Http2ClientNoProtobufMain.class);

    public static void main(String[] args) {
        ApplicationConfig application = new ApplicationConfig().setAppName("test-client");

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setApplication(application)
            .setInterfaceId(HelloService.class.getName())
            .setProtocol("h2c")
            .setDirectUrl("h2c://127.0.0.1:12300")
            //            .setSerialization("protobuf")  // 因为server端不是protobuf协议的serialization 所以不能加这句
            .setRegister(false)
            .setTimeout(1000); // 设成1s的话，首次访问可能会timeout（因为S端有很多是lazyStart的处理），这时间设成3s的话，就不会失败了。但反正之后会正常，所以不必在意首次访问失败。
        HelloService helloService = consumerConfig.refer();

        LOGGER.info("started at pid {}", RpcRuntimeContext.PID);

        while (true) {
            try {
                String s = helloService.sayHello("Bob", 21);
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

}

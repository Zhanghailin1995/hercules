一个rpc通信框架
---
该框架来源于sofajraft，剥离了其中的rpc通信模块

上手指南，参考RpcRequestsTest

### 跨语言调用
由于引入了grpc，所以该框架支持跨语言调用

要开启跨语言调用需要

- 使用grpc作为底层通信框架

    开启RpcFactory SPI项目，在classpath下新增 META-INF/services/org.hercules.RpcFactory 并且写入
    
    `org.hercules.impl.grpc.GrpcRpcFactory`
    
- 启用protobuf descriptor name作为服务调用方法名
    ```System.setProperty("hercules.rpc.rpc_processor_interest_prefer_proto_name", "true");```
    
- 客户端在注册自定义serialize时候需要修改className 参数为服务方法的名称，通常为 `package.serviceName/methodName` 
    ```
    rpcFactory.registerProtobufSerializer(
                           Utils.generateGrpcServiceName(Greeter.GreeterRequest.getDescriptor().getFullName()), // full method name greeter.GreeterRequestService/_call
                           Greeter.GreeterRequest.getDefaultInstance(), // request instance
                           Greeter.GreeterReply.getDefaultInstance()); // response instance
    ```
- 服务端在编写RpcProcessor时修改interest名称为
    ```$xslt
    @Override
    public String interest() {
        if (RpcFactoryHelper.rpcFactory().factoryType().equals(RpcFactoryHelper.RpcFactoryType.GRPC)
                && Utils.isRpcProcessorInterestPreferProtoName()) {
            return Utils.generateGrpcServiceName(Greeter.GreeterRequest.getDescriptor().getFullName());
        }
        return Greeter.GreeterRequest.class.getName();
    } 
    ```

    
具体可见hercules-example/greeter 例子    
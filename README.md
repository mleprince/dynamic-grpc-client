# dynamic-grpc-client
Call a gRPC server using only protobuf description file

## How to use it 

### Build

```java
        String hostName = "localhost";
        int port = 9090;
        Path protoFilePath = Path.of("service.proto");

        
        // build a client without authentication mechanism (`plainText` mode)

        DynamicGrpcClient dynamicGrpcClient = DynamicGrpcClient.build(protoFilePath, hostName, port);

        // build a client with custom grpc channel

        io.grpc.Channel customChannel = ManagedChannelBuilder.forAddress(hostName, port).build();

        dynamicGrpcClient = DynamicGrpcClient.build(protoFilePath, customChannel);
        
```
### Blocking call 
```java
        String fullyQualifiedMethodName = "com.tahitiste.grpc.TestService.TestMethod";
        String requestBodyInJson = "{\"property\" : 345}";
     
        Either<DynamicMessage, Iterator<DynamicMessage>> response = dynamicGrpcClient.blockingCall(
                fullyQualifiedMethodName,
                requestBodyInJson
        );
        
        // if call is unary call
        Optional<DynamicMessage> unaryResponse = response.getLeft();
        
        // if call is server-streaming call 
        Optional<Iterator<DynamicMessage>> serverStreamResponse = response.getRight();       
```
### Async call 
```java
        String fullyQualifiedMethodName = "com.tahitiste.grpc.TestService.TestMethod";
        String requestBodyInJson = "{\"property\" : 345}";
        
        dynamicGrpcClient.asyncCall(fullyQualifiedMethodName, requestBodyInJson, new StreamObserver<>() {
            @Override
            public void onNext(DynamicMessage value) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        });
```


package com.tahitiste.grpc;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.tahitiste.grpc.exception.InvalidJsonProtobufException;
import com.tahitiste.grpc.exception.InvalidMethodTypeException;
import com.tahitiste.grpc.exception.MethodNotFoundInProtoFileException;
import io.grpc.*;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

public class DynamicGrpcClient {

    private final Descriptors.FileDescriptor fileDescriptor;
    private final Channel channel;

    public DynamicGrpcClient(Descriptors.FileDescriptor fileDescriptor, Channel channel) {
        this.fileDescriptor = fileDescriptor;
        this.channel = channel;
    }

    /**
     * Build a client without authentication mechanism (`plainText` mode)
     * @param protoFilePath : *.proto file to parse
     * @param hostname : hostName of the gRPC server
     * @param port : port of the gRPC server
     * @return DynamicGrpcClient
     */
    public static DynamicGrpcClient build(Path protoFilePath, String hostname, int port) {

        Channel channel = ManagedChannelBuilder
                .forAddress(hostname, port)
                .usePlaintext()
                .build();

        return new DynamicGrpcClient(ProtoFileParser.parse(protoFilePath), channel);
    }

    /**
     * Build a client with custom gRPC Channel
     * @param protoFilePath : *.proto file to parse
     * @param channel : gRPC channel
     * @return DynamicGrpcClient
     */
    public static DynamicGrpcClient build(Path protoFilePath, Channel channel) {
        return new DynamicGrpcClient(ProtoFileParser.parse(protoFilePath), channel);
    }

    /**
     * Request an async call
     * @param fullyQualifiedMethodName : name of the method with package name . ex : com.bioserenity.TestService.MyMethod
     * @param requestBodyInJson : body of the request message in json. Can be null : message with default values will be sent
     * @param streamObserver : callback used to listen the response from the server
     * @throws MethodNotFoundInProtoFileException : method was not found in proto file
     * @throws InvalidJsonProtobufException : the proto request message cannot be built with provided json
     */
    public void asyncCall(String fullyQualifiedMethodName, String requestBodyInJson, StreamObserver<DynamicMessage> streamObserver) {
        findMethod(fullyQualifiedMethodName)
                .ifPresentOrElse(protobufMethodDescriptor -> {

                            ClientCall<DynamicMessage, DynamicMessage> clientCall = channel.newCall(getGrpcMethodDescriptor(protobufMethodDescriptor), CallOptions.DEFAULT);

                            DynamicMessage requestMessage = buildRequestMessage(requestBodyInJson, protobufMethodDescriptor.getInputType());

                            if (protobufMethodDescriptor.isClientStreaming()) {
                                throw new InvalidMethodTypeException(fullyQualifiedMethodName, "Client streaming call is not supported");
                            } else if (protobufMethodDescriptor.isServerStreaming()) {
                                ClientCalls.asyncClientStreamingCall(clientCall, streamObserver);
                            } else {
                                ClientCalls.asyncUnaryCall(clientCall, requestMessage, streamObserver);
                            }
                        },
                        () -> {
                            throw new MethodNotFoundInProtoFileException(fullyQualifiedMethodName + "was not found in proto file");
                        }
                );
    }


    /**
     * Request a blocking call
     * @param fullyQualifiedMethodName : name of the method with package name . ex : com.bioserenity.TestService.MyMethod
     * @param jsonBody : body of the request message in json. Can be null : message with default values will be sent
     * @return either a response containg one message if the call is unary or a response contains an Iterator if the call is a server-streaming call
     * @throws MethodNotFoundInProtoFileException : method was not found in proto file
     * @throws InvalidJsonProtobufException : the proto request message cannot be built with provided json
     */
    public Either<DynamicMessage, Iterator<DynamicMessage>> blockingCall(String fullyQualifiedMethodName, String jsonBody) {
        return findMethod(fullyQualifiedMethodName)
                .map(protobufMethodDescriptor -> {

                    ClientCall<DynamicMessage, DynamicMessage> clientCall = channel.newCall(getGrpcMethodDescriptor(protobufMethodDescriptor), CallOptions.DEFAULT);

                    DynamicMessage requestMessage = buildRequestMessage(jsonBody, protobufMethodDescriptor.getInputType());

                    if (protobufMethodDescriptor.isClientStreaming()) {
                        throw new InvalidMethodTypeException(fullyQualifiedMethodName, "Client streaming call is not supported");
                    } else if (protobufMethodDescriptor.isServerStreaming()) {
                        return Either.<DynamicMessage, Iterator<DynamicMessage>>right(ClientCalls.blockingServerStreamingCall(clientCall, requestMessage));
                    } else {
                        return Either.<DynamicMessage, Iterator<DynamicMessage>>left(ClientCalls.blockingUnaryCall(clientCall, requestMessage));
                    }
                }).orElseThrow(() -> new MethodNotFoundInProtoFileException(fullyQualifiedMethodName + "was not found in proto file"));
    }

    private DynamicMessage buildRequestMessage(String jsonBody, Descriptors.Descriptor requestDescriptor) {
        DynamicMessage.Builder messageBuilder = DynamicMessage.getDefaultInstance(requestDescriptor).toBuilder();

        if (jsonBody != null) {
            try {
                JsonFormat.parser().merge(jsonBody, messageBuilder);
            } catch (InvalidProtocolBufferException e) {
                throw new InvalidJsonProtobufException(e);
            }
        }

        return messageBuilder.build();
    }


    private Optional<Descriptors.MethodDescriptor> findMethod(String fullyQualifiedMethodName) {
        return fileDescriptor.getServices().stream()
                .flatMap(serviceDescriptor -> serviceDescriptor.getMethods().stream())
                .filter(methodDescriptor -> methodDescriptor.getFullName().equals(fullyQualifiedMethodName))
                .findAny();
    }

    private MethodDescriptor<DynamicMessage, DynamicMessage> getGrpcMethodDescriptor(Descriptors.MethodDescriptor methodDescriptor) {
        return MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                .setType(getMethodType(methodDescriptor))
                .setFullMethodName(MethodDescriptor.generateFullMethodName(methodDescriptor.getService().getFullName(), methodDescriptor.getName()))
                .setRequestMarshaller(ProtoUtils.marshaller(DynamicMessage.newBuilder(methodDescriptor.getInputType()).buildPartial()))
                .setResponseMarshaller(ProtoUtils.marshaller(DynamicMessage.newBuilder(methodDescriptor.getOutputType()).buildPartial()))
                .build();
    }

    private MethodDescriptor.MethodType getMethodType(Descriptors.MethodDescriptor methodDescriptor) {

        // UNKNOWN is fine, but the "correct" value can be computed from
        // methodDesc.toProto().getClientStreaming()/getServerStreaming()
        DescriptorProtos.MethodDescriptorProto methodDescriptorProto = methodDescriptor.toProto();

        if (methodDescriptorProto.getClientStreaming() && methodDescriptorProto.getServerStreaming()) {
            return MethodDescriptor.MethodType.BIDI_STREAMING;
        } else if (methodDescriptorProto.getClientStreaming()) {
            return MethodDescriptor.MethodType.CLIENT_STREAMING;
        } else if (methodDescriptorProto.getServerStreaming()) {
            return MethodDescriptor.MethodType.SERVER_STREAMING;
        } else {
            return MethodDescriptor.MethodType.UNARY;
        }
    }


}



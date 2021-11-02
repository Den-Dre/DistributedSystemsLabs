package org.example.AlmaOnline.server;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.38.0)",
    comments = "Source: AlmaOnline.proto")
public final class AlmaOnlineGrpc {

  private AlmaOnlineGrpc() {}

  public static final String SERVICE_NAME = "almaonline.AlmaOnline";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.example.AlmaOnline.server.RestaurantsRequest,
      org.example.AlmaOnline.server.RestaurantsReply> getGetRestaurantsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getRestaurants",
      requestType = org.example.AlmaOnline.server.RestaurantsRequest.class,
      responseType = org.example.AlmaOnline.server.RestaurantsReply.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.example.AlmaOnline.server.RestaurantsRequest,
      org.example.AlmaOnline.server.RestaurantsReply> getGetRestaurantsMethod() {
    io.grpc.MethodDescriptor<org.example.AlmaOnline.server.RestaurantsRequest, org.example.AlmaOnline.server.RestaurantsReply> getGetRestaurantsMethod;
    if ((getGetRestaurantsMethod = AlmaOnlineGrpc.getGetRestaurantsMethod) == null) {
      synchronized (AlmaOnlineGrpc.class) {
        if ((getGetRestaurantsMethod = AlmaOnlineGrpc.getGetRestaurantsMethod) == null) {
          AlmaOnlineGrpc.getGetRestaurantsMethod = getGetRestaurantsMethod =
              io.grpc.MethodDescriptor.<org.example.AlmaOnline.server.RestaurantsRequest, org.example.AlmaOnline.server.RestaurantsReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getRestaurants"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.example.AlmaOnline.server.RestaurantsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.example.AlmaOnline.server.RestaurantsReply.getDefaultInstance()))
              .setSchemaDescriptor(new AlmaOnlineMethodDescriptorSupplier("getRestaurants"))
              .build();
        }
      }
    }
    return getGetRestaurantsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.example.AlmaOnline.server.MenuRequest,
      org.example.AlmaOnline.server.MenuReply> getGetMenuMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getMenu",
      requestType = org.example.AlmaOnline.server.MenuRequest.class,
      responseType = org.example.AlmaOnline.server.MenuReply.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.example.AlmaOnline.server.MenuRequest,
      org.example.AlmaOnline.server.MenuReply> getGetMenuMethod() {
    io.grpc.MethodDescriptor<org.example.AlmaOnline.server.MenuRequest, org.example.AlmaOnline.server.MenuReply> getGetMenuMethod;
    if ((getGetMenuMethod = AlmaOnlineGrpc.getGetMenuMethod) == null) {
      synchronized (AlmaOnlineGrpc.class) {
        if ((getGetMenuMethod = AlmaOnlineGrpc.getGetMenuMethod) == null) {
          AlmaOnlineGrpc.getGetMenuMethod = getGetMenuMethod =
              io.grpc.MethodDescriptor.<org.example.AlmaOnline.server.MenuRequest, org.example.AlmaOnline.server.MenuReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getMenu"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.example.AlmaOnline.server.MenuRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.example.AlmaOnline.server.MenuReply.getDefaultInstance()))
              .setSchemaDescriptor(new AlmaOnlineMethodDescriptorSupplier("getMenu"))
              .build();
        }
      }
    }
    return getGetMenuMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AlmaOnlineStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AlmaOnlineStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AlmaOnlineStub>() {
        @java.lang.Override
        public AlmaOnlineStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AlmaOnlineStub(channel, callOptions);
        }
      };
    return AlmaOnlineStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AlmaOnlineBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AlmaOnlineBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AlmaOnlineBlockingStub>() {
        @java.lang.Override
        public AlmaOnlineBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AlmaOnlineBlockingStub(channel, callOptions);
        }
      };
    return AlmaOnlineBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AlmaOnlineFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AlmaOnlineFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AlmaOnlineFutureStub>() {
        @java.lang.Override
        public AlmaOnlineFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AlmaOnlineFutureStub(channel, callOptions);
        }
      };
    return AlmaOnlineFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class AlmaOnlineImplBase implements io.grpc.BindableService {

    /**
     */
    public void getRestaurants(org.example.AlmaOnline.server.RestaurantsRequest request,
        io.grpc.stub.StreamObserver<org.example.AlmaOnline.server.RestaurantsReply> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetRestaurantsMethod(), responseObserver);
    }

    /**
     */
    public void getMenu(org.example.AlmaOnline.server.MenuRequest request,
        io.grpc.stub.StreamObserver<org.example.AlmaOnline.server.MenuReply> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMenuMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetRestaurantsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.example.AlmaOnline.server.RestaurantsRequest,
                org.example.AlmaOnline.server.RestaurantsReply>(
                  this, METHODID_GET_RESTAURANTS)))
          .addMethod(
            getGetMenuMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.example.AlmaOnline.server.MenuRequest,
                org.example.AlmaOnline.server.MenuReply>(
                  this, METHODID_GET_MENU)))
          .build();
    }
  }

  /**
   */
  public static final class AlmaOnlineStub extends io.grpc.stub.AbstractAsyncStub<AlmaOnlineStub> {
    private AlmaOnlineStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AlmaOnlineStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AlmaOnlineStub(channel, callOptions);
    }

    /**
     */
    public void getRestaurants(org.example.AlmaOnline.server.RestaurantsRequest request,
        io.grpc.stub.StreamObserver<org.example.AlmaOnline.server.RestaurantsReply> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetRestaurantsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getMenu(org.example.AlmaOnline.server.MenuRequest request,
        io.grpc.stub.StreamObserver<org.example.AlmaOnline.server.MenuReply> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMenuMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class AlmaOnlineBlockingStub extends io.grpc.stub.AbstractBlockingStub<AlmaOnlineBlockingStub> {
    private AlmaOnlineBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AlmaOnlineBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AlmaOnlineBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.example.AlmaOnline.server.RestaurantsReply getRestaurants(org.example.AlmaOnline.server.RestaurantsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetRestaurantsMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.example.AlmaOnline.server.MenuReply getMenu(org.example.AlmaOnline.server.MenuRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMenuMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class AlmaOnlineFutureStub extends io.grpc.stub.AbstractFutureStub<AlmaOnlineFutureStub> {
    private AlmaOnlineFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AlmaOnlineFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AlmaOnlineFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.example.AlmaOnline.server.RestaurantsReply> getRestaurants(
        org.example.AlmaOnline.server.RestaurantsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetRestaurantsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.example.AlmaOnline.server.MenuReply> getMenu(
        org.example.AlmaOnline.server.MenuRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMenuMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_RESTAURANTS = 0;
  private static final int METHODID_GET_MENU = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AlmaOnlineImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(AlmaOnlineImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_RESTAURANTS:
          serviceImpl.getRestaurants((org.example.AlmaOnline.server.RestaurantsRequest) request,
              (io.grpc.stub.StreamObserver<org.example.AlmaOnline.server.RestaurantsReply>) responseObserver);
          break;
        case METHODID_GET_MENU:
          serviceImpl.getMenu((org.example.AlmaOnline.server.MenuRequest) request,
              (io.grpc.stub.StreamObserver<org.example.AlmaOnline.server.MenuReply>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class AlmaOnlineBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AlmaOnlineBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.example.AlmaOnline.server.AlmaOnlineProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AlmaOnline");
    }
  }

  private static final class AlmaOnlineFileDescriptorSupplier
      extends AlmaOnlineBaseDescriptorSupplier {
    AlmaOnlineFileDescriptorSupplier() {}
  }

  private static final class AlmaOnlineMethodDescriptorSupplier
      extends AlmaOnlineBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    AlmaOnlineMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (AlmaOnlineGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AlmaOnlineFileDescriptorSupplier())
              .addMethod(getGetRestaurantsMethod())
              .addMethod(getGetMenuMethod())
              .build();
        }
      }
    }
    return result;
  }
}

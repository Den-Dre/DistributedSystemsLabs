package org.example.AlmaOnline.required;


import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.example.AlmaOnline.defaults.Initializer;
import org.example.AlmaOnline.provided.client.AlmaOnlineClientAdapter;
import org.example.AlmaOnline.provided.client.RestaurantInfo;
import org.example.AlmaOnline.provided.server.AlmaOnlineServerAdapter;
import org.example.AlmaOnline.provided.service.*;
import org.example.AlmaOnline.provided.service.Restaurant;
import org.example.AlmaOnline.server.*;
import org.example.AlmaOnline.provided.service.exceptions.OrderException;

import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// AlmaOnlineServerGrpcAdapter implements the grpc-server side of the application.
// The implementation should not contain any additional business logic, only implement
// the code here that is required to couple your IDL definitions to the provided business logic.
public class AlmaOnlineServerGrpcAdapter extends AlmaOnlineGrpc.AlmaOnlineImplBase implements AlmaOnlineServerAdapter {

    // the service field contains the AlmaOnline service that the server will
    // call during testing.
    private final AlmaOnlineService service;

    public AlmaOnlineServerGrpcAdapter() {
        this.service = this.getInitializer().initialize();
    }

    // -- Put the code for your implementation down below -- //


    @Override
    public void getRestaurants(RestaurantsRequest request, StreamObserver<RestaurantsReply> responseObserver) {
        //super.getRestaurants(request, responseObserver);
        List<Restaurant> list = new ArrayList<>(service.getRestaurants());
        RestaurantsReply.Builder builder = RestaurantsReply.newBuilder();

        IntStream.range(0, list.size()).
                forEach(i -> builder.addInfo(i,
                    RestaurantMessage.newBuilder().
                            setId(list.get(i).getId()).
                            setName(list.get(i).getName())).
                    build()
        );

        RestaurantsReply reply = builder.build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void getMenu(MenuRequest request, StreamObserver<MenuReply> responseObserver) {
        String id = request.getId();
        Optional<Restaurant> restaurant = service.getRestaurant(id);
        Menu menu = restaurant.orElseThrow().getMenu();

        MenuReply.Builder builder = MenuReply.newBuilder();

        menu.getItems().forEach(item -> builder.addItem(
                ItemMessage.newBuilder().setName(item.getName()).setPrice(item.getPrice()).build()
        ));

        MenuReply reply = builder.build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void addDineInOrder(DineInOrderRequest request, StreamObserver<DineInOrderReply> responseObserver) {
        try {
            service.createDineInOrder(
                    request.getRestaurantId(),
                    new DineInOrderQuote(
                        request.getOrderId(),
                        new Date((long) request.getReservationDate()),
                        request.getCustomer(),
                        request.getItemsList(),
                        new Date((long) request.getReservationDate())
                    )
            );
        } catch (OrderException e) {
            System.out.println("Order-exception!!");
        }

        DineInOrderReply reply = DineInOrderReply.newBuilder().build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}

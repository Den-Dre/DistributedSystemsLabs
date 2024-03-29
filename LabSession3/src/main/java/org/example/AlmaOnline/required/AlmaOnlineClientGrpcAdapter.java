package org.example.AlmaOnline.required;

import com.google.common.util.concurrent.ListenableFuture;
import org.example.AlmaOnline.provided.client.*;
import org.example.AlmaOnline.server.*;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

// AlmaOnlineClientGrpcAdapter provides your own implementation of the AlmaOnlineClientAdapter
public class AlmaOnlineClientGrpcAdapter implements AlmaOnlineClientAdapter {
    // getRestaurants should retrieve the information on all the available restaurants.
    @Override
    public List<RestaurantInfo> getRestaurants(AlmaOnlineGrpc.AlmaOnlineBlockingStub stub) {
        List<RestaurantInfo> list = new ArrayList<>();
        var request = RestaurantsRequest.newBuilder().build();
        var result = stub.getRestaurants(request);
        // System.out.println(result.getAllFields().values());
        result.getInfoList().stream().forEach(info -> list.add(new RestaurantInfo(info.getId(), info.getName())));
        return list;
    }

    // getMenu should return the menu of a given restaurant
    @Override
    public MenuInfo getMenu(AlmaOnlineGrpc.AlmaOnlineBlockingStub stub, String restaurantId) {
        MenuRequest request = MenuRequest.newBuilder().setId(restaurantId).build();
        MenuReply response = stub.getMenu(request);
        HashMap<String, Double> menuItems = new HashMap<>();
        response.getItemList().forEach(item -> menuItems.put(item.getName(), item.getPrice()));
        return new MenuInfo(menuItems);
    }

    // createDineInOrder should create the given dine-in order at the AlmaOnline server
    @Override
    public ListenableFuture<?> createDineInOrder(AlmaOnlineGrpc.AlmaOnlineFutureStub stub, DineInOrderQuote order) {
        DineInOrderRequest.Builder builder = DineInOrderRequest.newBuilder();
        builder.setRestaurantId(order.getRestaurantId());
        builder.setOrderId(order.getOrderId());
        builder.setCustomer(order.getCustomer());
        builder.addAllItems(order.getItems());
        builder.setReservationDate(order.getReservationDate().getTime());

        DineInOrderRequest request = builder.build();

        return stub.addDineInOrder(request);
    }

    // createDeliveryOrder should create the given delivery order at the AlmaOnline server
    @Override
    public ListenableFuture<?> createDeliveryOrder(AlmaOnlineGrpc.AlmaOnlineFutureStub stub, DeliveryOrder order) {
        return null;
    }

    // getOrder should retrieve the order information at the AlmaOnline server given the restaurant the order is
    // placed at and the id of the order.
    @Override
    public BaseOrderInfo getOrder(AlmaOnlineGrpc.AlmaOnlineBlockingStub stub, String restaurantId, String orderId) {
        return null;
    }

    // getScript returns the script the application will run during testing.
    // You can leave the default implementation, as it will test most of the functionality.
    // Alternatively, you can provide your own implementation to test your own edge-cases.
    @Override
    public AppScript getScript() {
        return AlmaOnlineClientAdapter.super.getScript();
    }
}

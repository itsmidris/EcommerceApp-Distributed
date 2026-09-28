package in.sellzon.order.order.service;


import in.sellzon.order.order.dto.request.PlaceOrderRequestDTO;
import in.sellzon.order.order.dto.response.OrderResponseDTO;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponseDTO placeOrder(PlaceOrderRequestDTO request);
    List<OrderResponseDTO> getMyOrder();
    OrderResponseDTO getMyOrder(UUID orderId);
}

package in.sellzon.order.order.service;


import in.sellzon.order.order.dto.request.PlaceOrderRequestDTO;
import in.sellzon.order.order.dto.response.OrderPaymentContextResponseDTO;
import in.sellzon.order.order.dto.response.OrderResponseDTO;
import in.sellzon.order.order.enums.PaymentStatus;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponseDTO placeOrder(PlaceOrderRequestDTO request);
    List<OrderResponseDTO> getMyOrder();
    OrderResponseDTO getMyOrder(UUID orderId);
    OrderPaymentContextResponseDTO getPaymentContext(UUID orderId);
    void updatePaymentStatus(UUID orderId, PaymentStatus paymentStatus);
}

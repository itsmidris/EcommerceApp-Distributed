package in.sellzon.order.order.service;

import in.sellzon.order.order.dto.request.UpdateSellerOrderStatusRequestDTO;
import in.sellzon.order.order.dto.response.SellerOrderResponseDTO;
import in.sellzon.order.order.enums.SellerOrderStatus;

import java.util.List;
import java.util.UUID;

public interface SellerOrderService {
    List<SellerOrderResponseDTO> getMyOrders(SellerOrderStatus status);
    SellerOrderResponseDTO getMyOrder(UUID sellerOrderId);
    SellerOrderResponseDTO updateStatus(UUID sellerOrderId, UpdateSellerOrderStatusRequestDTO request);
}

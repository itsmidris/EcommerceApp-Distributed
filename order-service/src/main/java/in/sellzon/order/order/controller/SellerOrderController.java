package in.sellzon.order.order.controller;

import in.sellzon.order.order.dto.request.UpdateSellerOrderStatusRequestDTO;
import in.sellzon.order.order.dto.response.SellerOrderResponseDTO;
import in.sellzon.order.order.enums.SellerOrderStatus;
import in.sellzon.order.order.service.SellerOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;

    @GetMapping
    public ResponseEntity<List<SellerOrderResponseDTO>> getMyOrders(@RequestParam(required = false) SellerOrderStatus status) {
        return ResponseEntity.ok(sellerOrderService.getMyOrders(status));
    }

    @GetMapping("/{sellerOrderId}")
    public ResponseEntity<SellerOrderResponseDTO> getMyOrder(@PathVariable UUID sellerOrderId) {
        return ResponseEntity.ok(sellerOrderService.getMyOrder(sellerOrderId));
    }

    @PatchMapping("/{sellerOrderId}/status")
    public ResponseEntity<SellerOrderResponseDTO> updateStatus(
            @PathVariable UUID sellerOrderId,
            @Valid @RequestBody UpdateSellerOrderStatusRequestDTO request) {
        return ResponseEntity.ok(sellerOrderService.updateStatus(sellerOrderId, request));
    }
}

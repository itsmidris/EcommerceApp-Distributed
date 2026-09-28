package in.sellzon.order.order.controller;

import in.sellzon.order.order.dto.request.PlaceOrderRequestDTO;
import in.sellzon.order.order.dto.response.OrderResponseDTO;
import in.sellzon.order.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> placeOrder(@Valid @RequestBody PlaceOrderRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrder());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getMyOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getMyOrder(orderId));
    }
}

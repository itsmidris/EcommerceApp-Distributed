package in.sellzon.order.checkout.controller;

import in.sellzon.order.checkout.dto.response.CheckoutResponseDTO;
import in.sellzon.order.checkout.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @GetMapping
    public ResponseEntity<CheckoutResponseDTO> getCheckout() {
        return ResponseEntity.ok(checkoutService.getCheckout());
    }
}
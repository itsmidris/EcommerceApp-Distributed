package in.sellzon.order.checkout.dto.response;

import in.sellzon.order.address.response.AddressResponse;
import in.sellzon.order.cart.response.CartResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CheckoutResponseDTO(
        CartResponse cart,
        List<AddressResponse> addresses,
        UUID defaultAddressId,
        BigDecimal shippingAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount
) {
}

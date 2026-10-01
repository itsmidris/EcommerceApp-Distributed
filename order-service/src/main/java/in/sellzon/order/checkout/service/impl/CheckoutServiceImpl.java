package in.sellzon.order.checkout.service.impl;

import in.sellzon.order.address.client.IdentityAddressClient;
import in.sellzon.order.address.response.AddressResponse;
import in.sellzon.order.cart.client.CartClient;
import in.sellzon.order.cart.response.CartResponse;
import in.sellzon.order.checkout.dto.response.CheckoutResponseDTO;
import in.sellzon.order.checkout.service.CheckoutService;
import in.sellzon.order.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final CartClient cartClient;
    private final IdentityAddressClient identityAddressClient;
    private final CurrentUserService currentUserService;

    @Override
    public CheckoutResponseDTO getCheckout() {

        String accessToken = currentUserService.getCurrentAccessToken();

        CartResponse cart = cartClient.getCart(accessToken);

        List<AddressResponse> addresses = identityAddressClient.getMyAddresses(accessToken);

        AddressResponse defaultAddress =
                addresses
                        .stream()
                        .filter(AddressResponse::defaultAddress)
                        .findFirst()
                        .orElse(null);

        BigDecimal shipping = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        BigDecimal subtotal =
                cart.subtotal() == null
                        ? BigDecimal.ZERO
                        : cart.subtotal();

        BigDecimal total =
                subtotal
                        .add(shipping)
                        .subtract(discount);

        return new CheckoutResponseDTO(
                cart,
                addresses,
                defaultAddress == null ? null : defaultAddress.id(),
                shipping,
                discount,
                total
        );
    }
}

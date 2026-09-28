package in.sellzon.order.address.response;

import in.sellzon.order.address.AddressType;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        UUID customerId,
        String recipientName,
        String phoneNumber,
        String addressLine1,
        String addressLine2,
        String landmark,
        String city,
        String state,
        String postalCode,
        String country,
        AddressType addressType,
        boolean defaultAddress,
        String status
) {
}
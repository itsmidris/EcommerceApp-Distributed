package in.sellzon.order.order.dto.response;


import in.sellzon.order.address.AddressType;

public record OrderAddressResponseDTO(
        String recipientName,
        String phoneNumber,
        String addressLine1,
        String addressLine2,
        String landmark,
        String city,
        String state,
        String postalCode,
        String country,
        AddressType addressType
) {
}

package in.sellzon.identity.address.dto.response;

import in.sellzon.identity.common.enums.AddressType;
import in.sellzon.identity.common.enums.Status;

import java.util.UUID;

public record CustomerAddressResponseDTO(
        UUID id,
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
        Status status
) {}

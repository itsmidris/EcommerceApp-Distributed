package in.sellzon.identity.address.dto.request;

import in.sellzon.identity.common.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerAddressRequestDTO(
        @NotBlank
        @Size(max = 100)
        String recipientName,

        @NotBlank
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian phone number")
        String phoneNumber,

        @NotBlank
        @Size(max = 250)
        String addressLine1,

        @Size(max = 250)
        String addressLine2,

        @Size(max = 150)
        String landmark,

        @NotBlank
        @Size(max = 100)
        String city,

        @NotBlank
        @Size(max = 100)
        String state,

        @NotBlank
        @Size(max = 20)
        String postalCode,

        @Size(max = 100)
        String country,

        @NotNull AddressType addressType,

        boolean defaultAddress
) {}

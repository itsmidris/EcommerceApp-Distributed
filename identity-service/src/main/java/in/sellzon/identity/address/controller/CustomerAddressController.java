package in.sellzon.identity.address.controller;

import in.sellzon.identity.address.dto.request.CreateCustomerAddressRequestDTO;
import in.sellzon.identity.address.dto.request.UpdateCustomerAddressRequestDTO;
import in.sellzon.identity.address.dto.response.CustomerAddressResponseDTO;
import in.sellzon.identity.address.service.CustomerAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/addresses")
@RequiredArgsConstructor
public class CustomerAddressController {
    private final CustomerAddressService addressService;

    @GetMapping
    public ResponseEntity<List<CustomerAddressResponseDTO>> getAddresses() {
        return ResponseEntity.ok(addressService.getMyAddresses());
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<CustomerAddressResponseDTO> getAddress(@PathVariable UUID addressId) {
        return ResponseEntity.ok(addressService.getMyAddress(addressId));
    }

    @PostMapping
    public ResponseEntity<CustomerAddressResponseDTO> create(@Valid @RequestBody CreateCustomerAddressRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.createAddress(request));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<CustomerAddressResponseDTO> update(
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateCustomerAddressRequestDTO request) {
        return ResponseEntity.ok(addressService.updateAddress(addressId, request));
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<CustomerAddressResponseDTO> setDefault(@PathVariable UUID addressId) {
        return ResponseEntity.ok(addressService.setDefault(addressId));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@PathVariable UUID addressId) {
        addressService.deleteAddress(addressId);
        return ResponseEntity.noContent().build();
    }
}

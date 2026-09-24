package in.sellzon.identity.address.service;

import in.sellzon.identity.address.dto.request.CreateCustomerAddressRequestDTO;
import in.sellzon.identity.address.dto.request.UpdateCustomerAddressRequestDTO;
import in.sellzon.identity.address.dto.response.CustomerAddressResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CustomerAddressService {
    List<CustomerAddressResponseDTO> getMyAddresses();
    CustomerAddressResponseDTO getMyAddress(UUID addressId);
    CustomerAddressResponseDTO createAddress(CreateCustomerAddressRequestDTO request);
    CustomerAddressResponseDTO updateAddress(UUID addressId, UpdateCustomerAddressRequestDTO request);
    CustomerAddressResponseDTO setDefault(UUID addressId);
    void deleteAddress(UUID addressId);
}

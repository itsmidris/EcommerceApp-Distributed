package in.sellzon.identity.address.service.impl;

import in.sellzon.identity.address.dto.request.CreateCustomerAddressRequestDTO;
import in.sellzon.identity.address.dto.request.UpdateCustomerAddressRequestDTO;
import in.sellzon.identity.address.dto.response.CustomerAddressResponseDTO;
import in.sellzon.identity.address.entity.CustomerAddress;
import in.sellzon.identity.address.repository.CustomerAddressRepository;
import in.sellzon.identity.address.service.CustomerAddressService;
import in.sellzon.identity.common.enums.AddressType;
import in.sellzon.identity.common.enums.Status;
import in.sellzon.identity.common.exception.ResourceNotFoundException;
import in.sellzon.identity.security.CurrentUserService;
import in.sellzon.identity.user.Entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerAddressServiceImpl implements CustomerAddressService {
    private final CustomerAddressRepository addressRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAddressResponseDTO> getMyAddresses() {

        User customer = currentUserService.getCurrentCustomer();

        return addressRepository
                .findByCustomerIdAndStatusOrderByDefaultAddressDescCreatedAtDesc(customer.getId(), Status.ACTIVE)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerAddressResponseDTO getMyAddress(UUID addressId) {
        return map(getActiveAddress(addressId));
    }

    @Override
    public CustomerAddressResponseDTO createAddress(CreateCustomerAddressRequestDTO request) {

        User customer = currentUserService.getCurrentCustomer();

        CustomerAddress address = new CustomerAddress();

        address.setCustomer(customer);

        apply(
                address,
                request.recipientName(),
                request.phoneNumber(),
                request.addressLine1(),
                request.addressLine2(),
                request.landmark(),
                request.city(),
                request.state(),
                request.postalCode(),
                request.country(),
                request.addressType()
        );

        boolean hasActiveAddress = addressRepository.existsByCustomerIdAndStatus(customer.getId(), Status.ACTIVE);

        boolean userWantsDefault = request.defaultAddress();

        boolean isFirstAddress = !hasActiveAddress;

        boolean makeDefault = userWantsDefault || isFirstAddress;

        if (makeDefault) {
            clearDefault(customer.getId());
        }

        address.setDefaultAddress(makeDefault);

        CustomerAddress savedAddress = addressRepository.save(address);

        return map(savedAddress);
    }


    @Override
    public CustomerAddressResponseDTO updateAddress(UUID addressId, UpdateCustomerAddressRequestDTO request) {

        CustomerAddress address = getActiveAddress(addressId);

        apply(
                address,
                request.recipientName(),
                request.phoneNumber(),
                request.addressLine1(),
                request.addressLine2(),
                request.landmark(),
                request.city(),
                request.state(),
                request.postalCode(),
                request.country(),
                request.addressType()
        );

        if (request.defaultAddress()) {
            clearDefault(address.getCustomer().getId());
            address.setDefaultAddress(true);
        }

        return map(addressRepository.save(address));
    }

    @Override
    public CustomerAddressResponseDTO setDefault(UUID addressId) {

        CustomerAddress address = getActiveAddress(addressId);

        clearDefault(address.getCustomer().getId());

        address.setDefaultAddress(true);

        return map(addressRepository.save(address));
    }

    @Override
    public void deleteAddress(UUID addressId) {

        CustomerAddress address = getActiveAddress(addressId);

        UUID customerId = address.getCustomer().getId();

        boolean wasDefault = address.isDefaultAddress();

        address.setDefaultAddress(false);
        address.setStatus(Status.INACTIVE);

        addressRepository.save(address);

        if (wasDefault) {
            addressRepository
                    .findByCustomerIdAndStatusOrderByDefaultAddressDescCreatedAtDesc(customerId, Status.ACTIVE)
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefaultAddress(true);
                        addressRepository.save(next);
                    });
        }

    }

    private CustomerAddress getActiveAddress(UUID id) {

        User customer = currentUserService.getCurrentCustomer();

        return addressRepository
                .findByIdAndCustomerIdAndStatus(id, customer.getId(), Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    private void clearDefault(UUID customerId) {

        addressRepository
                .findByCustomerIdAndStatusOrderByDefaultAddressDescCreatedAtDesc(customerId, Status.ACTIVE)
                .forEach(a -> {
                    if (a.isDefaultAddress()) a.setDefaultAddress(false);
                });
    }

    private void apply(
            CustomerAddress address,
            String recipientName,
            String phoneNumber,
            String line1,
            String line2,
            String landmark,
            String city,
            String state,
            String postalCode,
            String country,
            AddressType type) {

        address.setRecipientName(clean(recipientName));
        address.setPhoneNumber(clean(phoneNumber));
        address.setAddressLine1(clean(line1));
        address.setAddressLine2(cleanNullable(line2));
        address.setLandmark(cleanNullable(landmark));
        address.setCity(clean(city));
        address.setState(clean(state));
        address.setPostalCode(clean(postalCode));
        address.setCountry(country == null || country.isBlank() ? "India" : clean(country));
        address.setAddressType(type);
        address.setStatus(Status.ACTIVE);
    }

    private String clean(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank() ? null : clean(value);
    }

    private CustomerAddressResponseDTO map(CustomerAddress a) {

        return new CustomerAddressResponseDTO(
                a.getId(),
                a.getRecipientName(),
                a.getPhoneNumber(),
                a.getAddressLine1(),
                a.getAddressLine2(),
                a.getLandmark(),
                a.getCity(),
                a.getState(),
                a.getPostalCode(),
                a.getCountry(),
                a.getAddressType(),
                a.isDefaultAddress(),
                a.getStatus()
        );
    }
}

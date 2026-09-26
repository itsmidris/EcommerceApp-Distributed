package in.sellzon.catalog.attribute.service.impl;

import in.sellzon.catalog.attribute.dto.request.CreateAttributeOptionRequestDTO;
import in.sellzon.catalog.attribute.dto.request.UpdateAttributeOptionRequestDTO;
import in.sellzon.catalog.attribute.dto.response.AttributeOptionResponseDTO;
import in.sellzon.catalog.attribute.entity.Attribute;
import in.sellzon.catalog.attribute.entity.AttributeOption;
import in.sellzon.catalog.attribute.enums.AttributeType;
import in.sellzon.catalog.attribute.repository.AttributeOptionRepository;
import in.sellzon.catalog.attribute.repository.AttributeRepository;
import in.sellzon.catalog.attribute.service.AttributeOptionService;
import in.sellzon.catalog.common.enums.Status;
import in.sellzon.catalog.common.exception.DuplicateResourceException;
import in.sellzon.catalog.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AttributeOptionServiceImpl implements AttributeOptionService {

    private final AttributeOptionRepository attributeOptionRepository;
    private final AttributeRepository attributeRepository;

    @Override
    public AttributeOptionResponseDTO createOption(UUID attributeId, CreateAttributeOptionRequestDTO request) {

        Attribute attribute = getActiveAttribute(attributeId);

        validateOptionSupported(attribute);

        String value = normalizeValue(request.value());
        String code = normalizeCode(request.code());

        validateValue(attributeId, value, null);
        validateCode(attributeId, code, null);

        AttributeOption option = new AttributeOption();

        option.setAttribute(attribute);
        option.setValue(value);
        option.setCode(code);
        option.setDisplayOrder(getNextDisplayOrder(attribute));
        option.setStatus(Status.ACTIVE);

        AttributeOption savedOption = attributeOptionRepository.save(option);

        return mapToResponse(savedOption);
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeOptionResponseDTO getOption(UUID optionId) {
        AttributeOption option = getActiveOption(optionId);
        return mapToResponse(option);
    }

    @Override
    public AttributeOptionResponseDTO updateOption(UUID optionId, UpdateAttributeOptionRequestDTO request) {
        AttributeOption option = getActiveOption(optionId);

        UUID attributeId = option.getAttribute().getId();

        String value = normalizeValue(request.value());

        validateValue(attributeId, value, optionId);

        option.setValue(value);

        AttributeOption savedOption = attributeOptionRepository.save(option);

        return mapToResponse(savedOption);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttributeOptionResponseDTO> getOptionsByAttribute(UUID attributeId) {

        Attribute attribute = getActiveAttribute(attributeId);

        validateOptionSupported(attribute);

        return attributeOptionRepository
                .findByAttributeIdAndStatusOrderByDisplayOrderAsc(attributeId, Status.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deactivateOption(UUID optionId) {

        AttributeOption option = getActiveOption(optionId);

        option.setStatus(Status.INACTIVE);

        attributeOptionRepository.save(option);
    }

    private Attribute getActiveAttribute(UUID attributeId) {
        return attributeRepository
                .findByIdAndStatus(attributeId, Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found: " + attributeId));
    }

    private void validateOptionSupported(Attribute attribute) {
        AttributeType type = attribute.getType();

        if (type != AttributeType.DROPDOWN && type != AttributeType.MULTI_SELECT) {
            throw new IllegalArgumentException("Attribute type " + type + " does not support options");
        }
    }

    private String normalizeValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Option value is required");
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Option code is required");
        }
        return code.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    private void validateValue(UUID attributeId, String value, UUID optionId) {
        boolean exists;

        if (optionId == null) {
            exists = attributeOptionRepository.existsByAttributeIdAndValueIgnoreCase(attributeId, value);
        } else {
            exists = attributeOptionRepository.existsByAttributeIdAndValueIgnoreCaseAndIdNot(attributeId, value, optionId);
        }

        if (exists) {
            throw new DuplicateResourceException("Option with value '" + value + "' already exists for this attribute");
        }
    }

    private void validateCode(UUID attributeId, String code, UUID optionId) {
        boolean exists;

        if (optionId == null) {
            exists = attributeOptionRepository.existsByAttributeIdAndCodeIgnoreCase(attributeId, code);
        } else {
            exists = attributeOptionRepository.existsByAttributeIdAndCodeIgnoreCaseAndIdNot(attributeId, code, optionId);
        }

        if (exists) {
            throw new DuplicateResourceException("Option with code '" + code + "' already exists for this attribute");
        }
    }

    private int getNextDisplayOrder(Attribute attribute) {
        return attributeOptionRepository
                .findTopByAttributeAndStatusOrderByDisplayOrderDesc(attribute, Status.ACTIVE)
                .map(option -> option.getDisplayOrder() + 1)
                .orElse(1);
    }

    private AttributeOption getActiveOption(UUID optionId) {
        return attributeOptionRepository
                .findByIdAndStatus(optionId, Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute option not found: " + optionId));
    }

    private AttributeOptionResponseDTO mapToResponse(AttributeOption option) {
        return new AttributeOptionResponseDTO(
                option.getId(),
                option.getAttribute().getId(),
                option.getValue(),
                option.getCode(),
                option.getDisplayOrder(),
                option.getStatus()
        );
    }
}

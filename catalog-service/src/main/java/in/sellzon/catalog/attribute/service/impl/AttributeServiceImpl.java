package in.sellzon.catalog.attribute.service.impl;

import in.sellzon.catalog.attribute.dto.request.CreateAttributeRequestDTO;
import in.sellzon.catalog.attribute.dto.request.UpdateAttributeRequestDTO;
import in.sellzon.catalog.attribute.dto.response.AttributeResponseDTO;
import in.sellzon.catalog.attribute.entity.Attribute;
import in.sellzon.catalog.attribute.enums.AttributeType;
import in.sellzon.catalog.attribute.repository.AttributeOptionRepository;
import in.sellzon.catalog.attribute.repository.AttributeRepository;
import in.sellzon.catalog.attribute.service.AttributeService;
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
public class AttributeServiceImpl implements AttributeService {

    private final AttributeRepository attributeRepository;
    private final AttributeOptionRepository attributeOptionRepository;
    @Override
    public AttributeResponseDTO createAttribute(CreateAttributeRequestDTO request) {
        String name = normalizeName(request.name());
        String code = normalizeCode(request.code());

        validateName(name, null);
        validateCode(code, null);

        Attribute attribute = new Attribute();

        attribute.setName(name);
        attribute.setCode(code);
        attribute.setType(request.type());
        attribute.setDescription(normalizeDescription(request.description()));
        attribute.setUnit(normalizeUnit(request.unit()));
        attribute.setStatus(Status.ACTIVE);

        Attribute saveAttribute = attributeRepository.save(attribute);

        return mapToResponse(saveAttribute);
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponseDTO getAttribute(UUID attributeId) {
        Attribute attribute = getActiveAttribute(attributeId);
        return mapToResponse(attribute);
    }

    @Override
    public AttributeResponseDTO updateAttribute(UUID attributeId, UpdateAttributeRequestDTO request) {
        Attribute attribute = getActiveAttribute(attributeId);

        String name = normalizeName(request.name());

        validateName(name, attributeId);

        attribute.setName(name);

        if (attribute.getType() != request.type()) {
            boolean hasOptions = attributeOptionRepository.existsByAttributeId(attributeId);
            if (hasOptions) {
                throw new IllegalArgumentException("Attribute type cannot be changed because this attribute already has options");
            }
        }

        attribute.setType(request.type());
        attribute.setDescription(normalizeDescription(request.description()));
        attribute.setUnit(normalizeUnit(request.unit()));

        Attribute savedAttribute = attributeRepository.save(attribute);

        return mapToResponse(savedAttribute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponseDTO> getAllAttributes() {
        return attributeRepository
                .findByStatusOrderByNameAsc(Status.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponseDTO> getAttributesByType(String type) {
        AttributeType attributeType = parseAttributeType(type);
        return attributeRepository
                .findByTypeAndStatusOrderByNameAsc(attributeType, Status.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deactivateAttribute(UUID attributeId) {
        Attribute attribute = getActiveAttribute(attributeId);

        attribute.setStatus(Status.INACTIVE);
        attributeRepository.save(attribute);
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }

        return name.trim().replaceAll("\\s+", " ");
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }

        return code.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    private void validateName(String name, UUID attributeId) {
        boolean exists;

        if (attributeId == null) {
            exists = attributeRepository.existsByNameIgnoreCase(name);
        } else {
            exists = attributeRepository.existsByNameIgnoreCaseAndIdNot(name, attributeId);
        }

        if (exists) {
            throw new DuplicateResourceException("Attribute with name '" + name + "' already exists");
        }
    }

    private void validateCode(String code, UUID attributeId) {
        boolean exists;

        if (attributeId == null){
            exists = attributeRepository.existsByCodeIgnoreCase(code);
        } else {
            exists = attributeRepository.existsByCodeIgnoreCaseAndIdNot(code, attributeId);
        }

        if (exists) {
            throw new DuplicateResourceException("Attribute with code '" + code + "' already exists");
        }
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    private String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }

        return unit.trim();
    }

    private AttributeResponseDTO mapToResponse(Attribute attribute) {
        return new AttributeResponseDTO(
                attribute.getId(),
                attribute.getName(),
                attribute.getCode(),
                attribute.getType(),
                attribute.getDescription(),
                attribute.getUnit(),
                attribute.getStatus()
        );
    }

    private Attribute getActiveAttribute(UUID attributeId) {
        return attributeRepository
                .findByIdAndStatus(attributeId, Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found: " + attributeId));
    }

    private AttributeType parseAttributeType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Attribute type is required");
        }

        try {
            return AttributeType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid attribute type: " + type);
        }
    }
}

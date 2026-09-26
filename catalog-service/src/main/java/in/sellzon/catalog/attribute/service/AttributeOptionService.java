package in.sellzon.catalog.attribute.service;

import in.sellzon.catalog.attribute.dto.request.CreateAttributeOptionRequestDTO;
import in.sellzon.catalog.attribute.dto.request.UpdateAttributeOptionRequestDTO;
import in.sellzon.catalog.attribute.dto.response.AttributeOptionResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AttributeOptionService {

    AttributeOptionResponseDTO createOption(UUID attributeId, CreateAttributeOptionRequestDTO request);
    AttributeOptionResponseDTO getOption(UUID optionId);
    AttributeOptionResponseDTO updateOption(UUID optionId, UpdateAttributeOptionRequestDTO request);
    List<AttributeOptionResponseDTO> getOptionsByAttribute(UUID attributeId);
    void deactivateOption(UUID optionId);
}

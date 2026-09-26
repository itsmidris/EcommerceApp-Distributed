package in.sellzon.catalog.attribute.service;

import in.sellzon.catalog.attribute.dto.request.CreateAttributeRequestDTO;
import in.sellzon.catalog.attribute.dto.request.UpdateAttributeRequestDTO;
import in.sellzon.catalog.attribute.dto.response.AttributeResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AttributeService {

    AttributeResponseDTO createAttribute(CreateAttributeRequestDTO request);
    AttributeResponseDTO getAttribute(UUID attributeId);
    AttributeResponseDTO updateAttribute(UUID attributeId, UpdateAttributeRequestDTO request);
    List<AttributeResponseDTO> getAllAttributes();
    List<AttributeResponseDTO> getAttributesByType(String type);
    void deactivateAttribute(UUID attributeId);
}

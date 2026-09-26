package in.sellzon.catalog.attribute.controller;

import in.sellzon.catalog.attribute.dto.request.CreateAttributeRequestDTO;
import in.sellzon.catalog.attribute.dto.request.UpdateAttributeRequestDTO;
import in.sellzon.catalog.attribute.dto.response.AttributeResponseDTO;
import in.sellzon.catalog.attribute.service.AttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/attributes")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

    @PostMapping
    public ResponseEntity<AttributeResponseDTO> createAttribute(@Valid @RequestBody CreateAttributeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attributeService.createAttribute(request));
    }

    @GetMapping("/{attributeId}")
    public ResponseEntity<AttributeResponseDTO> getAttribute(@PathVariable UUID attributeId) {
        return ResponseEntity.ok(attributeService.getAttribute(attributeId));
    }

    @PutMapping("/{attributeId}")
    public ResponseEntity<AttributeResponseDTO> updateAttribute(@PathVariable UUID attributeId, @Valid @RequestBody UpdateAttributeRequestDTO request) {
        return ResponseEntity.ok(attributeService.updateAttribute(attributeId, request));
    }

    @GetMapping
    public ResponseEntity<List<AttributeResponseDTO>> getAllAttributes() {
        return ResponseEntity.ok(attributeService.getAllAttributes());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<AttributeResponseDTO>> getAttributesByType(@PathVariable String type) {
        return ResponseEntity.ok(attributeService.getAttributesByType(type));
    }

    @PatchMapping("/{attributeId}/deactivate")
    public ResponseEntity<Void> deactivateAttribute(@PathVariable UUID attributeId) {
        attributeService.deactivateAttribute(attributeId);
        return ResponseEntity.noContent().build();
    }
}

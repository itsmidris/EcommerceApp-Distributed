package in.sellzon.catalog.attribute.controller;

import in.sellzon.catalog.attribute.dto.request.CreateAttributeOptionRequestDTO;
import in.sellzon.catalog.attribute.dto.request.UpdateAttributeOptionRequestDTO;
import in.sellzon.catalog.attribute.dto.response.AttributeOptionResponseDTO;
import in.sellzon.catalog.attribute.service.AttributeOptionService;
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
public class AttributeOptionController {

    private final AttributeOptionService attributeOptionService;

    @PostMapping("/{attributeId}/options")
    public ResponseEntity<AttributeOptionResponseDTO> createOption(@PathVariable UUID attributeId, @Valid @RequestBody CreateAttributeOptionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attributeOptionService.createOption(attributeId, request));
    }

    @GetMapping("/{attributeId}/options")
    public ResponseEntity<List<AttributeOptionResponseDTO>> getOptionsByAttribute(@PathVariable UUID attributeId) {
        return ResponseEntity.ok(attributeOptionService.getOptionsByAttribute(attributeId));
    }

    @GetMapping("/options/{optionId}")
    public ResponseEntity<AttributeOptionResponseDTO> getOption(@PathVariable UUID optionId) {
        return ResponseEntity.ok(attributeOptionService.getOption(optionId));
    }

    @PutMapping("/options/{optionId}")
    public ResponseEntity<AttributeOptionResponseDTO> updateOption(@PathVariable UUID optionId, @Valid @RequestBody UpdateAttributeOptionRequestDTO request) {
        return ResponseEntity.ok(attributeOptionService.updateOption(optionId, request));
    }

    @PatchMapping("/options/{optionId}/deactivate")
    public ResponseEntity<Void> deactivateOption(@PathVariable UUID optionId) {
        attributeOptionService.deactivateOption(optionId);
        return ResponseEntity.noContent().build();
    }
}

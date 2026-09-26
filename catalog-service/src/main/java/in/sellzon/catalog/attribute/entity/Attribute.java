package in.sellzon.catalog.attribute.entity;

import in.sellzon.catalog.attribute.enums.AttributeType;
import in.sellzon.catalog.common.entity.BaseEntity;
import in.sellzon.catalog.common.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "attributes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attribute_code", columnNames = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Attribute extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttributeType type;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;
}

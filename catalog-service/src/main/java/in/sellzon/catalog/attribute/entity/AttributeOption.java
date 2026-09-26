package in.sellzon.catalog.attribute.entity;

import in.sellzon.catalog.common.entity.BaseEntity;
import in.sellzon.catalog.common.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "attribute_options",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attribute_option_code", columnNames = {"attribute_id", "code"}),
                @UniqueConstraint(name = "uk_attribute_option_value", columnNames = {"attribute_id", "value"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AttributeOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attribute_option_attribute"))
    private Attribute attribute;

    @Column(nullable = false, length = 150)
    private String value;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false)
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;
}

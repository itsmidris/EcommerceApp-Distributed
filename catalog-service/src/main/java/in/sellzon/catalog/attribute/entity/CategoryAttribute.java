package in.sellzon.catalog.attribute.entity;

import in.sellzon.catalog.category.entity.Category;
import in.sellzon.catalog.common.entity.BaseEntity;
import in.sellzon.catalog.common.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "category_attributes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_category_attribute", columnNames = {"category_id", "attribute_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CategoryAttribute extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_category_attribute_category"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false, foreignKey = @ForeignKey(name = "fk_category_attribute_attribute"))
    private Attribute attribute;

    @Column(nullable = false)
    private boolean required = false;

    @Column(nullable = false)
    private boolean filterable = false;

    @Column(nullable = false)
    private boolean searchable = false;

    @Column(nullable = false)
    private boolean variantAttribute = false;

    @Column(nullable = false)
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;
}

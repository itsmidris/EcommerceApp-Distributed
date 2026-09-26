package in.sellzon.catalog.productattribute.entity;

import in.sellzon.catalog.attribute.entity.AttributeOption;
import in.sellzon.catalog.attribute.entity.CategoryAttribute;
import in.sellzon.catalog.common.entity.BaseEntity;
import in.sellzon.catalog.product.entity.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "product_attribute_values",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_category_attribute", columnNames = {"product_id", "category_attribute_id"})
        },
        indexes = {
                @Index(name = "idx_product_attribute_value_product", columnList = "product_id"),
                @Index(name = "idx_product_attribute_value_category_attribute", columnList = "category_attribute_id")
        }
)
public class ProductAttributeValue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_attribute_value_product")
    )
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "category_attribute_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_attribute_value_category_attribute")
    )
    private CategoryAttribute categoryAttribute;

    @Column(name = "value_text", length = 5000)
    private String valueText;

    @Column(name = "value_number")
    private Long valueNumber;

    @Column(
            name = "value_decimal",
            precision = 19,
            scale = 6
    )
    private BigDecimal valueDecimal;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "selected_option_id",
            foreignKey = @ForeignKey(name = "fk_product_attribute_value_selected_option")
    )
    private AttributeOption selectedOption;

    @ManyToMany
    @JoinTable(
            name = "product_attribute_value_options",
            joinColumns = @JoinColumn(
                    name = "product_attribute_value_id",
                    nullable = false,
                    foreignKey = @ForeignKey(
                            name = "fk_product_attribute_value_options_value"
                    )
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "attribute_option_id",
                    nullable = false,
                    foreignKey = @ForeignKey(
                            name = "fk_product_attribute_value_options_option"
                    )
            ),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_product_attribute_value_option",
                            columnNames = {
                                    "product_attribute_value_id",
                                    "attribute_option_id"
                            }
                    )
            }
    )
    @OrderBy("displayOrder ASC")
    private List<AttributeOption> selectedOptions = new ArrayList<>();
}
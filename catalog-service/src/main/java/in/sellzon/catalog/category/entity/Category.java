package in.sellzon.catalog.category.entity;

import in.sellzon.catalog.category.enums.CategoryLevel;
import in.sellzon.catalog.common.entity.BaseEntity;
import in.sellzon.catalog.common.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_categories_parent_id", columnList = "parent_id"),
                @Index(name = "idx_categories_level", columnList = "level"),
                @Index(name = "idx_categories_status", columnList = "status"),
                @Index(name = "idx_categories_slug", columnList = "slug")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_categories_parent_name", columnNames = {"parent_id", "name"}),
                @UniqueConstraint(name = "uk_categories_parent_slug", columnNames = {"parent_id", "slug"})
        }
)
public class Category extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryLevel level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, name ASC")
    private List<Category> children = new ArrayList<>();

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;
}

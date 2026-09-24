package in.sellzon.identity.address.entity;

import in.sellzon.identity.common.entity.BaseEntity;
import in.sellzon.identity.common.enums.AddressType;
import in.sellzon.identity.common.enums.Status;
import in.sellzon.identity.user.Entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "customer_addresses",
        indexes = {
                @Index(name = "idx_customer_address_customer", columnList = "customer_id"),
                @Index(name = "idx_customer_address_customer_status", columnList = "customer_id,status")
        }
        )
public class CustomerAddress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_customer_address_customer"))
    private User customer;

    @Column(nullable = false, length = 100)
    private String recipientName;

    @Column(nullable = false, length = 15)
    private String phoneNumber;

    @Column(nullable = false, length = 250)
    private String addressLine1;

    @Column(length = 250)
    private String addressLine2;

    @Column(length = 150)
    private String landmark;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String country = "India";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AddressType addressType = AddressType.HOME;

    @Column(nullable = false)
    private boolean defaultAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;
}

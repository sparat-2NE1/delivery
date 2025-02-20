package com.sparta.delivery.domain.user.entity;

import com.sparta.delivery.domain.common.Timestamped;
import com.sparta.delivery.domain.delivery_address.entity.DeliveryAddress;
import com.sparta.delivery.domain.user.dto.UserResDto;
import com.sparta.delivery.domain.user.enums.UserRoles;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "p_user")
public class User extends Timestamped {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(unique = true , nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRoles role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryAddress> deliveryAddresses = new ArrayList<>();

    public void addDeliveryAddress(DeliveryAddress deliveryAddress){
        this.deliveryAddresses.add(deliveryAddress);
    }

    public UserResDto toResponseDto() {

        return new UserResDto(
                this.userId,
                this.username,
                this.email,
                this.nickname,
                this.role.name()
        );
    }
}
package com.team1.cityfarm.entity;

import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 50, nullable = false, unique = true)
    private String nickname;

    @Column(length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RoleType roleType = RoleType.USER;

    private String businessName;

    private String businessRegistrationNumber;

    private String businessAddress;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void updateStatus(Status status){
        if (status == null) {
            throw new CustomException(CustomError.USER_STATUS_ERROR);
        }
        this.status = status;
    }

    public void updateRoleType(RoleType roleType){
        if (roleType == null) {
            throw new CustomException(CustomError.USER_STATUS_ERROR);
        }
        this.roleType = roleType;
    }

    public void promoteToHost(String businessName,String businessAddress, String businessRegistrationNumber){

        if (this.roleType == RoleType.HOST || this.roleType == RoleType.ADMIN){
            throw new CustomException(CustomError.ALREADY_HOST_ROLE);
        }

        this.businessName = businessName;
        this.businessAddress = businessAddress;
        this.businessRegistrationNumber = businessRegistrationNumber;
        this.roleType = RoleType.HOST;
    }
}
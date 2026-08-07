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

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    // 회원 수정 시 사용 예정이며 사용안할 경우 삭제
    @UpdateTimestamp
    @Column
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



}

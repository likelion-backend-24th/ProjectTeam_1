package com.team1.cityfarm.entity;

import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity@Table(name = "billing_keys")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class BillingKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 실제 DB에는 암호화하여 저장
     */
    @Column(nullable = false,name = "billing_key_encrypted")
    private String billingKeyEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingKeyStatus status;

    private LocalDateTime issuedAt;

    private LocalDateTime expiredAt;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void updateBillingKeyStatus(BillingKeyStatus status){
        if(status == null) throw new CustomException(CustomError.BILLING_KEY_STATUS_NOT_FOUND);
        this.status = status;
    }
}

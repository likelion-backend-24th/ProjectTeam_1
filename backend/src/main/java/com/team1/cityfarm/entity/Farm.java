package com.team1.cityfarm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "farms")
public class Farm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 판매자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 임대 땅 제목
    @Column(nullable = false, length = 50)
    private String title;

    // 주소 (자동으로 불러옴)
    @Column(nullable = false)
    private String location;

    // 주소 상세 작성
    @Column
    private String locationAddress;

    // 면적
    @Column(nullable = false)
    private int area;

    // 임대 가격
    @Column(nullable = false)
    private int monthlyRent;

    // 임대 기간
    @Column(nullable = false)
    private int rentalMonths;

    // 설명
    @Column(length = 500)
    private String description;

    // 임대 상태
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FarmStatus farmStatus = FarmStatus.AVAILABLE;

    // 등록일
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 수정일
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}

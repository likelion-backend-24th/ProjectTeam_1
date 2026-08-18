package com.team1.cityfarm.entity;

import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter@Builder@AllArgsConstructor
@NoArgsConstructor
public class Reply {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "is_adopted", nullable = false)
    private Boolean isAdopted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id",  nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",  nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void updateContent(String content) {
        //null 체크
        if (content == null || content.isBlank()) {
            throw new CustomException(CustomError.INVALID_INPUT_VALUE);
        }
        //상태 변경
        this.content = content;
    }
}

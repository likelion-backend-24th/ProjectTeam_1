package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.ReplyCommentRequestDto;
import com.team1.cityfarm.dto.ReplyCommentResponseDto;
import com.team1.cityfarm.entity.ReplyComment;
import com.team1.cityfarm.repository.ReplyCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplyCommentService {

    private final ReplyCommentRepository replyCommentRepository;

    // 답글 댓글 등록
    @Transactional
    public ReplyCommentResponseDto createComment(Long replyId, ReplyCommentRequestDto request, Long userId) {
        ReplyComment comment = new ReplyComment();
        comment.setContent(request.getContent());
        // comment.setReply(reply);  // TODO
        // comment.setUser(user);    // TODO

        ReplyComment saved = replyCommentRepository.save(comment);
        return new ReplyCommentResponseDto(saved);
    }

    // 답글 댓글 목록 조회
    public List<ReplyCommentResponseDto> getComments(Long replyId) {
        // TODO: Reply 연관관계 살아나면 아래 주석 해제하고 이 return 지우기
        /*
        List<ReplyComment> comments = replyCommentRepository.findByReplyIdOrderByCreatedAtAsc(replyId);
        return comments.stream()
                .map(comment -> new ReplyCommentResponseDto(comment))
                .collect(Collectors.toList());
        */
        return Collections.emptyList();
    }

    // 답글 댓글 수정
    @Transactional
    public ReplyCommentResponseDto updateComment(Long commentId, ReplyCommentRequestDto requestDto, Long userId) {
        ReplyComment comment = replyCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        // TODO: 작성자 본인 확인 로직 (user 연관관계 살아나면)
        /*if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인 댓글만 수정할 수 있습니다.");
        }*/

        comment.setContent(requestDto.getContent());
        ReplyComment updated = replyCommentRepository.saveAndFlush(comment);
        return new ReplyCommentResponseDto(updated);
    }

    // 답글 댓글 삭제
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        ReplyComment comment = replyCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        // TODO: 작성자 본인 확인 로직 (user 연관관계 살아나면)
        /*if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인 댓글만 삭제할 수 있습니다.");
        }*/

        replyCommentRepository.delete(comment);
    }
}
package com.team1.cityfarm.service;

import com.team1.cityfarm.repository.BoardCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCommentService {

    private final BoardCommentRepository boardCommentRepository;

//    댓글 목록 조회

//    댓글 작성

//    댓글 수정

//    댓글 삭제
}

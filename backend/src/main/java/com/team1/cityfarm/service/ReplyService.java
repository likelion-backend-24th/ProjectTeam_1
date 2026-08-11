package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.ReplyCreateRequestDto;
import com.team1.cityfarm.dto.ReplyResponseDto;
import com.team1.cityfarm.entity.Reply;
import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.BoardRepository;
import com.team1.cityfarm.repository.ReplyRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReplyService {
    private final ReplyRepository replyRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    //상세 게시글에 모든 답변 조회
    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getAllReply(Long id){
        //상세 게시글 존재 여부 확인
        if (!boardRepository.existsById(id)) {
            throw new CustomException(CustomError.BOARD_NOT_FOUND);
        }

        List<ReplyResponseDto> loadReplies =
                replyRepository.findAllByBoard_Id(id)
                        .stream()
                        .map(ReplyResponseDto::from)
                        .collect(Collectors.toList());

        return loadReplies;
    }

    //답글 작성
    @Transactional
    public ReplyResponseDto createReply(Long userId, Long boardId, ReplyCreateRequestDto dto) {
        /*컨트롤러에서 받아온 userid,
        board id로 불러온 board,
        dto에서 받아온 내용으로 답변 생성 및 저장*/
        User loadUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        Reply newReply = Reply.builder()
                .user(loadUser)
                .board(boardRepository.findById(boardId).orElseThrow(() -> new CustomException(CustomError.BOARD_NOT_FOUND)))
                .content(dto.getContent())
                .isAdopted(false)
                .build();

        replyRepository.save(newReply);

        //답변 responseDTO return
        return ReplyResponseDto.from(newReply);
    }

    //답글 수정
    @Transactional
    public ReplyResponseDto editReply(Long userId, Long id, ReplyCreateRequestDto dto) {
        User loadUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        /*본인만 수정하도록, user가 일치할 시
        dto로 받아온 내용으로 수정*/
        Reply loadReply = replyRepository.findById(id).orElseThrow(() -> new CustomException(CustomError.REPLY_NOT_FOUND));
        //사용자 본인 아닐시 예외 처리
        if (!loadReply.getUser().getId().equals(loadUser.getId())) throw new CustomException(CustomError.REPLY_NOT_OWNER);

        //더티 체크로 상태변경
        loadReply.updateContent(dto.getContent());
        return ReplyResponseDto.from(loadReply);
    }

    //답글 삭제
    @Transactional
    public void deleteReply(Long userId, Long id) {
        User loadUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        //관리자 경우 답글 id로만 찾아 삭제
        if (loadUser.getRoleType().equals(RoleType.ADMIN)) {
            //삭제, 비었을 경우 예외처리
            replyRepository.delete(replyRepository.findById(id).orElseThrow(() -> new CustomException(CustomError.REPLY_NOT_FOUND)));
            return;
        }
        //사용자 본인이 삭제하도록 유저 id와 답글 id로 조회 후 삭제
        replyRepository.delete(replyRepository.findReplyByIdAndUser_Id(id, loadUser.getId()).orElseThrow(() -> new CustomException(CustomError.REPLY_NOT_FOUND)));
    }
}
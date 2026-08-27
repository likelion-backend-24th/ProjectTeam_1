package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.UserRequestDto;
import com.team1.cityfarm.dto.UserResponseDto;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final SettlementService settlementService;

    //user 전체 조회
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAllUsers(Pageable pageable){
        return userRepository.findAll(pageable).map(UserResponseDto::from);
    }

    @Transactional
    public UserResponseDto modifyUser(Long id, UserRequestDto dto){
        //id로 user 조회
        User loadUser = userRepository.findById(id).orElseThrow(()->new CustomException(CustomError.USER_NOT_FOUND));
        //활동 상태, RoleType(ADMIN,USER) 변경
        loadUser.updateStatus(dto.getStatus());
        loadUser.updateRoleType(dto.getRoleType());
        return UserResponseDto.from(loadUser);
    }

    /**
     * [관리자] 정산 지급 완료 처리
     */
    @Transactional
    public void processSettlementPayout(Long settlementId) {
        settlementService.completeSettlement(settlementId);
    }
}

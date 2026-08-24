package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.RentalRequestDto;
import com.team1.cityfarm.dto.RentalResponseDto;
import com.team1.cityfarm.entity.*;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.FarmRepository;
import com.team1.cityfarm.repository.RentalRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalService {
    private final RentalRepository rentalRepository;
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;

    // 밭 임대 신청 (F163)
    // 결제 기능 미 포함 (신청하면 바로 확정)
    @Transactional
    public RentalResponseDto applyRental(Long farmId, RentalRequestDto rentalRequestDto, Long userId){

        // 토큰 오류 대비 예외처리 (탈퇴 회원, 토큰 위조 시)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new CustomException(CustomError.FARM_NOT_FOUND));

        // 이미 임대 중일 경우 신청 불가
        if (farm.getFarmStatus() != FarmStatus.AVAILABLE){
            throw new CustomException(CustomError.FARM_ALREADY_RENTED);
        }

        Rental rental = Rental.builder()
                .farm(farm)
                .user(user)
                .description(rentalRequestDto.description())
                .rentalStatus(RentalStatus.CONFIRMED)
                .build();

        Rental saveRental = rentalRepository.save(rental);
        // 밭 상태를 임대중으로 변경
        farm.setFarmStatus(FarmStatus.RENTED);

        return RentalResponseDto.from(saveRental);
    }

    // 밭 임대 신청 취소 (F-164)
    @Transactional
    public void cancelRental(Long rentalId, Long userId){

        // 신청자 본인만 취소 가능
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new CustomException(CustomError.RENTAL_NOT_FOUND));

        // 신청한 본인 또는 그 밭을 등록한 호스트만 취소 가능
        boolean isTenant = rental.getUser().getId().equals(userId);
        boolean isHostOwner = rental.getFarm().getUser().getId().equals(userId);
        if (!isTenant && !isHostOwner){
            throw new CustomException(CustomError.RENTAL_NOT_FOUND);
        }

        // 중복 취소 불가
        if(rental.getRentalStatus() == RentalStatus.CANCELLED){
            throw new CustomException(CustomError.RENTAL_ALREADY_CANCELLED);
        }

        // 신청 상태 변경 (완전 삭제 x, 상태만 취소로 남김)
        rental.setRentalStatus(RentalStatus.CANCELLED);

        // 밭 상태 임대 가능으로 변경
        Farm farm = rental.getFarm();
        farm.setFarmStatus(FarmStatus.AVAILABLE);
    }

    // 호스트가 내 밭을 임대한 사람들 목록 조회
    @Transactional(readOnly = true)
    public Page<RentalResponseDto> getHostRentals(Long hostId, Pageable pageable){
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new CustomException(CustomError.NOT_HOST_ROLE));

        if(host.getRoleType() != RoleType.HOST){
            throw new CustomException(CustomError.NOT_HOST_ROLE);
        }

        Page<Rental> rentals = rentalRepository.findByFarm_User_Id(hostId, pageable);
        return rentals.map(RentalResponseDto::from);
    }
}

package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.FarmRequestDto;
import com.team1.cityfarm.dto.FarmResponseDto;
import com.team1.cityfarm.dto.FarmUpdateRequestDto;
import com.team1.cityfarm.entity.Farm;
import com.team1.cityfarm.entity.FarmImage;
import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.FarmImageRepository;
import com.team1.cityfarm.repository.FarmRepository;
import com.team1.cityfarm.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FarmService {
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final FarmImageRepository farmImageRepository;
    private final FileStorageService fileStorageService;

    // 밭 등록 (F-160)
    @Transactional
    public FarmResponseDto createFarm(FarmRequestDto requestDto, List<MultipartFile> images, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomError.USER_NOT_FOUND));

        // HOST만 밭 등록 권한 부여
        if (user.getRoleType() != RoleType.HOST) {
            throw new CustomException(CustomError.AUTH_FORBIDDEN);
        }

        // 사진 특정 수 넘으면 등록 거부
        if (images != null && images.size() > 5) {
            throw new CustomException(CustomError.FARM_IMAGE_LIMIT);
        }

        // Farm DB 저장
        Farm farm = Farm.builder()
                .user(user)
                .title(requestDto.title())
                .location(requestDto.location())
                .locationAddress(requestDto.locationAddress())
                .area(requestDto.area())
                .monthlyRent(requestDto.monthlyRent())
                .rentalMonths(requestDto.rentalMonths())
                .description(requestDto.description())
                .build();

        // 대표 사진 경로 담을 변수(사진 없으면 null로 남은 아직 구현 안했기 때문)
        Farm savedFarm = farmRepository.save(farm);

        // 사진 업로드 로직
        String thumbnailUrl = null;
            // 사진 리스트 로직
        List<String> imageUrls = new ArrayList<>();

        if (images != null && !images.isEmpty()){
            for (int i = 0; i < images.size(); i++){
                String imagesUrl = fileStorageService.store(images.get(i));

                FarmImage farmImage = FarmImage.builder()
                        .farm(savedFarm)
                        .imageUrl(imagesUrl)
                        .build();
                farmImageRepository.save(farmImage);

                imageUrls.add(imagesUrl);

                // 첫 번째 사진을 대표 사진으로 설정
                if(i == 0){
                    thumbnailUrl = imagesUrl;
                }
            }
        }

        return FarmResponseDto.from(savedFarm, thumbnailUrl, imageUrls);
    }

    // 밭 등록 (F-161)
    @Transactional(readOnly = true)
    public Page<FarmResponseDto> getFarms(Pageable pageable) {
        Page<Farm> farms = farmRepository.findAll(pageable);

        return farms.map(farm -> {
            String thumbnailUrl = farmImageRepository.findFirstByFarm_IdOrderByIdAsc(farm.getId())
                    .map(FarmImage::getImageUrl)
                    .orElse(null);
            return FarmResponseDto.from(farm, thumbnailUrl, null);
        });
    }

    // 밭 상세 조회 (F-162)
    @Transactional(readOnly = true)
    public FarmResponseDto getFarm(Long farmId){
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new CustomException(CustomError.FARM_NOT_FOUND));

        List<FarmImage> farmImages = farmImageRepository.findByFarm_IdOrderByIdAsc(farmId);
        List<String> imageUrls = farmImages.stream()
                .map(FarmImage::getImageUrl)
                .toList();
        String thumbnailUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);

        return FarmResponseDto.from(farm, thumbnailUrl, imageUrls);
    }

    // 밭 정보 수정 (F-165)
    @Transactional
    public FarmResponseDto updateFarm(Long farmId, FarmUpdateRequestDto requestDto, List<MultipartFile> images, Long userId){
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new CustomException(CustomError.FARM_NOT_FOUND));

        // 신청자 본인만 글 수정 가능
        if (!farm.getUser().getId().equals(userId)){
            throw new CustomException(CustomError.FARM_NOT_OWNER);
        }

        // 이미지 특정 수 초과 시
        if (images != null && images.size() > 5) {
            throw new CustomException(CustomError.FARM_IMAGE_LIMIT);
        }

        // 기본 정보 수정
        farm.setTitle(requestDto.title());
        farm.setArea(requestDto.area());
        farm.setMonthlyRent(requestDto.monthlyRent());
        farm.setRentalMonths(requestDto.rentalMonths());
        farm.setDescription(requestDto.description());

        // 새 사진이 들어온 경우 기존 사진 전체 교체
        if (images != null && !images.isEmpty()){
            // 기존 사진 삭제
            List<FarmImage> oldImages = farmImageRepository.findByFarm_IdOrderByIdAsc(farmId);
            farmImageRepository.deleteAll(oldImages);

            // 새 사진 저장
            for (MultipartFile image : images){
                String imageUrl = fileStorageService.store(image);
                FarmImage farmImage = FarmImage.builder()
                        .farm(farm)
                        .imageUrl(imageUrl)
                        .build();
                farmImageRepository.save(farmImage);
            }
        }


        // 대표 사진, 전체 사진 목록 재조회
        String thumbnailUrl = farmImageRepository.findFirstByFarm_IdOrderByIdAsc(farmId)
                .map(FarmImage::getImageUrl)
                .orElse(null);
        List<String> imageUrls = farmImageRepository.findByFarm_IdOrderByIdAsc(farmId)
                .stream()
                .map(FarmImage::getImageUrl)
                .toList();

        return  FarmResponseDto.from(farm, thumbnailUrl, imageUrls);


    }

}

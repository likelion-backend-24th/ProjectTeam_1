package com.team1.cityfarm.service;

import com.team1.cityfarm.dto.FarmRequestDto;
import com.team1.cityfarm.dto.FarmResponseDto;
import com.team1.cityfarm.entity.Farm;
import com.team1.cityfarm.entity.FarmImage;
import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.repository.FarmImageRepository;
import com.team1.cityfarm.repository.FarmRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

        if (images != null && images.size() > 5) {
            throw new CustomException(CustomError.FARM_IMAGE_LIMIT);
        }

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

        Farm savedFarm = farmRepository.save(farm);

        String thumbnailUrl = null;
        if (images != null && !images.isEmpty()){
            for (int i = 0; i < images.size(); i++){
                String imagesUrl = fileStorageService.store(images.get(i));

                FarmImage farmImage = FarmImage.builder()
                        .farm(savedFarm)
                        .imageUrl(imagesUrl)
                        .build();
                farmImageRepository.save(farmImage);

                if(i == 0){
                    thumbnailUrl = imagesUrl;
                }
            }
        }

        return FarmResponseDto.from(savedFarm, thumbnailUrl);
    }
}

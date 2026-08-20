package com.team1.cityfarm.service;

import com.team1.cityfarm.entity.BillingKey;
import com.team1.cityfarm.entity.BillingKeyStatus;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import com.team1.cityfarm.portone.PortoneBillingKeyResponseDto;
import com.team1.cityfarm.portone.PortonePaymentClient;
import com.team1.cityfarm.repository.BillingKeyRepository;
import com.team1.cityfarm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingKeyServiceTest {

    @Mock private BillingKeyRepository billingKeyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PortonePaymentClient portonePaymentClient;
    @Mock private SubscriptionService subscriptionService;

    private BillingKeyService service;

    @BeforeEach
    void setUp() {
        service = new BillingKeyService(billingKeyRepository, userRepository, portonePaymentClient, subscriptionService);
    }

    private User user(Long id) {
        return User.builder().id(id).email("u@test.com").password("pw").nickname("nick").build();
    }

    private PortoneBillingKeyResponseDto portOneKey(String status, String billingKey) {
        PortoneBillingKeyResponseDto dto = new PortoneBillingKeyResponseDto();
        ReflectionTestUtils.setField(dto, "status", status);
        ReflectionTestUtils.setField(dto, "billingKey", billingKey);
        return dto;
    }

    // ===== getMyActiveBillingKey =====

    @Test
    void 활성키가_없으면_조회시_NOT_FOUND() {
        when(billingKeyRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyActiveBillingKey(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.BILLING_KEY_NOT_FOUND));
    }

    @Test
    void 활성키가_있으면_DTO로_반환() {
        BillingKey key = BillingKey.builder().id(1L).billingKeyEncrypted("bk_1").status(BillingKeyStatus.ACTIVE).build();
        when(billingKeyRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE)).thenReturn(Optional.of(key));

        var result = service.getMyActiveBillingKey(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(BillingKeyStatus.ACTIVE);
    }

    // ===== createIssuanceIntent =====

    @Test
    void 유저가_없으면_인텐트_생성_실패() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createIssuanceIntent(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.USER_NOT_FOUND));
    }

    @Test
    void 인텐트_생성시_issueId와_customerId가_채워진다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        var result = service.createIssuanceIntent(1L);

        assertThat(result.getIssueId()).startsWith("issue_");
        assertThat(result.getCustomerId()).isEqualTo("user_1");
    }

    private void stubSave() {
        when(billingKeyRepository.save(any(BillingKey.class))).thenAnswer(inv -> {
            BillingKey k = inv.getArgument(0);
            ReflectionTestUtils.setField(k, "id", 100L);
            return k;
        });
    }

    // ===== confirmIssuance =====

    @Test
    void PortOne_상태가_ISSUED가_아니면_검증실패() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(portonePaymentClient.getBillingKeyDetails("bk_new")).thenReturn(portOneKey("DELETE_READY", "bk_new"));

        assertThatThrownBy(() -> service.confirmIssuance("issue_1", "bk_new", 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.BILLING_KEY_VERIFY_FAILED));

        verify(billingKeyRepository, never()).save(any());
    }

    @Test
    void 요청한_빌링키와_PortOne_응답값이_다르면_검증실패() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(portonePaymentClient.getBillingKeyDetails("bk_new")).thenReturn(portOneKey("ISSUED", "bk_other"));

        assertThatThrownBy(() -> service.confirmIssuance("issue_1", "bk_new", 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.BILLING_KEY_VERIFY_FAILED));
    }

    @Test
    void 기존_키가_없으면_그냥_저장만_하고_마이그레이션_해지는_안_한다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(portonePaymentClient.getBillingKeyDetails("bk_new")).thenReturn(portOneKey("ISSUED", "bk_new"));
        when(billingKeyRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE)).thenReturn(Optional.empty());
        stubSave();

        service.confirmIssuance("issue_1", "bk_new", 1L);

        verify(billingKeyRepository).save(any(BillingKey.class));
        verifyNoInteractions(subscriptionService);
        verify(portonePaymentClient, never()).deleteBillingKey(any(), any());
    }

    @Test
    void 기존_키가_있으면_마이그레이션후_이전키를_해지하고_REVOKED로_남긴다() {
        User u = user(1L);
        BillingKey oldKey = BillingKey.builder().id(5L).user(u)
                .billingKeyEncrypted("bk_old").status(BillingKeyStatus.ACTIVE).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(portonePaymentClient.getBillingKeyDetails("bk_new")).thenReturn(portOneKey("ISSUED", "bk_new"));
        when(billingKeyRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE)).thenReturn(Optional.of(oldKey));
        stubSave();

        service.confirmIssuance("issue_1", "bk_new", 1L);

        verify(subscriptionService).migrateActiveScheduleToNewBillingKey(eq(1L), any(BillingKey.class));
        verify(portonePaymentClient).deleteBillingKey(eq("bk_old"), any());
        assertThat(oldKey.getStatus()).isEqualTo(BillingKeyStatus.REVOKED);
    }

    @Test
    void 마이그레이션이_실패하면_이전키_해지_없이_예외가_그대로_전파된다() {
        User u = user(1L);
        BillingKey oldKey = BillingKey.builder().id(5L).user(u)
                .billingKeyEncrypted("bk_old").status(BillingKeyStatus.ACTIVE).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(portonePaymentClient.getBillingKeyDetails("bk_new")).thenReturn(portOneKey("ISSUED", "bk_new"));
        when(billingKeyRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE)).thenReturn(Optional.of(oldKey));
        stubSave();
        doThrow(new CustomException(CustomError.PORTONE_BILLING_FAILED))
                .when(subscriptionService).migrateActiveScheduleToNewBillingKey(eq(1L), any());

        assertThatThrownBy(() -> service.confirmIssuance("issue_1", "bk_new", 1L))
                .isInstanceOf(CustomException.class);

        verify(portonePaymentClient, never()).deleteBillingKey(any(), any());
        assertThat(oldKey.getStatus()).isEqualTo(BillingKeyStatus.ACTIVE); // REVOKED로 안 바뀜
    }

    @Test
    void 이전키_PortOne_해지가_실패해도_REVOKED_처리는_계속된다() {
        User u = user(1L);
        BillingKey oldKey = BillingKey.builder().id(5L).user(u)
                .billingKeyEncrypted("bk_old").status(BillingKeyStatus.ACTIVE).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(portonePaymentClient.getBillingKeyDetails("bk_new")).thenReturn(portOneKey("ISSUED", "bk_new"));
        when(billingKeyRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE)).thenReturn(Optional.of(oldKey));
        stubSave();
        doThrow(new CustomException(CustomError.PORTONE_CANCEL_FAILED))
                .when(portonePaymentClient).deleteBillingKey(any(), any());

        service.confirmIssuance("issue_1", "bk_new", 1L); // 예외 없이 끝나야 함

        assertThat(oldKey.getStatus()).isEqualTo(BillingKeyStatus.REVOKED);
    }

    // ===== revokeMyBillingKey =====

    @Test
    void 활성키가_없으면_NOT_FOUND() {
        when(billingKeyRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revokeMyBillingKey(1L, "사유"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.BILLING_KEY_NOT_FOUND));
    }

    @Test
    void 예약이_걸려있으면_삭제_거부() {
        BillingKey key = BillingKey.builder().id(1L).billingKeyEncrypted("bk_1").status(BillingKeyStatus.ACTIVE).build();
        when(billingKeyRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE)).thenReturn(Optional.of(key));
        when(subscriptionService.hasActiveScheduledPayment(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.revokeMyBillingKey(1L, "사유"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCustomError())
                        .isEqualTo(CustomError.BILLING_KEY_IN_USE));

        verify(portonePaymentClient, never()).deleteBillingKey(any(), any());
    }

    @Test
    void 정상_삭제시_PortOne_호출과_REVOKED_전환() {
        BillingKey key = BillingKey.builder().id(1L).billingKeyEncrypted("bk_1").status(BillingKeyStatus.ACTIVE).build();
        when(billingKeyRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE)).thenReturn(Optional.of(key));
        when(subscriptionService.hasActiveScheduledPayment(1L)).thenReturn(false);

        service.revokeMyBillingKey(1L, "사용자 사유");

        verify(portonePaymentClient).deleteBillingKey("bk_1", "사용자 사유");
        assertThat(key.getStatus()).isEqualTo(BillingKeyStatus.REVOKED);
    }

    @Test
    void 사유가_없으면_기본_사유가_쓰인다() {
        BillingKey key = BillingKey.builder().id(1L).billingKeyEncrypted("bk_1").status(BillingKeyStatus.ACTIVE).build();
        when(billingKeyRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE)).thenReturn(Optional.of(key));
        when(subscriptionService.hasActiveScheduledPayment(1L)).thenReturn(false);

        service.revokeMyBillingKey(1L, null);

        verify(portonePaymentClient).deleteBillingKey(eq("bk_1"), eq("사용자 요청에 의한 카드 삭제"));
    }
}

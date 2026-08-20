# 트러블슈팅: PortOne DELETE 요청의 쿼리파라미터 URI 템플릿 충돌

## 왜 발생했는가?

`cancelSchedule`, `cancelScheduleByBillingKey`(`PortonePaymentClient`)는 PortOne V2 API 스펙상
DELETE 요청의 "바디"에 해당하는 내용을 `requestBody`라는 쿼리파라미터에 JSON 문자열째로 실어
보내야 한다. 이를 위해 아래처럼 구현했었다.

```java
String requestBody = toJson(Map.of("scheduleIds", List.of(portoneScheduleId)));
restClient.method(DELETE)
        .uri(uriBuilder -> uriBuilder
                .path("/payment-schedules")
                .queryParam("requestBody", requestBody)   // 문제 지점
                .build())
```

Spring의 `UriComponentsBuilder`는 URI 문자열 어디에 있든(쿼리파라미터 값 포함) `{...}`를
URI 템플릿 변수로 해석한다. `requestBody`는 JSON이라 `{`/`}`를 포함하는데, 이 문자열을
`queryParam`에 직접 꽂아 넣으면 빌더가 `{"scheduleIds":...}`를 "치환해야 할 템플릿 변수"로
오인한다. `.build()`에 대응하는 값을 안 넘겨줬으니 즉시
`IllegalArgumentException: Not enough variable values available to expand '"scheduleIds"'`가
발생하고, 이게 catch 블록에서 `PORTONE_CANCEL_FAILED`로 감싸져서 호출할 때마다 무조건
실패했다. `PortonePaymentClientTest`에서 `MockRestServiceServer`로 실제 나가는 요청을
검증하다가 발견함 — `deleteBillingKey`는 쿼리값이 순수 텍스트(`reason=...`)라 `{`/`}`가
없어서 우연히 이 문제를 안 밟았을 뿐, 같은 패턴이라 잠재적으로 동일한 위험을 안고 있었다.

## 어떻게 해결했는가?

값 자체를 URI 문자열에 직접 끼워 넣지 않고, **템플릿 변수로 선언한 뒤 `.build(값)`으로
치환**하도록 바꿨다. 이렇게 하면 `{requestBody}`는 빌더가 해석할 템플릿이지만, 그 자리에
채워지는 실제 값(JSON 문자열)은 이미 완성된 문자열로 취급되어 안에 있는 `{`/`}`가 재귀적으로
다시 해석되지 않는다(치환 시점에 URL 인코딩만 적용됨).

```java
restClient.method(DELETE)
        .uri(uriBuilder -> uriBuilder
                .path("/payment-schedules")
                .queryParam("requestBody", "{requestBody}")
                .build(requestBody))
```

`cancelSchedule`, `cancelScheduleByBillingKey`에 적용했고, 같은 위험을 안고 있던
`deleteBillingKey`의 `reason` 파라미터도 동일 패턴으로 미리 방어 처리했다
(`.queryParam("reason", "{reason}").build(billingKey, reason)` — `.build()` varargs는
URI에 나타나는 템플릿 변수 순서대로 채워지므로 `billingKey`, `reason` 순서를 지켰다).

`PortonePaymentClientTest`로 수정 전/후 재현·검증 완료(8개 테스트 전부 통과).

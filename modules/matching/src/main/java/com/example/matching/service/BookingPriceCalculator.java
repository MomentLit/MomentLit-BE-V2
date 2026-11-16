package com.example.matching.service;

import com.example.matching.global.exception.BadRequestException;
import com.example.space.entity.UsageUnit;

/**
 * 예약 요청 금액 계산. 프론트가 보낸 금액은 믿지 않고 공간 요금으로 서버에서 다시 계산한다.
 * 프론트 `calculateBookingPrice`(entities/space)와 같은 식을 써야 화면에 보인 금액과 저장 금액이 같다.
 *
 * <ul>
 *   <li>가격 문의 공간: 0원</li>
 *   <li>DAILY: 1일 요금 + 초과 인원 × 1인당 추가 요금 (시간과 무관)</li>
 *   <li>HOURLY(또는 이용 단위 미정): (시간당 요금 + 초과 인원 × 1인당 추가 요금) × 이용 시간, 원 단위 반올림</li>
 * </ul>
 * 초과 인원 = max(0, 인원 − 기준 인원). 인원이나 초과 요금 설정이 없으면 0명으로 본다.
 */
public final class BookingPriceCalculator {

    private BookingPriceCalculator() {
    }

    public static int calculate(
            Integer pricePerHour,
            boolean priceInquiry,
            UsageUnit usageUnit,
            Integer baseGuestCount,
            Integer extraGuestFee,
            Integer guestCount,
            long durationMinutes
    ) {
        if (priceInquiry || pricePerHour == null) {
            return 0;
        }

        int extraGuests = baseGuestCount != null && extraGuestFee != null && guestCount != null
                ? Math.max(0, guestCount - baseGuestCount)
                : 0;
        long unitPrice = pricePerHour + (long) extraGuests * (extraGuestFee != null ? extraGuestFee : 0);

        long total = usageUnit == UsageUnit.DAILY
                ? unitPrice
                : Math.round(unitPrice * durationMinutes / 60.0);

        if (total > Integer.MAX_VALUE) {
            throw new BadRequestException("예약 금액이 너무 큽니다.");
        }
        return (int) total;
    }
}

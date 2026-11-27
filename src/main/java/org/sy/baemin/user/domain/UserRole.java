package org.sy.baemin.user.domain;

public enum UserRole {
    CUSTOMER,   // 고객: 주문, 결제
    OWNER,      // 사장님: 가게&메뉴 관리, 주문 수락
    RIDER       // 라이더: 배차 수락, 배달
}

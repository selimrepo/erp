package com.erplancers.erp.sales.pricing.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Coupon extends EntityBase {
    String couponCode;
    String couponName;
    String couponDescription;
    String couponType;
    Float discount;
    Long couponAmount;
    Long couponPeriod;
    LocalDateTime couponPeriodStart;
    LocalDateTime couponPeriodEnd;
    
}

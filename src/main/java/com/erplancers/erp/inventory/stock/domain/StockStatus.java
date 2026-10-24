package com.erplancers.erp.inventory.stock.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

public enum StockStatus {
    IN_STOCK,
    OUT_OF_STOCK,
    BACKORDER
}
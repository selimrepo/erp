package com.erplancers.erp.crm.campaign.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class CampaignLog extends EntityBase {
    String trackerKey;
    String trackerName;
    Boolean trackerStatus;
    Long hits;
    String moreInformation;

    
}

package com.erplancers.erp.crm.campaign.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.leads.domain.Lead;
import com.erplancers.erp.crm.opportunity.domain.Opportunity;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.communication.email.domain.EmailMarketing;
import com.erplancers.erp.projects.domain.Project;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Campaign extends EntityBase {
    String campaignName;
    String trackerKey;
    Integer trackerCount;
    String trackerText;
    LocalDate trackerDate;
    LocalDateTime trackerTime;
    LocalDateTime startTime;
    LocalDateTime endTime;
    String note;
    String description;
    Integer impressions;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Currency currency;
    Long budget;
    Long expectedCost;
    Long actualCost;
    Long expectedRevenue;
    Long actualRevenue;
    String campaignType;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<CampaignLog> campaigns;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    EmailMarketing emailMarketings;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Lead> leads;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Project project;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Opportunity> opportunities;
    
}

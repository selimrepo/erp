package com.erplancers.erp.content.folder.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.communication.email.domain.*;
import com.erplancers.erp.communication.message.domain.Message;
import com.erplancers.erp.content.document.domain.Document;
import com.erplancers.erp.content.document.domain.DocumentLink;
import com.erplancers.erp.content.document.domain.DocumentRevision;
import com.erplancers.erp.content.knowledge.domain.Help;
import com.erplancers.erp.content.knowledge.domain.HelpCategory;
import com.erplancers.erp.content.knowledge.domain.Knowledgebase;
import com.erplancers.erp.content.knowledge.domain.KnowledgebaseCategory;
import com.erplancers.erp.crm.activity.domain.Call;
import com.erplancers.erp.crm.activity.domain.Meeting;
import com.erplancers.erp.crm.activity.domain.Note;
import com.erplancers.erp.crm.campaign.domain.Campaign;
import com.erplancers.erp.crm.campaign.domain.CampaignLog;
import com.erplancers.erp.crm.contact.domain.*;
import com.erplancers.erp.crm.contract.domain.Contract;
import com.erplancers.erp.crm.customer.domain.Customer;
import com.erplancers.erp.crm.customer.domain.CustomerGroup;
import com.erplancers.erp.crm.customer.domain.CustomerTransaction;
import com.erplancers.erp.crm.leads.domain.Lead;
import com.erplancers.erp.crm.leads.domain.Prospect;
import com.erplancers.erp.crm.leads.domain.ProspectList;
import com.erplancers.erp.crm.opportunity.domain.Opportunity;
import com.erplancers.erp.events.domain.Event;
import com.erplancers.erp.events.domain.EventLocation;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.finance.domain.Invoice;
import com.erplancers.erp.finance.domain.PaymentTransaction;
import com.erplancers.erp.inventory.product.domain.*;
import com.erplancers.erp.inventory.stock.domain.ProductStockLocation;
import com.erplancers.erp.inventory.stock.domain.StockCategory;
import com.erplancers.erp.inventory.stock.domain.StockItem;
import com.erplancers.erp.inventory.stock.domain.StockStatus;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import com.erplancers.erp.platform.classification.domain.Category;
import com.erplancers.erp.platform.notification.domain.Alert;
import com.erplancers.erp.platform.notification.domain.Announcement;
import com.erplancers.erp.platform.workflow.domain.Action;
import com.erplancers.erp.platform.workflow.domain.Condition;
import com.erplancers.erp.procurement.domain.Purchase;
import com.erplancers.erp.procurement.domain.PurchaseDetail;
import com.erplancers.erp.procurement.domain.Supplier;
import com.erplancers.erp.projects.domain.BusinessHour;
import com.erplancers.erp.projects.domain.Project;
import com.erplancers.erp.projects.domain.ProjectTask;
import com.erplancers.erp.sales.domain.Sale;
import com.erplancers.erp.sales.order.domain.Order;
import com.erplancers.erp.sales.order.domain.OrderData;
import com.erplancers.erp.sales.order.domain.OrderOption;
import com.erplancers.erp.sales.pricing.domain.Coupon;
import com.erplancers.erp.sales.pricing.domain.ProductDiscount;
import com.erplancers.erp.sales.quote.domain.Quote;
import com.erplancers.erp.support.domain.Case;
import com.erplancers.erp.support.domain.CaseEvent;
import com.erplancers.erp.support.domain.CaseUpdate;
import com.erplancers.erp.survey.domain.*;
import com.erplancers.erp.tax.domain.TaxClass;
import com.erplancers.erp.tax.domain.TaxRate;
import com.erplancers.erp.tax.domain.Zone;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Folder extends EntityBase {
    String name;
    LocalDateTime created;
    LocalDateTime updated;
    //include all entities OR folders recursive !!
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Action> actions;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Address> addresses;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Alert> alerts;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Announcement> announcements;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<BusinessHour> businessHours;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Call> calls;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<CampaignLog> campaignLogs;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Campaign> campaigns;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<CaseEvent> caseEvents;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<CaseUpdate> caseUpdates;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Category> categories;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<City> cities;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Condition> conditions;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Contact> contacts;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Contract> contracts;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Country> countries;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Coupon> coupons;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Currency> currencies;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Customer> customers;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<CustomerGroup> customerGroups;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<CustomerTransaction> customerTransactions;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Document> documents;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<DocumentLink> documentLinks;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<DocumentRevision> documentRevisions;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Email> emails;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<EmailAddress> emailAddresses;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<EmailMarketing> emailMarketings;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<EmailTask> emailTasks;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<EmailText> emailTexts;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Case> erpCases;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Order> erpOrders;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Event> events;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<EventLocation> eventLocations;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Favorite> favorites;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Folder> folders;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<FolderSubscription> folderSubscriptions;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Help> helpSets;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<HelpCategory> helpCategories;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<InboundMail> inboundMails;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Invoice> invoices;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Knowledgebase> knowledgeBases;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<KnowledgebaseCategory> knowledgebaseCategories;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Lead> leads;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Location> locations;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Manufacturer> manufacturers;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Meeting> meetings;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Message> messages;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Note> notes;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Opportunity> opportunities;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<OrderData> orderDatas;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<OrderOption> orderOptions;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<OutboundMail> outboundMails;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<PaymentTransaction> paymentTransactions;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Product> products;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProductDiscount> productDiscounts;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProductOption> productOptions;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProductOptionValue> productOptionValues;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProductQuote> productQuotes;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProductsFeatured> productsFeatureds;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProductStockLocation> productStockLocations;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Project> projects;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProjectTask> projectTasks;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Prospect> prospects;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProspectList> prospectLists;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Purchase> purchases;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<PurchaseDetail> purchaseDetails;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Quote> quotes;

    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Sale> sales;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<StockCategory> stockCategory;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<StockItem> stockItems;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Store> stores;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Supplier> suppliers;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Survey> surveys;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<SurveyQuestion> surveyQuestions;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<SurveyQuestionOption> surveyQuestionOptions;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<SurveyQuestionResponse> surveyQuestionResponses;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<SurveyResponse> surveyResponses;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<TaxClass> taxClasses;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<TaxRate> taxRates;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Zone> zones;

}

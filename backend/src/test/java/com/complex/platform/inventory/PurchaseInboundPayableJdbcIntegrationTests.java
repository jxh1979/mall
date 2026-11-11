package com.complex.platform.inventory;

// 文件版本：V0.08

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.complex.platform.access.AccessActor;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.finance.dto.ArApAdjustmentRequest;
import com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest;
import com.complex.platform.governance.review.*;
import com.complex.platform.inventory.dto.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** FS-008/009/010：真实合同审批、采购审批、库存、应付与现金记录的交叉验证。 */
@SpringBootTest(properties={
 "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
 "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
 "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
@AutoConfigureMockMvc
class PurchaseInboundPayableJdbcIntegrationTests {
 @Autowired PurchaseWorkflowService workflow;
 @Autowired InventoryController inventoryController;
 @Autowired PurchasePrepaymentService prepayments;
 @Autowired PurchaseRequestService purchases;
 @Autowired SupplyContractService contracts;
 @Autowired FinanceService financeService;
 @Autowired com.complex.platform.finance.FinanceRepository financeRepository;
 @Autowired SupervisorReviewService reviews;
 @Autowired EvidenceEventService evidence;
 @Autowired JdbcTemplate jdbc;
 @Autowired MockMvc mockMvc;
 @Autowired com.complex.platform.analytics.AnalyticsRepository analytics;
 String suffix,project,merchant,item,supplier,entity,warehouse,location,requestor,manager,finance,business,legal,role;

 @BeforeEach void seed(){
  suffix=UUID.randomUUID().toString().substring(0,8);project="pc_project_"+suffix;
  merchant="pc_merchant_"+suffix;item="pc_item_"+suffix;supplier="pc_supplier_"+suffix;entity="pc_entity_"+suffix;
  warehouse="pc_wh_"+suffix;location="pc_loc_"+suffix;role="pc_role_"+suffix;
  requestor="pc_requestor_"+suffix;manager="pc_manager_"+suffix;finance="pc_finance_"+suffix;
  business="pc_business_"+suffix;legal="pc_legal_"+suffix;
  jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','采购回归项目','active',now(),now())",project);
  jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'采购主体','project_company','active',now(),now())",entity);
  jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)",entity,project);
  jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,'采购商户','retail',?,?,'active',now(),now())",merchant,entity,project);
  jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())","pc_admission_"+suffix,merchant,entity,entity);
  jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'采购测试耗材','consumable','包',0,'active',?,now(),now())",item,merchant);
  jdbc.update("INSERT INTO inventory_suppliers(id,supplier_name,supplier_type,status,merchant_id,created_at,updated_at) VALUES(?,'采购测试供应商','general','active',?,now(),now())",supplier,merchant);
  jdbc.update("INSERT INTO supplier_qualifications(id,supplier_id,qualification_type,valid_from,valid_to,status,merchant_id) VALUES(?,?,'business_license',current_date-1,current_date+365,'active',?)","pc_qual_"+suffix,supplier,merchant);
  jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'main','本项目仓库','active',?)",warehouse,project,merchant);
  jdbc.update("INSERT INTO warehouse_locations(id,warehouse_id) VALUES(?,?)",location,warehouse);
  jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'采购测试显式岗位','project','active',now())",role,role);
  jdbc.update("""
    INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(
    'inventory.purchase_request.view','inventory.purchase_request.manage','inventory.purchase.approve',
    'inventory.supply_contract.manage','inventory.cost.view','finance.ar_ap.manage','finance.ar_ap.view','finance.payment.register',
    'finance.daily_close.lock','finance.locked_backfill.request','finance.locked_backfill.approve',
    'evidence.attachment.create','evidence.timeline.view','governance.supervisor_review.view','governance.supervisor_review.approve','governance.supervisor_review.evaluate')
    """,role);
  for(String user:List.of(requestor,manager,finance,business,legal)){
   jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",user,user,project,merchant);
   jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)",user,role);
   jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)",user,project);
  }
  jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)",requestor,manager);
  for(var line:contractLines())jdbc.update("INSERT INTO approval_lines(line_code,line_name,min_start_rank,entry_user_id,status) VALUES(?,?,1,?,'active') ON CONFLICT(line_code) DO UPDATE SET entry_user_id=EXCLUDED.entry_user_id,status='active'",line.lineCode(),line.lineCode(),line.finalApproverId());
  act(requestor);
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}

 @Test void contractAndPurchaseRequireEveryRealApprovalBeforeOrdering(){
  String contract=draftContract("credit_term","each_receipt",30);
  contracts.approveSupplyContract(contract,new SupplyContractApproveRequest(key(),"approved","合同原件核对",manager,contractLines(),null));
  String review=string("SELECT review_id FROM supply_contracts WHERE id=?",contract);
  approve(review,business);approve(review,finance);act(requestor);
  assertThat(string("SELECT approval_status FROM supply_contracts WHERE id=?",contract)).isEqualTo("draft");
  approve(review,legal);approve(review,manager);act(requestor);
  String purchase=createPurchase(contract,100);
  submit(purchase,contract);
  assertThat(string("SELECT status FROM inventory_purchase_requests WHERE id=?",purchase)).isEqualTo("submitted");
  assertThatThrownBy(()->purchases.order(purchase,new PurchaseRequestTransitionRequest(key(),"正式下单"))).isInstanceOf(ApiException.class);
  approve(string("SELECT review_id FROM inventory_purchase_requests WHERE id=?",purchase),manager);act(requestor);
  purchases.order(purchase,new PurchaseRequestTransitionRequest(key(),"正式下单"));
  assertThat(string("SELECT status FROM inventory_purchase_requests WHERE id=?",purchase)).isEqualTo("ordered");
 }

 @Test void partialAcceptanceRejectsOnlyBadQuantityAndApprovedTerminationClosesOriginalQuantity(){
  String purchase=ordered("credit_term","all_fulfilled",30,100);
  receive(purchase,80,70,10,1500L);
  assertThat(number("SELECT quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?",item,location)).isEqualTo(70);
  var first=receipt(purchase);String payable=(String)first.get("payable_id");
  assertThat(number("SELECT amount_cent FROM ar_ap_items WHERE id=?",payable)).isEqualTo(105000);
  assertThat(jdbc.queryForMap("SELECT due_date FROM ar_ap_items WHERE id=?",payable).get("due_date")).isNull();
  var replenishment=workflow.resolveQuantity(purchase,new PurchaseWorkflowRequests.Resolution(key(),"replenishment",30,today().plusDays(3),"追补未交及拒收",proof(purchase),null,List.of(),null));
  assertThat(replenishment.get("status")).isEqualTo("recorded");
  var resolution=workflow.resolveQuantity(purchase,new PurchaseWorkflowRequests.Resolution(key(),"terminate",30,null,"供应商不能补齐，申请终止",proof(purchase),manager,List.of(),null));
  var original=workflow.readMap(reviews.findReview((String)resolution.get("review_id")).orElseThrow().reason());
  assertThat(original).containsKeys("purchase","contract_snapshot","receipts","remaining_quantity");
  assertThat(((Number)original.get("remaining_quantity")).intValue()).isEqualTo(30);
  assertThat((List<?>)original.get("receipts")).hasSize(1);
  assertThat(number("SELECT terminated_quantity FROM inventory_purchase_requests WHERE id=?",purchase)).isZero();
  approve((String)resolution.get("review_id"),manager);act(requestor);
  assertThat(workflow.detail(purchase)).containsEntry("accepted_quantity",70).containsEntry("terminated_quantity",30).containsEntry("remaining_quantity",0);
  assertThat(number("SELECT quantity FROM inventory_purchase_requests WHERE id=?",purchase)).isEqualTo(100);
  assertThat(string("SELECT status FROM inventory_purchase_requests WHERE id=?",purchase)).isEqualTo("received");
  assertThat(jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE id=?",LocalDate.class,payable)).isEqualTo(today().plusDays(30));
 }

 /** ROOT-04-R2（BSM「逐次记录采购、实到、合格接收和拒收数量」；裁决 M01「采购到货按商品及单位分别显示」）：采购收货唯一写点写出的这一次到货，就是经营看板「采购到货」按商品及单位分列的数量。 */
 @Test void realReceiveIsWhatPurchaseReceivedMetricGroupsByItemAndUnit(){
  String purchase=ordered("credit_term","each_receipt",30,100);
  Instant from=Instant.now().minusSeconds(1);
  receive(purchase,12,10,2,1500L);
  var groups=analytics.purchaseReceivedGroups(from,Instant.now().plusSeconds(1),List.of(project),null);
  assertThat(groups).hasSize(1);
  assertThat(groups.getFirst()).containsEntry("itemId",item).containsEntry("unit","包").containsEntry("acceptedQuantity",10L)
   .containsEntry("arrivedQuantity",12L).containsEntry("rejectedQuantity",2L).containsEntry("receiptCount",1L);
 }

 @Test void receiveCreatesSupplierPayableAndReplayDoesNotDuplicate(){
  String purchase=ordered("credit_term","each_receipt",30,10);
  var request=receiveRequest(purchase,10,10,0,1500L);
  purchases.receive(purchase,request);purchases.receive(purchase,request);
  var receipt=receipt(purchase);String payable=(String)receipt.get("payable_id");
  assertThat(number("SELECT count(*) FROM purchase_receipts WHERE purchase_id=?",purchase)).isEqualTo(1);
  assertThat(number("SELECT amount_cent FROM ar_ap_items WHERE id=?",payable)).isEqualTo(15000);
  assertThat(jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE id=?",LocalDate.class,payable)).isEqualTo(today().plusDays(30));
  assertThat(number("SELECT count(*) FROM inventory_stock_movements WHERE business_type='purchase_request' AND business_id=?",purchase)).isEqualTo(1);
  assertThat(number("SELECT quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?",item,location)).isEqualTo(10);
 }

 @Test void monthlySettlementUsesLockedContractMonthEndAndDays(){
  String purchase=ordered("monthly","month_end",5,10);receive(purchase,10,10,0,200L);
  assertThat(jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE id=?",LocalDate.class,receipt(purchase).get("payable_id")))
   .isEqualTo(today().withDayOfMonth(today().lengthOfMonth()).plusDays(5));
 }

 @Test void receiveWithoutLockedContractMustNotInventZeroDayPayable(){
  String contract=approvedContract("credit_term","each_receipt",30);String purchase=createPurchase(contract,10);
  // 复现到货事实存在而合同快照缺失，不让状态准入提前遮住该校验。
  jdbc.update("UPDATE inventory_purchase_requests SET status='arrived' WHERE id=?",purchase);
  assertThatThrownBy(()->purchases.receive(purchase,receiveRequest(purchase,10,10,0,100L)))
   .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PURCHASE_CONTRACT_REQUIRED"));
  assertThat(number("SELECT count(*) FROM purchase_receipts WHERE purchase_id=?",purchase)).isZero();
 }

 @Test void missingActualPriceNeverCreatesStock(){
  String purchase=ordered("credit_term","each_receipt",30,10);
  assertThatThrownBy(()->purchases.receive(purchase,receiveRequest(purchase,10,10,0,null)))
   .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PURCHASE_WORKFLOW_INVALID"));
  assertThat(number("SELECT count(*) FROM purchase_receipts WHERE purchase_id=?",purchase)).isZero();
 }
 @Test void crossProjectWarehouseNeverCreatesStock(){
  String purchase=ordered("credit_term","each_receipt",30,10);
  String other="pc_other_"+suffix;
  jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','他项目','active',now(),now())",other);
  jdbc.update("UPDATE warehouses SET project_id=? WHERE id=?",other,warehouse);
  assertThatThrownBy(()->receive(purchase,10,10,0,100L))
   .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PURCHASE_WORKFLOW_INVALID"));
  assertThat(number("SELECT count(*) FROM purchase_receipts WHERE purchase_id=?",purchase)).isZero();
 }

 /** 第十八批 M 段第 119 条 119-R1（BSM:1936「……库房、库位……都要先在基础管理中定义清楚。」）：验收只收接收仓库下已建档的库位——未建档、或建在别的库房下的库位都拦住，不入库。 */
 @Test void receiveRejectsUnregisteredLocation(){
  receiveIsRejectedAt("pc_loc_new_"+suffix);
 }
 @Test void receiveRejectsLocationRegisteredUnderAnotherWarehouse(){
  String elsewhere="pc_loc_other_"+suffix,otherWarehouse="pc_wh_other_"+suffix;
  jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'main','另一仓库','active',?)",otherWarehouse,project,merchant);
  jdbc.update("INSERT INTO warehouse_locations(id,warehouse_id) VALUES(?,?)",elsewhere,otherWarehouse);
  receiveIsRejectedAt(elsewhere);
 }
 void receiveIsRejectedAt(String loc){
  String purchase=ordered("credit_term","each_receipt",30,10);
  var request=new PurchaseRequestReceiveRequest(key(),loc,warehouse,proof(purchase),"实际逐次验收",1500L,10,10,0,Instant.now());
  assertThatThrownBy(()->purchases.receive(purchase,request))
   .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.getMessage()).contains("已建档的库位"));
  assertThat(number("SELECT count(*) FROM inventory_stock_balances WHERE location_id=?",loc)).isZero();
  assertThat(number("SELECT accepted_quantity FROM inventory_purchase_requests WHERE id=?",purchase)).isZero();
 }

 @Test void prepaidAllocationUsesActualPaymentWithoutSecondCashRow(){
  String purchase=ordered("prepaid","ordered",2,10);
  String prepaid=string("SELECT id FROM ar_ap_items WHERE source_type='purchase_prepayment' AND source_id=?",purchase);
  act(finance);financeService.registerArApPayment(prepaid,payment(key(),1000L));
  String payment=string("SELECT id FROM ar_ap_payments WHERE ar_ap_item_id=?",prepaid);
  act(requestor);receive(purchase,10,10,0,200L);String payable=(String)receipt(purchase).get("payable_id");
  act(finance);
  var allocation=new PurchaseWorkflowRequests.Allocation(key(),payment,payable,1000L,proof(purchase));
  workflow.allocate(purchase,allocation);workflow.allocate(purchase,allocation);
  assertThat(number("SELECT count(*) FROM ar_ap_payments WHERE ar_ap_item_id IN (?,?)",prepaid,payable)).isEqualTo(1);
  assertThat(number("SELECT paid_amount_cent FROM ar_ap_items WHERE id=?",payable)).isEqualTo(1000);
  assertThat(number("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id=?",payable)).isEqualTo(1000);
  assertThatThrownBy(()->workflow.allocate(purchase,new PurchaseWorkflowRequests.Allocation(key(),payment,payable,1L,proof(purchase)))).isInstanceOf(ApiException.class);
 }

 @Test void consignmentRequiresIndependentConfirmedSaleAndDoesNotInvoiceReceipt(){
  String purchase=ordered("consignment","confirmed_sale",10,10);receive(purchase,10,10,0,200L);
  var source=receipt(purchase);assertThat(source.get("payable_id")).isNull();
  var sale=workflow.sale(purchase,new PurchaseWorkflowRequests.Sale(key(),(String)source.get("id"),3,"已核验门店售出清单","SALES-"+key(),Instant.now(),proof(purchase)));
  String saleId=(String)sale.get("id");
  assertThat(number("SELECT count(*) FROM ar_ap_items WHERE source_type='purchase_consignment' AND source_id=?",saleId)).isZero();
  var confirm=new PurchaseWorkflowRequests.SaleConfirmation(key(),"confirmed","核对合同指定售出来源",proof(purchase));
  assertThatThrownBy(()->workflow.confirmSale(saleId,confirm)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("SELF_APPROVAL_FORBIDDEN"));
  act(finance);workflow.confirmSale(saleId,new PurchaseWorkflowRequests.SaleConfirmation(key(),"confirmed","独立核实售出来源",proof(purchase)));
  assertThat(number("SELECT amount_cent FROM ar_ap_items WHERE source_type='purchase_consignment' AND source_id=?",saleId)).isEqualTo(600);
 }

 @Test void returnApprovalHandoverAndFinanceAdjustmentRemainSeparateAndOldPaymentIsPreserved(){
  String purchase=ordered("credit_term","each_receipt",30,10);receive(purchase,10,10,0,200L);
  String original=(String)receipt(purchase).get("payable_id");var payment=payment(key(),500L);
  act(finance);financeService.registerArApPayment(original,payment);act(requestor);
  var ret=workflow.proposeReturn(purchase,new PurchaseWorkflowRequests.Return(key(),(String)receipt(purchase).get("id"),3,200L,"质量费用按已核合同承担","退供原件",proof(purchase),manager,List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance)),null,null));
  var originalFacts=workflow.readMap(reviews.findReview((String)ret.get("review_id")).orElseThrow().reason());
  assertThat(originalFacts).containsKeys("purchase","contract_snapshot","receipt","consignment_sale");
  assertThat(((Map<?,?>)originalFacts.get("receipt")).containsKey("unit_cost_cent")).isTrue();
  assertThat(((Number)originalFacts.get("confirmed_amount_cent")).longValue()).isEqualTo(600);
  String returnId=(String)ret.get("id");approve((String)ret.get("review_id"),finance);approve((String)ret.get("review_id"),manager);act(requestor);
  assertThat(number("SELECT quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?",item,location)).isEqualTo(10);
  workflow.handover(returnId,new PurchaseWorkflowRequests.Handover(key(),Instant.now(),List.of(upload("purchase_return",returnId))));
  assertThat(number("SELECT quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?",item,location)).isEqualTo(7);
  assertThat(number("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id=?",original)).isEqualTo(1500);
  act(finance);workflow.reviewReturnFinance(returnId,new PurchaseWorkflowRequests.ReturnFinanceReview(key(),null,"核对货款与出库成本",List.of(upload("purchase_return",returnId))));
  String replacement=string("SELECT replacement_item_id FROM purchase_payable_adjustments WHERE return_id=?",returnId);
  String reversal=string("SELECT reversal_item_id FROM purchase_payable_adjustments WHERE return_id=?",returnId);
  assertThat(number("SELECT amount_cent FROM ar_ap_items WHERE id=?",replacement)).isEqualTo(1400);
  assertThat(number("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id=?",replacement)).isEqualTo(900);
  assertThat(number("SELECT sum(amount_cent) FROM ar_ap_payments WHERE ar_ap_item_id=?",original)).isEqualTo(500);
  for(String retired:List.of(original,reversal))assertThatThrownBy(()->financeService.registerArApPayment(retired,payment(key(),1L))).isInstanceOf(ApiException.class);
  assertThatThrownBy(()->financeService.registerArApPayment(original,payment)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PURCHASE_PAYABLE_REPLACED"));
  financeService.registerArApPayment(replacement,payment(key(),900L));
  assertThat(number("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id=?",replacement)).isZero();
 }

 @Test void crossProjectReadAndMissingFinancialPermissionCannotExposePurchaseMoney(){
  String purchase=ordered("credit_term","each_receipt",30,10);receive(purchase,10,10,0,200L);
  jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code IN ('inventory.cost.view','finance.ar_ap.view','finance.ar_ap.manage')",role);
  assertThat(workflow.detail(purchase)).doesNotContainKeys("payables","allocations","settlement_terms");
  String other="pc_other_"+suffix;
  jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','他项目','active',now(),now())",other);
  jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?",other,requestor);
  act(requestor,other);
  assertThatThrownBy(()->workflow.detail(purchase)).isInstanceOf(ApiException.class);
 }


 @Test void platformPurchaseStaffUsesRealProjectScopeWithoutAdministratorIdentity(){
  jdbc.update("UPDATE access_users SET merchant_id=NULL WHERE id IN (?,?,?,?,?)",requestor,manager,finance,business,legal);
  jdbc.update("UPDATE inventory_items SET merchant_id=NULL WHERE id=?",item);
  jdbc.update("UPDATE inventory_suppliers SET merchant_id=NULL WHERE id=?",supplier);
  jdbc.update("UPDATE supplier_qualifications SET merchant_id=NULL WHERE supplier_id=?",supplier);
  jdbc.update("UPDATE warehouses SET merchant_id=NULL WHERE id=?",warehouse);
  merchant=null;act(requestor);
  String purchase=ordered("credit_term","each_receipt",30,10);receive(purchase,10,10,0,200L);
  assertThat(workflow.detail(purchase)).containsEntry("project_id",project);
  assertThat(jdbc.queryForMap("SELECT merchant_id FROM purchase_receipts WHERE purchase_id=?",purchase).get("merchant_id")).isNull();
  String other="pc_other_"+suffix;
  jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','他项目','active',now(),now())",other);
  jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?",other,requestor);act(requestor,other);
  assertThatThrownBy(()->workflow.detail(purchase)).isInstanceOf(ApiException.class);
 }
 @Test void workbenchOptionsDoesNotRequireCatalogManagementAndKeepsGlobalContractResources(){
  String contract=approvedContract("credit_term","each_receipt",30);
  jdbc.update("UPDATE inventory_items SET merchant_id=NULL WHERE id=?",item);
  jdbc.update("UPDATE inventory_suppliers SET merchant_id=NULL WHERE id=?",supplier);
  jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code='inventory.supply_contract.manage'",role);
  var options=workflow.options();
  assertThat((List<?>)options.get("inventory_items")).isNotEmpty();
  assertThat((List<?>)options.get("suppliers")).isNotEmpty();
  assertThat((List<?>)options.get("warehouses")).isNotEmpty();
  assertThat((List<?>)options.get("contracts")).isNotEmpty();
 }
 @Test void forwardedDraftCannotPlaceOriginalApplicantOnApprovalChain(){
  String contract=approvedContract("credit_term","each_receipt",30);
  act(manager);String purchase=createPurchase(contract,10);
  act(requestor);
  assertThatThrownBy(()->submit(purchase,contract)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("SELF_APPROVAL_FORBIDDEN"));
 }


 @Test void unusedPrepaymentRefundReservesCashAndClosesOnlyAfterSupplierActuallyPays(){
  String purchase=ordered("prepaid","ordered",2,10);
  String original=string("SELECT id FROM ar_ap_items WHERE source_type='purchase_prepayment' AND source_id=?",purchase);
  act(finance);financeService.registerArApPayment(original,payment(key(),1000L));
  String cash=string("SELECT id FROM ar_ap_payments WHERE ar_ap_item_id=?",original);
  act(requestor);receive(purchase,10,10,0,200L);
  String target=(String)receipt(purchase).get("payable_id");
  var request=new PurchaseWorkflowRequests.PrepaymentResolution(key(),"refund_unused",cash,400L,today().plusDays(5),
   "合同确认未使用预付可申请原路径退回","申请供应商退未用余额",proof(purchase),manager,List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance)),null);
  var resolution=prepayments.resolve(purchase,request);prepayments.resolve(purchase,request);
  String id=(String)resolution.get("id");
  act(finance);
  assertThatThrownBy(()->workflow.allocate(purchase,new PurchaseWorkflowRequests.Allocation(key(),cash,target,601L,proof(purchase)))).isInstanceOf(ApiException.class);
  workflow.allocate(purchase,new PurchaseWorkflowRequests.Allocation(key(),cash,target,600L,proof(purchase)));
  assertThat(number("SELECT count(*) FROM ar_ap_payments WHERE ar_ap_item_id IN (?,?)",original,target)).isEqualTo(1);
  approve((String)resolution.get("review_id"),finance);approve((String)resolution.get("review_id"),manager);act(requestor);
  assertThatThrownBy(()->prepayments.post(id,new PurchaseWorkflowRequests.PrepaymentPost(key(),"复核",List.of(upload("purchase_prepayment_resolution",id)))))
   .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("SELF_APPROVAL_FORBIDDEN"));
  act(finance);var post=new PurchaseWorkflowRequests.PrepaymentPost(key(),"核对合同与未用现金",List.of(upload("purchase_prepayment_resolution",id)));
  prepayments.post(id,post);prepayments.post(id,post);
  String receivable=string("SELECT supplier_refund_item_id FROM purchase_prepayment_resolutions WHERE id=?",id);
  assertThat(number("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id=?",receivable)).isEqualTo(400);
  assertThat(string("SELECT status FROM ar_ap_items WHERE id=?",receivable)).isEqualTo("pending_collection");
  assertThat(number("SELECT count(*) FROM ar_ap_payments WHERE ar_ap_item_id=?",original)).isEqualTo(1);
  financeService.registerArApPayment(receivable,payment(key(),400L));
  assertThat(string("SELECT status FROM ar_ap_items WHERE id=?",receivable)).isEqualTo("settled");
  assertThatThrownBy(()->workflow.allocate(purchase,new PurchaseWorkflowRequests.Allocation(key(),cash,target,1L,proof(purchase)))).isInstanceOf(ApiException.class);
  assertThat((List<?>)workflow.detail(purchase).get("prepayment_payments")).hasSize(1);
 }
 @Test void rejectedUnusedPrepaymentRefundReleasesItsReservation(){
  String purchase=ordered("prepaid","ordered",2,10);
  String original=string("SELECT id FROM ar_ap_items WHERE source_type='purchase_prepayment' AND source_id=?",purchase);
  act(finance);financeService.registerArApPayment(original,payment(key(),1000L));
  String cash=string("SELECT id FROM ar_ap_payments WHERE ar_ap_item_id=?",original);
  act(requestor);receive(purchase,10,10,0,200L);
  var resolution=prepayments.resolve(purchase,new PurchaseWorkflowRequests.PrepaymentResolution(key(),"refund_unused",cash,1000L,today().plusDays(3),
   "合同未用款条款","申请退回",proof(purchase),manager,List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance)),null));
  reject((String)resolution.get("review_id"),manager);act(finance);
  workflow.allocate(purchase,new PurchaseWorkflowRequests.Allocation(key(),cash,(String)receipt(purchase).get("payable_id"),1000L,proof(purchase)));
  assertThat(number("SELECT amount_cent FROM purchase_prepayment_allocations WHERE purchase_id=?",purchase)).isEqualTo(1000);
  assertThat(number("SELECT count(*) FROM ar_ap_items WHERE source_type='purchase_prepayment_refund' AND source_id=?",resolution.get("id"))).isZero();
 }
 @Test void approvedReductionRetiresOldObligationWithoutRewritingCashHistory(){
  String purchase=ordered("prepaid","ordered",2,10);
  String original=string("SELECT id FROM ar_ap_items WHERE source_type='purchase_prepayment' AND source_id=?",purchase);
  var paid=payment(key(),400L);act(finance);financeService.registerArApPayment(original,paid);
  String cash=string("SELECT id FROM ar_ap_payments WHERE ar_ap_item_id=?",original);
  act(requestor);
  var resolution=prepayments.resolve(purchase,new PurchaseWorkflowRequests.PrepaymentResolution(key(),"reduce_unpaid",null,300L,null,
   "双方确认部分终止相应未付预付义务","减少未付义务",proof(purchase),manager,List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance)),null));
  String id=(String)resolution.get("id");approve((String)resolution.get("review_id"),finance);approve((String)resolution.get("review_id"),manager);act(finance);
  prepayments.post(id,new PurchaseWorkflowRequests.PrepaymentPost(key(),"核实尚未付款余额",List.of(upload("purchase_prepayment_resolution",id))));
  String replacement=string("SELECT replacement_item_id FROM purchase_prepayment_resolutions WHERE id=?",id);
  String reversal=string("SELECT reversal_item_id FROM purchase_prepayment_resolutions WHERE id=?",id);
  assertThat(number("SELECT amount_cent FROM ar_ap_items WHERE id=?",replacement)).isEqualTo(700);
  assertThat(number("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id=?",replacement)).isEqualTo(300);
  assertThat(number("SELECT sum(amount_cent) FROM ar_ap_payments WHERE ar_ap_item_id=?",original)).isEqualTo(400);
  financeService.registerArApPayment(replacement,payment(key(),300L));
  act(requestor);receive(purchase,10,10,0,100L);act(finance);
  workflow.allocate(purchase,new PurchaseWorkflowRequests.Allocation(key(),cash,(String)receipt(purchase).get("payable_id"),400L,proof(purchase)));
  assertThat(number("SELECT sum(amount_cent) FROM ar_ap_payments WHERE ar_ap_item_id IN(?,?)",original,replacement)).isEqualTo(700);
  assertThat(((Number)workflow.detail(purchase).get("prepayment_cash_paid_cent")).longValue()).isEqualTo(700);
  // 预期拒付由事务代理标记外层回滚；完成真实链后再验证拒绝，避免影响后续准入保存点。
  assertThatThrownBy(()->financeService.registerArApPayment(original,paid)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PURCHASE_PAYABLE_REPLACED"));
  for(String retired:List.of(original,reversal))assertThatThrownBy(()->financeService.registerArApPayment(retired,payment(key(),1L))).isInstanceOf(ApiException.class);
  assertThat(number("SELECT sum(amount_cent) FROM ar_ap_payments WHERE ar_ap_item_id IN(?,?)",original,replacement)).isEqualTo(700);
  assertThat(number("SELECT count(*) FROM ar_ap_payments WHERE ar_ap_item_id=?",reversal)).isZero();
 }

 @Test void contractModesAndRequiredOwnerUseRealScope(){
  var merchantContract=contracts.createSupplyContract(contractRequest("merchant_self_purchase",merchant,List.of(item)));
  assertThat(merchantContract.contractMode()).isEqualTo("merchant_self_purchase");
  assertThatThrownBy(()->contracts.createSupplyContract(contractRequest("wrong_mode",merchant,List.of(item)))).isInstanceOf(ApiException.class);
  jdbc.update("UPDATE access_users SET merchant_id=NULL WHERE id=?",requestor);jdbc.update("UPDATE inventory_suppliers SET merchant_id=NULL WHERE id=?",supplier);
  jdbc.update("UPDATE inventory_items SET merchant_id=NULL WHERE id=?",item);merchant=null;act(requestor);
  assertThatThrownBy(()->contracts.createSupplyContract(contractRequest("merchant_self_purchase",null,List.of(item)))).isInstanceOf(ApiException.class);
  for(String mode:List.of("platform_self_use","platform_purchase_distribution"))
   assertThat(contracts.createSupplyContract(contractRequest(mode,null,List.of(item))).contractMode()).isEqualTo(mode);
 }
 @Test void directContractDecisionAndRepeatSubmissionCannotBypassApproval(){
  String contract=draftContract("credit_term","each_receipt",30);
  for(String invalid:List.of("rejected","unknown"))assertThatThrownBy(()->contracts.approveSupplyContract(contract,new SupplyContractApproveRequest(key(),invalid,"非节点决议",manager,contractLines(),null))).isInstanceOf(ApiException.class);
  assertThat(string("SELECT review_id FROM supply_contracts WHERE id=?",contract)).isNull();
  var submit=new SupplyContractApproveRequest(key(),"approved","合同原件",manager,contractLines(),null);
  contracts.approveSupplyContract(contract,submit);
  assertThatThrownBy(()->contracts.approveSupplyContract(contract,new SupplyContractApproveRequest(key(),"approved","重复提交",manager,contractLines(),null))).isInstanceOf(ApiException.class);
  String review=string("SELECT review_id FROM supply_contracts WHERE id=?",contract);reject(review,legal);act(requestor);
  assertThat(string("SELECT approval_status FROM supply_contracts WHERE id=?",contract)).isEqualTo("rejected");
  assertThatThrownBy(()->createPurchase(contract,1)).isInstanceOf(ApiException.class);
  assertThatThrownBy(()->contracts.approveSupplyContract(contract,new SupplyContractApproveRequest(key(),"approved","再次提交",manager,contractLines(),null))).isInstanceOf(ApiException.class);
 }
 @Test void contractScopeAndEffectiveDatesBlockUnsupportedPurchases(){
  String draft=draftContract("credit_term","each_receipt",30);
  assertThatThrownBy(()->createPurchase(draft,1)).isInstanceOf(ApiException.class);
  String contract=approvedContract("credit_term","each_receipt",30);
  assertThat(contracts.requirePurchasable(contract,item).id()).isEqualTo(contract);
  assertThatThrownBy(()->contracts.requirePurchasable(contract,"not-listed")).isInstanceOf(ApiException.class);
  jdbc.update("UPDATE supply_contracts SET purchasable_scope='[]'::jsonb WHERE id=?",contract);
  assertThatThrownBy(()->createPurchase(contract,1)).isInstanceOf(ApiException.class);
  jdbc.update("UPDATE supply_contracts SET purchasable_scope=jsonb_build_array(?::text),valid_from=? WHERE id=?",item,today().plusDays(1),contract);
  assertThatThrownBy(()->createPurchase(contract,1)).isInstanceOf(ApiException.class);
  jdbc.update("UPDATE supply_contracts SET valid_from=current_date-30,valid_to=current_date-1 WHERE id=?",contract);
  assertThatThrownBy(()->createPurchase(contract,1)).isInstanceOf(ApiException.class);
 }
 @Test void sameProjectOtherMerchantCannotSeeOrSubmitContractsAndPurchases(){
  String contract=approvedContract("credit_term","each_receipt",30),purchase=createPurchase(contract,1);
  String other="pc_other_merchant_"+suffix;
  jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,'另一商户','retail',?,?,'active',now(),now())",other,entity,project);
  jdbc.update("UPDATE access_users SET merchant_id=? WHERE id=?",other,requestor);merchant=other;act(requestor);
  assertThat(contracts.listSupplyContracts().items()).noneMatch(c->c.id().equals(contract));
  assertThat(workflow.list().items()).noneMatch(p->p.id().equals(purchase));
  assertThatThrownBy(()->contracts.approveSupplyContract(contract,new SupplyContractApproveRequest(key(),"approved","越商户",manager,contractLines(),null))).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
  assertThatThrownBy(()->purchases.order(purchase,new PurchaseRequestTransitionRequest(key(),"越商户"))).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
 }
 @Test void purchaseCannotSkipOrderAndRejectedApprovalCannotOrder(){
  String contract=approvedContract("credit_term","each_receipt",30),purchase=createPurchase(contract,1);
  assertThatThrownBy(()->purchases.arrive(purchase,new PurchaseRequestTransitionRequest(key(),"未下单到货"))).isInstanceOf(ApiException.class);
  assertThatThrownBy(()->receive(purchase,1,1,0,100L)).isInstanceOf(ApiException.class);
  submit(purchase,contract);reject(string("SELECT review_id FROM inventory_purchase_requests WHERE id=?",purchase),manager);act(requestor);
  assertThat(string("SELECT status FROM inventory_purchase_requests WHERE id=?",purchase)).isEqualTo("rejected");
  assertThatThrownBy(()->purchases.order(purchase,new PurchaseRequestTransitionRequest(key(),"驳回后下单"))).isInstanceOf(ApiException.class);
 }
 @Test void purchaseSubmissionRechecksLockedContractScope(){
  String contract=approvedContract("credit_term","each_receipt",30),purchase=createPurchase(contract,1);
  jdbc.update("UPDATE supply_contracts SET purchasable_scope='[]'::jsonb WHERE id=?",contract);
  assertThatThrownBy(()->submit(purchase,contract)).isInstanceOf(ApiException.class);
  assertThat(string("SELECT review_id FROM inventory_purchase_requests WHERE id=?",purchase)).isNull();
 }
 @Test void expiredQualificationBlocksSubmitApprovalAndOrder(){
  String contract=approvedContract("credit_term","each_receipt",30),purchase=createPurchase(contract,1);
  jdbc.update("UPDATE supplier_qualifications SET valid_to=current_date-1 WHERE supplier_id=?",supplier);
  assertThatThrownBy(()->submit(purchase,contract)).isInstanceOf(ApiException.class);
  jdbc.update("UPDATE supplier_qualifications SET valid_to=current_date+365 WHERE supplier_id=?",supplier);submit(purchase,contract);
  String review=string("SELECT review_id FROM inventory_purchase_requests WHERE id=?",purchase);
  jdbc.update("UPDATE supplier_qualifications SET valid_to=current_date-1 WHERE supplier_id=?",supplier);
  assertThatThrownBy(()->approve(review,manager)).isInstanceOf(ApiException.class);
  act(requestor);jdbc.update("UPDATE supplier_qualifications SET valid_to=current_date+365 WHERE supplier_id=?",supplier);
  String second=createPurchase(contract,1);submit(second,contract);approve(string("SELECT review_id FROM inventory_purchase_requests WHERE id=?",second),manager);act(requestor);
  jdbc.update("UPDATE supplier_qualifications SET valid_to=current_date-1 WHERE supplier_id=?",supplier);
  assertThatThrownBy(()->purchases.order(second,new PurchaseRequestTransitionRequest(key(),"过期后下单"))).isInstanceOf(ApiException.class);
 }
 @Test void expiredQualificationDoesNotBlockActualApprovalNodeRejection(){
  String contract=approvedContract("credit_term","each_receipt",30),purchase=createPurchase(contract,1);submit(purchase,contract);
  jdbc.update("UPDATE supplier_qualifications SET valid_to=current_date-1 WHERE supplier_id=?",supplier);
  reject(string("SELECT review_id FROM inventory_purchase_requests WHERE id=?",purchase),manager);act(requestor);
  assertThat(string("SELECT status FROM inventory_purchase_requests WHERE id=?",purchase)).isEqualTo("rejected");
 }
 @Test void contractOriginalApplicantCannotApproveAfterAnotherEmployeeSubmits(){
  act(manager);String contract=draftContract("credit_term","each_receipt",30);act(requestor);
  assertThatThrownBy(()->contracts.approveSupplyContract(contract,new SupplyContractApproveRequest(key(),"approved","代提交",manager,contractLines(),null)))
   .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("SELF_APPROVAL_FORBIDDEN"));
 }

 SupplyContractCreateRequest contractRequest(String mode,String owner,List<String> scope){return new SupplyContractCreateRequest(key(),supplier,project,owner,mode,"本商品","见执行条款",null,today().minusDays(1),today().plusYears(1),"采购合同",scope);}
 void reject(String review,String user){act(user);reviews.approve(review,new SupervisorReviewApprovalRequest(key(),"复核驳回",List.of(),null,"rejected","真实节点驳回原因"));}


 @Test void creationAuditLinksTheActualContractAndPurchaseRatherThanTheirParentIds(){
  String contract=draftContract("credit_term","each_receipt",30);
  assertThat(number("SELECT count(*) FROM audit_events WHERE event_name='inventory.purchase.completed' AND business_type='supply_contract' AND business_id=? AND metadata->>'operation'='createSupplyContract'",contract)).isEqualTo(1);
  String active=approvedContract("credit_term","each_receipt",30),purchase=createPurchase(active,1);
  assertThat(number("SELECT count(*) FROM audit_events WHERE event_name='inventory.purchase.completed' AND business_type='inventory_purchase_request' AND business_id=?",purchase)).isEqualTo(1);
 }
 @Test void returningAlreadyConfirmedConsignmentUnitsDoesNotConsumeUnsoldUnitsAgain(){
  String purchase=ordered("consignment","confirmed_sale",10,10);receive(purchase,10,10,0,200L);
  String receiptId=(String)receipt(purchase).get("id");
  var sale=workflow.sale(purchase,new PurchaseWorkflowRequests.Sale(key(),receiptId,3,"已核验门店售出清单","SALE-"+key(),Instant.now(),proof(purchase)));
  act(finance);workflow.confirmSale((String)sale.get("id"),new PurchaseWorkflowRequests.SaleConfirmation(key(),"confirmed","核实售出",proof(purchase)));act(requestor);
  var ret=workflow.proposeReturn(purchase,new PurchaseWorkflowRequests.Return(key(),receiptId,1,200L,"原合同已售退供条款","已售原物退供",proof(purchase),manager,List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance)),null,(String)sale.get("id")));
  approve((String)ret.get("review_id"),finance);approve((String)ret.get("review_id"),manager);act(requestor);
  workflow.handover((String)ret.get("id"),new PurchaseWorkflowRequests.Handover(key(),Instant.now(),List.of(upload("purchase_return",(String)ret.get("id")))));
  var remaining=workflow.sale(purchase,new PurchaseWorkflowRequests.Sale(key(),receiptId,7,"已核验门店售出清单","SALE-"+key(),Instant.now(),proof(purchase)));
  assertThat(((Number)remaining.get("quantity")).intValue()).isEqualTo(7);
 }


 @Test void purchaseReadsKeepActualSuccessAuditForAllFiveEntryPoints(){
  String contract=approvedContract("credit_term","each_receipt",30),purchase=createPurchase(contract,1);
  var events=List.of("inventory.purchase_request_viewed","inventory.supply_contract.viewed","inventory.purchase_workbench.viewed");
  var before=new ArrayList<Long>();for(String event:events)before.add(auditCount(event));
  inventoryController.listPurchaseRequests();inventoryController.listSupplyContracts();workflow.options();workflow.detail(purchase);workflow.contractDetail(contract);
  assertThat(auditCount(events.get(0))).isEqualTo(before.get(0)+2);
  assertThat(auditCount(events.get(1))).isEqualTo(before.get(1)+2);
  assertThat(auditCount(events.get(2))).isEqualTo(before.get(2)+1);
 }
 @Test void deniedPurchaseReadsAuditFailureAndKeepPermissionRejection(){
  String contract=approvedContract("credit_term","each_receipt",30),purchase=createPurchase(contract,1);
  jdbc.update("DELETE FROM role_permissions WHERE role_id=?",role);
  for(Runnable read:List.<Runnable>of(()->inventoryController.listPurchaseRequests(),()->inventoryController.listSupplyContracts(),()->workflow.options(),()->workflow.detail(purchase),()->workflow.contractDetail(contract)))
   assertThatThrownBy(read::run).isInstanceOf(ApiException.class);
  assertThat(auditCount("inventory.purchase_request_view_failed")).isEqualTo(2);
  assertThat(auditCount("inventory.supply_contract.view_failed")).isEqualTo(2);
  assertThat(auditCount("inventory.purchase_workbench.view_failed")).isEqualTo(1);
 }
 long auditCount(String event){return number("SELECT count(*) FROM audit_events WHERE actor_id=? AND event_name=?",requestor,event);}

 // 第十八批第 33 条（BSM:1211「系统根据安全库存、销量和在途数量生成建议采购数量及理由，由采购人员人工调整并按审批链批准后生成采购单，不得自动向供应商下单」；
 // 第十八批裁决 5「建议量＝安全库存＋近 7 天销量－当前可用库存－在途量，大于 0 才列出」）。
 @Test void suggestionFollowsRulingFiveAndOnlyApprovedPurchasesCountAsInTransit() throws Exception{
  jdbc.update("UPDATE inventory_items SET min_quantity=20 WHERE id=?",item);
  balance(location,merchant,12);
  movement(location,"outbound","bar_order",30,true,"1 hour");
  movement(location,"dealer_outbound","dealer_order",8,true,"1 hour");
  movement(location,"inbound","bar_order_item_void",3,false,"1 hour");
  movement(location,"return","dealer_return_line",2,false,"1 hour");
  // 不计入销量：礼物领取消耗、门店要货签收（调拨）、手工出库、7 天窗口外的售出。
  movement(location,"gift_claim_consume","bar_gift_redemption",5,true,"1 hour");
  movement(location,"inbound","inventory_transfer",6,false,"1 hour");
  movement(location,"outbound","manual",4,true,"1 hour");
  movement(location,"outbound","bar_order",100,true,"8 days");
  String placed=ordered("credit_term","each_receipt",30,10),contract=string("SELECT supply_contract_id FROM inventory_purchase_requests WHERE id=?",placed);
  createPurchase(contract,5);String waiting=createPurchase(contract,7);submit(waiting,contract);
  long requests=number("SELECT count(*) FROM inventory_purchase_requests"),viewed=auditCount("inventory.purchase_suggestion.viewed");
  suggestions(project).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
   .andExpect(jsonPath("$.data[0].project_id").value(project)).andExpect(jsonPath("$.data[0].merchant_id").value(merchant))
   .andExpect(jsonPath("$.data[0].item_id").value(item)).andExpect(jsonPath("$.data[0].item_name").value("采购测试耗材"))
   .andExpect(jsonPath("$.data[0].safety_stock").value(20)).andExpect(jsonPath("$.data[0].sales_quantity").value(33))
   .andExpect(jsonPath("$.data[0].available_quantity").value(12)).andExpect(jsonPath("$.data[0].in_transit_quantity").value(10))
   .andExpect(jsonPath("$.data[0].suggested_quantity").value(31))
   .andExpect(jsonPath("$.data[0].reason").value("安全库存 20 ＋ 近 7 天销量 33 － 当前可用 12 － 在途 10 ＝ 建议采购 31"));
  assertThat(number("SELECT count(*) FROM inventory_purchase_requests")).isEqualTo(requests);
  assertThat(auditCount("inventory.purchase_suggestion.viewed")).isEqualTo(viewed+1);
  // 采购人员按建议人工改数后用现有建单入口提交（前端蛇形请求体），待批不计在途；批准后计在途，系统不下单。
  String body="{\"idempotency_key\":\""+key()+"\",\"supplier_id\":\""+supplier+"\",\"supply_contract_id\":\""+contract
   +"\",\"items\":[{\"item_id\":\""+item+"\",\"quantity\":30}],\"expected_arrival_date\":\""+today().plusDays(3)+"\"}";
  String created=com.jayway.jsonpath.JsonPath.read(postJson("/api/v1/inventory/purchase-requests",body).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.status").value("created")).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),"$.data.id");
  suggestions(project).andExpect(jsonPath("$.data[0].suggested_quantity").value(31));
  submit(created,contract);
  suggestions(project).andExpect(jsonPath("$.data[0].in_transit_quantity").value(10)).andExpect(jsonPath("$.data[0].suggested_quantity").value(31));
  approve(string("SELECT review_id FROM inventory_purchase_requests WHERE id=?",created),manager);act(requestor);
  suggestions(project).andExpect(jsonPath("$.data[0].in_transit_quantity").value(40)).andExpect(jsonPath("$.data[0].suggested_quantity").value(1));
  assertThat(string("SELECT status FROM inventory_purchase_requests WHERE id=?",created)).isEqualTo("approved");
  assertThat(jdbc.queryForMap("SELECT ordered_at FROM inventory_purchase_requests WHERE id=?",created).get("ordered_at")).isNull();
 }
 @Test void purchaseSuggestionListsOnlyPositiveQuantities() throws Exception{
  jdbc.update("UPDATE inventory_items SET min_quantity=20 WHERE id=?",item);
  balance(location,merchant,50);movement(location,"outbound","bar_order",30,true,"1 hour");
  suggestions(project).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
 }
 @Test void purchaseSuggestionsStayWithinOwnerProjectAndPermission() throws Exception{
  String other="pc_other_merchant_"+suffix,otherProject="pc_other_project_"+suffix;
  jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,'同项目另一商户','retail',?,?,'active',now(),now())",other,entity,project);
  jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','他项目','active',now(),now())",otherProject);
  jdbc.update("UPDATE inventory_items SET min_quantity=5 WHERE id=?",item);
  balance(location,merchant,1);
  balance("pc_loc_other_"+suffix,other,0);movement("pc_loc_other_"+suffix,"outbound","bar_order",9,true,"1 hour");
  balance("pc_loc_platform_"+suffix,null,0);movement("pc_loc_platform_"+suffix,"dealer_outbound","dealer_order",7,true,"1 hour");
  suggestions(project).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
   .andExpect(jsonPath("$.data[0].merchant_id").value(merchant)).andExpect(jsonPath("$.data[0].suggested_quantity").value(4));
  long failed=auditCount("inventory.purchase_suggestion.view_failed");
  suggestions(otherProject).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
  jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code='inventory.purchase_request.manage'",role);
  suggestions(project).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
  assertThat(auditCount("inventory.purchase_suggestion.view_failed")).isEqualTo(failed+2);
 }
 // 第十八批第 33 条 33-R4：已有物料的安全库存可改（权限与新建物料同码 inventory.stock.move），改后建议量按新安全库存计算。
 @Test void safetyStockOfExistingItemIsEditableAndFeedsSuggestion() throws Exception{
  jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'inventory.stock.move')",role);
  balance(location,merchant,12);
  suggestions(project).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
  long done=auditCount("inventory.item.safety_stock_updated"),failed=auditCount("inventory.item.safety_stock_update_failed");
  postJson("/api/v1/inventory/items/"+item+"/safety-stock","{\"idempotency_key\":\""+key()+"\",\"min_quantity\":20}")
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(item)).andExpect(jsonPath("$.data.min_quantity").value(20));
  assertThat(number("SELECT min_quantity FROM inventory_items WHERE id=?",item)).isEqualTo(20);
  assertThat(auditCount("inventory.item.safety_stock_updated")).isEqualTo(done+1);
  suggestions(project).andExpect(jsonPath("$.data[0].safety_stock").value(20)).andExpect(jsonPath("$.data[0].suggested_quantity").value(8));
  postJson("/api/v1/inventory/items/"+item+"/safety-stock","{\"idempotency_key\":\""+key()+"\",\"min_quantity\":-1}")
   .andExpect(status().isBadRequest());
  String other="pc_item_merchant_"+suffix,foreign="pc_foreign_item_"+suffix;
  jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,'他商户','retail',?,?,'active',now(),now())",other,entity,project);
  jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'他商户耗材','consumable','包',0,'active',?,now(),now())",foreign,other);
  postJson("/api/v1/inventory/items/"+foreign+"/safety-stock","{\"idempotency_key\":\""+key()+"\",\"min_quantity\":9}")
   .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
  jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code='inventory.stock.move'",role);
  postJson("/api/v1/inventory/items/"+item+"/safety-stock","{\"idempotency_key\":\""+key()+"\",\"min_quantity\":30}")
   .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
  assertThat(number("SELECT min_quantity FROM inventory_items WHERE id=?",item)).isEqualTo(20);
  assertThat(number("SELECT min_quantity FROM inventory_items WHERE id=?",foreign)).isZero();
  assertThat(auditCount("inventory.item.safety_stock_update_failed")).isEqualTo(failed+3);
 }
 void balance(String loc,String owner,int available){
  jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,updated_at,warehouse_id,project_id,merchant_id) VALUES(?,?,?,?,0,?,now(),?,?,?)",
   "pc_bal_"+loc,item,loc,available,available,warehouse,project,owner);
 }
 void movement(String loc,String type,String business,int quantity,boolean outgoing,String age){
  jdbc.update("INSERT INTO inventory_stock_movements(id,movement_type,item_id,source_location_id,target_location_id,quantity,business_type,business_id,status,created_at,merchant_id,project_id) VALUES(?,?,?,?,?,?,?,?,'completed',now()-CAST(? AS interval),?,?)",
   "pc_mv_"+key().substring(0,13),type,item,outgoing?loc:null,outgoing?null:loc,quantity,business,"pc_src_"+suffix,age,merchant,project);
 }
 RequestPostProcessor asRequestor(){
  return authentication(new UsernamePasswordAuthenticationToken(new AccessActor(requestor,requestor,"staff",project,merchant,"active"),"test",List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
 }
 ResultActions suggestions(String projectId) throws Exception{
  var result=mockMvc.perform(get("/api/v1/inventory/purchase-suggestions").param("project_id",projectId).with(asRequestor()).header("X-Terminal-Type","web"));
  act(requestor);return result;
 }
 ResultActions postJson(String path,String body) throws Exception{
  var result=mockMvc.perform(post(path).with(asRequestor()).header("X-Terminal-Type","web").contentType(MediaType.APPLICATION_JSON).content(body));
  act(requestor);return result;
 }

 @Test void forwardedApprovalCannotBeSentBackToOriginalApplicant(){
  for(String type:List.of("contract","purchase")){
   String review=delegatedReview(type);act(manager);
   assertThatThrownBy(()->reviews.forward(review,new SupervisorReviewForwardRequest(key(),"转交原申请人",requestor,"同单原申请人")))
    .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("SUPERVISOR_FORWARD_TARGET_FORBIDDEN"));
   String independent=delegateSubmitter();act(manager);
   reviews.forward(review,new SupervisorReviewForwardRequest(key(),"由独立人员接手",independent,"真实转交"));
   if("contract".equals(type))for(String user:List.of(business,finance,legal))approve(review,user);
   approve(review,independent);
   assertThat(reviews.findReview(review).orElseThrow().reviewStatus()).isEqualTo("approved");
  }
 }
 @Test void legacyAssignedOriginalApplicantCannotDecideAnotherPersonsSubmission(){
  for(String type:List.of("contract","purchase")){
   String review=delegatedReview(type);
   // 仅复现历史待审指派，全部审批状态保持 pending，决议仍必须走真实入口。
   jdbc.update("UPDATE supervisor_review_nodes SET forwarded_to=? WHERE review_id=? AND line_code='main' AND status='pending'",requestor,review);
   act(requestor);
   assertThatThrownBy(()->reviews.approve(review,new SupervisorReviewApprovalRequest(key(),"原申请人试图裁决",List.of(),null,"approved","历史转交")))
    .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
   assertThat(reviews.findReview(review).orElseThrow().reviewStatus()).isEqualTo("pending_review");
  }
 }
 String delegatedReview(String type){
  act(requestor);
  String source="contract".equals(type)?draftContract("credit_term","each_receipt",30):createPurchase(approvedContract("credit_term","each_receipt",30),1);
  String delegate=delegateSubmitter();act(delegate);
  if("contract".equals(type))contracts.approveSupplyContract(source,new SupplyContractApproveRequest(key(),"approved","代提交原件",manager,contractLines(),null));
  else submit(source,string("SELECT supply_contract_id FROM inventory_purchase_requests WHERE id=?",source));
  return string("SELECT review_id FROM "+("contract".equals(type)?"supply_contracts":"inventory_purchase_requests")+" WHERE id=?",source);
 }
 String delegateSubmitter(){
  String user="pc_delegate_"+key();
  jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",user,user,project,merchant);
  jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)",user,role);
  jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)",user,project);
  jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)",user,manager);
  return user;
 }

 @Test void lockedReceiptCannotBeChangedByNonCashPrepaymentAllocation(){
  String purchase=ordered("prepaid","ordered",2,10);
  String prepaid=string("SELECT id FROM ar_ap_items WHERE source_type='purchase_prepayment' AND source_id=?",purchase);
  act(finance);financeService.registerArApPayment(prepaid,payment(key(),600L));
  String cash=string("SELECT id FROM ar_ap_payments WHERE ar_ap_item_id=?",prepaid);
  act(requestor);receive(purchase,10,10,0,100L);String original=(String)receipt(purchase).get("payable_id");
  lockPayable(original);var before=jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?",original);
  var allocation=new PurchaseWorkflowRequests.Allocation(key(),cash,original,600L,proof(purchase));
  assertThatThrownBy(()->workflow.allocate(purchase,allocation)).isInstanceOf(ApiException.class);
  assertThat(jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?",original)).isEqualTo(before);
  assertThat(number("SELECT count(*) FROM purchase_prepayment_allocations WHERE purchase_id=?",purchase)).isZero();
 }

 @Test void generalBackfillUsesCurrentReceiptForAllocationAndOnlyRemainingCash(){
  String purchase=ordered("prepaid","ordered",2,10);
  String prepaid=string("SELECT id FROM ar_ap_items WHERE source_type='purchase_prepayment' AND source_id=?",purchase);
  act(finance);financeService.registerArApPayment(prepaid,payment(key(),600L));
  String cash=string("SELECT id FROM ar_ap_payments WHERE ar_ap_item_id=?",prepaid);
  var cashBefore=jdbc.queryForMap("SELECT * FROM ar_ap_payments WHERE id=?",cash);
  act(requestor);receive(purchase,10,10,0,100L);String original=(String)receipt(purchase).get("payable_id");
  lockPayable(original);var before=jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?",original);
  String current=backfill(original,100L);act(finance);
  @SuppressWarnings("unchecked") var ledger=(List<Map<String,Object>>)workflow.detail(purchase).get("payables");
  assertThat(ledger.stream().filter(row->original.equals(row.get("id"))).findFirst().orElseThrow().get("is_current")).isEqualTo(false);
  assertThat(ledger.stream().filter(row->current.equals(row.get("id"))).findFirst().orElseThrow().get("is_current")).isEqualTo(true);
  var allocation=new PurchaseWorkflowRequests.Allocation(key(),cash,current,600L,proof(purchase));
  workflow.allocate(purchase,allocation);workflow.allocate(purchase,allocation);
  assertThat(number("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id=?",current)).isEqualTo(500);
  financeService.registerArApPayment(current,payment(key(),500L));
  assertThat(number("SELECT count(*) FROM purchase_prepayment_allocations WHERE purchase_id=?",purchase)).isEqualTo(1);
  assertThat(number("SELECT sum(p.amount_cent) FROM ar_ap_payments p JOIN ar_ap_items a ON a.id=p.ar_ap_item_id WHERE a.id IN(?,?,?)",prepaid,original,current)).isEqualTo(1100);
  assertThat(jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?",original)).isEqualTo(before);
  assertThat(jdbc.queryForMap("SELECT * FROM ar_ap_payments WHERE id=?",cash)).isEqualTo(cashBefore);
  assertThatThrownBy(()->workflow.allocate(purchase,new PurchaseWorkflowRequests.Allocation(key(),cash,original,1L,proof(purchase)))).isInstanceOf(ApiException.class);
  assertThatThrownBy(()->financeService.registerArApPayment(original,payment(key(),1L))).isInstanceOf(ApiException.class);
 }

 @Test void restatedPayableKeepsEveryPartialCashPaymentAndReplayDoesNotAddCash(){
  String purchase=ordered("credit_term","each_receipt",30,10);receive(purchase,10,10,0,100L);
  String original=(String)receipt(purchase).get("payable_id");lockPayable(original);
  var originalBefore=jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?",original);
  String current=backfill(original,100L);act(finance);
  var firstRequest=payment(key(),400L);var secondRequest=payment(key(),700L);
  var first=financeService.registerArApPayment(current,firstRequest);
  var second=financeService.registerArApPayment(current,secondRequest);
  financeService.registerArApPayment(current,firstRequest);financeService.registerArApPayment(current,secondRequest);
  assertThat(second.payment().id()).isNotEqualTo(first.payment().id());
  assertThat(number("SELECT count(*) FROM ar_ap_payments WHERE ar_ap_item_id=?",current)).isEqualTo(2);
  assertThat(number("SELECT sum(amount_cent) FROM ar_ap_payments WHERE ar_ap_item_id=?",current)).isEqualTo(1100);
  assertThat(number("SELECT paid_amount_cent FROM ar_ap_items WHERE id=?",current)).isEqualTo(1100);
  assertThat(number("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id=?",current)).isZero();
  assertThat(jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?",original)).isEqualTo(originalBefore);
 }

 @Test void generalBackfillReturnReducesCurrentRestatementRatherThanFrozenOriginal(){
  String purchase=ordered("credit_term","each_receipt",30,10);receive(purchase,10,10,0,100L);
  String original=(String)receipt(purchase).get("payable_id");act(finance);financeService.registerArApPayment(original,payment(key(),400L));
  String cash=string("SELECT id FROM ar_ap_payments WHERE ar_ap_item_id=?",original);
  var cashBefore=jdbc.queryForMap("SELECT * FROM ar_ap_payments WHERE id=?",cash);
  lockPayable(original);var before=jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?",original);String current=backfill(original,100L);
  String ret=approvedReturn(purchase,2,100L);workflow.handover(ret,new PurchaseWorkflowRequests.Handover(key(),Instant.now(),List.of(upload("purchase_return",ret))));
  act(finance);workflow.reviewReturnFinance(ret,new PurchaseWorkflowRequests.ReturnFinanceReview(key(),null,"核对通用补录后的真实应付",List.of(upload("purchase_return",ret))));
  assertThat(string("SELECT original_item_id FROM purchase_payable_adjustments WHERE return_id=?",ret)).isEqualTo(current);
  String replacement=string("SELECT replacement_item_id FROM purchase_payable_adjustments WHERE return_id=?",ret);
  assertThat(number("SELECT amount_cent FROM ar_ap_items WHERE id=?",replacement)).isEqualTo(900);
  assertThat(number("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id=?",replacement)).isEqualTo(500);
  financeService.registerArApPayment(replacement,payment(key(),500L));
  assertThat(jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?",original)).isEqualTo(before);
  assertThat(jdbc.queryForMap("SELECT * FROM ar_ap_payments WHERE id=?",cash)).isEqualTo(cashBefore);
  assertThat(number("SELECT sum(amount_cent) FROM ar_ap_payments WHERE ar_ap_item_id IN(?,?,?)",original,current,replacement)).isEqualTo(900);
 }

 @Test void generalBackfillBeforePrepaymentReductionKeepsOneCurrentObligation(){
  String purchase=ordered("prepaid","ordered",2,10);
  String original=string("SELECT id FROM ar_ap_items WHERE source_type='purchase_prepayment' AND source_id=?",purchase);
  act(finance);financeService.registerArApPayment(original,payment(key(),400L));lockPayable(original);
  String current=backfill(original,100L);String replacement=reducePrepayment(purchase,300L);
  assertThat(string("SELECT original_item_id FROM purchase_prepayment_resolutions WHERE replacement_item_id=?",replacement)).isEqualTo(current);
  assertThat(number("SELECT amount_cent FROM ar_ap_items WHERE id=?",replacement)).isEqualTo(800);
  assertThat(number("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id=?",replacement)).isEqualTo(400);
  act(finance);financeService.registerArApPayment(replacement,payment(key(),400L));
  assertThat(number("SELECT sum(p.amount_cent) FROM ar_ap_payments p JOIN ar_ap_items a ON a.id=p.ar_ap_item_id WHERE a.source_type='purchase_prepayment' AND a.source_id=?",purchase)).isEqualTo(800);
 }

 @Test void prepaymentReductionThenGeneralBackfillStillAllowsNextApprovedReduction(){
  String purchase=ordered("prepaid","ordered",2,10);
  String original=string("SELECT id FROM ar_ap_items WHERE source_type='purchase_prepayment' AND source_id=?",purchase);
  act(finance);financeService.registerArApPayment(original,payment(key(),400L));
  String first=reducePrepayment(purchase,200L);lockPayable(first);String restated=backfill(first,100L);
  String current=reducePrepayment(purchase,100L);
  assertThat(string("SELECT original_item_id FROM purchase_prepayment_resolutions WHERE replacement_item_id=?",current)).isEqualTo(restated);
  assertThat(number("SELECT amount_cent FROM ar_ap_items WHERE id=?",current)).isEqualTo(800);
  act(finance);financeService.registerArApPayment(current,payment(key(),400L));
  assertThat(number("SELECT sum(p.amount_cent) FROM ar_ap_payments p JOIN ar_ap_items a ON a.id=p.ar_ap_item_id WHERE a.source_type='purchase_prepayment' AND a.source_id=?",purchase)).isEqualTo(800);
 }

 @Test void returnHandoverWithoutCostPermissionKeepsRealCostButMasksResponse(){
  String purchase=ordered("credit_term","each_receipt",30,10);receive(purchase,10,10,0,100L);
  String ret=approvedReturn(purchase,2,100L);
  jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code='inventory.cost.view'",role);
  var response=workflow.handover(ret,new PurchaseWorkflowRequests.Handover(key(),Instant.now(),List.of(upload("purchase_return",ret))));
  assertThat(response.get("actual_cost_cent")).isNull();assertThat(response.get("cost_difference_cent")).isNull();
  assertThat(number("SELECT actual_cost_cent FROM purchase_returns WHERE id=?",ret)).isEqualTo(200);
  assertThat(number("SELECT quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?",item,location)).isEqualTo(8);
 }

 @Test void returnHandoverReplayRechecksRevokedCostPermissionForSameAndOtherOperator(){
  String purchase=ordered("credit_term","each_receipt",30,10);receive(purchase,10,10,0,100L);
  String ret=approvedReturn(purchase,2,100L);
  var request=new PurchaseWorkflowRequests.Handover(key(),Instant.now(),List.of(upload("purchase_return",ret)));
  assertThat(((Number)workflow.handover(ret,request).get("actual_cost_cent")).longValue()).isEqualTo(200);
  jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code='inventory.cost.view'",role);
  assertThat(workflow.handover(ret,request).get("actual_cost_cent")).isNull();
  act(manager);assertThat(workflow.handover(ret,request).get("actual_cost_cent")).isNull();
  assertThat(number("SELECT count(*) FROM inventory_stock_movements WHERE business_type='purchase_return' AND business_id=?",ret)).isEqualTo(1);
 }

 @Test void supplyContractCreationReplayCannotExposeRevokedPriceTerms(){
  var request=contractRequest("merchant_self_purchase",merchant,List.of(item));
  assertThat(contracts.createSupplyContract(request).priceTerms()).isNotNull();
  jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code='inventory.cost.view'",role);
  assertThat(contracts.createSupplyContract(request).priceTerms()).isNull();
 }

 @Test void supplyContractTermsReplayCannotExposeRevokedStructuredPrices(){
  String contract=draftContract("credit_term","each_receipt",30);
  var request=new PurchaseWorkflowRequests.Terms(key(),fixedTerms(Map.of(item,100L)),List.of(upload("supply_contract",contract)));
  assertThat(workflow.terms(contract,request).get("terms")).isNotNull();
  jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code='inventory.cost.view'",role);
  assertThat(workflow.terms(contract,request).get("terms")).isNull();
 }

 @Test void supplyContractSubmissionReplayCannotExposeRevokedPriceTerms(){
  String contract=draftContract("credit_term","each_receipt",30);
  var request=new SupplyContractApproveRequest(key(),"approved","核合同原件",manager,contractLines(),null);
  assertThat(contracts.approveSupplyContract(contract,request).priceTerms()).isNotNull();
  jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code='inventory.cost.view'",role);
  assertThat(contracts.approveSupplyContract(contract,request).priceTerms()).isNull();
 }

 @Test void fixedPriceContractCannotSubmitWhenAllowedSkuHasNoPrice(){
  String second=secondItem();
  String contract=contracts.createSupplyContract(contractRequest("merchant_self_purchase",merchant,List.of(item,second))).id();
  // 保存草稿或提交环节均可明确拒绝，但不能生成可批准的不完整合同链。
  assertThatThrownBy(()->{
   workflow.terms(contract,new PurchaseWorkflowRequests.Terms(key(),fixedTerms(Map.of(item,100L)),List.of(upload("supply_contract",contract))));
   contracts.approveSupplyContract(contract,new SupplyContractApproveRequest(key(),"approved","提交缺价合同",manager,contractLines(),null));
  }).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PURCHASE_CONTRACT_TERMS_INCOMPLETE"));
  assertThat(string("SELECT review_id FROM supply_contracts WHERE id=?",contract)).isNull();
 }

 @Test void historicalApprovedContractWithMissingFixedPriceCannotCreatePurchase(){
  String contract=approvedContract("credit_term","each_receipt",30);
  // 复现历史已批准合同的漏价条款，保留真实审批链，不伪造批准状态。
  jdbc.update("UPDATE supply_contracts SET settlement_terms=?::jsonb WHERE id=?",workflow.json(fixedTerms(Map.of("other-item",100L))),contract);
  assertThatThrownBy(()->createPurchase(contract,1)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PURCHASE_CONTRACT_TERMS_INCOMPLETE"));
  assertThat(number("SELECT count(*) FROM inventory_purchase_requests WHERE supply_contract_id=?",contract)).isZero();
 }

 @Test void explicitlyZeroFixedPriceIsDifferentFromMissingPrice(){
  String second=secondItem();
  String contract=contracts.createSupplyContract(contractRequest("merchant_self_purchase",merchant,List.of(item,second))).id();
  workflow.terms(contract,new PurchaseWorkflowRequests.Terms(key(),fixedTerms(Map.of(item,0L,second,100L)),List.of(upload("supply_contract",contract))));
  contracts.approveSupplyContract(contract,new SupplyContractApproveRequest(key(),"approved","包含明示零价商品的合同",manager,contractLines(),null));
  String review=string("SELECT review_id FROM supply_contracts WHERE id=?",contract);
  for(String user:List.of(business,finance,legal,manager))approve(review,user);act(requestor);
  String purchase=createPurchase(contract,1);submit(purchase,contract);approve(string("SELECT review_id FROM inventory_purchase_requests WHERE id=?",purchase),manager);act(requestor);
  purchases.order(purchase,new PurchaseRequestTransitionRequest(key(),"按零价条款下单"));receive(purchase,1,1,0,0L);
  assertThat(receipt(purchase).get("payable_id")).isNull();
  assertThat(number("SELECT quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?",item,location)).isEqualTo(1);
 }

 @Test void alreadyRestatedPurchaseCannotRequestAnotherBackfillAgainstRetiredOriginal(){
  String purchase=ordered("credit_term","each_receipt",30,10);receive(purchase,10,10,0,100L);
  String original=(String)receipt(purchase).get("payable_id");lockPayable(original);String current=backfill(original,100L);act(finance);
  assertThatThrownBy(()->financeService.requestAdjustment(new ArApAdjustmentRequest(key(),"旧原件再次补录",List.of(),original,50L,"重复引用原单")))
   .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PURCHASE_PAYABLE_REPLACED"));
  assertThat(number("SELECT count(*) FROM ar_ap_adjustments WHERE ar_ap_item_id=?",original)).isEqualTo(1);
  assertThat(number("SELECT amount_cent FROM ar_ap_items WHERE id=?",current)).isEqualTo(1100);
 }
 @Test void twoPendingBackfillsCannotBothCreateCurrentPurchaseObligations(){
  String purchase=ordered("credit_term","each_receipt",30,10);receive(purchase,10,10,0,100L);
  String original=(String)receipt(purchase).get("payable_id");lockPayable(original);act(finance);
  var first=financeService.requestAdjustment(new ArApAdjustmentRequest(key(),"第一份差额",List.of(),original,100L,"第一份供应商确认"));
  var second=financeService.requestAdjustment(new ArApAdjustmentRequest(key(),"第二份差额",List.of(),original,50L,"第二份供应商确认"));
  act(manager);financeService.approveAdjustment(first.id(),new ArApAdjustmentDecisionRequest(key(),"独立确认第一份",List.of(),"approve"));
  assertThatThrownBy(()->financeService.approveAdjustment(second.id(),new ArApAdjustmentDecisionRequest(key(),"原件已被替代",List.of(),"approve")))
   .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PURCHASE_PAYABLE_REPLACED"));
  assertThat(string("SELECT approval_status FROM ar_ap_adjustments WHERE id=?",second.id())).isEqualTo("pending");
  assertThat(number("SELECT count(*) FROM ar_ap_items WHERE adjustment_id=? AND amount_cent>0",second.id())).isZero();
  assertThat(number("SELECT amount_cent FROM ar_ap_items WHERE id=?","arap_res_"+first.id())).isEqualTo(1100);
 }

 @Test void financeReviewMasksInventoryCostWithoutHidingConfirmedFinancialAmount(){
  String purchase=ordered("credit_term","each_receipt",30,10);receive(purchase,10,10,0,100L);
  String ret=approvedReturn(purchase,2,200L);
  workflow.handover(ret,new PurchaseWorkflowRequests.Handover(key(),Instant.now(),List.of(upload("purchase_return",ret))));
  jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code='inventory.cost.view'",role);act(finance);
  var request=new PurchaseWorkflowRequests.ReturnFinanceReview(key(),null,"核对已批准退供货款",List.of(upload("purchase_return",ret)));
  var response=workflow.reviewReturnFinance(ret,request);
  assertThat(response.get("actual_cost_cent")).isNull();assertThat(response.get("cost_difference_cent")).isNull();
  assertThat(((Number)response.get("confirmed_amount_cent")).longValue()).isEqualTo(400);
  assertThat(((Number)response.get("payable_reduction_cent")).longValue()).isEqualTo(400);
  assertThat(workflow.reviewReturnFinance(ret,request).get("actual_cost_cent")).isNull();
  assertThat(number("SELECT actual_cost_cent FROM purchase_returns WHERE id=?",ret)).isEqualTo(200);
 }
 @Test void financialDetailDoesNotGrantInventoryPricePermissionThroughContractTerms(){
  String contract=draftContract("credit_term","each_receipt",30);
  workflow.terms(contract,new PurchaseWorkflowRequests.Terms(key(),fixedTerms(Map.of(item,100L)),List.of(upload("supply_contract",contract))));
  contracts.approveSupplyContract(contract,new SupplyContractApproveRequest(key(),"approved","固定价格完整合同",manager,contractLines(),null));
  String review=string("SELECT review_id FROM supply_contracts WHERE id=?",contract);
  for(String user:List.of(business,finance,legal,manager))approve(review,user);act(requestor);
  String purchase=createPurchase(contract,10);submit(purchase,contract);approve(string("SELECT review_id FROM inventory_purchase_requests WHERE id=?",purchase),manager);act(requestor);
  purchases.order(purchase,new PurchaseRequestTransitionRequest(key(),"按合同下单"));receive(purchase,10,10,0,200L);
  jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code='inventory.cost.view'",role);act(finance);
  var detail=workflow.detail(purchase);
  assertThat(detail.get("settlement_terms")).isNull();
  assertThat(detail.get("settlement_method")).isEqualTo("credit_term");
  @SuppressWarnings("unchecked") var payables=(List<Map<String,Object>>)detail.get("payables");
  assertThat(((Number)payables.getFirst().get("amount_cent")).longValue()).isEqualTo(1000);
  @SuppressWarnings("unchecked") var receipts=(List<Map<String,Object>>)detail.get("receipts");
  assertThat(receipts.getFirst().get("unit_cost_cent")).isNull();
 }

 void lockPayable(String id){
  act(finance);LocalDate due=jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE id=?",LocalDate.class,id);
  com.complex.platform.support.DailyCloseFixture.lock(financeService,financeRepository,new DailyCloseRequest(key(),"核实本商户日结",List.of(),due,"merchant",merchant));
  assertThat(string("SELECT locked_day_close_id FROM ar_ap_items WHERE id=?",id)).isNotNull();
 }
 String backfill(String id,long change){
  act(finance);var adjustment=financeService.requestAdjustment(new ArApAdjustmentRequest(key(),"锁账后补录",List.of(),id,change,"供应商原件复核确认差额"));
  act(manager);var approved=financeService.approveAdjustment(adjustment.id(),new ArApAdjustmentDecisionRequest(key(),"独立复核补录",List.of(),"approve"));
  assertThat(approved.approvalStatus()).isEqualTo("approved");return "arap_res_"+adjustment.id();
 }
 String approvedReturn(String purchase,int quantity,long price){
  act(requestor);var ret=workflow.proposeReturn(purchase,new PurchaseWorkflowRequests.Return(key(),(String)receipt(purchase).get("id"),quantity,price,
    "双方确认退供责任费用","退供原件",proof(purchase),manager,List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance)),null,null));
  approve((String)ret.get("review_id"),finance);approve((String)ret.get("review_id"),manager);act(requestor);return (String)ret.get("id");
 }
 String reducePrepayment(String purchase,long amount){
  act(requestor);var resolution=prepayments.resolve(purchase,new PurchaseWorkflowRequests.PrepaymentResolution(key(),"reduce_unpaid",null,amount,null,
    "双方确认减少尚未付款义务","按确认条款减少未付义务",proof(purchase),manager,List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance)),null));
  String id=(String)resolution.get("id");approve((String)resolution.get("review_id"),finance);approve((String)resolution.get("review_id"),manager);act(finance);
  prepayments.post(id,new PurchaseWorkflowRequests.PrepaymentPost(key(),"核实当前未付义务",List.of(upload("purchase_prepayment_resolution",id))));
  return string("SELECT replacement_item_id FROM purchase_prepayment_resolutions WHERE id=?",id);
 }
 PurchaseContractTerms fixedTerms(Map<String,Long> prices){return new PurchaseContractTerms(entity,"credit_term","contract_unit_price",prices,"each_receipt",30,null,today(),null,null,null);}
 String secondItem(){
  String second="pc_second_"+suffix;
  jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'第二采购耗材','consumable','包',0,'active',?,now(),now())",second,merchant);return second;
 }

 String draftContract(String method,String anchor,int days){
  var contract=contracts.createSupplyContract(new SupplyContractCreateRequest(key(),supplier,project,merchant,merchant==null?"platform_purchase_distribution":"merchant_self_purchase","本商品","见合同执行价",null,today().minusDays(1),today().plusYears(1),"采购链真实测试合同",List.of(item)));
  var terms=new PurchaseContractTerms(entity,method,"accepted_actual_unit_price",Map.of(),anchor,days,null,today(),
    "prepaid".equals(method)?1000L:null,"consignment".equals(method)?"已核验门店售出清单":null,"prepaid".equals(method)?"未使用余额按原付款路径由供应商退回":null);
  workflow.terms(contract.id(),new PurchaseWorkflowRequests.Terms(key(),terms,List.of(upload("supply_contract",contract.id()))));return contract.id();
 }
 String approvedContract(String method,String anchor,int days){
  String contract=draftContract(method,anchor,days);
  contracts.approveSupplyContract(contract,new SupplyContractApproveRequest(key(),"approved","核合同",manager,contractLines(),null));
  String review=string("SELECT review_id FROM supply_contracts WHERE id=?",contract);
  for(String user:List.of(business,finance,legal,manager))approve(review,user);act(requestor);return contract;
 }
 String createPurchase(String contract,int quantity){return workflow.create(new PurchaseRequestCreate(key(),"申请采购",List.of(),supplier,List.of(Map.of("item_id",item,"quantity",quantity)),today(),contract)).id();}
 void submit(String id,String contract){purchases.approve(id,new PurchaseRequestApproveRequest(key(),"approved",contract,"采购原件",manager,List.of(),null,proof(id)));}
 String ordered(String method,String anchor,int days,int quantity){
  String c=approvedContract(method,anchor,days);String id=createPurchase(c,quantity);submit(id,c);
  approve(string("SELECT review_id FROM inventory_purchase_requests WHERE id=?",id),manager);act(requestor);
  purchases.order(id,new PurchaseRequestTransitionRequest(key(),"对供应商正式下单"));return id;
 }
 PurchaseRequestReceiveRequest receiveRequest(String id,int arrived,int accepted,int rejected,Long price){return new PurchaseRequestReceiveRequest(key(),location,warehouse,proof(id),"实际逐次验收",price,arrived,accepted,rejected,Instant.now());}
 void receive(String id,int arrived,int accepted,int rejected,Long price){purchases.receive(id,receiveRequest(id,arrived,accepted,rejected,price));}
 Map<String,Object> receipt(String id){return jdbc.queryForMap("SELECT * FROM purchase_receipts WHERE purchase_id=? ORDER BY received_at DESC LIMIT 1",id);}
 List<String> proof(String id){return List.of(upload("inventory_purchase_request",id));}
 String upload(String type,String id){return evidence.uploadAttachment(new MockMultipartFile("file","receipt.png","image/png",Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=")),type,id,"other").id();}
 List<SupervisorReviewEvaluateRequest.ParallelLine> contractLines(){return List.of(new SupervisorReviewEvaluateRequest.ParallelLine("business_review",business),new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance),new SupervisorReviewEvaluateRequest.ParallelLine("legal_review",legal));}
 void approve(String review,String user){act(user);reviews.approve(review,new SupervisorReviewApprovalRequest(key(),"核对原件",List.of(),null,"approved","真实组织节点核对"));}
 ArApPaymentRequest payment(String key,long amount){return new ArApPaymentRequest(key,"核实实际银行流水",List.of(),amount,"bank_transfer",Instant.now(),"FLOW-"+key);}
 void act(String user){act(user,project);}
 void act(String user,String org){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(user,user,"staff",org,merchant,"active"),"test",List.of()));}
 long number(String sql,Object...args){Long value=jdbc.queryForObject(sql,Long.class,args);return value==null?0:value;}
 String string(String sql,Object...args){return jdbc.queryForObject(sql,String.class,args);}
 LocalDate today(){return LocalDate.now(LocalZone.ID);}
 String key(){return UUID.randomUUID().toString();}
}

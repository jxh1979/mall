package com.complex.platform.inventory;

// 文件版本：V0.04

import static com.complex.platform.common.Strings.requireText;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.*;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.*;
import com.complex.platform.finance.*;
import com.complex.platform.governance.review.*;
import com.complex.platform.gate.GuardedAction;
import com.complex.platform.inventory.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 采购数量、原合同、实物及资金各留事实；所有采购公开入口复用这一条执行路径。 */
@Service
public class PurchaseWorkflowService {
    private static final String VIEW="inventory.purchase_request.view", MANAGE="inventory.purchase_request.manage",
            APPROVE="inventory.purchase.approve", CONTRACT="inventory.supply_contract.manage", FINANCE="finance.ar_ap.manage";
    private final JdbcTemplate jdbc;
    private final InventoryRepository inventory;
    private final FinanceRepository finance;
    private final AuthorizationService auth;
    private final IdempotencyService idem;
    private final AuditEventService audit;
    private final ObjectMapper mapper;
    private final SupplierQualificationService qualifications;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator files;
    private final SupervisorReviewService reviews;
    public PurchaseWorkflowService(JdbcTemplate jdbc,InventoryRepository inventory,FinanceRepository finance,
            AuthorizationService auth,IdempotencyService idem,AuditEventService audit,ObjectMapper mapper,
            SupplierQualificationService qualifications,EvidenceEventRepository evidence,EvidenceAttachmentValidator files,SupervisorReviewService reviews) {
        this.jdbc=jdbc;this.inventory=inventory;this.finance=finance;this.auth=auth;this.idem=idem;this.audit=audit;
        this.mapper=mapper;this.qualifications=qualifications;this.evidence=evidence;this.files=files;this.reviews=reviews;
    }


    /** 采购岗位读取实际可操作范围的目录，不借用主数据管理权限。 */
    public Map<String,Object> options() {
        return read("inventory.purchase_workbench.viewed","inventory.purchase_workbench.view_failed",VIEW,"inventory_purchase_request","all",()->{
        if(List.of(VIEW,MANAGE,CONTRACT,APPROVE,FINANCE).stream().noneMatch(auth::currentActorHasPermission))
            throw ApiException.forbidden("PERMISSION_DENIED","没有采购工作台权限");
        var owner=merchantScope();
        var projects=jdbc.queryForList("SELECT id,name FROM projects WHERE status='active' ORDER BY name,id").stream()
                .filter(row->optionVisible((String)row.get("id"),owner.orElse(null))).toList();
        var projectIds=projects.stream().map(row->(String)row.get("id")).collect(java.util.stream.Collectors.toSet());
        // 第十八批 G 段 82b-R8（PGM:659「一个商户要在别的项目经营的……加经营项目，要经过审批才生效」）：商户带主项目与已批准的经营项目，任一在可见项目内即列出。
        var operating=new HashMap<Object,List<Object>>();
        jdbc.queryForList("SELECT merchant_id,project_id FROM merchant_projects ORDER BY project_id")
                .forEach(row->operating.computeIfAbsent(row.get("merchant_id"),k->new ArrayList<>()).add(row.get("project_id")));
        var merchants=jdbc.queryForList("SELECT id,merchant_name,project_id,business_entity_id FROM merchants WHERE status='active' ORDER BY merchant_name,id").stream()
                .map(row->{var ids=new ArrayList<Object>();ids.add(row.get("project_id"));ids.addAll(operating.getOrDefault(row.get("id"),List.of()));row.put("operating_project_ids",ids);return row;})
                .filter(row->((List<?>)row.get("operating_project_ids")).stream().anyMatch(projectIds::contains)&&(owner.isEmpty()||owner.get().equals(row.get("id")))).toList();
        var merchantIds=merchants.stream().map(row->(String)row.get("id")).collect(java.util.stream.Collectors.toSet());
        var result=new LinkedHashMap<String,Object>();result.put("projects",projects);result.put("merchants",merchants);
        result.put("entities",jdbc.queryForList("SELECT e.id,e.entity_name,p.project_id FROM business_entities e JOIN business_entity_projects p ON p.business_entity_id=e.id WHERE e.status='active' ORDER BY e.entity_name,e.id").stream()
                .filter(row->projectIds.contains(row.get("project_id"))&&(owner.isEmpty()||merchants.stream().anyMatch(m->Objects.equals(m.get("business_entity_id"),row.get("id"))))).toList());
        result.put("suppliers",jdbc.queryForList("SELECT id,supplier_name,merchant_id FROM inventory_suppliers WHERE status='active' ORDER BY supplier_name,id").stream()
                .filter(row->row.get("merchant_id")==null||(owner.isPresent()?owner.get().equals(row.get("merchant_id")):merchantIds.contains(row.get("merchant_id")))).toList());
        result.put("inventory_items",inventory.listInventoryItems().stream().filter(item->"active".equals(item.status()))
                .filter(item->item.merchantId()==null||(owner.isPresent()?owner.get().equals(item.merchantId()):merchantIds.contains(item.merchantId()))).toList());
        var warehouses=inventory.listWarehouses().stream().filter(w->"active".equals(w.status())&&optionVisible(w.projectId(),w.merchantId())).toList();
        result.put("warehouses",warehouses);
        // 第十八批 M 段第 119 条 119-R1（BSM:1936）：验收「实际库位编号」只能选所选仓库下已建档的库位，随可见仓库带出（不借用库房管理权限）。
        var warehouseIds=warehouses.stream().map(WarehouseRecord::id).collect(java.util.stream.Collectors.toSet());
        result.put("locations",inventory.listWarehouseLocations().stream().filter(l->warehouseIds.contains(l.warehouseId()))
                .map(l->Map.of("id",l.id(),"warehouse_id",l.warehouseId())).toList());
        result.put("contracts",inventory.listSupplyContracts().stream().filter(c->optionVisible(c.projectId(),c.merchantId()))
                .filter(c->"approved".equals(c.approvalStatus())||auth.currentActorHasPermission(CONTRACT)&&visible(c.projectId(),c.merchantId(),CONTRACT))
                .map(this::maskContract).toList());
        return result;
        });
    }
    /**
     * 第十八批第 33 条 智能采购建议（BSM:1211「系统根据安全库存、销量和在途数量生成建议采购数量及理由，由采购人员人工调整并按审批链批准后生成采购单，
     * 不得自动向供应商下单」；第十八批裁决 5「建议量＝安全库存＋近 7 天销量－当前可用库存－在途量，大于 0 才列出」）。
     * 按（项目，货权，物料）即时计算：安全库存取物料 min_quantity；近 7 天为滚动 7×24 小时；销量只计酒吧售出（减退酒回补、加退酒驳回重扣）
     * 与经销出库（减经销退货及交付差异退回的实物回库），门店要货（调拨）、赠饮、积分兑换、手工出库不计，货权取流水库位所在库存行；
     * 在途＝已批准未收完的采购（approved/ordered/expected/arrived 的 数量－合格接收－批准终止）。只读：不存表、不建单、不下单，人工调整走现有建单与审批。
     */
    public List<Map<String,Object>> suggestions(String projectId) {
        return read("inventory.purchase_suggestion.viewed","inventory.purchase_suggestion.view_failed",MANAGE,"inventory_purchase_suggestion",projectId,()->{
        String project=requireText(projectId,"project_id");
        String owner=merchantScope().orElse(null);
        requireScope(project,owner,MANAGE);
        Instant to=Instant.now(),from=to.minus(Duration.ofDays(7));
        var sales=new HashMap<List<Object>,Integer>();
        jdbc.queryForList("""
                SELECT b.merchant_id,m.item_id,SUM(CASE WHEN m.movement_type IN('inbound','return') THEN -m.quantity ELSE m.quantity END) AS quantity
                  FROM inventory_stock_movements m
                  JOIN inventory_stock_balances b ON b.item_id=m.item_id AND b.location_id=COALESCE(m.source_location_id,m.target_location_id)
                 WHERE b.project_id=? AND (?::text IS NULL OR b.merchant_id=?) AND m.status='completed' AND m.created_at>=? AND m.created_at<?
                   AND ((m.movement_type='outbound' AND m.business_type IN('bar_order','bar_order_item_void_reject'))
                     OR (m.movement_type='inbound' AND m.business_type='bar_order_item_void')
                     OR m.movement_type='dealer_outbound'
                     OR (m.movement_type='return' AND m.business_type IN('dealer_return_line','dealer_delivery_resolution')))
                 GROUP BY b.merchant_id,m.item_id
                """,project,owner,owner,Timestamp.from(from),Timestamp.from(to))
                .forEach(r->sales.put(Arrays.asList(r.get("merchant_id"),r.get("item_id")),number(r,"quantity")));
        var transit=new HashMap<List<Object>,Integer>();
        jdbc.queryForList("""
                SELECT merchant_id,item_id,SUM(quantity-accepted_quantity-terminated_quantity) AS quantity FROM inventory_purchase_requests
                 WHERE project_id=? AND (?::text IS NULL OR merchant_id=?) AND status IN('approved','ordered','expected','arrived')
                 GROUP BY merchant_id,item_id
                """,project,owner,owner)
                .forEach(r->transit.put(Arrays.asList(r.get("merchant_id"),r.get("item_id")),number(r,"quantity")));
        var rows=new ArrayList<Map<String,Object>>();
        for(var r:jdbc.queryForList("""
                SELECT b.merchant_id,b.item_id,i.item_name,i.min_quantity,SUM(b.available_quantity) AS available_quantity
                  FROM inventory_stock_balances b JOIN inventory_items i ON i.id=b.item_id AND i.status='active'
                 WHERE b.project_id=? AND (?::text IS NULL OR b.merchant_id=?)
                 GROUP BY b.merchant_id,b.item_id,i.item_name,i.min_quantity
                 ORDER BY i.item_name,b.merchant_id NULLS FIRST,b.item_id
                """,project,owner,owner)){
            var key=Arrays.asList(r.get("merchant_id"),r.get("item_id"));
            int safety=number(r,"min_quantity"),sold=sales.getOrDefault(key,0),available=number(r,"available_quantity"),inTransit=transit.getOrDefault(key,0);
            int suggested=safety+sold-available-inTransit;
            if(suggested<=0)continue;
            var row=new LinkedHashMap<String,Object>();
            row.put("project_id",project);row.put("merchant_id",r.get("merchant_id"));row.put("item_id",r.get("item_id"));row.put("item_name",r.get("item_name"));
            row.put("safety_stock",safety);row.put("sales_quantity",sold);row.put("available_quantity",available);row.put("in_transit_quantity",inTransit);
            row.put("suggested_quantity",suggested);
            row.put("reason","安全库存 "+safety+" ＋ 近 7 天销量 "+sold+" － 当前可用 "+available+" － 在途 "+inTransit+" ＝ 建议采购 "+suggested);
            rows.add(row);
        }
        return rows;
        });
    }
    private boolean optionVisible(String project,String owner){
        return List.of(VIEW,MANAGE,CONTRACT,APPROVE,FINANCE).stream().anyMatch(permission->auth.currentActorHasPermission(permission)&&visible(project,owner,permission));
    }

    public PageResponse<PurchaseRequestRecord> list() {
        return read("inventory.purchase_request_viewed","inventory.purchase_request_view_failed",VIEW,"inventory_purchase_request","all",()->{
        auth.requirePermission(VIEW);
        var rows=inventory.listPurchaseRequests().stream().filter(p->visible(purchaseFacts(p.id()),VIEW)).toList();
        return new PageResponse<>(rows,1,rows.size(),rows.size());
        });
    }
    public PageResponse<SupplyContractRecord> contracts() {
        return read("inventory.supply_contract.viewed","inventory.supply_contract.view_failed",CONTRACT,"supply_contract","all",()->{
        auth.requirePermission(CONTRACT);
        var rows=inventory.listSupplyContracts().stream().filter(c->visible(c.projectId(),c.merchantId(),CONTRACT)).map(this::maskContract).toList();
        return new PageResponse<>(rows,1,rows.size(),rows.size());
        });
    }
    @Transactional
    @GuardedAction(capability=MANAGE)
    public PurchaseRequestRecord create(PurchaseRequestCreate r) {
        if(r==null) throw invalid("请填写采购申请");
        if(r.items()==null||r.items().size()!=1) throw ApiException.badRequest("PURCHASE_SINGLE_ITEM_REQUIRED","每份采购申请只允许一个商品，请分单提交");
        if(r.supplierId()==null||r.supplierId().isBlank()) throw ApiException.badRequest("PURCHASE_SUPPLIER_REQUIRED","采购申请须关联供应商");
        String contractId=requireText(r.supplyContractId(),"supply_contract_id");
        var contract=contract(contractId,MANAGE);
        String item=requireText(Objects.toString(r.items().getFirst().get("item_id"),null),"item_id");
        Object rawQuantity=r.items().getFirst().get("quantity");
        int quantity;try{quantity=new java.math.BigDecimal(Objects.toString(rawQuantity,"")).intValueExact();}catch(RuntimeException e){throw invalid("采购数量须为正整数");}
        if(quantity<=0) throw invalid("采购数量须为正整数");
        requirePurchasable(contract,item); if(!contract.supplierId().equals(r.supplierId())) throw invalid("供应商与所选合同不一致");
        var product=inventory.findInventoryItemById(item).orElseThrow(()->invalid("采购商品不存在"));
        if(!"active".equals(product.status()) || (product.merchantId()!=null&&!Objects.equals(product.merchantId(),contract.merchantId()))) throw invalid("商品不在合同货权范围");
        return write("createPurchaseRequest",r.idempotencyKey(),r,MANAGE,"inventory_purchase_request",contractId,()->{
            var current=contract(contractId,MANAGE);requirePurchasable(current,item);
            String id="purchase_"+slug(r.idempotencyKey());Instant now=Instant.now();
            var saved=inventory.savePurchaseRequest(new PurchaseRequestRecord(id,"PR-"+slug(r.idempotencyKey()),r.supplierId(),item,quantity,
                    r.expectedArrivalDate()==null?today():r.expectedArrivalDate(),"created",safe(r.evidenceAttachmentIds()),now,now,current.merchantId(),
                    contractId,null,null,null,null,null));
            jdbc.update("UPDATE inventory_purchase_requests SET project_id=?,requested_by=? WHERE id=?",current.projectId(),actor(),id);
            return saved;
        },PurchaseRequestRecord.class);
    }
    @Transactional
    @GuardedAction(capability=CONTRACT)
    public SupplyContractRecord createContract(SupplyContractCreateRequest r) {
        if(r==null) throw invalid("请填写供货合同");
        String project=requireText(r.projectId(),"project_id"),supplier=requireText(r.supplierId(),"supplier_id");
        String owner=merchantScope().orElse(r.merchantId());requireScope(project,owner,CONTRACT);requireOwner(project,owner);
        if(!Set.of("platform_self_use","merchant_self_purchase","platform_purchase_distribution").contains(Objects.toString(r.contractMode(),""))) throw invalid("请选择合同采购模式");
        if("merchant_self_purchase".equals(r.contractMode())&&owner==null) throw invalid("商户自采必须指定商户");
        if(!"merchant_self_purchase".equals(r.contractMode())&&owner!=null) throw invalid("平台采购合同不能登记为商户自采货权");
        requireSupplier(supplier,owner);
        if(r.validFrom()==null||r.validTo()==null||r.validTo().isBefore(r.validFrom())) throw invalid("须完整登记有效期");
        return maskContract(write("createSupplyContract",r.idempotencyKey(),r,CONTRACT,"supply_contract",project,()->{
            String id="supply_contract_"+slug(r.idempotencyKey());Instant now=Instant.now();
            var saved=inventory.saveSupplyContract(new SupplyContractRecord(id,"SC-"+slug(r.idempotencyKey()),supplier,project,owner,r.contractMode(),
                    r.supplyScope(),r.priceTerms(),r.paymentTermDays(),r.validFrom(),r.validTo(),r.summary(),safe(r.purchasableScope()),"draft","active",now,now));
            jdbc.update("UPDATE supply_contracts SET requested_by=? WHERE id=?",actor(),id);return saved;
        },SupplyContractRecord.class));
    }
    @Transactional
    @GuardedAction(capability=CONTRACT)
    public Map<String,Object> terms(String id,PurchaseWorkflowRequests.Terms r) {
        var initial=contract(id,CONTRACT);if(r==null||r.terms()==null) throw invalid("请填写合同执行条款");
        return maskContractResult(write("purchaseContractTerms:"+id,r.idempotencyKey(),r,CONTRACT,"supply_contract",id,()->{
            var c=contract(id,CONTRACT);var facts=contractFacts(id);
            if(!"draft".equals(c.approvalStatus())||facts.get("review_id")!=null) throw invalid("合同已提交审批或已生效，条款不得改写");
            r.terms().requirePricesFor(c.purchasableScope());requirePurchaser(c,r.terms());checkEvidence("supply_contract",id,r.evidenceAttachmentIds());
            jdbc.update("UPDATE supply_contracts SET settlement_terms=?::jsonb,terms_evidence_ids=?::jsonb,updated_at=now() WHERE id=?",json(r.terms()),json(r.evidenceAttachmentIds()),id);
            return contractDetail(id);
        },Map.class),initial.projectId());
    }
    public Map<String,Object> contractDetail(String id) {
        return read("inventory.supply_contract.viewed","inventory.supply_contract.view_failed",CONTRACT,"supply_contract",id,()->{
        var c=contract(id,CONTRACT);var result=new LinkedHashMap<String,Object>();
        result.put("contract",maskContract(c));var f=contractFacts(id);result.put("review_id",f.get("review_id"));
        result.put("approved_at",f.get("approved_at"));
        result.put("terms",canPrice(c.projectId())?readTerms(f.get("settlement_terms")):null);return result;
        });
    }
    @Transactional
    @GuardedAction(capability=CONTRACT)
    public SupplyContractRecord submitContract(String id,SupplyContractApproveRequest r) {
        var c=contract(id,CONTRACT);if(r==null||!"approved".equals(r.decision())) throw invalid("请发起合同审批；驳回通过审批节点办理");
        return maskContract(write("approveSupplyContract:"+id,r.idempotencyKey(),r,CONTRACT,"supply_contract",id,()->{
            var current=contract(id,CONTRACT);var f=contractFacts(id);
            if(!"draft".equals(current.approvalStatus())||f.get("review_id")!=null) throw invalid("合同已进入审批或已处理");
            var terms=readTerms(f.get("settlement_terms"));if(terms==null) throw incomplete();terms.requirePricesFor(current.purchasableScope());requirePurchaser(current,terms);
            requireText(r.reason(),"reason");checkEvidence("supply_contract",id,contractProof(id));
            var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",r.reason(),contractProof(id),
                    "inventory.supply_contract","supply_contract",id,null,json(Map.of("reason",r.reason(),"contract",current,"terms",terms,"evidence_attachment_ids",contractProof(id))),r.finalApproverId(),r.parallelLines(),r.beneficiaryId()));
            requireBlocked(review);requireApplicantExcluded(review.reviewId(),f.get("requested_by"));jdbc.update("UPDATE supply_contracts SET review_id=? WHERE id=?",review.reviewId(),id);return current;
        },SupplyContractRecord.class));
    }
    @Transactional
    @GuardedAction(capability=MANAGE,admissionAction="inventory.purchase_approve",objectType="inventory_purchase_request")
    public PurchaseRequestRecord submitPurchase(String id,PurchaseRequestApproveRequest r) {
        scopedPurchase(id,MANAGE);
        if(r==null||!"approved".equals(r.decision())) throw invalid("请发起采购审批；驳回通过审批节点办理");
        return write("approvePurchaseRequest:"+id,r.idempotencyKey(),r,MANAGE,"inventory_purchase_request",id,()->{
            var p=lockedPurchase(id,MANAGE);var f=purchaseFacts(id);
            if(!Set.of("created","submitted").contains(p.status())||f.get("review_id")!=null) throw invalid("采购申请已进入审批或已处理");
            if(r.supplyContractId()!=null&&!r.supplyContractId().equals(p.supplyContractId())) throw invalid("采购审批不得替换原合同");
            var c=contract(p.supplyContractId(),MANAGE);requirePurchasable(c,p.itemId());requireQualified(p);
            var terms=readTerms(contractFacts(c.id()).get("settlement_terms"));if(terms==null)throw incomplete();terms.requireComplete();
            requireText(r.note(),"note");checkEvidence("inventory_purchase_request",id,r.evidenceAttachmentIds());
            Map<String,Object> snapshot=new LinkedHashMap<>();snapshot.put("contract",c);snapshot.put("terms",terms);
            snapshot.put("approved_at",contractFacts(c.id()).get("approved_at"));snapshot.put("locked_at",Instant.now().toString());
            var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",r.note(),r.evidenceAttachmentIds(),
                    "inventory.purchase_request","inventory_purchase_request",id,null,json(Map.of("reason",r.note(),"purchase",p,"contract_snapshot",snapshot)),r.finalApproverId(),r.parallelLines(),r.beneficiaryId()));
            requireBlocked(review);requireApplicantExcluded(review.reviewId(),f.get("requested_by"));
            jdbc.update("UPDATE inventory_purchase_requests SET review_id=?,contract_snapshot=?::jsonb,evidence_attachment_ids=?::jsonb,status='submitted',updated_at=now() WHERE id=?",review.reviewId(),json(snapshot),json(r.evidenceAttachmentIds()),id);
            return readPurchase(id);
        },PurchaseRequestRecord.class);
    }
    @Transactional
    @GuardedAction(capability=MANAGE,objectType="inventory_purchase_request")
    public PurchaseRequestRecord transition(String id,PurchaseRequestTransitionRequest r,String state) {
        scopedPurchase(id,MANAGE);if(r==null)throw invalid("请填写采购进度");
        return write("transitionPurchaseRequest:"+state+":"+id,r.idempotencyKey(),r,MANAGE,"inventory_purchase_request",id,()->{
            var p=lockedPurchase(id,MANAGE);requireSnapshot(p);
            if("ordered".equals(state)){
                if(!"approved".equals(p.status()))throw invalid("只有已完整批准采购才能下单");requireQualified(p);
                jdbc.update("UPDATE inventory_purchase_requests SET status='ordered',ordered_at=now(),updated_at=now() WHERE id=?",id);
                createPrepayment(readPurchase(id));
            }else{
                if(!Set.of("ordered","expected").contains(p.status()))throw invalid("仅已下单采购可登记到货");
                jdbc.update("UPDATE inventory_purchase_requests SET status='arrived',actual_arrival_date=?,updated_at=now() WHERE id=?",today(),id);
            }
            event(p,state,r.note());return readPurchase(id);
        },PurchaseRequestRecord.class);
    }
    @Transactional
    @GuardedAction(capability=MANAGE,admissionAction="inventory.purchase_receive",objectType="inventory_purchase_request")
    public PurchaseRequestRecord receive(String id,PurchaseRequestReceiveRequest r) {
        var before=scopedPurchase(id,MANAGE);requireSnapshot(before);
        if(r==null)throw invalid("请填写实际收货");
        return write("receivePurchaseRequest:"+id,r.idempotencyKey(),r,MANAGE,"inventory_purchase_request",id,()->{
            var p=lockedPurchase(id,MANAGE);var f=purchaseFacts(id);var terms=requireSnapshot(p);
            if(!Set.of("ordered","expected","arrived").contains(p.status()))throw invalid("采购单不在待收货状态");
            requireQualified(p);requireSupplier(p.supplierId(),p.merchantId());
            if(r.arrivedQuantity()==null||r.acceptedQuantity()==null||r.rejectedQuantity()==null||r.arrivedQuantity()<=0
                    ||r.acceptedQuantity()<0||r.rejectedQuantity()<0||(long)r.acceptedQuantity()+r.rejectedQuantity()!=r.arrivedQuantity())throw invalid("实到须完整分为合格接收和拒收数量");
            int remaining=p.quantity()-number(f,"accepted_quantity")-number(f,"terminated_quantity");
            if(r.arrivedQuantity()>remaining)throw invalid("本次实到数量超过采购未处理数量");
            if(r.unitCostCent()==null||r.unitCostCent()<0)throw invalid("须录入实际到货单价");
            requireActualTime(r.receivedAt(),p.orderedAt());checkEvidence("inventory_purchase_request",id,r.evidenceAttachmentIds());
            String wh=requireText(r.warehouseId(),"warehouse_id"),location=requireText(r.locationId(),"location_id");requireWarehouse(p,wh,location);
            // 第十八批 M 段第 119 条 119-R1（BSM:1936「……库房、库位……都要先在基础管理中定义清楚。」）：只收接收仓库下已建档的库位（退供交接只扣减，不在 requireWarehouse 里查）。
            if(!inventory.isWarehouseLocation(wh,location))throw invalid("库位须为接收仓库下已建档的库位，请先在库存台账「库房」区块建库位");
            String receipt="prec_"+slug(id+":"+r.idempotencyKey());String batch=null,movement=null;
            if(r.acceptedQuantity()>0){
                lockStock(p.itemId(),location,wh,(String)f.get("project_id"),p.merchantId());
                jdbc.update("UPDATE inventory_stock_balances SET quantity=quantity+?,available_quantity=available_quantity+?,updated_at=now() WHERE item_id=? AND location_id=?",r.acceptedQuantity(),r.acceptedQuantity(),p.itemId(),location);
                batch="pb_"+slug(receipt);inventory.saveStockBatch(new InventoryStockBatchRecord(batch,p.itemId(),location,receipt,p.supplierId(),r.acceptedQuantity(),null,"active",r.evidenceAttachmentIds(),r.receivedAt(),r.receivedAt(),p.merchantId()),(String)f.get("project_id"));
                var m=inventory.saveStockMovement(new StockMovementRecord("pm_"+slug(receipt),"inbound",p.itemId(),null,location,r.acceptedQuantity(),r.unitCostCent(),null,"purchase_request",p.id(),null,actor(),r.evidenceAttachmentIds(),"completed",r.receivedAt(),p.merchantId()),(String)f.get("project_id"));movement=m.id();
            }
            jdbc.update("""
                    INSERT INTO purchase_receipts(id,purchase_id,project_id,merchant_id,supplier_id,item_id,warehouse_id,location_id,batch_id,
                     arrived_quantity,accepted_quantity,rejected_quantity,unit_cost_cent,received_at,received_by,evidence_attachment_ids,movement_id)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?)
                    """,receipt,id,f.get("project_id"),p.merchantId(),p.supplierId(),p.itemId(),wh,location,batch,r.arrivedQuantity(),r.acceptedQuantity(),r.rejectedQuantity(),r.unitCostCent(),Timestamp.from(r.receivedAt()),actor(),json(r.evidenceAttachmentIds()),movement);
            jdbc.update("UPDATE inventory_purchase_requests SET accepted_quantity=accepted_quantity+?,status='arrived',actual_arrival_date=?,updated_at=now() WHERE id=?",r.acceptedQuantity(),r.receivedAt().atZone(LocalZone.ID).toLocalDate(),id);
            if(r.acceptedQuantity()>0&&!"consignment".equals(terms.settlementMethod())){
                long amount=Math.multiplyExact((long)r.acceptedQuantity(),terms.payableUnitPrice(p.itemId(),r.unitCostCent()));
                if(amount>0){String payable=postPayable(p,"purchase_inbound",receipt,amount,due(p,terms,r.receivedAt()));
                    jdbc.update("UPDATE purchase_receipts SET payable_id=? WHERE id=?",payable,receipt);}
            }
            closeQuantityIfComplete(id);event(p,readPurchase(id).status(),r.note());return readPurchase(id);
        },PurchaseRequestRecord.class);
    }

    @Transactional
    @GuardedAction(capability=MANAGE,objectType="inventory_purchase_request")
    public Map<String,Object> resolveQuantity(String id,PurchaseWorkflowRequests.Resolution r) {
        scopedPurchase(id,MANAGE);if(r==null)throw invalid("请填写余量处理");
        return write("purchaseResolution:"+id,r.idempotencyKey(),r,MANAGE,"inventory_purchase_request",id,()->{
            var p=lockedPurchase(id,MANAGE);var f=purchaseFacts(id);requireSnapshot(p);
            if(!Set.of("ordered","expected","arrived").contains(p.status()))throw invalid("仅待交付采购可处理余量");
            if(r.quantity()<=0||r.quantity()>p.quantity()-number(f,"accepted_quantity")-number(f,"terminated_quantity"))throw invalid("处理数量超过未完成采购量");
            if(!Set.of("replenishment","terminate").contains(Objects.toString(r.resolutionType(),"")))throw invalid("请选择补货或终止余量");
            requireText(r.reason(),"reason");checkEvidence("inventory_purchase_request",id,r.evidenceAttachmentIds());
            String resolution="pqr_"+slug(id+":"+r.idempotencyKey());String reviewId=null,status="recorded";
            if("terminate".equals(r.resolutionType())){
                var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",r.reason(),r.evidenceAttachmentIds(),
                        "inventory.purchase_terminate","purchase_quantity_resolution",resolution,null,json(Map.of("reason",r.reason(),"purchase",p,"contract_snapshot",readMap(f.get("contract_snapshot")),
                                "resolution_type",r.resolutionType(),"quantity",r.quantity(),"accepted_quantity",f.get("accepted_quantity"),
                                "terminated_quantity",f.get("terminated_quantity"),"remaining_quantity",p.quantity()-number(f,"accepted_quantity")-number(f,"terminated_quantity"),
                                "receipts",jdbc.queryForList("SELECT * FROM purchase_receipts WHERE purchase_id=? ORDER BY received_at,id",id))),r.finalApproverId(),r.parallelLines(),r.beneficiaryId()));
                requireBlocked(review);reviewId=review.reviewId();status="pending_approval";
            }else if(r.expectedArrivalDate()==null||r.expectedArrivalDate().isBefore(today()))throw invalid("补货须登记有效预计到货日");
            jdbc.update("""
                    INSERT INTO purchase_quantity_resolutions(id,purchase_id,resolution_type,quantity,expected_arrival_date,reason,review_id,status,evidence_attachment_ids,requested_by)
                    VALUES(?,?,?,?,?,?,?,?,?::jsonb,?)
                    """,resolution,id,r.resolutionType(),r.quantity(),r.expectedArrivalDate(),r.reason(),reviewId,status,json(r.evidenceAttachmentIds()),actor());
            return resolution(resolution);
        },Map.class);
    }
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord d) {
        if(!Set.of("approved","rejected").contains(d.reviewStatus()))return;
        if(!Set.of("inventory.supply_contract","inventory.purchase_request","inventory.purchase_terminate","inventory.purchase_return").contains(d.actionType()))return;
        if(!reviews.findReview(d.reviewId()).filter(actual->Objects.equals(actual.actionType(),d.actionType())
                &&Objects.equals(actual.objectType(),d.objectType())&&Objects.equals(actual.objectId(),d.objectId())
                &&Objects.equals(actual.reviewStatus(),d.reviewStatus())).isPresent())return;
        if("inventory.supply_contract".equals(d.actionType())&&"supply_contract".equals(d.objectType())){
            contractLock(d.objectId());var f=contractFacts(d.objectId());
            if(!d.reviewId().equals(f.get("review_id"))||!"draft".equals(f.get("approval_status")))return;
            if("approved".equals(d.reviewStatus())) {
                var c=inventory.findSupplyContractByIdForUpdate(d.objectId()).orElseThrow();var terms=readTerms(f.get("settlement_terms"));
                if(terms==null)throw incomplete();terms.requirePricesFor(c.purchasableScope());requirePurchaser(c,terms);
            }
            jdbc.update("UPDATE supply_contracts SET approval_status=?,approved_at=CASE WHEN ?='approved' THEN now() ELSE NULL END,updated_at=now() WHERE id=?",
                    d.reviewStatus(),d.reviewStatus(),d.objectId());return;
        }
        if("inventory.purchase_request".equals(d.actionType())&&"inventory_purchase_request".equals(d.objectType())){
            purchaseLock(d.objectId());var p=readPurchase(d.objectId());var f=purchaseFacts(p.id());
            if(!d.reviewId().equals(f.get("review_id"))||!"submitted".equals(p.status()))return;
            if("approved".equals(d.reviewStatus())){requireSnapshot(p);requireQualified(p);}
            jdbc.update("UPDATE inventory_purchase_requests SET status=?,approved_by=?,updated_at=now() WHERE id=?",d.reviewStatus(),actor(),p.id());return;
        }
        if("inventory.purchase_terminate".equals(d.actionType())&&"purchase_quantity_resolution".equals(d.objectType())){
            var initial=resolution(d.objectId());String purchase=(String)initial.get("purchase_id");purchaseLock(purchase);
            var current=resolution(d.objectId());if(!d.reviewId().equals(current.get("review_id"))||!"pending_approval".equals(current.get("status")))return;
            if("approved".equals(d.reviewStatus())){
                var f=purchaseFacts(purchase);int qty=number(current,"quantity");
                if(qty>number(f,"quantity")-number(f,"accepted_quantity")-number(f,"terminated_quantity"))throw invalid("审批期间余量已变化，请重新核对终止数量");
                jdbc.update("UPDATE inventory_purchase_requests SET terminated_quantity=terminated_quantity+?,updated_at=now() WHERE id=?",qty,purchase);
            }
            jdbc.update("UPDATE purchase_quantity_resolutions SET status=? WHERE id=?",d.reviewStatus(),d.objectId());closeQuantityIfComplete(purchase);return;
        }
        if("inventory.purchase_return".equals(d.actionType())&&"purchase_return".equals(d.objectType())){
            var ret=returnRow(d.objectId());var source=receipt((String)ret.get("receipt_id"));purchaseLock((String)source.get("purchase_id"));
            if(d.reviewId().equals(ret.get("review_id"))&&"pending_approval".equals(ret.get("status")))
                jdbc.update("UPDATE purchase_returns SET status=? WHERE id=?",d.reviewStatus(),d.objectId());
        }
    }
    @Transactional
    @GuardedAction(capability=MANAGE,objectType="inventory_purchase_request")
    public Map<String,Object> sale(String id,PurchaseWorkflowRequests.Sale r) {
        scopedPurchase(id,MANAGE);if(r==null)throw invalid("请登记寄售售出事实");
        return write("purchaseConsignmentSale:"+id,r.idempotencyKey(),r,MANAGE,"inventory_purchase_request",id,()->{
            var p=lockedPurchase(id,MANAGE);var terms=requireSnapshot(p);var source=receipt(r.receiptId());
            if(!id.equals(source.get("purchase_id"))||!"consignment".equals(terms.settlementMethod()))throw invalid("仅可登记本采购寄售入库的售出事实");
            if(!terms.consignmentSaleBasis().equals(r.saleBasis()))throw invalid("售出依据须与锁定合同一致");
            int accounted=count("SELECT COALESCE(SUM(quantity),0) FROM purchase_consignment_sales WHERE receipt_id=? AND status IN('pending_confirmation','confirmed')",r.receiptId());
            int returned=count("SELECT COALESCE(SUM(quantity),0) FROM purchase_returns WHERE receipt_id=? AND consignment_sale_id IS NULL AND status IN('pending_approval','approved','handed_over')",r.receiptId());
            if(r.quantity()<=0||r.quantity()>number(source,"accepted_quantity")-accounted-returned)throw invalid("售出数量超过该寄售来源尚未确认及退回的数量");
            requireActualTime(r.soldAt(),instant(source.get("received_at")));requireText(r.sourceReference(),"source_reference");
            checkEvidence("inventory_purchase_request",id,r.evidenceAttachmentIds());
            String sale="pcs_"+slug(id+":"+r.idempotencyKey());
            jdbc.update("""
                    INSERT INTO purchase_consignment_sales(id,receipt_id,quantity,sale_basis,source_reference,sold_at,evidence_attachment_ids,status,requested_by)
                    VALUES(?,?,?,?,?,?,?::jsonb,'pending_confirmation',?)
                    """,sale,r.receiptId(),r.quantity(),r.saleBasis(),r.sourceReference(),Timestamp.from(r.soldAt()),json(r.evidenceAttachmentIds()),actor());
            return saleRow(sale);
        },Map.class);
    }
    @Transactional
    @GuardedAction(capability=APPROVE)
    public Map<String,Object> confirmSale(String id,PurchaseWorkflowRequests.SaleConfirmation r) {
        var initial=saleRow(id);var receipt=receipt((String)initial.get("receipt_id"));String purchase=(String)receipt.get("purchase_id");
        var p=scopedPurchase(purchase,APPROVE);requireScope((String)purchaseFacts(purchase).get("project_id"),p.merchantId(),FINANCE);
        if(r==null)throw invalid("请确认真实售出事实");
        return write("confirmPurchaseConsignmentSale:"+id,r.idempotencyKey(),r,APPROVE,"inventory_purchase_request",purchase,()->{
            var current=lockedPurchase(purchase,APPROVE);requireScope((String)purchaseFacts(purchase).get("project_id"),current.merchantId(),FINANCE);
            var sale=saleRow(id);if(!"pending_confirmation".equals(sale.get("status")))throw invalid("售出确认已处理");
            if(actor().equals(sale.get("requested_by")))throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN","售出事实须由不同人员复核");
            if(!Set.of("confirmed","rejected").contains(Objects.toString(r.decision(),"")))throw invalid("请选择确认或驳回");
            requireText(r.reason(),"reason");checkEvidence("inventory_purchase_request",purchase,r.evidenceAttachmentIds());String payable=null;
            if("confirmed".equals(r.decision())){
                var terms=requireSnapshot(current);long amount=Math.multiplyExact((long)number(sale,"quantity"),terms.payableUnitPrice(current.itemId(),longNumber(receipt,"unit_cost_cent")));
                if(amount>0)payable=postPayable(current,"purchase_consignment",id,amount,due(current,terms,instant(sale.get("sold_at"))));
            }
            jdbc.update("UPDATE purchase_consignment_sales SET status=?,confirmed_by=?,confirmed_at=now(),payable_id=? WHERE id=?",r.decision(),actor(),payable,id);
            return saleRow(id);
        },Map.class);
    }
    @Transactional
    @GuardedAction(capability=FINANCE,objectType="inventory_purchase_request")
    public Map<String,Object> allocate(String id,PurchaseWorkflowRequests.Allocation r) {
        scopedPurchase(id,FINANCE);if(r==null)throw invalid("请填写预付款冲抵");
        return write("allocatePurchasePrepayment:"+id,r.idempotencyKey(),r,FINANCE,"inventory_purchase_request",id,()->{
            var p=lockedPurchase(id,FINANCE);var terms=requireSnapshot(p);if(!"prepaid".equals(terms.settlementMethod()))throw invalid("本合同不是预付款结算");
            checkEvidence("inventory_purchase_request",id,r.evidenceAttachmentIds());
            var payment=prepaymentSource(p,r.sourcePaymentId());
            var target=finance.findArApItemByIdForUpdate(r.targetItemId()).orElseThrow(()->invalid("目标应付不存在"));
            requireFinancialTarget(p,target);if(!"purchase_inbound".equals(target.sourceType()))throw invalid("预付只能冲本采购合格入库货款");
            if(count("SELECT count(*) FROM purchase_receipts WHERE purchase_id=? AND id=?",id,target.sourceId())!=1)throw invalid("应付不是本采购合格入库的真实来源");
            if(!finance.findCurrentPurchasePayable(target.sourceType(),target.sourceId()).map(current->target.id().equals(current.id())).orElse(false))
                throw invalid("应付不是本采购的当前有效账款");
            if(finance.findArApItemLockedDayCloseId(target.id()).isPresent())throw ApiException.conflict("DAY_CLOSE_LOCKED","应付已锁账，不能直接冲抵；须沿补录审批处理");
            long used=prepaymentAllocated(r.sourcePaymentId())+prepaymentRefundReserved(r.sourcePaymentId(),null);
            if(r.amountCent()<=0||r.amountCent()>longNumber(payment,"amount_cent")-used||r.amountCent()>target.unpaidAmountCent())throw invalid("冲抵超过原实付余额或本次未付货款");
            String allocation="ppa_"+slug(id+":"+r.idempotencyKey());
            jdbc.update("INSERT INTO purchase_prepayment_allocations(id,purchase_id,source_payment_id,target_item_id,amount_cent,allocated_by,evidence_attachment_ids) VALUES(?,?,?,?,?,?,?::jsonb)",
                    allocation,id,r.sourcePaymentId(),target.id(),r.amountCent(),actor(),json(r.evidenceAttachmentIds()));
            long paid=Math.addExact(target.paidAmountCent(),r.amountCent()),unpaid=target.unpaidAmountCent()-r.amountCent();
            finance.updateArApItemLedger(new ArApItemRecord(target.id(),target.itemNo(),target.direction(),target.counterpartyId(),target.sourceType(),target.sourceId(),target.amountCent(),paid,unpaid,
                    target.currency(),target.dueDate(),target.accountingPeriod(),unpaid==0?"settled":"partial_paid",target.createdAt(),Instant.now(),target.merchantId(),target.projectId(),target.originItemId(),target.adjustmentId()));
            return jdbc.queryForMap("SELECT * FROM purchase_prepayment_allocations WHERE id=?",allocation);
        },Map.class);
    }
    @Transactional
    @GuardedAction(capability=MANAGE,objectType="inventory_purchase_request")
    public Map<String,Object> proposeReturn(String id,PurchaseWorkflowRequests.Return r) {
        scopedPurchase(id,MANAGE);if(r==null)throw invalid("请填写退供应商方案");
        return maskMoney(write("purchaseReturn:"+id,r.idempotencyKey(),r,MANAGE,"inventory_purchase_request",id,()->{
            var p=lockedPurchase(id,MANAGE);var source=receipt(r.receiptId());if(!id.equals(source.get("purchase_id")))throw invalid("退供须关联本采购入库");
            int pending=count("SELECT COALESCE(SUM(quantity),0) FROM purchase_returns WHERE receipt_id=? AND status IN('pending_approval','approved','handed_over')",r.receiptId());
            if(r.quantity()<=0||r.quantity()>number(source,"accepted_quantity")-pending||r.returnUnitPriceCent()<0)throw invalid("退供数量或单价无效");
            requireText(r.reason(),"reason");requireText(r.responsibilityTerms(),"responsibility_terms");checkEvidence("inventory_purchase_request",id,r.evidenceAttachmentIds());
            var lockedTerms=requireSnapshot(p);
            if("consignment".equals(lockedTerms.settlementMethod())&&r.consignmentSaleId()==null){
                int sold=count("SELECT COALESCE(SUM(quantity),0) FROM purchase_consignment_sales WHERE receipt_id=? AND status IN(\'pending_confirmation\',\'confirmed\')",r.receiptId());
                int unsoldReturns=count("SELECT COALESCE(SUM(quantity),0) FROM purchase_returns WHERE receipt_id=? AND consignment_sale_id IS NULL AND status IN(\'pending_approval\',\'approved\',\'handed_over\')",r.receiptId());
                if(r.quantity()>number(source,"accepted_quantity")-sold-unsoldReturns)throw invalid("未售退回不能占用已确认售出或待核实售出数量");
            }
            long amount=Math.multiplyExact((long)r.quantity(),r.returnUnitPriceCent());String ret="pret_"+slug(id+":"+r.idempotencyKey());
            if(r.consignmentSaleId()!=null){var sale=saleRow(r.consignmentSaleId());
                if(!r.receiptId().equals(sale.get("receipt_id"))||!"confirmed".equals(sale.get("status")))throw invalid("寄售退供须引用本入库已确认售出结算");
                int occupied=count("SELECT COALESCE(SUM(quantity),0) FROM purchase_returns WHERE consignment_sale_id=? AND status IN(\'pending_approval\',\'approved\',\'handed_over\')",r.consignmentSaleId());
                if(r.quantity()>number(sale,"quantity")-occupied)throw invalid("退供超过该售出结算尚未退回数量");}
            var original=new LinkedHashMap<String,Object>();original.put("reason",r.reason());original.put("purchase",p);
            original.put("contract_snapshot",readMap(purchaseFacts(id).get("contract_snapshot")));original.put("receipt",source);
            original.put("quantity",r.quantity());original.put("return_unit_price_cent",r.returnUnitPriceCent());original.put("confirmed_amount_cent",amount);
            original.put("responsibility_terms",r.responsibilityTerms());original.put("consignment_sale",r.consignmentSaleId()==null?null:saleRow(r.consignmentSaleId()));
            var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",r.reason(),r.evidenceAttachmentIds(),
                    "inventory.purchase_return","purchase_return",ret,amount,json(original),r.finalApproverId(),r.parallelLines(),r.beneficiaryId()));
            requireBlocked(review);jdbc.update("""
                    INSERT INTO purchase_returns(id,receipt_id,quantity,return_unit_price_cent,confirmed_amount_cent,responsibility_terms,reason,evidence_attachment_ids,review_id,status,requested_by,consignment_sale_id)
                    VALUES(?,?,?,?,?,?,?,?::jsonb,?,'pending_approval',?,?)
                    """,ret,r.receiptId(),r.quantity(),r.returnUnitPriceCent(),amount,r.responsibilityTerms(),r.reason(),json(r.evidenceAttachmentIds()),review.reviewId(),actor(),r.consignmentSaleId());
            return returnRow(ret);
        },Map.class),(String)purchaseFacts(id).get("project_id"));
    }
    @Transactional
    @GuardedAction(capability=MANAGE,objectType="purchase_return")
    public Map<String,Object> handover(String id,PurchaseWorkflowRequests.Handover r) {
        var ret=returnRow(id);var receipt=receipt((String)ret.get("receipt_id"));String purchase=(String)receipt.get("purchase_id");
        scopedPurchase(purchase,MANAGE);if(r==null)throw invalid("请登记供应商实际交接");
        return maskMoney(write("purchaseReturnHandover:"+id,r.idempotencyKey(),r,MANAGE,"purchase_return",id,()->{
            var p=lockedPurchase(purchase,MANAGE);var current=returnRow(id);var source=receipt((String)current.get("receipt_id"));
            if(!"approved".equals(current.get("status"))||!reviews.findReview((String)current.get("review_id")).map(d->"approved".equals(d.reviewStatus())).orElse(false))throw invalid("退供须完整批准后才能交接，且不能重复出库");
            requireActualTime(r.handedOverAt(),instant(current.get("requested_at")));checkEvidence("purchase_return",id,r.evidenceAttachmentIds());
            String location=(String)source.get("location_id"),warehouse=(String)source.get("warehouse_id"),project=(String)source.get("project_id");
            requireWarehouse(p,warehouse,location);int qty=number(current,"quantity");
            lockStock(p.itemId(),location,warehouse,project,p.merchantId());
            var batch=jdbc.queryForMap("SELECT * FROM inventory_stock_batches WHERE id=? FOR UPDATE",source.get("batch_id"));
            if(number(batch,"quantity")<qty)throw invalid("原入库批次现存实物不足，不得退回已经消耗的货物");
            int changed=jdbc.update("UPDATE inventory_stock_balances SET quantity=quantity-?,available_quantity=available_quantity-?,updated_at=now() WHERE item_id=? AND location_id=? AND available_quantity>=?",qty,qty,p.itemId(),location,qty);
            if(changed!=1)throw invalid("可用库存不足，不能完成实物退供");
            jdbc.update("UPDATE inventory_stock_batches SET quantity=quantity-?,status=CASE WHEN quantity=? THEN 'consumed' ELSE status END,updated_at=now() WHERE id=?",qty,qty,source.get("batch_id"));
            var movement=inventory.saveStockMovement(new StockMovementRecord("prm_"+slug(id),"return_to_supplier",p.itemId(),location,null,qty,null,null,
                    "purchase_return",id,null,actor(),r.evidenceAttachmentIds(),"completed",r.handedOverAt(),p.merchantId()),project);
            Long cost=movement.totalCostCent(),difference=cost==null?null:Math.subtractExact(longNumber(current,"confirmed_amount_cent"),cost);
            jdbc.update("""
                    UPDATE purchase_returns SET status='handed_over',handed_over_by=?,handed_over_at=?,handover_evidence_ids=?::jsonb,movement_id=?,
                     actual_cost_cent=?,cost_difference_cent=? WHERE id=?
                    """,actor(),Timestamp.from(r.handedOverAt()),json(r.evidenceAttachmentIds()),movement.id(),cost,difference,id);
            return returnRow(id);
        },Map.class),(String)receipt.get("project_id"));
    }

    @Transactional
    @GuardedAction(capability=FINANCE,objectType="purchase_return")
    public Map<String,Object> reviewReturnFinance(String id,PurchaseWorkflowRequests.ReturnFinanceReview r) {
        var initial=returnRow(id);var receipt=receipt((String)initial.get("receipt_id"));String purchase=(String)receipt.get("purchase_id");
        scopedPurchase(purchase,FINANCE);if(r==null)throw invalid("请填写退供财务复核");
        return maskInventoryCosts(write("reviewPurchaseReturnFinance:"+id,r.idempotencyKey(),r,FINANCE,"purchase_return",id,()->{
            var p=lockedPurchase(purchase,FINANCE);var current=returnRow(id);var source=receipt((String)current.get("receipt_id"));
            if(!"handed_over".equals(current.get("status"))||!"pending_review".equals(current.get("accounting_status")))throw invalid("须先完成实际交接，财务事项不能重复入账");
            if(actor().equals(current.get("requested_by")))throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN","退供申请人不能自行完成财务复核");
            requireText(r.reason(),"reason");checkEvidence("purchase_return",id,r.evidenceAttachmentIds());
            if(current.get("actual_cost_cent")==null)throw invalid("实际出库成本尚未确定，须核实成本差额");
            long refund=longNumber(current,"confirmed_amount_cent"),reduced=0;String receivable=null;
            var terms=requireSnapshot(p);String payableId=(String)source.get("payable_id");String saleId=(String)current.get("consignment_sale_id");
            if("consignment".equals(terms.settlementMethod())){
                if(saleId==null)refund=0;
                else{var sale=saleRow(saleId);if(!source.get("id").equals(sale.get("receipt_id"))||!"confirmed".equals(sale.get("status")))throw invalid("寄售退供的原售出结算不一致");payableId=(String)sale.get("payable_id");}
            } else if(saleId!=null)throw invalid("普通采购退供不能引用寄售售出结算");
            if(refund>0&&payableId!=null){
                var payable=currentFinancialTarget(p,saleId==null?"purchase_inbound":"purchase_consignment",saleId==null?(String)source.get("id"):saleId);
                reduced=Math.min(refund,payable.unpaidAmountCent());
                if(reduced>0)restatePayable(p,payable,reduced,id,(String)source.get("id"),saleId);
            }
            if(refund>reduced){if(r.supplierRefundDueDate()==null)throw invalid("须录入双方确认的供应商退款到期日");
                receivable=postReceivable(p,id,refund-reduced,r.supplierRefundDueDate());}
            jdbc.update("""
                    UPDATE purchase_returns SET accounting_status='posted',finance_reviewed_by=?,finance_reviewed_at=now(),finance_evidence_ids=?::jsonb,
                     supplier_refund_due_date=?,payable_reduction_cent=?,supplier_refund_item_id=? WHERE id=?
                    """,actor(),json(r.evidenceAttachmentIds()),r.supplierRefundDueDate(),reduced,receivable,id);
            return returnRow(id);
        },Map.class),(String)receipt.get("project_id"));
    }

    public Map<String,Object> detail(String id) {
        return read("inventory.purchase_request_viewed","inventory.purchase_request_view_failed",VIEW,"inventory_purchase_request",id,()->{
        var p=scopedPurchase(id,VIEW);var f=purchaseFacts(id);var result=new LinkedHashMap<String,Object>();
        result.put("purchase",p);result.put("project_id",f.get("project_id"));result.put("requested_by",f.get("requested_by"));
        result.put("review_id",f.get("review_id"));result.put("accepted_quantity",f.get("accepted_quantity"));result.put("terminated_quantity",f.get("terminated_quantity"));
        result.put("remaining_quantity",p.quantity()-number(f,"accepted_quantity")-number(f,"terminated_quantity"));
        var lockedTerms=f.get("contract_snapshot")==null?null:requireSnapshot(p);
        result.put("settlement_method",lockedTerms==null?null:lockedTerms.settlementMethod());
        result.put("consignment_sale_basis",lockedTerms==null?null:lockedTerms.consignmentSaleBasis());
        result.put("receipts",jdbc.queryForList("SELECT * FROM purchase_receipts WHERE purchase_id=? ORDER BY received_at,id",id).stream().map(row->maskMoney(row,(String)f.get("project_id"))).toList());
        result.put("resolutions",jdbc.queryForList("SELECT * FROM purchase_quantity_resolutions WHERE purchase_id=? ORDER BY requested_at,id",id));
        result.put("consignment_sales",jdbc.queryForList("SELECT s.* FROM purchase_consignment_sales s JOIN purchase_receipts r ON r.id=s.receipt_id WHERE r.purchase_id=? ORDER BY s.requested_at,s.id",id));
        result.put("returns",jdbc.queryForList("SELECT t.* FROM purchase_returns t JOIN purchase_receipts r ON r.id=t.receipt_id WHERE r.purchase_id=? ORDER BY t.requested_at,t.id",id).stream().map(row->maskMoney(row,(String)f.get("project_id"))).toList());
        boolean financial=auth.currentActorHasPermission(FINANCE)&&visible(f,FINANCE)
                ||auth.currentActorHasPermission("finance.ar_ap.view")&&visible(f,"finance.ar_ap.view");
        if(financial){
            result.put("allocations",jdbc.queryForList("SELECT * FROM purchase_prepayment_allocations WHERE purchase_id=? ORDER BY allocated_at,id",id));
            result.put("payables",jdbc.queryForList("""
                    SELECT a.* FROM ar_ap_items a WHERE
                    (a.source_type='purchase_prepayment' AND a.source_id=?)
                    OR (a.source_type='purchase_inbound' AND a.source_id IN(SELECT id FROM purchase_receipts WHERE purchase_id=?))
                    OR (a.source_type='purchase_consignment' AND a.source_id IN(SELECT s.id FROM purchase_consignment_sales s JOIN purchase_receipts r ON r.id=s.receipt_id WHERE r.purchase_id=?))
                    OR (a.source_type='purchase_return_refund' AND a.source_id IN(SELECT t.id FROM purchase_returns t JOIN purchase_receipts r ON r.id=t.receipt_id WHERE r.purchase_id=?))
                    OR (a.source_type='purchase_prepayment_refund' AND a.source_id IN(SELECT id FROM purchase_prepayment_resolutions WHERE purchase_id=?))
                    ORDER BY a.created_at,a.id
                    """,id,id,id,id,id));
            @SuppressWarnings("unchecked") var payables=(List<Map<String,Object>>)result.get("payables");
            result.put("payables",payables.stream().map(row->{
                var entry=new LinkedHashMap<String,Object>(row);
                boolean current="payable".equals(row.get("direction"))&&longNumber(row,"amount_cent")>0&&!"reversed".equals(row.get("status"))
                        &&Set.of("purchase_inbound","purchase_consignment","purchase_prepayment").contains(row.get("source_type"))
                        &&!finance.isPurchasePayableReplaced((String)row.get("id"));
                entry.put("is_current",current);return entry;
            }).toList());
            result.put("prepayment_cash_paid_cent",sum("SELECT COALESCE(SUM(p.amount_cent),0) FROM ar_ap_payments p JOIN ar_ap_items a ON a.id=p.ar_ap_item_id WHERE a.source_type='purchase_prepayment' AND a.source_id=?",id));
            result.put("prepayment_payments",jdbc.queryForList("SELECT p.* FROM ar_ap_payments p JOIN ar_ap_items a ON a.id=p.ar_ap_item_id WHERE a.source_type='purchase_prepayment' AND a.source_id=? ORDER BY p.payment_time,p.id",id));
            result.put("prepayment_resolutions",jdbc.queryForList("SELECT * FROM purchase_prepayment_resolutions WHERE purchase_id=? ORDER BY requested_at,id",id));
            result.put("prepayment_refund_reserved_cent",sum("SELECT COALESCE(SUM(amount_cent),0) FROM purchase_prepayment_resolutions WHERE purchase_id=? AND resolution_type='refund_unused' AND status IN('pending_approval','approved','posted')",id));
            result.put("prepayment_allocated_cent",sum("SELECT COALESCE(SUM(amount_cent),0) FROM purchase_prepayment_allocations WHERE purchase_id=?",id));
            result.put("settlement_terms",canPrice((String)f.get("project_id"))&&f.get("contract_snapshot")!=null?requireSnapshot(p):null);
        }
        return jsonMap(result);
        });
    }
    public String requireEvidenceScope(String type,String id,String evidencePermission) {
        if("supply_contract".equals(type)){
            var c=contract(id,CONTRACT);requireScope(c.projectId(),c.merchantId(),evidencePermission);return c.merchantId();
        }
        String purchase=switch(type){
            case "inventory_purchase_request"->id;
            case "purchase_prepayment_resolution"->(String)row("SELECT * FROM purchase_prepayment_resolutions WHERE id=?",id).get("purchase_id");
            case "purchase_quantity_resolution"->(String)resolution(id).get("purchase_id");
            case "purchase_return"->(String)receipt((String)returnRow(id).get("receipt_id")).get("purchase_id");
            default->throw invalid("不支持的采购证据来源");
        };
        String permission="evidence.timeline.view".equals(evidencePermission)?VIEW:MANAGE;
        if(auth.currentActorHasPermission(FINANCE)&&visible(purchaseFacts(purchase),FINANCE))permission=FINANCE;
        var p=scopedPurchase(purchase,permission);
        requireScope((String)purchaseFacts(purchase).get("project_id"),p.merchantId(),evidencePermission);
        return p.merchantId();
    }
    public SupplyContractRecord requirePurchasable(String id,String item) {
        var c=contract(id,MANAGE);requirePurchasable(c,item);return c;
    }
    private void requirePurchasable(SupplyContractRecord c,String item) {
        if(!"approved".equals(c.approvalStatus())||!"active".equals(c.status()))throw ApiException.conflict("SUPPLY_CONTRACT_NOT_APPROVED","供应合同尚未完整批准生效");
        if(c.validFrom()==null||c.validTo()==null||today().isBefore(c.validFrom())||today().isAfter(c.validTo()))throw invalid("供货合同不在有效期");
        if(c.purchasableScope()==null||!c.purchasableScope().contains(item))throw invalid("商品不在合同明确可采购范围");
        var terms=readTerms(contractFacts(c.id()).get("settlement_terms"));if(terms==null)throw incomplete();terms.requirePricesFor(c.purchasableScope());requirePurchaser(c,terms);
    }
    private void requirePurchaser(SupplyContractRecord c,PurchaseContractTerms terms) {
        if(count("SELECT count(*) FROM business_entities e JOIN business_entity_projects p ON p.business_entity_id=e.id WHERE e.id=? AND e.status='active' AND p.project_id=?",terms.purchaserEntityId(),c.projectId())!=1)
            throw invalid("合同我方主体须为该综合体已关联的有效公司");
        if(c.merchantId()!=null&&count("SELECT count(*) FROM merchants m WHERE m.id=? AND m.business_entity_id=? AND (m.project_id=? OR EXISTS(SELECT 1 FROM merchant_projects mp WHERE mp.merchant_id=m.id AND mp.project_id=?))",c.merchantId(),terms.purchaserEntityId(),c.projectId(),c.projectId())!=1)
            throw invalid("商户自采主体与商户及项目归属不一致");
    }

    private Optional<String> merchantScope(){
        var actor=auth.currentActor();
        if(Set.of("outsourced_vendor","customer","dealer").contains(actor.accountType()))
            throw ApiException.forbidden("PURCHASE_INTERNAL_ACCOUNT_REQUIRED","采购仅由具备权限的内部人员办理");
        return actor.merchantId()==null||actor.merchantId().isBlank()?Optional.empty():Optional.of(actor.merchantId());
    }
    private void requireQualified(PurchaseRequestRecord purchase){
        requireSupplier(purchase.supplierId(),purchase.merchantId());
        String supplierOwner=(String)jdbc.queryForMap("SELECT merchant_id FROM inventory_suppliers WHERE id=?",purchase.supplierId()).get("merchant_id");
        qualifications.requireQualifiedRecords(inventory.listSupplierQualificationsBySupplier(purchase.supplierId()).stream()
                .filter(q->Objects.equals(q.merchantId(),supplierOwner)).toList());
    }
    private void requireSupplier(String supplier,String owner) {
        var rows=jdbc.queryForList("SELECT merchant_id,status FROM inventory_suppliers WHERE id=?",supplier);
        if(rows.isEmpty()||!"active".equals(rows.getFirst().get("status")))throw invalid("供应商不存在或未启用");
        if(rows.getFirst().get("merchant_id")!=null&&!Objects.equals(owner,rows.getFirst().get("merchant_id")))throw invalid("供应商不在采购货权范围");
    }
    /** 第十八批 G 段 82b-R8：商户须以该项目为主项目或已批准的经营项目（merchant_projects 只在审批通过后写入）。 */
    private void requireOwner(String project,String merchant) {
        if(merchant!=null&&count("SELECT count(*) FROM merchants m WHERE m.id=? AND (m.project_id=? OR EXISTS(SELECT 1 FROM merchant_projects mp WHERE mp.merchant_id=m.id AND mp.project_id=?))",merchant,project,project)!=1)throw invalid("商户不属于该综合体");
    }
    private SupplyContractRecord contract(String id,String permission) {
        var c=inventory.findSupplyContractByIdForUpdate(requireText(id,"supply_contract_id")).orElseThrow(()->ApiException.notFound("SUPPLY_CONTRACT_NOT_FOUND","供货合同不存在"));
        requireScope(c.projectId(),c.merchantId(),permission);return c;
    }
    private SupplyContractRecord maskContract(SupplyContractRecord c) {
        if(canPrice(c.projectId()))return c;
        return new SupplyContractRecord(c.id(),c.contractNo(),c.supplierId(),c.projectId(),c.merchantId(),c.contractMode(),c.supplyScope(),null,c.paymentTermDays(),c.validFrom(),c.validTo(),c.summary(),c.purchasableScope(),c.approvalStatus(),c.status(),c.createdAt(),c.updatedAt());
    }
    private Map<String,Object> maskContractResult(Map<String,Object> result,String project) {
        if(canPrice(project))return result;
        var copy=new LinkedHashMap<String,Object>(result);copy.put("terms",null);
        if(copy.get("contract") instanceof Map<?,?> contract){
            var masked=new LinkedHashMap<>(contract);
            masked.replaceAll((key,value)->"price_terms".equals(key)||"priceTerms".equals(key)?null:value);
            copy.put("contract",masked);
        }else if(copy.get("contract") instanceof SupplyContractRecord contract)copy.put("contract",maskContract(contract));
        return copy;
    }
    private Map<String,Object> maskInventoryCosts(Map<String,Object> row,String project) {
        if(canPrice(project))return row;
        var copy=new LinkedHashMap<String,Object>(row);
        copy.put("actual_cost_cent",null);copy.put("cost_difference_cent",null);return copy;
    }
    private Map<String,Object> maskMoney(Map<String,Object> row,String project) {
        var copy=new LinkedHashMap<>(row);if(!canPrice(project))copy.replaceAll((key,value)->key.endsWith("_cent")?null:value);return copy;
    }
    private boolean canPrice(String project){return auth.currentActorHasPermission("inventory.cost.view")&&auth.canReachProject(auth.currentActor(),project,"inventory.cost.view");}
    PurchaseRequestRecord scopedPurchase(String id,String permission) {
        var p=readPurchase(id);var f=purchaseFacts(id);requireScope((String)f.get("project_id"),p.merchantId(),permission);return p;
    }
    PurchaseRequestRecord lockedPurchase(String id,String permission) {purchaseLock(id);return scopedPurchase(id,permission);}
    void purchaseLock(String id){jdbc.queryForObject("SELECT id FROM inventory_purchase_requests WHERE id=? FOR UPDATE",String.class,id);}
    private void contractLock(String id){jdbc.queryForObject("SELECT id FROM supply_contracts WHERE id=? FOR UPDATE",String.class,id);}
    private PurchaseRequestRecord readPurchase(String id){return inventory.findPurchaseRequestByIdForUpdate(requireText(id,"purchase_id")).orElseThrow(()->ApiException.notFound("PURCHASE_REQUEST_NOT_FOUND","采购申请不存在"));}
    Map<String,Object> purchaseFacts(String id){return jdbc.queryForMap("SELECT * FROM inventory_purchase_requests WHERE id=?",id);}
    private Map<String,Object> contractFacts(String id){return jdbc.queryForMap("SELECT * FROM supply_contracts WHERE id=?",id);}
    private Map<String,Object> receipt(String id){return row("SELECT * FROM purchase_receipts WHERE id=?",id);}
    private Map<String,Object> resolution(String id){return row("SELECT * FROM purchase_quantity_resolutions WHERE id=?",id);}
    private Map<String,Object> returnRow(String id){return row("SELECT * FROM purchase_returns WHERE id=?",id);}
    private Map<String,Object> saleRow(String id){return row("SELECT * FROM purchase_consignment_sales WHERE id=?",id);}
    private Map<String,Object> row(String sql,String id){return jdbc.queryForList(sql,requireText(id,"id")).stream().findFirst().orElseThrow(()->ApiException.notFound("PURCHASE_FACT_NOT_FOUND","采购事实记录不存在"));}
    PurchaseContractTerms requireSnapshot(PurchaseRequestRecord p) {
        Object value=purchaseFacts(p.id()).get("contract_snapshot");
        if(p.supplyContractId()==null||value==null)throw ApiException.conflict("PURCHASE_CONTRACT_REQUIRED","采购尚未锁定已批准合同，不能入库或结算");
        var snapshot=readMap(value);return mapper.convertValue(snapshot.get("terms"),PurchaseContractTerms.class);
    }
    private void requireScope(String project,String owner,String permission) {
        auth.requirePermission(permission);if(!visible(project,owner,permission))throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN","采购事实不在当前项目及货权权限范围");
    }
    private boolean visible(Map<String,Object> facts,String permission){return visible((String)facts.get("project_id"),(String)facts.get("merchant_id"),permission);}
    private boolean visible(String project,String owner,String permission) {
        if(project==null||!auth.canReachProject(auth.currentActor(),project,permission))return false;
        var merchant=merchantScope();return merchant.isEmpty()||merchant.get().equals(owner);
    }
    private void requireWarehouse(PurchaseRequestRecord p,String warehouse,String location) {
        var f=purchaseFacts(p.id());var w=row("SELECT * FROM warehouses WHERE id=?",warehouse);
        if(!"active".equals(w.get("status"))||!Objects.equals(f.get("project_id"),w.get("project_id"))||!Objects.equals(p.merchantId(),w.get("merchant_id")))throw invalid("仓库与采购项目或货权不一致");
        if(inventory.hasActiveStocktakeForWarehouse(warehouse)||inventory.hasActiveStocktakeForWarehouse(location))throw ApiException.conflict("INVENTORY_WAREHOUSE_STOCKTAKE_LOCKED","库房盘点中不能变动库存");
        var balances=jdbc.queryForList("SELECT warehouse_id,project_id,merchant_id FROM inventory_stock_balances WHERE location_id=?",location);
        if(balances.stream().anyMatch(b->!warehouse.equals(b.get("warehouse_id"))||!Objects.equals(f.get("project_id"),b.get("project_id"))||!Objects.equals(p.merchantId(),b.get("merchant_id"))))throw invalid("库位已有库存属于其他仓库、项目或货权");
    }
    private void lockStock(String item,String location,String warehouse,String project,String owner) {
        jdbc.update("""
                INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,updated_at,warehouse_id,project_id,merchant_id)
                VALUES(?,?,?,0,0,0,now(),?,?,?) ON CONFLICT(item_id,location_id) DO NOTHING
                ""","pbal_"+slug(item+":"+location),item,location,warehouse,project,owner);
        var f=jdbc.queryForMap("SELECT * FROM inventory_stock_balances WHERE item_id=? AND location_id=? FOR UPDATE",item,location);
        if(!Objects.equals(warehouse,f.get("warehouse_id"))||!Objects.equals(project,f.get("project_id"))||!Objects.equals(owner,f.get("merchant_id")))throw invalid("库位实际归属发生变化");
    }
    private void closeQuantityIfComplete(String id) {
        var f=purchaseFacts(id);
        if(number(f,"accepted_quantity")+number(f,"terminated_quantity")==number(f,"quantity")){
            jdbc.update("UPDATE inventory_purchase_requests SET status='received',received_at=COALESCE(received_at,now()),received_by=COALESCE(received_by,?),updated_at=now() WHERE id=?",actor(),id);
            var p=readPurchase(id);var terms=requireSnapshot(p);
            if("all_fulfilled".equals(terms.dueAnchor())){
                LocalDate date=due(p,terms,Instant.now());
                jdbc.update("UPDATE ar_ap_items SET due_date=?,updated_at=now() WHERE source_type='purchase_inbound' AND due_date IS NULL AND source_id IN(SELECT id FROM purchase_receipts WHERE purchase_id=?)",date,id);
            }
        }
    }
    private LocalDate due(PurchaseRequestRecord p,PurchaseContractTerms terms,Instant event) {
        if("fixed_date".equals(terms.dueAnchor()))return terms.fixedDueDate();
        LocalDate anchor=switch(terms.dueAnchor()){
            case "contract_signed"->terms.signedOn();
            case "ordered"->p.orderedAt()==null?null:p.orderedAt().atZone(LocalZone.ID).toLocalDate();
            case "all_fulfilled"->p.receivedAt()==null?null:p.receivedAt().atZone(LocalZone.ID).toLocalDate();
            case "month_end"->{var day=event.atZone(LocalZone.ID).toLocalDate();yield day.withDayOfMonth(day.lengthOfMonth());}
            default->event.atZone(LocalZone.ID).toLocalDate();
        };return anchor==null?null:anchor.plusDays(terms.termDays());
    }

    Map<String,Object> prepaymentSource(PurchaseRequestRecord p,String paymentId) {
        var rows=jdbc.queryForList("SELECT * FROM ar_ap_payments WHERE id=? FOR UPDATE",requireText(paymentId,"source_payment_id"));
        if(rows.size()!=1)throw invalid("真实预付款流水不存在");
        var payment=rows.getFirst();var source=finance.findArApItemByIdForUpdate((String)payment.get("ar_ap_item_id")).orElseThrow(()->invalid("预付款原账款不存在"));
        if(!"purchase_prepayment".equals(source.sourceType())||!p.id().equals(source.sourceId())
                ||!"payable".equals(source.direction())||!p.supplierId().equals(source.counterpartyId())
                ||!Objects.equals(p.merchantId(),source.merchantId())||!Objects.equals(purchaseFacts(p.id()).get("project_id"),source.projectId())
                ||source.amountCent()<=0||longNumber(payment,"amount_cent")<=0)
            throw invalid("只能使用本采购真实已付预付款");
        return payment;
    }
    long prepaymentAllocated(String paymentId){return sum("SELECT COALESCE(SUM(amount_cent),0) FROM purchase_prepayment_allocations WHERE source_payment_id=?",paymentId);}
    long prepaymentRefundReserved(String paymentId,String excludedId){
        return sum("SELECT COALESCE(SUM(amount_cent),0) FROM purchase_prepayment_resolutions WHERE source_payment_id=? AND resolution_type='refund_unused' AND status IN('pending_approval','approved','posted') AND (?::text IS NULL OR id<>?)",paymentId,excludedId,excludedId);
    }
    private void createPrepayment(PurchaseRequestRecord p){
        var terms=requireSnapshot(p);if(!"prepaid".equals(terms.settlementMethod()))return;
        postPayable(p,"purchase_prepayment",p.id(),terms.prepaymentAmountCent(),due(p,terms,p.orderedAt()));
    }
    private String postPayable(PurchaseRequestRecord p,String type,String source,long amount,LocalDate due) {
        String id=("purchase_prepayment".equals(type)?"pap_":"paya_")+slug(source);
        Instant now=Instant.now();finance.appendArApItem(new ArApItemRecord(id,"AP-"+slug(type+":"+source),"payable",p.supplierId(),type,source,
                amount,0,amount,"CNY",due,null,"pending_payment",now,now,p.merchantId(),(String)purchaseFacts(p.id()).get("project_id")));
        finance.saveArApItemSource(new ArApItemSourceRecord("pas_"+slug(id),id,type,source,amount,now));return id;
    }
    private String postReceivable(PurchaseRequestRecord p,String source,long amount,LocalDate due) {
        return postReceivable(p,source,amount,due,"purchase_return_refund");
    }
    String postReceivable(PurchaseRequestRecord p,String source,long amount,LocalDate due,String sourceType) {
        String id="par_"+slug(source);Instant now=Instant.now();
        finance.appendArApItem(new ArApItemRecord(id,"AR-"+slug(source),"receivable",p.supplierId(),sourceType,source,amount,0,amount,"CNY",due,null,"pending_collection",now,now,p.merchantId(),(String)purchaseFacts(p.id()).get("project_id")));
        finance.saveArApItemSource(new ArApItemSourceRecord("pas_"+slug(id),id,sourceType,source,amount,now));return id;
    }
    void requireFinancialTarget(PurchaseRequestRecord p,ArApItemRecord target) {
        if(!"payable".equals(target.direction())||!p.supplierId().equals(target.counterpartyId())||!Objects.equals(p.merchantId(),target.merchantId())
                ||!Objects.equals(purchaseFacts(p.id()).get("project_id"),target.projectId()))throw invalid("账款供应商、项目或货权不一致");
        if(target.amountCent()<=0||"reversed".equals(target.status())||finance.isPurchasePayableReplaced(target.id()))
            throw invalid("应付已被更正，须使用当前有效账款");
    }
    ArApItemRecord currentFinancialTarget(PurchaseRequestRecord p,String sourceType,String sourceId) {
        var candidate=finance.findCurrentPurchasePayable(sourceType,sourceId).orElseThrow(()->invalid("该采购来源没有当前有效应付"));
        var current=finance.findArApItemByIdForUpdate(candidate.id()).orElseThrow(()->invalid("当前应付不存在"));
        requireFinancialTarget(p,current);return current;
    }
    private void restatePayable(PurchaseRequestRecord p,ArApItemRecord old,long reduction,String returnId,String receiptId,String saleId) {
        Instant now=Instant.now();String reversal="ppr_"+slug(returnId+":"+old.id()),replacement=null;
        finance.appendArApItem(new ArApItemRecord(reversal,"CZ-"+slug(reversal),old.direction(),old.counterpartyId(),old.sourceType(),old.sourceId(),
                -old.amountCent(),-old.paidAmountCent(),-old.unpaidAmountCent(),old.currency(),old.dueDate(),old.accountingPeriod(),"reversed",now,now,old.merchantId(),old.projectId(),old.id(),null));
        long amount=old.amountCent()-reduction,unpaid=old.unpaidAmountCent()-reduction;
        if(amount>0){
            replacement="pprp_"+slug(returnId+":"+old.id());
            finance.appendArApItem(new ArApItemRecord(replacement,"AP-"+slug(replacement),old.direction(),old.counterpartyId(),old.sourceType(),old.sourceId(),amount,old.paidAmountCent(),unpaid,
                    old.currency(),old.dueDate(),old.accountingPeriod(),unpaid==0?"settled":"pending_payment",now,now,old.merchantId(),old.projectId(),old.id(),null));
        }
        jdbc.update("INSERT INTO purchase_payable_adjustments(id,return_id,original_item_id,amount_cent,reversal_item_id,replacement_item_id) VALUES(?,?,?,?,?,?)",
                "ppadj_"+slug(returnId+":"+old.id()),returnId,old.id(),reduction,reversal,replacement);
        if(saleId==null)jdbc.update("UPDATE purchase_receipts SET payable_id=? WHERE id=?",replacement,receiptId);
        else jdbc.update("UPDATE purchase_consignment_sales SET payable_id=? WHERE id=?",replacement,saleId);
    }
    private List<String> contractProof(String id){try{return mapper.readValue(contractFacts(id).get("terms_evidence_ids").toString(),new TypeReference<List<String>>(){});}catch(Exception e){throw invalid("合同凭证快照无效");}}
    void checkEvidence(String type,String id,List<String> ids){
        if(ids==null||ids.isEmpty())throw invalid("须上传本业务真实凭证");
        for(String key:ids){var attachment=evidence.findAttachmentById(key).orElseThrow(()->invalid("凭证不存在"));
            if(!type.equals(attachment.businessType())||!id.equals(attachment.businessId()))throw invalid("凭证未绑定本采购业务");files.requireUploaded(attachment);}
    }
    private void requireApplicantExcluded(String reviewId,Object applicant) {
        if(applicant!=null&&reviews.chain(reviewId).stream().anyMatch(n->applicant.equals(n.approverId())||applicant.equals(n.forwardedTo())))
            throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN","原申请人不得通过转交提交进入本单审批链");
    }
    void requireBlocked(SupervisorReviewDecisionRecord r){if(!r.blocked()||!"pending_review".equals(r.reviewStatus()))throw invalid("采购事项须进入真实审批链");}
    private void requireActualTime(Instant value,Instant earliest){if(value==null||value.isAfter(Instant.now())||(earliest!=null&&value.isBefore(earliest)))throw invalid("实际发生时间无效");}
    private void event(PurchaseRequestRecord p,String status,String note){inventory.savePurchaseRequestEvent(new PurchaseRequestEventRecord("pe_"+UUID.randomUUID(),p.id(),p.status(),status,actor(),note,Instant.now()));}
    private <T>T read(String successEvent,String failureEvent,String permission,String type,String id,Supplier<T> action){
        try{T result=action.get();audit.recordSuccess(successEvent,permission,type,id,null,null,Map.of());return result;}
        catch(RuntimeException e){audit.recordFailure(failureEvent,permission,type,id,null,null,e instanceof ApiException a?a.code():"INTERNAL_ERROR");throw e;}
    }
    @SuppressWarnings("unchecked")
    <T>T write(String operation,String key,Object request,String permission,String type,String id,Supplier<T> action,Class<T> resultType){
        try{return idem.execute(operation,key,request,()->{T result=action.get();if(result instanceof Map<?,?> map)result=(T)jsonMap(map);audit.recordSuccess("inventory.purchase.completed",permission,type,result instanceof PurchaseRequestRecord purchase?purchase.id():result instanceof SupplyContractRecord contract?contract.id():id,null,key,Map.of("operation",operation));return result;},resultType);}
        catch(RuntimeException e){audit.recordFailure("inventory.purchase.failed",permission,type,id,null,key,e instanceof ApiException a?a.code():"INTERNAL_ERROR");throw e;}
    }
    private PurchaseContractTerms readTerms(Object value){if(value==null)return null;try{return mapper.readValue(value.toString(),PurchaseContractTerms.class);}catch(Exception e){throw incomplete();}}
    Map<String,Object> readMap(Object value){try{return value instanceof Map<?,?>?mapper.convertValue(value,new TypeReference<Map<String,Object>>(){}):mapper.readValue(value.toString(),new TypeReference<Map<String,Object>>(){});}catch(Exception e){throw invalid("采购快照格式无效");}}
    String json(Object value){try{return mapper.writeValueAsString(jsonValue(value));}catch(Exception e){throw new IllegalStateException(e);}}
    Map<String,Object> jsonMap(Map<?,?> value) {
        return mapper.convertValue(jsonValue(value),new TypeReference<Map<String,Object>>(){});
    }
    private Object jsonValue(Object value) {
        if(value instanceof Map<?,?> map){var out=new LinkedHashMap<String,Object>();map.forEach((k,v)->out.put(k.toString(),jsonValue(v)));return out;}
        if(value instanceof List<?> list)return list.stream().map(this::jsonValue).toList();
        if(value!=null&&"org.postgresql.util.PGobject".equals(value.getClass().getName())){
            try{return mapper.readValue(value.toString(),Object.class);}catch(Exception e){throw invalid("采购JSON事实格式无效");}
        }
        return value;
    }
    private int count(String sql,Object... values){Integer value=jdbc.queryForObject(sql,Integer.class,values);return value==null?0:value;}
    private long sum(String sql,Object... values){Long value=jdbc.queryForObject(sql,Long.class,values);return value==null?0:value;}
    private int number(Map<String,Object> f,String key){return ((Number)f.get(key)).intValue();}
    private long longNumber(Map<String,Object> f,String key){return ((Number)f.get(key)).longValue();}
    private Instant instant(Object v){return v instanceof Timestamp t?t.toInstant():v instanceof java.time.OffsetDateTime t?t.toInstant():(Instant)v;}
    private String actor(){return auth.currentActor().id();}
    private String slug(String seed){return com.complex.platform.common.Hashing.idempotencyCode(requireText(seed,"idempotency_key"));}
    private LocalDate today(){return LocalDate.now(LocalZone.ID);}
    private <T>List<T> safe(List<T> values){return values==null?List.of():List.copyOf(values);}
    private ApiException invalid(String message){return ApiException.conflict("PURCHASE_WORKFLOW_INVALID",message);}
    private ApiException incomplete(){return ApiException.conflict("PURCHASE_CONTRACT_TERMS_INCOMPLETE","须补齐并批准可执行合同条款");}
}

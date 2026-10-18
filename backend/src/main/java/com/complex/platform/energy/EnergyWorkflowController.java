package com.complex.platform.energy;

// 文件版本：V0.01
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.evidence.EvidenceEventService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/energy")
public class EnergyWorkflowController {
    private final EnergyService service;
    private final EvidenceEventService evidence;
    public EnergyWorkflowController(EnergyService service,EvidenceEventService evidence){this.service=service;this.evidence=evidence;}
    @PostMapping("/meters/{meter_id}/opening")
    public ApiResponse<EnergyWorkflowRepository.Opening> opening(@PathVariable("meter_id")String id,@RequestBody EnergyWorkflowRequests.Opening r){return ApiResponse.ok(service.initialize(id,r));}
    @PostMapping("/meters/{meter_id}/replacement")
    public ApiResponse<EnergyWorkflowRepository.Replacement> replacement(@PathVariable("meter_id")String id,@RequestBody EnergyWorkflowRequests.Replacement r){return ApiResponse.ok(service.replace(id,r));}
    @PostMapping("/meters/{meter_id}/retirement")
    public ApiResponse<EnergyWorkflowRepository.Retirement> retirement(@PathVariable("meter_id")String id,@RequestBody EnergyWorkflowRequests.Retirement r){return ApiResponse.ok(service.retire(id,r));}
    @GetMapping("/meters/{meter_id}/history")
    public ApiResponse<Map<String,Object>> history(@PathVariable("meter_id")String id){return ApiResponse.ok(service.history(id));}
    @PostMapping("/readings/{reading_id}/verification")
    public ApiResponse<EnergyWorkflowRepository.Verification> verify(@PathVariable("reading_id")String id,@RequestBody EnergyWorkflowRequests.Verification r){return ApiResponse.ok(service.verify(id,r));}
    @PostMapping("/readings/{reading_id}/assignment")
    public ApiResponse<EnergyWorkflowRepository.Assignment> assign(@PathVariable("reading_id")String id,@RequestBody EnergyWorkflowRequests.Assignment r){return ApiResponse.ok(service.assign(id,r));}
    @GetMapping("/escalations")
    public ApiResponse<List<Map<String,Object>>> escalations(){return ApiResponse.ok(service.escalations());}
    @GetMapping("/actual-charges")
    public ApiResponse<List<EnergyWorkflowRepository.Charge>> charges(@RequestParam("period")String period){return ApiResponse.ok(service.charges(period));}
    @PostMapping("/actual-charges")
    public ApiResponse<EnergyWorkflowRepository.Charge> charge(@RequestBody EnergyWorkflowRequests.ActualCharge r){return ApiResponse.ok(service.recordCharge(r));}
    @GetMapping("/projects/{project_id}/review-settings")
    public ApiResponse<List<EnergyWorkflowRepository.Settings>> settings(@PathVariable("project_id")String id){return ApiResponse.ok(service.settings(id));}
    @PostMapping("/projects/{project_id}/review-settings")
    public ApiResponse<EnergyWorkflowRepository.Settings> settings(@PathVariable("project_id")String id,@RequestBody EnergyWorkflowRequests.ReviewSettings r){return ApiResponse.ok(service.configure(id,r));}
    @GetMapping("/meters/{meter_id}/attachments/{attachment_id}/file")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> attachment(@PathVariable("meter_id")String id,@PathVariable("attachment_id")String attachmentId){
        service.requireMeterAttachment(id,attachmentId);
        return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType(evidence.attachmentContentType(attachmentId)))
                .body(new org.springframework.core.io.FileSystemResource(evidence.resolveAttachmentFile(attachmentId)));
    }
}

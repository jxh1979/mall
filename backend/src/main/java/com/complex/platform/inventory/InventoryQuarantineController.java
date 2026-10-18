package com.complex.platform.inventory;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory/quarantine")
public class InventoryQuarantineController {
    private final InventoryQuarantineService service;
    public InventoryQuarantineController(InventoryQuarantineService service) {this.service=service;}
    @GetMapping("/warehouses") public ApiResponse<List<InventoryQuarantineService.WarehouseOption>> warehouses() {return ApiResponse.ok(service.visibleWarehouses());}
    @GetMapping public ApiResponse<List<InventoryQuarantineLotRecord>> list(@RequestParam("warehouse_id") String warehouseId) {return ApiResponse.ok(service.list(warehouseId));}
    @GetMapping("/{id}/dispositions") public ApiResponse<List<InventoryQuarantineDispositionRecord>> dispositions(@PathVariable String id) {return ApiResponse.ok(service.dispositions(id));}
    @PostMapping("/{id}/dispositions") public ApiResponse<InventoryQuarantineDispositionRecord> propose(@PathVariable String id,@RequestBody InventoryQuarantineDispositionRequest request) {return ApiResponse.ok(service.propose(id,request));}
    @PostMapping("/dispositions/{id}/complete") public ApiResponse<InventoryQuarantineDispositionRecord> complete(@PathVariable String id,@RequestBody InventoryQuarantineCompleteRequest request) {return ApiResponse.ok(service.complete(id,request));}
}

package com.complex.platform.evidence;

public interface AuditedOperation {
    String successEvent();

    String failureEvent();

    String permissionCode();

    String businessType();

    /** Builds an ad-hoc operation from raw audit metadata for services not backed by an enum. */
    static AuditedOperation of(String successEvent, String failureEvent, String permissionCode, String businessType) {
        record SimpleAuditedOperation(String successEvent, String failureEvent, String permissionCode, String businessType)
                implements AuditedOperation {
        }
        return new SimpleAuditedOperation(successEvent, failureEvent, permissionCode, businessType);
    }
}

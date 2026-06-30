package com.complex.platform.facility.dto;

import java.util.List;

/** Outsourced construction completion material upload request. */
public record SiteEntryCompletionUploadRequest(
        List<String> attachmentIds,
        String note,
        String idempotencyKey) {}

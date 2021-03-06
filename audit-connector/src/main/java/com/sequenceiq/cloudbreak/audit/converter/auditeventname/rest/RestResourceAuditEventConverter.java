package com.sequenceiq.cloudbreak.audit.converter.auditeventname.rest;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;

public interface RestResourceAuditEventConverter {

    AuditEventName auditEventName(StructuredRestCallEvent structuredRestCallEvent);

    boolean shouldAudit(StructuredRestCallEvent structuredRestCallEvent);

    Crn.Service eventSource(StructuredRestCallEvent structuredEvent);

    default Map<String, Object> requestParameters(StructuredRestCallEvent structuredEvent) {
        return new HashMap<>();
    }
}

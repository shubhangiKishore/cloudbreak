package com.sequenceiq.cloudbreak.core.flow2.service;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.validation.diagnostics.DiagnosticsCollectionValidator;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.telemetry.converter.DiagnosticsDataToParameterConverter;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowConstants;

import reactor.bus.Event;
import reactor.rx.Promise;

@Service
public class DiagnosticsTriggerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsTriggerService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ReactorNotifier reactorNotifier;

    @Inject
    private DiagnosticsDataToParameterConverter diagnosticsDataToParameterConverter;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private DiagnosticsCollectionValidator diagnosticsCollectionValidator;

    public FlowIdentifier startDiagnosticsCollection(BaseDiagnosticsCollectionRequest request, String stackCrn, String userCrn) {
        Stack stack = stackService.getByCrn(stackCrn);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Starting diagnostics collection for Stack. Crn: '{}'", stack.getResourceCrn());
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stack.getId());
        diagnosticsCollectionValidator.validate(request, telemetry, stackCrn);
        DiagnosticParameters parameters = diagnosticsDataToParameterConverter.convert(request, telemetry, stack.getRegion());
        DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceId(stack.getId())
                .withResourceCrn(stack.getResourceCrn())
                .withSelector(DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_INIT_EVENT.selector())
                .withParameters(parameters)
                .build();
        return reactorNotifier.notify(diagnosticsCollectionEvent, getFlowHeaders(userCrn));
    }

    private Event.Headers getFlowHeaders(String userCrn) {
        return new Event.Headers(Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn));
    }
}

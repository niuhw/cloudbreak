package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_AMBARI_REPO_INFO_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_STACK_REPO_INFO_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATION_FLOW_FINISHED_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.MaintenanceModeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

@Configuration
public class MaintenanceModeValidationActions {

    private static final String STACK_REPO = "stack_repo";

    @Inject
    private MaintenanceModeValidationService maintenanceModeValidationService;

    @Bean(name = "FETCH_STACK_REPO_STATE")
    public AbstractMaintenanceModeValidationAction<?> fetchStackRepo() {
        return new AbstractMaintenanceModeValidationAction<>(MaintenanceModeValidationTriggerEvent.class) {

            @Override
            protected void doExecute(StackContext context, MaintenanceModeValidationTriggerEvent payload, Map<Object, Object> variables) {
                String stackRepo = maintenanceModeValidationService.
                        fetchStackRepository(context.getStack().getId());
                variables.put(STACK_REPO, stackRepo);
                sendEvent(context.getFlowId(), VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "VALIDATE_STACK_REPO_INFO_STATE")
    public AbstractMaintenanceModeValidationAction<?> validateStackRepo() {
        return new AbstractMaintenanceModeValidationAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                String stackRepo = (String) variables.get(STACK_REPO);
                maintenanceModeValidationService.validateStackRepository(
                        context.getStack().getCluster().getId(), stackRepo);
                sendEvent(context.getFlowId(), VALIDATE_STACK_REPO_INFO_FINISHED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "VALIDATE_AMBARI_REPO_INFO_STATE")
    public AbstractMaintenanceModeValidationAction<?> validateAmbariRepo() {
        return new AbstractMaintenanceModeValidationAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                maintenanceModeValidationService.validateAmbariRepository(context.getStack().getCluster().getId());
                sendEvent(context.getFlowId(), VALIDATE_AMBARI_REPO_INFO_FINISHED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "VALIDATE_IMAGE_COMPATIBILITY_STATE")
    public AbstractMaintenanceModeValidationAction<?> validateImageCompatibility() {
        return new AbstractMaintenanceModeValidationAction<Payload>(Payload.class) {
            @Override
            protected void doExecute(StackContext context, Payload payload, Map<Object, Object> variables) {
                maintenanceModeValidationService.validateImageCatalog(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new Selectable() {

                    @Override
                    public String selector() {
                        return VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT.event();
                    }

                    @Override
                    public Long getStackId() {
                        return context.getStack().getId();
                    }
                };
            }
        };
    }

    @Bean(name = "VALIDATION_FINISHED_STATE")
    public AbstractMaintenanceModeValidationAction<?> finishedAction() {
        return new AbstractMaintenanceModeValidationAction<Payload>(Payload.class) {
            @Override
            protected void doExecute(StackContext context, Payload payload, Map<Object, Object> variables) {
                maintenanceModeValidationService.handleValidationSuccess(context.getStack().getId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new Selectable() {

                    @Override
                    public String selector() {
                        return VALIDATION_FLOW_FINISHED_EVENT.event();
                    }

                    @Override
                    public Long getStackId() {
                        return context.getStack().getId();
                    }
                };
            }
        };
    }

    @Bean(name = "VALIDATION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractStackFailureAction<MaintenanceModeValidationState, MaintenanceModeValidationEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                maintenanceModeValidationService.handleValidationFailure(context.getStackView().getId(),
                        payload.getException());
                sendEvent(context.getFlowId(), VALIDATION_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

}

package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_AMBARI_REPO_INFO_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_STACK_REPO_INFO_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATION_FAIL_HANDLED_EVENT;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.MaintenanceModeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@Configuration
public class MaintenanceModeValidationActions {

    @Bean(name = "FETCH_STACK_REPO_STATE")
    public AbstractMaintenanceModeValidationAction<?> fetchStackRepo() {
        return new AbstractMaintenanceModeValidationAction<>(MaintenanceModeValidationTriggerEvent.class) {

            @Override
            protected void doExecute(StackContext context, MaintenanceModeValidationTriggerEvent payload, Map<Object, Object> variables) {
                getFlowMessageService().fireEventAndLog(context.getStack().getId(), Msg.MAINTENANCE_MODE_VALIDATION_STARTED, Status.UPDATE_IN_PROGRESS.name());
                getMaintenanceModeValidationService().validateStackRepository(context.getStack().getId());
                sendEvent(context.getFlowId(), VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "VALIDATE_STACK_REPO_INFO_STATE")
    public AbstractMaintenanceModeValidationAction<?> validateStackRepo() {
        return new AbstractMaintenanceModeValidationAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                getMaintenanceModeValidationService().validateStackRepositoryNaming(context.getStack().getCluster().getId());
                sendEvent(context.getFlowId(), VALIDATE_STACK_REPO_INFO_FINISHED_EVENT.event(), payload);

            }
        };
    }

    @Bean(name = "VALIDATE_AMBARI_REPO_INFO_STATE")
    public AbstractMaintenanceModeValidationAction<?> validateAmbariRepo() {
        return new AbstractMaintenanceModeValidationAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                getMaintenanceModeValidationService().validateAmbariRepositoryNaming(context.getStack().getCluster().getId());
                sendEvent(context.getFlowId(), VALIDATE_AMBARI_REPO_INFO_FINISHED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "VALIDATE_IMAGE_COMPATIBILITY_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractMaintenanceModeValidationAction<Payload>(Payload.class) {
            @Override
            protected void doExecute(StackContext context, Payload payload, Map<Object, Object> variables) {
                getMaintenanceModeValidationService().validateImageCatalogSettings(context.getStack());
                getFlowMessageService().fireEventAndLog(context.getStack().getId(), Msg
                        .MAINTENANCE_MODE_VALIDATION_FINISHED, Status.AVAILABLE.name());
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

    @Bean(name = "VALIDATION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractMaintenanceModeValidationAction<Payload>(Payload.class) {

            @Override
            protected void doExecute(StackContext context, Payload payload, Map<Object, Object> variables) {
                getFlowMessageService().fireEventAndLog(context.getStack().getId(),
                        Msg.MAINTENANCE_MODE_VALIDATION_FAILED, Status.UPDATE_FAILED.name());
                sendEvent(context.getFlowId(), VALIDATION_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

}

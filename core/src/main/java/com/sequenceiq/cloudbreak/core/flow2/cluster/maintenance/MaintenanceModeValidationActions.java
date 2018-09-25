package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldEvent.FINALIZE_HELLO_WORLD_EVENT;

import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.CommonContext;

@Configuration
public class MaintenanceModeValidationActions {

    @Bean(name = "FETCH_STACK_REPO_STATE")
    public Action<?, ?> startAction() {
        return new AbstractMaintenanceModeValidationAction<Payload>(Payload.class) {

            @Override
            protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
                sendEvent(context.getFlowId(), VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "VALIDATE_IMAGE_COMPATIBILITY_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractMaintenanceModeValidationAction<Payload>(Payload.class) {
            @Override
            protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(CommonContext context) {
                return new Selectable() {

                    @Override
                    public String selector() {
                        return FINALIZE_HELLO_WORLD_EVENT.event();
                    }

                    @Override
                    public Long getStackId() {
                        return null;
                    }
                };
            }
        };
    }

    @Bean(name = "VALIDATION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractMaintenanceModeValidationAction<Payload>(Payload.class) {

            @Override
            protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
                sendEvent(context.getFlowId(), VALIDATION_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

    private abstract static class AbstractMaintenanceModeValidationAction<P extends Payload> extends AbstractAction<MaintenanceModeValidationState, MaintenanceModeValidationEvent, CommonContext, P> {

        protected AbstractMaintenanceModeValidationAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected CommonContext createFlowContext(String flowId, StateContext<MaintenanceModeValidationState, MaintenanceModeValidationEvent> stateContext, P payload) {
            return new CommonContext(flowId);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
            return (Payload) () -> null;
        }
    }
}

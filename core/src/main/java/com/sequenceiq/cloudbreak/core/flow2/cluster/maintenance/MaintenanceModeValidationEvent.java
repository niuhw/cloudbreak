package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum MaintenanceModeValidationEvent implements FlowEvent {
    START_VALIDATION_EVENT,
    FETCH_STACK_REPO_INFO_FINISHED_EVENT,
    VALIDATE_STACK_REPO_INFO_FINISHED_EVENT,
    VALIDATE_AMBARI_REPO_INFO_FINISHED_EVENT,
    VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT,

    VALIDATION_SOMETHING_WENT_WRONG,
    VALIDATION_FAIL_HANDLED_EVENT;

    @Override
    public String event() {
        return name();
    }
}

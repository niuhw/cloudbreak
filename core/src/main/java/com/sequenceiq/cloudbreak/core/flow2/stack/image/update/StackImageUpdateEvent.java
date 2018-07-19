package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum StackImageUpdateEvent implements FlowEvent {
    STACK_IMAGE_UPDATE_EVENT("STACK_IMAGE_UPDATE_EVENT"),
    STACK_IMAGE_UPDATE_FAILED_EVENT("STACK_IMAGE_UPDATE_FAILED_EVENT"),
    STACK_IMAGE_UPDATE_FAILE_HANDLED_EVENT("STACK_IMAGE_UPDATE_FAILE_HANDLED_EVENT"),
    CHECK_IMAGE_VERESIONS_FINISHED_EVENT("CHECK_IMAGE_VERESIONS_FINISHED_EVENT"),
    CHECK_PACKAGE_VERSIONS_FINISHED_EVENT("CHECK_PACKAGE_VERSIONS_FINISHED_EVENT"),
    UPDATE_IMAGE_FINESHED_EVENT("UPDATE_IMAGE_FINESHED_EVENT");

    private final String event;

    StackImageUpdateEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
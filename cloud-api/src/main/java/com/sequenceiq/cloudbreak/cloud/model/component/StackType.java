package com.sequenceiq.cloudbreak.cloud.model.component;

import com.sequenceiq.cloudbreak.common.type.ComponentType;

public enum StackType {
    HDP(ComponentType.HDP_REPO_DETAILS),
    HDF(ComponentType.HDP_REPO_DETAILS);

    private ComponentType componentType;

    StackType(ComponentType componentType) {
        this.componentType = componentType;
    }

    public ComponentType getComponentType() {
        return componentType;
    }
}

package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class MaintenanceModeValidationTriggerEvent extends StackEvent {

    private final String newImageId;

    private String imageCatalogName;

    private String imageCatalogUrl;

    public MaintenanceModeValidationTriggerEvent(String selector, Long stackId, String newImageId) {
        super(selector, stackId);
        this.newImageId = newImageId;
    }

    public MaintenanceModeValidationTriggerEvent(String selector, Long stackId, String newImageId, String imageCatalogName, String imageCatalogUrl) {
        super(selector, stackId);
        this.newImageId = newImageId;
        this.imageCatalogName = imageCatalogName;
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public MaintenanceModeValidationTriggerEvent(String selector, Long stackId, Promise<Boolean> accepted, String newImageId) {
        super(selector, stackId, accepted);
        this.newImageId = newImageId;
    }

    public String getNewImageId() {
        return newImageId;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }
}

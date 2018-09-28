package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.PackageVersionChecker;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ambari.InstanceMetadataUpdater;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class MaintenanceModeValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceModeValidationService.class);

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ImageService imageService;

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetadataUpdater instanceMetadataUpdater;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private PackageVersionChecker packageVersionChecker;


}

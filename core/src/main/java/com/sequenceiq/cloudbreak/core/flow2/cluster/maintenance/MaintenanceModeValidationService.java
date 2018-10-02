package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.CheckResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

@Service
public class MaintenanceModeValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceModeValidationService.class);

    private static final String BASE_URL = "baseUrl";

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackImageUpdateService stackImageUpdateService;

    public void validateStackRepository(Long stackId) {
        String stackRepo = clusterService.getStackRepositoryJson(stackId);
        if (stackRepo == null || "".equals(stackRepo)) {
            throw new CloudbreakServiceException("Stack repository info could not be validated!");
        }
    }

    public Map<String, String> validateStackRepositoryNaming(Long clusterId) {

        Map<String, String> stackWarnings = new HashMap<>();
        StackRepoDetails repoDetails = clusterComponentConfigProvider.getStackRepoDetails(clusterId);

        Map<String, String> stack = repoDetails.getStack();
        stack.remove(StackRepoDetails.REPO_ID_TAG);
        stack.entrySet().stream().filter(element -> !element.getValue().contains(repoDetails.getHdpVersion())).
                map(element -> {
                    LOGGER.warn("Stack repo naming validation warning! {} cannot be found in {}",
                            repoDetails.getHdpVersion(), element.getValue());
                    stackWarnings.put(element.getKey(), element.getValue());
                    return element;
                });
        return stackWarnings;
    }

    public Map<String, String> validateAmbariRepositoryNaming(Long clusterId) {

        Map<String, String> ambariWarnings = new HashMap<>();
        AmbariRepo repoDetails = clusterComponentConfigProvider.getAmbariRepo(clusterId);
        String baseUrl = repoDetails.getBaseUrl();
        String version = repoDetails.getVersion();
        if (!baseUrl.contains(version)) {
            LOGGER.warn("Ambari repo naming validation warning! {} cannot be found in {}", version, baseUrl);
            ambariWarnings.put(BASE_URL, baseUrl);
        }
        return ambariWarnings;
    }

    public CheckResult validateImageCatalogSettings(Stack stack) {
        try {
            Image image = componentConfigProvider.getImage(stack.getId());
            StatedImage statedImage = imageCatalogService.getImage(image.getImageCatalogUrl(),
                    image.getImageCatalogName(), image.getImageId());

            return stackImageUpdateService.checkPackageVersions(stack, statedImage);

        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            throw new CloudbreakServiceException("Image info could not be validated!", e);
        }
    }
}
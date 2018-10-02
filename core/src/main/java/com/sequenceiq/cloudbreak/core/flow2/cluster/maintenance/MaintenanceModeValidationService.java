package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.CheckResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.StackUpdater;
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

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private JsonHelper jsonHelper;

    public String fetchStackRepository(Long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_OPERATION, "Validating repos and images...");
        flowMessageService.fireEventAndLog(stackId, Msg.MAINTENANCE_MODE_VALIDATION_STARTED, Status.UPDATE_IN_PROGRESS.name());

        String stackRepo = clusterService.getStackRepositoryJson(stackId);
        if (stackRepo == null || "".equals(stackRepo)) {
            throw new CloudbreakServiceException("Stack repository info could not be validated!");
        }
        return stackRepo;
    }

    public Map<String, String> validateStackRepository(Long clusterId, String stackRepo) {

        JsonNode stackRepoJson = jsonHelper.createJsonFromString(stackRepo).path("Repositories");
        String baseUrl = stackRepoJson.path("base_url").asText();
        String osType = stackRepoJson.path("os_type").asText();
        String repoId = stackRepoJson.path("repo_id").asText();
        StackRepoDetails repoDetails = clusterComponentConfigProvider.getStackRepoDetails(clusterId);
        Map<String, String> stack = repoDetails.getStack();
        if (!stack.get(StackRepoDetails.REPO_ID_TAG).contentEquals(repoId)) {
            // TODO
            return null;
        }
        if ((stack.get(osType) == null) || !stack.get(osType).contentEquals(baseUrl)) {
            // TODO
            return null;
        }

        Map<String, String> stackWarnings = new HashMap<>();
        stack.remove(StackRepoDetails.REPO_ID_TAG);
        stack.entrySet().stream().filter(element -> !element.getValue().contains(repoDetails.getHdpVersion())).
                peek(element -> {
                    LOGGER.warn("Stack repo naming validation warning! {} cannot be found in {}",
                            repoDetails.getHdpVersion(), element.getValue());
                    stackWarnings.put(element.getKey(), element.getValue());
                });
        return stackWarnings;
    }

    public Map<String, String> validateAmbariRepository(Long clusterId) {

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

    public CheckResult validateImageCatalog(Stack stack) {
        try {
            Image image = componentConfigProvider.getImage(stack.getId());
            StatedImage statedImage = imageCatalogService.getImage(image.getImageCatalogUrl(),
                    image.getImageCatalogName(), image.getImageId());

            return stackImageUpdateService.checkPackageVersions(stack, statedImage);

        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            throw new CloudbreakServiceException("Image info could not be validated!", e);
        }
    }

    public void handleValidationSuccess(long stackId) {
        LOGGER.info("Maintenance mode validation flow has been finished successfully");
        flowMessageService.fireEventAndLog(stackId, Msg.MAINTENANCE_MODE_VALIDATION_FINISHED, AVAILABLE.name());
    }

    public void handleValidationFailure(long stackId, Exception error) {
        String errorDetailes = error.getMessage();
        LOGGER.warn("Error during Maintenance mode validation flow: ", error);
        flowMessageService.fireEventAndLog(stackId, Msg.MAINTENANCE_MODE_VALIDATION_FAILED, UPDATE_FAILED.name(),
                errorDetailes);

    }

}
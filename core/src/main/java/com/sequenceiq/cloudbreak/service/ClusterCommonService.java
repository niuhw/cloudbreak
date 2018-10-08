package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.MAINTENANCE_MODE_ENABLED;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.MaintenanceModeStatus;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.users.UserNamePasswordJson;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.decorator.HostGroupDecorator;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ClusterCommonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCommonService.class);

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private HostGroupDecorator hostGroupDecorator;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private HostGroupService hostGroupService;

    @Autowired
    private BlueprintValidator blueprintValidator;

    @Autowired
    private StackService stackService;

    @Autowired
    private ClusterCreationSetupService clusterCreationSetupService;

    public Response put(Long stackId, UpdateClusterJson updateJson, User user, Workspace workspace) {
        Stack stack = stackService.getById(stackId);
        MDCBuilder.buildMdcContext(stack);
        UserNamePasswordJson userNamePasswordJson = updateJson.getUserNamePasswordJson();
        if (userNamePasswordJson != null) {
            ambariUserNamePasswordChange(stackId, stack, userNamePasswordJson);
            return Response.status(Status.NO_CONTENT).build();
        }

        if (updateJson.getStatus() != null) {
            LOGGER.info("Cluster status update request received. Stack id:  {}, status: {} ", stackId, updateJson.getStatus());
            clusterService.updateStatus(stackId, updateJson.getStatus());
            return Response.status(Status.NO_CONTENT).build();
        }

        if (updateJson.getBlueprintId() != null && updateJson.getHostgroups() != null && stack.getCluster().isCreateFailed()) {
            LOGGER.info("Cluster rebuild request received. Stack id:  {}", stackId);
            try {
                recreateCluster(stack, updateJson, user, workspace);
            } catch (TransactionExecutionException e) {
                throw new TransactionRuntimeExecutionException(e);
            }
            return Response.status(Status.NO_CONTENT).build();
        }

        if (updateJson.getHostGroupAdjustment() != null) {
            clusterHostgroupAdjustmentChange(stackId, updateJson, stack);
            return Response.status(Status.NO_CONTENT).build();
        }

        if (Objects.nonNull(updateJson.getAmbariStackDetails())) {
            updateStackDetails(updateJson, stack);
            return Response.status(Status.NO_CONTENT).build();
        }
        LOGGER.error("Invalid cluster update request received. Stack id: {}", stackId);
        throw new BadRequestException("Invalid update cluster request!");
    }

    private void updateStackDetails(UpdateClusterJson updateJson, Stack stack) {
        Cluster cluster = stack.getCluster();
        if (!MAINTENANCE_MODE_ENABLED.equals(cluster.getStatus())) {
            return;
        }

        AmbariStackDetailsJson ambariStackDetails = updateJson.getAmbariStackDetails();
        Long clusterId = cluster.getId();
        if ("AMBARI".equals(ambariStackDetails.getStack())) {
            clusterService.updateAmbariRepoDetails(clusterId, ambariStackDetails);
        } else if ("HDP".equals(ambariStackDetails.getStack())) {
            clusterService.updateHdpRepoDetails(clusterId, ambariStackDetails);
        } else if ("HDF".equals(ambariStackDetails.getStack())) {
            clusterService.updateHdfRepoDetails(clusterId, ambariStackDetails);
        }
    }

    private void clusterHostgroupAdjustmentChange(Long stackId, UpdateClusterJson updateJson, Stack stack) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                    stack.getStatus()));
        }
        LOGGER.info("Cluster host adjustment request received. Stack id: {} ", stackId);
        Blueprint blueprint = stack.getCluster().getBlueprint();
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), updateJson.getHostGroupAdjustment().getHostGroup());
        if (hostGroup == null) {
            throw new BadRequestException(String.format("Host group '%s' not found or not member of the cluster '%s'",
                    updateJson.getHostGroupAdjustment().getHostGroup(), stack.getName()));
        }
        blueprintValidator.validateHostGroupScalingRequest(blueprint, hostGroup, updateJson.getHostGroupAdjustment().getScalingAdjustment());
        clusterService.updateHosts(stackId, updateJson.getHostGroupAdjustment());
    }

    private void recreateCluster(Stack stack, UpdateClusterJson updateJson, User user, Workspace workspace) throws TransactionExecutionException {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupRequest json : updateJson.getHostgroups()) {
            HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
            hostGroup = hostGroupDecorator.decorate(hostGroup, json, stack, false, workspace, user);
            hostGroups.add(hostGroup);
        }
        AmbariStackDetailsJson stackDetails = updateJson.getAmbariStackDetails();
        StackRepoDetails stackRepoDetails = null;
        if (stackDetails != null) {
            stackRepoDetails = conversionService.convert(stackDetails, StackRepoDetails.class);
        }
        clusterService.recreate(stack, updateJson.getBlueprintId(), hostGroups, updateJson.getValidateBlueprint(), stackRepoDetails,
                updateJson.getKerberosPassword(), updateJson.getKerberosPrincipal());
    }

    private void ambariUserNamePasswordChange(Long stackId, Stack stack, UserNamePasswordJson userNamePasswordJson) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                    stack.getStatus()));
        }
        if (!userNamePasswordJson.getOldPassword().equals(stack.getCluster().getPassword())) {
            throw new BadRequestException(String.format(
                    "Cluster actual password does not match in the request, please pass the real password on Stack '%s' with status '%s'.", stackId,
                    stack.getStatus()));
        }
        LOGGER.info("Cluster username password update request received. Stack id:  {}, username: {}",
                stackId, userNamePasswordJson.getUserName());
        clusterService.updateUserNamePassword(stackId, userNamePasswordJson);
    }

    public Response setMaintenanceMode(Stack stack, MaintenanceModeStatus maintenanceMode) {
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("Cluster does not exist on stack with '%s' id.", stack.getId()));
        }
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. Maintenance mode can be set to a cluster if the underlying stack is 'AVAILABLE'.",
                    stack.getId(), stack.getStatus()));
        }
        if (!cluster.isAvailable() && !MAINTENANCE_MODE_ENABLED.equals(cluster.getStatus())) {
            throw new BadRequestException(String.format(
                    "Cluster '%s' is currently in '%s' state. Maintenance mode can be set to a cluster is 'AVAILABLE'.",
                    cluster.getId(), cluster.getStatus()));
        }
        switch (maintenanceMode) {
            case ENABLED:
                cluster.setStatus(MAINTENANCE_MODE_ENABLED);
                break;
            case DISABLED:
                cluster.setStatus(AVAILABLE);
                break;
            default:
                // Nothing to do here
                break;

        }

        Response status = Response.ok().build();
        if (maintenanceMode.equals(MaintenanceModeStatus.VALIDATION_REQUESTED)) {
            if (!MAINTENANCE_MODE_ENABLED.equals(cluster.getStatus())) {
                throw new BadRequestException(String.format(
                        "Maintenance mode is not enabled for cluster '%s' (status:'%s'), it should be enabled before validation.",
                        cluster.getId(),
                        cluster.getStatus()));
            }
            clusterService.triggerMaintenanceModeValidation(stack);
            status = Response.accepted().build();
        }
        clusterService.save(cluster);
        return status;
    }
}

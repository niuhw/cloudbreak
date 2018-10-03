package com.sequenceiq.it.cloudbreak.v2.mock.maintenancemode;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.MAINTENANCE_MODE_ENABLED;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.testng.AssertJUnit.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.springframework.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsResponse;
import com.sequenceiq.cloudbreak.api.model.MaintenanceModeJson;
import com.sequenceiq.cloudbreak.api.model.MaintenanceModeStatus;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;

public class MockStackMaintenanceModeTest extends AbstractCloudbreakIntegrationTest {

    private String stackName;

    @BeforeClass
    @Parameters({"mockPort", "sshPort"})
    public void configMockServer(@Optional("9443") int mockPort, @Optional("2020") int sshPort) {
        IntegrationTestContext itContext = getItContext();
        Map<String, InstanceGroupV2Request> instanceGroupV2RequestMap = itContext.getContextParam(CloudbreakV2Constants.INSTANCEGROUP_MAP, Map.class);
        int numberOfServers = 0;
        for (InstanceGroupV2Request igr : instanceGroupV2RequestMap.values()) {
            numberOfServers += igr.getNodeCount();
        }

        stackName = itContext.getContextParam(CloudbreakV2Constants.STACK_NAME);

        MaintenanceModeMock maintenanceModeMock = applicationContext.getBean(MaintenanceModeMock.class, mockPort, sshPort, numberOfServers);
        maintenanceModeMock.addSPIEndpoints();
        maintenanceModeMock.addAmbariMappings(stackName);
        itContext.putContextParam(CloudbreakV2Constants.MOCK_SERVER, maintenanceModeMock);
        itContext.putContextParam(CloudbreakITContextConstants.MOCK_INSTANCE_MAP, maintenanceModeMock.getInstanceMap());
    }

    @Test(priority = 1)
    public void testEnableMaintenanceMode() {
        Long workspaceId = getWorkspaceId();
        getItContext().putContextParam(CloudbreakITContextConstants.WORKSPACE_ID, workspaceId);

        postMaintenanceMode(workspaceId, MaintenanceModeStatus.ENABLED, OK);

        Long stackId = Long.parseLong(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID));
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", AVAILABLE.name());
        desiredStatuses.put("clusterStatus", MAINTENANCE_MODE_ENABLED.name());
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), stackId.toString(), desiredStatuses);
    }

    private void postMaintenanceMode(Long workspaceId, MaintenanceModeStatus maintenanceModeStatus, HttpStatus expected) {
        MaintenanceModeJson maintenanceMode = new MaintenanceModeJson();
        maintenanceMode.setStatus(maintenanceModeStatus);
        try (Response response = getCloudbreakClient().stackV3Endpoint().setClusterMaintenanceMode(workspaceId, stackName, maintenanceMode)) {
            assertEquals(expected.value(), response.getStatus());
        }
    }

    private Long getWorkspaceId() {
        StackResponse stackResponse = getCloudbreakClient().stackV2Endpoint().getPublic(stackName, new HashSet<>());
        return stackResponse.getWorkspace().getId();
    }

    @Test(priority = 2)
    public void testUpdateAmbariDetailsInMaintenanceMode() {
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        UpdateClusterJson udateJson = new UpdateClusterJson();
        AmbariStackDetailsJson ambariStackDetails = new AmbariStackDetailsJson();
        ambariStackDetails.setStack("AMBARI");
        ambariStackDetails.setVersion("2.7.0.0");
        ambariStackDetails.setStackBaseURL("http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.7.0.0");
        ambariStackDetails.setGpgKeyUrl("http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.7.0.0/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins");
        udateJson.setAmbariStackDetails(ambariStackDetails);

        try (Response response = getCloudbreakClient().stackV3Endpoint().put(workspaceId, stackName, udateJson)) {
            assertEquals(NO_CONTENT.value(), response.getStatus());
        }

        StackResponse stackResponse = getCloudbreakClient().stackV3Endpoint().getByNameInWorkspace(workspaceId, stackName, new HashSet<>());
        assertEquals("Ambari", ambariStackDetails.getVersion(), stackResponse.getCluster().getAmbariRepoDetailsJson().getVersion());
        assertEquals("Ambari", ambariStackDetails.getStackBaseURL(), stackResponse.getCluster().getAmbariRepoDetailsJson().getBaseUrl());
        assertEquals("Ambari", ambariStackDetails.getGpgKeyUrl(), stackResponse.getCluster().getAmbariRepoDetailsJson().getGpgKeyUrl());
    }

    @Test(priority = 3)
    public void testUpdateHDPDetailsInMaintenanceMode() {
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        UpdateClusterJson udateJson = new UpdateClusterJson();
        AmbariStackDetailsJson ambariStackDetails = new AmbariStackDetailsJson();
        ambariStackDetails.setStack("HDP");
        ambariStackDetails.setVersion("2.6.5.0");
        ambariStackDetails.setVersionDefinitionFileUrl("http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.0/HDP-2.6.5.0-292.xml");
        udateJson.setAmbariStackDetails(ambariStackDetails);

        try (Response response = getCloudbreakClient().stackV3Endpoint().put(workspaceId, stackName, udateJson)) {
            assertEquals(NO_CONTENT.value(), response.getStatus());
        }

        StackResponse stackResponse = getCloudbreakClient().stackV3Endpoint().getByNameInWorkspace(workspaceId, stackName, new HashSet<>());
        AmbariStackDetailsResponse stackDetailsResponse = stackResponse.getCluster().getAmbariStackDetails();
        assertEquals("HDP", ambariStackDetails.getVersion(), stackDetailsResponse.getHdpVersion());
        assertEquals("HDP", ambariStackDetails.getVersionDefinitionFileUrl(), stackDetailsResponse.getStack().get("vdf-url"));

    }

    @Test(priority = 4)
    public void testRequestValidationForMaintenanceMode() {
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);

        postMaintenanceMode(workspaceId, MaintenanceModeStatus.VALIDATION_REQUESTED, ACCEPTED);

        Long stackId = Long.parseLong(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID));
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", AVAILABLE.name());
        desiredStatuses.put("clusterStatus", AVAILABLE.name());
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), stackId.toString(), desiredStatuses);

        MaintenanceModeMock maintenanceModeMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, MaintenanceModeMock.class);
        maintenanceModeMock.verify();
    }

    @Test(priority = 5)
    public void testDisableMaintenanceMode() {
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);

        postMaintenanceMode(workspaceId, MaintenanceModeStatus.DISABLED, OK);

        Long stackId = Long.parseLong(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID));
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", AVAILABLE.name());
        desiredStatuses.put("clusterStatus", AVAILABLE.name());
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), stackId.toString(), desiredStatuses);
    }

    @AfterClass
    public void breakDown() {
        MaintenanceModeMock maintenanceModeMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, MaintenanceModeMock.class);
        maintenanceModeMock.stop();
    }
}

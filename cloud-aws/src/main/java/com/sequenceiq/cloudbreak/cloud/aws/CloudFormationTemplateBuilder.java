package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsGroupView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service("CloudFormationTemplateBuilder")
public class CloudFormationTemplateBuilder {
    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private DefaultCostTaggingService defaultCostTaggingService;

    public String build(ModelContext context) {
        Map<String, Object> model = new HashMap<>();
        Collection<AwsGroupView> awsGroupViews = new ArrayList<>();
        Collection<AwsGroupView> awsGatewayGroupViews = new ArrayList<>();
        int i = 0;
        boolean multigw = context.stack.getGroups().stream().filter(g -> g.getType() == InstanceGroupType.GATEWAY).count() > 1;
        for (Group group : context.stack.getGroups()) {
            AwsInstanceView awsInstanceView = new AwsInstanceView(group.getReferenceInstanceConfiguration().getTemplate());
            String snapshotId = context.snapshotId.get(group.getName());
            String encryptedAMI = context.encryptedAMIByGroupName.get(group.getName());
            AwsGroupView groupView = new AwsGroupView(
                    group.getInstancesSize(),
                    group.getType().name(),
                    awsInstanceView.getFlavor(),
                    group.getName(),
                    awsInstanceView.getVolumes().size(),
                    awsInstanceView.isEncryptedVolumes(),
                    awsInstanceView.getVolumeSize(),
                    group.getRootVolumeSize(),
                    awsInstanceView.getVolumeType(),
                    awsInstanceView.getSpotPrice(),
                    group.getSecurity().getRules(),
                    group.getSecurity().getCloudSecurityIds(),
                    getSubnetIds(context.existingSubnetIds, i, group, multigw),
                    awsInstanceView.isKmsEnabled(),
                    awsInstanceView.getKmsKey(),
                    snapshotId,
                    encryptedAMI);
            awsGroupViews.add(groupView);
            if (group.getType() == InstanceGroupType.GATEWAY) {
                awsGatewayGroupViews.add(groupView);
            }
            i++;
        }
        model.put("instanceGroups", awsGroupViews);
        model.put("gatewayGroups", awsGatewayGroupViews);
        model.put("existingVPC", context.existingVPC);
        model.put("existingIGW", context.existingIGW);
        model.put("existingSubnet", !isNullOrEmptyList(context.existingSubnetCidr));
        model.put("enableInstanceProfile", context.enableInstanceProfile || context.instanceProfileAvailable);
        model.put("existingRole", context.instanceProfileAvailable);
        model.put("cbSubnet", (isNullOrEmptyList(context.existingSubnetCidr)) ? Lists.newArrayList(context.defaultSubnet)
                : context.existingSubnetCidr);
        model.put("dedicatedInstances", areDedicatedInstancesRequested(context.stack));
        model.put("availabilitySetNeeded", context.ac.getCloudContext().getLocation().getAvailabilityZone().value() != null);
        model.put("mapPublicIpOnLaunch", context.mapPublicIpOnLaunch);
        model.putAll(defaultCostTaggingService.prepareAllTagsForTemplate());
        try {
            String template = processTemplateIntoString(new Template("aws-template", context.template, freemarkerConfiguration), model);
            return template.replaceAll("\\t|\\n| [\\s]+", "");
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process CloudFormation freemarker template", e);
        }
    }

    private String getSubnetIds(List<String> existingSubnetIds, int i, Group group, boolean multigw) {
        return (multigw && group.getType() == InstanceGroupType.GATEWAY && !isNullOrEmptyList(existingSubnetIds))
                ? existingSubnetIds.get(i % existingSubnetIds.size()) : null;
    }

    private boolean isNullOrEmptyList(Collection<?> list) {
        return list == null || list.isEmpty();
    }

    public boolean areDedicatedInstancesRequested(CloudStack cloudStack) {
        boolean result = false;
        if (isDedicatedInstancesParamExistAndTrue(cloudStack)) {
            result = true;
        }
        return result;
    }

    private boolean isDedicatedInstancesParamExistAndTrue(CloudStack stack) {
        return stack.getParameters().containsKey("dedicatedInstances")
                && Boolean.valueOf(stack.getParameters().get("dedicatedInstances"));
    }

    public static class ModelContext {
        private AuthenticatedContext ac;

        private CloudStack stack;

        private boolean existingVPC;

        private boolean existingIGW;

        private List<String> existingSubnetIds = new ArrayList<>();

        private List<String> existingSubnetCidr = new ArrayList<>();

        private boolean mapPublicIpOnLaunch;

        private String template;

        private boolean enableInstanceProfile;

        private boolean instanceProfileAvailable;

        private String defaultSubnet;

        private Map<String, String> snapshotId = new HashMap<>();

        private Map<String, String> encryptedAMIByGroupName = new HashMap<>();

        public ModelContext withAuthenticatedContext(AuthenticatedContext ac) {
            this.ac = ac;
            return this;
        }

        public ModelContext withStack(CloudStack stack) {
            this.stack = stack;
            return this;
        }

        public ModelContext withExistingVpc(boolean existingVpc) {
            existingVPC = existingVpc;
            return this;
        }

        public ModelContext withExistingIGW(boolean existingIGW) {
            this.existingIGW = existingIGW;
            return this;
        }

        public ModelContext withExistingSubnetCidr(List<String> cidr) {
            existingSubnetCidr = cidr;
            return this;
        }

        public ModelContext withExistingSubnetIds(List<String> subnetIds) {
            existingSubnetIds = subnetIds;
            return this;
        }

        public ModelContext mapPublicIpOnLaunch(boolean mapPublicIpOnLaunch) {
            this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
            return this;
        }

        public ModelContext withEnableInstanceProfile(boolean enableInstanceProfile) {
            this.enableInstanceProfile = enableInstanceProfile;
            return this;
        }

        public ModelContext withInstanceProfileAvailable(boolean instanceProfileAvailable) {
            this.instanceProfileAvailable = instanceProfileAvailable;
            return this;
        }

        public ModelContext withTemplate(String template) {
            this.template = template;
            return this;
        }

        public ModelContext withDefaultSubnet(String subnet) {
            defaultSubnet = subnet;
            return this;
        }

        public ModelContext withSnapshotId(Map<String, String> snapshotId) {
            this.snapshotId = snapshotId;
            return this;
        }

        public ModelContext withEncryptedAMIByGroupName(Map<String, String> encryptedAMIByGroupName) {
            this.encryptedAMIByGroupName.putAll(encryptedAMIByGroupName);
            return this;
        }

    }
}

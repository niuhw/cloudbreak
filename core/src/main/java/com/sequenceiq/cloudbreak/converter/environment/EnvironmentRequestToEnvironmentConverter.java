package com.sequenceiq.cloudbreak.converter.environment;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Environment;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class EnvironmentRequestToEnvironmentConverter extends AbstractConversionServiceAwareConverter<EnvironmentRequest, Environment> {

    @Inject
    private CredentialService credentialService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Override
    public Environment convert(EnvironmentRequest source) {
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        Environment environment = new Environment();
        environment.setWorkspace(workspaceService.getByIdForCurrentUser(workspaceId));
        environment.setName(source.getName());
        environment.setDescription(source.getDescription());
        environment.setRegionsSet(source.getRegions());
        environment.setCloudPlatform(source.getCloudPlatform());
        return environment;
    }
}

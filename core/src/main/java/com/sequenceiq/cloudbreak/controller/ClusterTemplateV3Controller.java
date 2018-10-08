package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ClusterTemplateV3EndPoint;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.converter.mapper.ClusterTemplateMapper;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.repository.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class ClusterTemplateV3Controller extends NotificationController
        implements ClusterTemplateV3EndPoint, WorkspaceAwareResourceController<ClusterTemplate> {

    @Inject
    private ClusterTemplateMapper clusterTemplateMapper;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ClusterTemplateService clusterTemplateService;

    @Override
    public ClusterTemplateResponse createInWorkspace(Long workspaceId, @Valid ClusterTemplateRequest request) {
        ClusterTemplate clusterTemplate = clusterTemplateMapper.mapRequestToEntity(request);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        clusterTemplate = clusterTemplateService.create(clusterTemplate, workspaceId, user);
        return clusterTemplateMapper.mapEntityToResponse(clusterTemplate);
    }

    @Override
    public Set<ClusterTemplateResponse> listByWorkspace(Long workspaceId) {
        return null;
    }

    @Override
    public ClusterTemplateResponse getByNameInWorkspace(Long workspaceId, String name) {
        return null;
    }

    @Override
    public ClusterTemplateResponse deleteInWorkspace(Long workspaceId, String name) {
        return null;
    }

    @Override
    public Class<? extends WorkspaceResourceRepository<ClusterTemplate, ?>> getWorkspaceAwareResourceRepository() {
        return ClusterTemplateRepository.class;
    }
}

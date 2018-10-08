package com.sequenceiq.cloudbreak.service.template;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.repository.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;

@Service
public class ClusterTemplateService extends AbstractWorkspaceAwareResourceService<ClusterTemplate> {

    @Inject
    private ClusterTemplateRepository repository;

    @Override
    protected WorkspaceResourceRepository<ClusterTemplate, Long> repository() {
        return repository;
    }

    @Override
    protected void prepareDeletion(ClusterTemplate resource) {

    }

    @Override
    protected void prepareCreation(ClusterTemplate resource) {

    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.CLUSTER_TEMPLATE;
    }
}

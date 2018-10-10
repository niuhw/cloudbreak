package com.sequenceiq.cloudbreak.service.environment;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.domain.environment.EnvironmentAwareResource;
import com.sequenceiq.cloudbreak.repository.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;

public abstract class AbstractEnvironmentAwareService<T extends EnvironmentAwareResource> extends AbstractWorkspaceAwareResourceService<T> {

    public Set<T> findByNamesInWorkspace(Set<String> names, @NotNull Long workspaceId) {
        return repository().findAllByNameInAndWorkspaceId(names, workspaceId);
    }

    protected abstract EnvironmentResourceRepository<T, Long> repository();
}

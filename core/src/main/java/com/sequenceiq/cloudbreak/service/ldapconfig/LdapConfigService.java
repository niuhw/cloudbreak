package com.sequenceiq.cloudbreak.service.ldapconfig;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Environment;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.repository.EnvironmentRepository;
import com.sequenceiq.cloudbreak.repository.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.repository.LdapConfigRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.AbstractEnvironmentAwareService;

@Service
public class LdapConfigService extends AbstractEnvironmentAwareService<LdapConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigService.class);

    @Inject
    private LdapConfigRepository ldapConfigRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private EnvironmentRepository environmentRepository;

    public LdapConfig get(Long id) {
        return ldapConfigRepository.findById(id).orElseThrow(notFound("LdapConfig", id));
    }

    public void delete(Long id) {
        delete(get(id));
    }

    @Override
    public EnvironmentResourceRepository<LdapConfig, Long> repository() {
        return ldapConfigRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.LDAP;
    }

    @Override
    protected void prepareDeletion(LdapConfig ldapConfig) {
        List<Cluster> clustersWithLdap = clusterService.findByLdapConfigWithoutAuth(ldapConfig);
        if (!clustersWithLdap.isEmpty()) {
            if (clustersWithLdap.size() > 1) {
                String clusters = clustersWithLdap
                        .stream()
                        .map(Cluster::getName)
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with LDAP config '%s'. Please remove these before deleting the LDAP config. "
                                + "The following clusters are using this LDAP: [%s]", ldapConfig.getName(), clusters));
            }
            throw new BadRequestException(String.format("There is a cluster ['%s'] which uses LDAP config '%s'. Please remove this "
                    + "cluster before deleting the LDAP config", clustersWithLdap.get(0).getName(), ldapConfig.getName()));
        }
    }

    @Override
    protected void prepareCreation(LdapConfig resource) {
        // !!!! Just for tests !!!!
        Set<Environment> envs = environmentRepository.findAllByWorkspaceId(1L);
        Set<LdapConfig> ldaps = ldapConfigRepository.findAllByWorkspaceIdAndEnvironments_Id(1L, 1L);
        Set<LdapConfig> ldaps2 = ldapConfigRepository.findAllByWorkspaceIdAndEnvironments_Id(1L, 2L);
        Set<EnvironmentView> envviews = envs.stream().map(env -> {
            EnvironmentView envView = new EnvironmentView();
            envView.setWorkspace(env.getWorkspace());
            envView.setId(env.getId());
            envView.setName(env.getName());
            envView.setDescription(env.getDescription());
            return envView;
        }).collect(Collectors.toSet());
        resource.setEnvironments(envviews);
    }
}

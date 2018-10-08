package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.EnvironmentV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentAttachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.SimpleEnvironmentResponse;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentAttachRequestValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentDetachRequestValidator;
import com.sequenceiq.cloudbreak.domain.Environment;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;

@Controller
@Transactional(TxType.NEVER)
public class EnvironmentV3Controller implements EnvironmentV3Endpoint {

    @Inject
    private EnvironmentService environmentService;

    @Override
    public DetailedEnvironmentResponse create(Long workspaceId, @Valid EnvironmentRequest request) {
        return null;
    }

    @Override
    public DetailedEnvironmentResponse get(Long workspaceId, String environmentName) {
        return null;
    }

    @Override
    public SimpleEnvironmentResponse list(Long workspaceId) {
        return null;
    }

    // TODO: finish
    @Override
    public SimpleEnvironmentResponse attachResources(Long workspaceId, String environmentName, @Valid EnvironmentAttachRequest request) {
        Environment environment = environmentService.getByNameForWorkspaceId(environmentName, workspaceId);
        ValidationResult validationResult = EnvironmentAttachRequestValidator
                .of(environment, environmentService).validate(request);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        environment = environmentService.attachResources(environment, request);
        return null;
    }

    // TODO: finish
    @Override
    public SimpleEnvironmentResponse detachResources(Long workspaceId, String environmentName, @Valid EnvironmentDetachRequest request) {
        Environment environment = environmentService.getByNameForWorkspaceId(environmentName, workspaceId);
        ValidationResult validationResult = EnvironmentDetachRequestValidator
                .of(environment, environmentService).validate(request);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        environment = environmentService.detachResources(environment, request);
        return null;
    }
}

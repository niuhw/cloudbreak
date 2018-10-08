package com.sequenceiq.cloudbreak.controller.validation.environment;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentAttachRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.domain.Environment;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;

public class EnvironmentAttachRequestValidator implements Validator<EnvironmentAttachRequest> {

    private final Environment environment;

    private final EnvironmentService environmentService;

    public static EnvironmentAttachRequestValidator of(Environment environment, EnvironmentService environmentService) {
        return new EnvironmentAttachRequestValidator(environment, environmentService);
    }

    private EnvironmentAttachRequestValidator(Environment environment, EnvironmentService environmentService) {
        this.environment = environment;
        this.environmentService = environmentService;
    }

    @Override
    public ValidationResult validate(EnvironmentAttachRequest subject) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (environment == null) {
            return validationResultBuilder.error("Environment cannot be null.").build();
        }
        // TODO: actaul validation
        return validationResultBuilder.build();
    }
}

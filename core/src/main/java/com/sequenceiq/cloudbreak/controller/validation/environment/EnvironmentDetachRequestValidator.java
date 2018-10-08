package com.sequenceiq.cloudbreak.controller.validation.environment;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.domain.Environment;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;

public class EnvironmentDetachRequestValidator implements Validator<EnvironmentDetachRequest> {

    private final Environment environment;

    private final EnvironmentService environmentService;

    public static EnvironmentDetachRequestValidator of(Environment environment, EnvironmentService environmentService) {
        return new EnvironmentDetachRequestValidator(environment, environmentService);
    }

    private EnvironmentDetachRequestValidator(Environment environment, EnvironmentService environmentService) {
        this.environment = environment;
        this.environmentService = environmentService;
    }

    @Override
    public ValidationResult validate(EnvironmentDetachRequest subject) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (environment == null) {
            return validationResultBuilder.error("Environment cannot be null.").build();
        }
        // TODO: actaul validation
        return validationResultBuilder.build();
    }
}

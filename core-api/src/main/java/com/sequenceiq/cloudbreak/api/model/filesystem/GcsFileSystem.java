package com.sequenceiq.cloudbreak.api.model.filesystem;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcsFileSystem extends BaseFileSystem {

    private String serviceAccountEmail;

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GcsFileSystem)) {
            return false;
        }
        GcsFileSystem that = (GcsFileSystem) o;
        return Objects.equals(getServiceAccountEmail(), that.getServiceAccountEmail());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServiceAccountEmail());
    }

}

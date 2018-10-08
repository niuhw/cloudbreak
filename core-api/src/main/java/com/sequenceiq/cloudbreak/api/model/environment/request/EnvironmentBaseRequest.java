package com.sequenceiq.cloudbreak.api.model.environment.request;

import java.util.Set;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("Environment")
public abstract class EnvironmentBaseRequest {

    @ApiModelProperty(EnvironmentRequestModelDescription.PROXY_CONFIGS)
    private Set<String> proxyConfigs;

    @ApiModelProperty(EnvironmentRequestModelDescription.LDAP_CONFIGS)
    private Set<String> ldapConfigs;

    @ApiModelProperty(EnvironmentRequestModelDescription.RDS_CONFIGS)
    private Set<String> rdsConfigs;

    public Set<String> getProxyConfigs() {
        return proxyConfigs;
    }

    public void setProxyConfigs(Set<String> proxyConfigs) {
        this.proxyConfigs = proxyConfigs;
    }

    public Set<String> getLdapConfigs() {
        return ldapConfigs;
    }

    public void setLdapConfigs(Set<String> ldapConfigs) {
        this.ldapConfigs = ldapConfigs;
    }

    public Set<String> getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(Set<String> rdsConfigs) {
        this.rdsConfigs = rdsConfigs;
    }
}

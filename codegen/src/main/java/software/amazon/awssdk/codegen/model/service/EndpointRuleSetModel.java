/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.codegen.model.service;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.RuleModel;

public class EndpointRuleSetModel {
    private String serviceId;
    private String version;
    private Map<String, ParameterModel> parameters;
    private List<RuleModel> rules;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, ParameterModel> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, ParameterModel> parameters) {
        this.parameters = parameters;
    }

    public List<RuleModel> getRules() {
        return rules;
    }

    public void setRules(List<RuleModel> rules) {
        this.rules = rules;
    }
}

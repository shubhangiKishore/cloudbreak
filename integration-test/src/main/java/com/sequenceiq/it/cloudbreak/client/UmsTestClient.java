package com.sequenceiq.it.cloudbreak.client;


import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.ums.AssignUmsRoleAction;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Service
public class UmsTestClient {

    private static final String DH_CREATOR_CRN = "crn:altus:iam:us-west-1:altus:resourceRole:DataHubCreator";

    public Action<EnvironmentTestDto, UmsClient> assignDatahubCreatorRole(String userKey) {
        return new AssignUmsRoleAction(userKey, DH_CREATOR_CRN);
    }
}

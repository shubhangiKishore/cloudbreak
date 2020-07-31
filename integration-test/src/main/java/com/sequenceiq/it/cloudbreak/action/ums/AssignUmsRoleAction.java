package com.sequenceiq.it.cloudbreak.action.ums;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

public class AssignUmsRoleAction implements Action<EnvironmentTestDto, UmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignUmsRoleAction.class);

    private String userKey;

    private String userRoleCrn;

    public AssignUmsRoleAction(String userKey, String userRoleCrn) {
        this.userKey = userKey;
        this.userRoleCrn = userRoleCrn;
    }

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, UmsClient client) throws Exception {
        LOGGER.info("Assigning resourceRole Datahubcreator for environment");
        CloudbreakUser user = testContext.getUser(userKey);
        client.getUmsClient().assignResourceRole(user.getCrn(), testDto.getResponse().getCrn(), userRoleCrn, Optional.of(""));
        return testDto;
    }
}

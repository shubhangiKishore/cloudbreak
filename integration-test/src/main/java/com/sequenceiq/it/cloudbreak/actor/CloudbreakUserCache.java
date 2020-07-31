package com.sequenceiq.it.cloudbreak.actor;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class CloudbreakUserCache {

    private List<CloudbreakUser> users;

    public CloudbreakUser getByName(String name) {
        if (users == null) {
            initUsers();
        }
        return users.stream().filter(u -> u.getDisplayName().equals(name)).findFirst().get();
    }

    public void initUsers() {
        String userConfigPath = "ums-users/api-credentials.json";
        try {
            this.users = JsonUtil.readValue(
                    FileReaderUtils.readFileFromClasspathQuietly(userConfigPath), new TypeReference<List<CloudbreakUser>>() {
                    });
        } catch (IOException e) {
            throw new RuntimeException("Can't read file: " + userConfigPath);
        }
        users.stream().forEach(u -> CloudbreakUser.validateRealUmsUser(u));
    }
}

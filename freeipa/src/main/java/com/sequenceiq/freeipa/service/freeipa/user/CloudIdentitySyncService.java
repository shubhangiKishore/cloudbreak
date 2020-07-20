package com.sequenceiq.freeipa.service.freeipa.user;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CloudIdentity;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;
import com.sequenceiq.sdx.api.model.SetRangerCloudIdentityMappingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
public class CloudIdentitySyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudIdentitySyncService.class);

    private static final int SYNC_TIMEOUT_MS = 1000 * 4;

    private static final int SYNC_SLEEP_INTERVAL_MS = 400;

    @Inject
    private Clock clock;

    @Inject
    private SdxEndpoint sdxEndpoint;

    public void syncCloudIdentites(Stack stack, UmsUsersState umsUsersState, BiConsumer<String, String> warnings) {
        LOGGER.info("Syncing cloud identities for stack = {}", stack);
        if (CloudPlatform.AZURE.equalsIgnoreCase(stack.getCloudPlatform())) {
            LOGGER.info("Syncing Azure Object IDs for stack = {}", stack);
            syncAzureObjectIds(stack, umsUsersState, warnings);
        }
    }

    private void syncAzureObjectIds(Stack stack, UmsUsersState umsUsersState, BiConsumer<String, String> warnings) {
        String envCrn = stack.getEnvironmentCrn();
        LOGGER.info("Syncing Azure Object IDs for environment {}", envCrn);

        try {
            Map<String, List<CloudIdentity>> userCloudIdentites = getUserCloudIdentitiesToSync(umsUsersState);

            Map<String, String> userToAzureObjectIdMap = getAzureObjectIdMap(userCloudIdentites);

            SetRangerCloudIdentityMappingRequest setRangerCloudIdentityMappingRequest = new SetRangerCloudIdentityMappingRequest();
            setRangerCloudIdentityMappingRequest.setAzureUserMapping(userToAzureObjectIdMap);

            LOGGER.debug("Setting ranger cloud identity mapping: {}", setRangerCloudIdentityMappingRequest);
            RangerCloudIdentitySyncStatus syncStatus = sdxEndpoint.setRangerCloudIdentityMapping(envCrn, setRangerCloudIdentityMappingRequest);

            // The sync status represents a cloud identity sync that may still be in progress, which we need to poll to check for completion.
            pollSyncStatus(syncStatus, envCrn, warnings);
        } catch (Exception e) {
            LOGGER.warn("Failed to set cloud identity mapping for environment {}", envCrn, e);
            warnings.accept(envCrn, "Failed to set cloud identity mapping");
        }
    }

    private void pollSyncStatus(RangerCloudIdentitySyncStatus syncStatus, String envCrn, BiConsumer<String, String> warnings) {
        // NOTE: Although it's synchronously polling, in practice this sync takes less than a second to complete
        Instant startTime = clock.getCurrentInstant();
        while (true) {
            LOGGER.info("syncStatus = {}", syncStatus);
            switch (syncStatus.getState()) {
                case SUCCESS:
                    LOGGER.info("Successfully synced cloud identity, envCrn = {}", envCrn);
                    return;
                case FAILED:
                    LOGGER.error("Failed to sync cloud identity, envCrn = {}", envCrn);
                    warnings.accept(envCrn, "Failed to sync cloud identity into environment");
                    return;
                default:
                    LOGGER.info("Sync is in progress, retrying");
                    try {
                        Thread.sleep(SYNC_SLEEP_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        LOGGER.error("Interrupted during cloud identity sync", e);
                        warnings.accept(envCrn, "Interrupted during cloud identity sync");
                    }
                    long commandId = syncStatus.getCommandId();
                    syncStatus = sdxEndpoint.getRangerCloudIdentitySyncStatus(envCrn, commandId);
            }
            if (clock.getCurrentInstant().isAfter(startTime.plusMillis(SYNC_TIMEOUT_MS))) {
                LOGGER.error("Timed out syncing cloud identity into environment, envCrn = {}, syncStatus = {}", envCrn, syncStatus);
                warnings.accept(envCrn, "Timed out syncing cloud identity into environment");
            }
        }
    }

    private Map<String, String> getAzureObjectIdMap(Map<String, List<CloudIdentity>> cloudIdentityMapping) {
        LOGGER.debug("Exracting Azure Object ID mapping from {}", cloudIdentityMapping);
        ImmutableMap.Builder<String, String> azureObjectIdMap = ImmutableMap.builder();
        cloudIdentityMapping.forEach((key, cloudIdentities) -> {
            Optional<String> azureObjectId = getOptionalAzureObjectId(cloudIdentities);
            if (azureObjectId.isPresent()) {
                azureObjectIdMap.put(key, azureObjectId.get());
            }
        });
        return azureObjectIdMap.build();
    }

    private Optional<String> getOptionalAzureObjectId(List<CloudIdentity> cloudIdentities) {
        List<CloudIdentity> azureCloudIdentities = cloudIdentities.stream()
                .filter(cloudIdentity -> cloudIdentity.getCloudIdentityName().hasAzureCloudIdentityName())
                .collect(Collectors.toList());
        if (azureCloudIdentities.isEmpty()) {
            return Optional.empty();
        } else if (azureCloudIdentities.size() > 1) {
            throw new IllegalStateException(String.format("List contains multiple azure cloud identities = %s", cloudIdentities));
        } else {
            String azureObjectId = Iterables.getOnlyElement(azureCloudIdentities).getCloudIdentityName().getAzureCloudIdentityName().getObjectId();
            return Optional.of(azureObjectId);
        }
    }

    private Map<String, List<CloudIdentity>> getUserCloudIdentitiesToSync(UmsUsersState umsUsersState) {
        Map<String, List<CloudIdentity>> allUserCloudIdentites = umsUsersState.getUserToCloudIdentityMap();
        Set<String> userFilter = usersWithEnvironmentAccess(umsUsersState);
        return allUserCloudIdentites.entrySet().stream()
                .filter(entry -> userFilter.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> usersWithEnvironmentAccess(UmsUsersState umsUsersState) {
        return umsUsersState.getUsersState().getUsers().stream()
                .map(FmsUser::getName)
                .collect(Collectors.toSet());
    }

}

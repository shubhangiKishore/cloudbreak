package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr;

import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.AWS_REGION_KEY;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.DISASTER_RECOVERY_KEY;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.OBJECT_STORAGE_URL_KEY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sequenceiq.cloudbreak.domain.stack.*;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.*;

import java.net.URISyntaxException;
import java.util.*;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {BackupRestoreSaltConfigGenerator.class})
public class BackupRestoreSaltConfigGeneratorTest {

    private static final String BACKUP_ID = "backupId";
    public static final String US_WEST_2 = "us-west-2";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Inject
    private BackupRestoreSaltConfigGenerator saltConfigGenerator;

    @Test
    public void testCreateSaltConfig() throws URISyntaxException {
        String cloudPlatform = "aws";
        String location = "/test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);
        placeholderStack.setRegion(US_WEST_2);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, placeholderStack);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>)disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("s3://test/backups/backupId_database_backup", disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
        assertEquals(US_WEST_2, disasterRecoveryKeyValuePairs.get(AWS_REGION_KEY));
    }
}

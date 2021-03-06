package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sequenceiq.authorization.annotation.InternalOnly;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

@RunWith(MockitoJUnitRunner.class)
public class PermissionCheckServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String INTERNAL_ACTOR_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private PermissionChecker permissionChecker;

    @Spy
    private List<PermissionChecker> permissionCheckers = new ArrayList<PermissionChecker>();

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private PermissionCheckService underTest;

    @Before
    public void setup() {
        permissionCheckers.add(permissionChecker);
        underTest.populatePermissionCheckMap();
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
    }

    @Test
    public void testHasPermissionIfAnnotationIsNotPresent() throws NoSuchMethodException {
        when(commonPermissionCheckingUtils.getAuthorizationClass(proceedingJoinPoint)).thenReturn(Optional.empty());
        when(methodSignature.getMethod()).thenReturn(String.class.getMethod("length"));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no access to this resource.");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));
    }

    @Test
    public void testHasPermissionIfAuthorizationDisabled() throws NoSuchMethodException {
        when(commonPermissionCheckingUtils.getAuthorizationClass(proceedingJoinPoint)).thenReturn(Optional.of(ExampleClass.class));
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("disabledAuthorization"));
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(null);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(permissionChecker, times(0)).checkPermissions(any(), anyString(), any(), any(), anyLong());
    }

    @Test
    public void testHasPermissionIfThereAreTooManyAnnotationOnMethod() throws NoSuchMethodException {
        when(commonPermissionCheckingUtils.getAuthorizationClass(proceedingJoinPoint)).thenReturn(Optional.of(ExampleClass.class));
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("tooManyAnnotation"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(permissionChecker, times(0)).checkPermissions(any(), anyString(), any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
    }

    @Test
    public void testHasPermission() throws NoSuchMethodException {
        when(commonPermissionCheckingUtils.getAuthorizationClass(proceedingJoinPoint)).thenReturn(Optional.of(ExampleClass.class));
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("correctMethod"));
        when(permissionChecker.supportedAnnotation()).thenReturn(CheckPermissionByAccount.class);
        doNothing().when(permissionChecker).checkPermissions(any(), any(), any(), any(), anyLong());
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(null);

        underTest.populatePermissionCheckMap();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(permissionChecker).checkPermissions(any(), any(), any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
    }

    @Test
    public void testInternalOnlyIfNotInternalActor() throws NoSuchMethodException {
        when(commonPermissionCheckingUtils.getAuthorizationClass(proceedingJoinPoint)).thenReturn(Optional.of(ExampleClass.class));
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("internalOnlyMethod"));
        when(permissionChecker.supportedAnnotation()).thenReturn(InternalOnly.class);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no access to this resource.");

        underTest.populatePermissionCheckMap();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));
    }

    @Test
    public void testInternalOnlyIfInternalActor() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("internalOnlyMethod"));
        when(permissionChecker.supportedAnnotation()).thenReturn(InternalOnly.class);

        underTest.populatePermissionCheckMap();
        ThreadBasedUserCrnProvider.doAs(INTERNAL_ACTOR_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
    }

    private static class ExampleClass {

        @DisableCheckPermissions
        public void disabledAuthorization() {

        }

        @DisableCheckPermissions
        @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
        public void tooManyAnnotation() {

        }

        @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
        public void correctMethod() {

        }

        @InternalOnly
        public void internalOnlyMethod() {

        }
    }
}

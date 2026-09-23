package vn.vinamik.erp_backend.platform.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class identity_admin_service_tests {
    @Test
    void refuses_to_lock_own_account_before_database_call() {
        identity_admin_service service = new identity_admin_service(null, null, null, null);
        authenticated_user actor = new authenticated_user(7L, "admin", List.of("identity_user_update"));

        assertThrows(IllegalArgumentException.class, () -> service.update_user(
                7L,
                new identity_user_update_request("locked", null),
                actor,
                "test-correlation"));
    }

    @Test
    void refuses_to_remove_system_admin_role_from_super_admin() {
        identity_admin_repository repository = mock(identity_admin_repository.class);
        identity_admin_service service = new identity_admin_service(repository, null, null, mock(audit_event_writer.class));
        authenticated_user actor = new authenticated_user(
                7L, "admin", List.of("identity_role_update", "identity_role_admin"));
        when(repository.exists_user(1L)).thenReturn(true);
        when(repository.is_super_admin(1L)).thenReturn(true);
        when(repository.active_role_codes()).thenReturn(Set.of("system_admin", "read_only"));

        assertThrows(AccessDeniedException.class, () -> service.update_roles(
                1L,
                new identity_user_roles_request(List.of("read_only")),
                actor,
                "test-correlation"));

        verify(repository, never()).delete_user_roles(1L);
    }

    @Test
    void refuses_to_disable_super_admin_before_database_update() {
        identity_admin_repository repository = mock(identity_admin_repository.class);
        identity_admin_service service = new identity_admin_service(repository, null, null, mock(audit_event_writer.class));
        authenticated_user actor = new authenticated_user(
                7L, "admin", List.of("identity_user_update"));
        when(repository.exists_user(1L)).thenReturn(true);
        when(repository.is_super_admin(1L)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> service.update_user(
                1L,
                new identity_user_update_request("disabled", null),
                actor,
                "test-correlation"));

        verify(repository, never()).update_user(1L, "disabled", null, 7L);
    }}


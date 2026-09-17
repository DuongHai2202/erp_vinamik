function has_permission(current_user, permission_code) {
  return Boolean(current_user?.permission_codes?.includes(permission_code));
}

function has_any_permission(current_user, permission_codes) {
  return permission_codes.some((permission_code) => has_permission(current_user, permission_code));
}

export { has_any_permission, has_permission };
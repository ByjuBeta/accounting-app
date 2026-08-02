package com.accountingapp.auth;

import com.accountingapp.common.context.CurrentMembershipContext;
import org.springframework.stereotype.Component;

/** SpEL-callable RBAC checks for {@code @PreAuthorize}, e.g. {@code @PreAuthorize("@access.isOrgAdmin()")}. */
@Component("access")
public class AccessControl {

    public boolean isOrgAdmin() {
        return CurrentMembershipContext.isAdmin();
    }
}

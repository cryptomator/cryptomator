package org.cryptomator.ui.keyloading;

import javax.inject.Scope;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Scope for identity selection component.
 */
@Scope
@Documented
@Retention(RUNTIME)
public @interface IdentitySelectionScoped {
}

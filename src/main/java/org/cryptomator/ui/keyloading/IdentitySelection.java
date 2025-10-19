package org.cryptomator.ui.keyloading;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import javax.inject.Qualifier;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifier for identity selection in key loading.
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface IdentitySelection {
}

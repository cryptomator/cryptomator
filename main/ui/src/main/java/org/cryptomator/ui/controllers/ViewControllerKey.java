package org.cryptomator.ui.controllers;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import dagger.MapKey;

@Documented
@Target(METHOD)
@Retention(RUNTIME)
@MapKey
public @interface ViewControllerKey {
	Class<? extends ViewController> value();
}

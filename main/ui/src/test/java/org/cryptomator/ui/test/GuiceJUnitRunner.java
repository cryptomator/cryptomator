package org.cryptomator.ui.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Taken from http://fabiostrozzi.eu/2011/03/27/junit-tests-easy-guice/
 */
public class GuiceJUnitRunner extends BlockJUnit4ClassRunner {
	private final Injector injector;

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	public @interface GuiceModules {
		Class<?>[] value();
	}

	@Override
	public Object createTest() throws Exception {
		Object obj = super.createTest();
		injector.injectMembers(obj);
		return obj;
	}

	public GuiceJUnitRunner(Class<?> klass) throws InitializationError {
		super(klass);
		Class<?>[] classes = getModulesFor(klass);
		injector = createInjectorFor(classes);
	}

	private Injector createInjectorFor(Class<?>[] classes) throws InitializationError {
		Module[] modules = new Module[classes.length];
		for (int i = 0; i < classes.length; i++) {
			try {
				modules[i] = (Module) (classes[i]).newInstance();
			} catch (InstantiationException e) {
				throw new InitializationError(e);
			} catch (IllegalAccessException e) {
				throw new InitializationError(e);
			}
		}
		return Guice.createInjector(modules);
	}

	private Class<?>[] getModulesFor(Class<?> klass) throws InitializationError {
		GuiceModules annotation = klass.getAnnotation(GuiceModules.class);
		if (annotation == null)
			throw new InitializationError("Missing @GuiceModules annotation for unit test '" + klass.getName() + "'");
		return annotation.value();
	}
}
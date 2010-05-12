/******************************************************************************
 * Copyright (c) 2006, 2010 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * The Eclipse Public License is available at 
 * http://www.eclipse.org/legal/epl-v10.html and the Apache License v2.0
 * is available at http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses. 
 * 
 * Contributors:
 *   VMware Inc.
 *****************************************************************************/

package org.eclipse.gemini.blueprint.iandt.proxycreator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.DocumentEvent;

import org.eclipse.gemini.blueprint.iandt.BaseIntegrationTest;
import org.osgi.framework.AdminPermission;
import org.eclipse.gemini.blueprint.service.importer.support.Cardinality;
import org.eclipse.gemini.blueprint.service.importer.support.ImportContextClassLoader;
import org.eclipse.gemini.blueprint.service.importer.support.OsgiServiceProxyFactoryBean;

/**
 * Integration test for bug OSGI-597.
 * 
 * This test tries to create a proxy for DocumentEvent w/o importing its
 * dependency, namely javax.swing.text.Element.
 * 
 * @author Costin Leau
 */
public class ClassDependenciesVisibilityTest extends BaseIntegrationTest {

	private static String DEPENDENCY_CLASS = "javax.swing.text.Element";


	public void testPackageDependency() throws Exception {
		ClassLoader cl = applicationContext.getClassLoader();
		System.out.println(cl);
		OsgiServiceProxyFactoryBean fb = new OsgiServiceProxyFactoryBean();
		fb.setBundleContext(bundleContext);
		fb.setCardinality(Cardinality.C_0__1);
		fb.setContextClassLoader(ImportContextClassLoader.UNMANAGED);
		fb.setInterfaces(new Class<?>[] { DocumentEvent.class });
		fb.setBeanClassLoader(cl);
		fb.setApplicationEventPublisher(applicationContext);
		fb.afterPropertiesSet();

		checkPackageVisibility(cl);

		Object proxy = fb.getObject();
		assertNotNull(proxy);
		assertTrue(proxy instanceof DocumentEvent);
		System.out.println(proxy.getClass());

	}

	public void testJdkProxy() throws Exception {
		InvocationHandler ih = new InvocationHandler() {

			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return null;
			}
		};
		ClassLoader cl = applicationContext.getClassLoader();
		checkPackageVisibility(cl);

		try {
			Object proxy = Proxy.newProxyInstance(cl, new Class<?>[] { DocumentEvent.class }, ih);
			assertNotNull(proxy);
			System.out.println(proxy.getClass());

			fail("should have failed");
		}
		catch (Throwable cnfe) {
			// expected
		}
	}

	private void checkPackageVisibility(ClassLoader cl) throws Exception {

		try {
			cl.loadClass(DEPENDENCY_CLASS);
			fail("should not be able to load " + DEPENDENCY_CLASS);
		}
		catch (ClassNotFoundException cnfe) {
			// expected
		}
	}

	// remove the javax.* boot delegation
	protected List getBootDelegationPackages() {
		List packages = super.getBootDelegationPackages();
		packages.remove("javax.*");
		packages.remove("javax.swing.*");
		return packages;
	}

	protected List getTestPermissions() {
		List perms = super.getTestPermissions();
		// export package
		perms.add(new RuntimePermission("*", "getClassLoader"));
		return perms;
	}
}

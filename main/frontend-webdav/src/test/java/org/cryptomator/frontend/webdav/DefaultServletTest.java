/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import static org.mockito.Mockito.mock;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;


public class DefaultServletTest {
	
	private Tarpit tarpit = mock(Tarpit.class);
	
	private DefaultServlet inTest = new DefaultServlet(tarpit);
	
	@Test
	public void testFactory() throws ServletException {
		
		ServletHolder[] holders = inTest.createServletContextHandler().getServletHandler().getServlets();
		Assert.assertEquals(1, holders.length);
		ServletHolder holder = holders[0];
		
		Servlet servlet = holder.getServlet();
		Assert.assertTrue(servlet instanceof DefaultServlet);
	}
	
	@Test
	public void testResponse() throws IOException, ServletException {
		final DefaultServlet servlet = inTest;
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		
		servlet.doOptions(request, response);
		
		Mockito.verify(response).addHeader("MS-Author-Via", "DAV");
		Mockito.verify(response).addHeader("DAV", "1, 2");
		Mockito.verify(response).setStatus(204);
	}

}

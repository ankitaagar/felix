/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.test.scenarios.service.dependency.statics;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;

public class OptionalDependencies extends OSGiTestCase {
	
	ComponentInstance instance1, instance2, instance3, instance4, instance5;
	ComponentInstance fooProvider;
	
	public void setUp() {
		try {
			Properties prov = new Properties();
			prov.put("name", "FooProvider");
			fooProvider = Utils.getFactoryByName(context, "FooProviderType-1").createComponentInstance(prov);
			fooProvider.stop();
			
			Properties i1 = new Properties();
			i1.put("name", "Simple");
			instance1 = Utils.getFactoryByName(context, "StaticSimpleOptionalCheckServiceProvider").createComponentInstance(i1);
		
			Properties i2 = new Properties();
			i2.put("name", "Void");
			instance2 = Utils.getFactoryByName(context, "StaticVoidOptionalCheckServiceProvider").createComponentInstance(i2);
		
			Properties i3 = new Properties();
			i3.put("name", "Object");
			instance3 = Utils.getFactoryByName(context, "StaticObjectOptionalCheckServiceProvider").createComponentInstance(i3);
		
			Properties i4 = new Properties();
			i4.put("name", "Ref");
			instance4 = Utils.getFactoryByName(context, "StaticRefOptionalCheckServiceProvider").createComponentInstance(i4);
			
			Properties i5 = new Properties();
            i5.put("name", "Both");
            instance5 = Utils.getFactoryByName(context, "StaticBothOptionalCheckServiceProvider").createComponentInstance(i5);
		} catch(Exception e) { fail(e.getMessage()); }		
	}
	
	public void tearDown() {
		instance1.dispose();
		instance2.dispose();
		instance3.dispose();
		instance4.dispose();
		instance5.dispose();
		fooProvider.dispose();
		instance1 = null;
		instance2 = null;
		instance3 = null;
		instance4 = null;
		instance5 = null;
		fooProvider = null;
	}
	
	public void atestSimple() {
		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance1.getInstanceName());
		assertNotNull("Check architecture availability", arch_ref);
		InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
		
		ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
		assertNotNull("Check CheckService availability", cs_ref);
		CheckService cs = (CheckService) context.getService(cs_ref);
		Properties props = cs.getProps();
		
		//Check properties
		assertFalse("check CheckService invocation - 1", ((Boolean)props.get("result")).booleanValue()); // False is returned (nullable)
		assertEquals("check void bind invocation - 1", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation - 1", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation - 1", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation - 1", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation - 1", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation - 1", ((Integer)props.get("refU")).intValue(), 0);
		assertNull("Check FS invocation (object) - 1", props.get("object"));
		assertEquals("Check FS invocation (int) - 1", ((Integer)props.get("int")).intValue(), 0);
		assertEquals("Check FS invocation (long) - 1", ((Long)props.get("long")).longValue(), 0);
		assertEquals("Check FS invocation (double) - 1", ((Double)props.get("double")).doubleValue(), 0.0);
		
		fooProvider.start();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);
		
		assertNotNull("Check CheckService availability", cs_ref);
		cs = (CheckService) context.getService(cs_ref);
		props = cs.getProps();
		
		//Check properties
		assertFalse("check CheckService invocation - 2", ((Boolean)props.get("result")).booleanValue()); // False, the provider was not bound
		assertEquals("check void bind invocation - 2", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation - 2", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation - 2", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation - 2", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation - 2", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation - 2", ((Integer)props.get("refU")).intValue(), 0);
		assertNull("Check FS invocation (object) - 2", props.get("object"));
		assertEquals("Check FS invocation (int) - 2", ((Integer)props.get("int")).intValue(), 0);
		assertEquals("Check FS invocation (long) - 2", ((Long)props.get("long")).longValue(), 0);
		assertEquals("Check FS invocation (double) - 2", ((Double)props.get("double")).doubleValue(), 0.0);
		
		fooProvider.stop();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
		
		id = null;
		cs = null;
		context.ungetService(arch_ref);
		context.ungetService(cs_ref);		
	}
	
	public void atestDelayedSimple() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
	    instance1.stop();
	    fooProvider.start();
	    instance1.start();
        
	    ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance1.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) context.getService(cs_ref);
        Properties props = cs.getProps();
        
        //Check properties
        assertTrue("check CheckService invocation - 2", ((Boolean)props.get("result")).booleanValue()); // True, the provider was bound
        assertEquals("check void bind invocation - 2", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 2", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 2", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 2", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 2", ((Integer)props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 2", ((Integer)props.get("refU")).intValue(), 0);
        assertNotNull("Check FS invocation (object) - 2", props.get("object"));
        assertEquals("Check FS invocation (int) - 2", ((Integer)props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 2", ((Long)props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 2", ((Double)props.get("double")).doubleValue(), 1.0);
        
        fooProvider.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.INVALID); // Dependency broken
        
        id = null;
        cs = null;
        context.ungetService(arch_ref);
        context.ungetService(cs_ref);       
    }
	
	public void atestVoid() {
		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance2.getInstanceName());
		assertNotNull("Check architecture availability", arch_ref);
		InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
		
		ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance2.getInstanceName());
		assertNotNull("Check CheckService availability", cs_ref);
		CheckService cs = (CheckService) context.getService(cs_ref);
		Properties props = cs.getProps();
		//Check properties
		assertFalse("check CheckService invocation - 1", ((Boolean)props.get("result")).booleanValue()); // False is returned (nullable)
		assertEquals("check void bind invocation - 1", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation - 1", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation - 1", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation - 1", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation - 1", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation - 1", ((Integer)props.get("refU")).intValue(), 0);
		assertNull("Check FS invocation (object) - 1", props.get("object"));
		assertEquals("Check FS invocation (int) - 1", ((Integer)props.get("int")).intValue(), 0);
		assertEquals("Check FS invocation (long) - 1", ((Long)props.get("long")).longValue(), 0);
		assertEquals("Check FS invocation (double) - 1", ((Double)props.get("double")).doubleValue(), 0.0);
		
		fooProvider.start();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);
		
		assertNotNull("Check CheckService availability", cs_ref);
		cs = (CheckService) context.getService(cs_ref);
		props = cs.getProps();
		//Check properties
		assertFalse("check CheckService invocation -2", ((Boolean)props.get("result")).booleanValue());
		assertEquals("check void bind invocation -2", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation -2", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation -2", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation -2", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation -2", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation -2", ((Integer)props.get("refU")).intValue(), 0);
		assertNull("Check FS invocation (object) - 2", props.get("object"));
		assertEquals("Check FS invocation (int) - 2", ((Integer)props.get("int")).intValue(), 0);
		assertEquals("Check FS invocation (long) - 2", ((Long)props.get("long")).longValue(), 0);
		assertEquals("Check FS invocation (double) - 2", ((Double)props.get("double")).doubleValue(), 0.0);
		
		fooProvider.stop();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
		
		cs = (CheckService) context.getService(cs_ref);
		props = cs.getProps();
		//Check properties
		assertFalse("check CheckService invocation -3", ((Boolean)props.get("result")).booleanValue());
		assertEquals("check void bind invocation -3", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation -3 ("+((Integer)props.get("voidU")) + ")", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation -3", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation -3", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation -3", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation -3", ((Integer)props.get("refU")).intValue(), 0);
		assertNull("Check FS invocation (object) - 3", props.get("object"));
		assertEquals("Check FS invocation (int) - 3", ((Integer)props.get("int")).intValue(), 0);
		assertEquals("Check FS invocation (long) - 3", ((Long)props.get("long")).longValue(), 0);
		assertEquals("Check FS invocation (double) - 3", ((Double)props.get("double")).doubleValue(), 0.0);
		
		id = null;
		cs = null;
		context.ungetService(arch_ref);
		context.ungetService(cs_ref);	
	}
	
	public void testDelayedVoid() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        instance2.stop();
        fooProvider.start();
        instance2.start();
        
        ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance2.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) context.getService(cs_ref);
        Properties props = cs.getProps();
        
        //Check properties
        assertTrue("check CheckService invocation - 2", ((Boolean)props.get("result")).booleanValue()); // True, the provider was bound
        assertEquals("check void bind invocation - 2", ((Integer)props.get("voidB")).intValue(), 1);
        assertEquals("check void unbind callback invocation - 2", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 2", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 2", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 2", ((Integer)props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 2", ((Integer)props.get("refU")).intValue(), 0);
        assertNotNull("Check FS invocation (object) - 2", props.get("object"));
        assertEquals("Check FS invocation (int) - 2", ((Integer)props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 2", ((Long)props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 2", ((Double)props.get("double")).doubleValue(), 1.0);
        
        fooProvider.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3 (" + id.getState() + ")", id.getState() == ComponentInstance.INVALID); // Dependency broken
        
        id = null;
        cs = null;
        context.ungetService(arch_ref);
        context.ungetService(cs_ref);       
    }
	
	public void atestObject() {
		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance3.getInstanceName());
		assertNotNull("Check architecture availability", arch_ref);
		InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
		
		ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance3.getInstanceName());
		assertNotNull("Check CheckService availability", cs_ref);
		CheckService cs = (CheckService) context.getService(cs_ref);
		Properties props = cs.getProps();
		//Check properties
		assertFalse("check CheckService invocation -1", ((Boolean)props.get("result")).booleanValue()); // False is returned (nullable)
		assertEquals("check void bind invocation -1", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation -1", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation -1", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation -1", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation -1", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation -1", ((Integer)props.get("refU")).intValue(), 0);
		
		fooProvider.start();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);
		
		assertNotNull("Check CheckService availability", cs_ref);
		cs = (CheckService) context.getService(cs_ref);
		props = cs.getProps();
		//Check properties
		assertFalse("check CheckService invocation -2", ((Boolean)props.get("result")).booleanValue());
		assertEquals("check void bind invocation -2", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation -2", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation -2 (" + ((Integer)props.get("objectB")).intValue() + ")", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation -2", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation -2", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation -2", ((Integer)props.get("refU")).intValue(), 0);
		
		fooProvider.stop();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
		
		cs = (CheckService) context.getService(cs_ref);
		props = cs.getProps();
		//Check properties
		assertFalse("check CheckService invocation -3", ((Boolean)props.get("result")).booleanValue());
		assertEquals("check void bind invocation -3", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation -3", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation -3", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation -3", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation -3", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation -3", ((Integer)props.get("refU")).intValue(), 0);
		
		id = null;
		cs = null;
		context.ungetService(arch_ref);
		context.ungetService(cs_ref);		
	}
	
	public void atestDelayedObject() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        instance3.stop();
        fooProvider.start();
        instance3.start();
        
        ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance3.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) context.getService(cs_ref);
        Properties props = cs.getProps();
        
        //Check properties
        assertTrue("check CheckService invocation - 2", ((Boolean)props.get("result")).booleanValue()); // True, the provider was bound
        assertEquals("check void bind invocation - 2", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 2", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 2", ((Integer)props.get("objectB")).intValue(), 1);
        assertEquals("check object unbind callback invocation - 2", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 2", ((Integer)props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 2", ((Integer)props.get("refU")).intValue(), 0);
        assertNotNull("Check FS invocation (object) - 2", props.get("object"));
        assertEquals("Check FS invocation (int) - 2", ((Integer)props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 2", ((Long)props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 2", ((Double)props.get("double")).doubleValue(), 1.0);
        
        fooProvider.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.INVALID); // Dependency broken
        
        id = null;
        cs = null;
        context.ungetService(arch_ref);
        context.ungetService(cs_ref);       
    }
	
	public void atestRef() {
		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance4.getInstanceName());
		assertNotNull("Check architecture availability", arch_ref);
		InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
		
		ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance4.getInstanceName());
		assertNotNull("Check CheckService availability", cs_ref);
		CheckService cs = (CheckService) context.getService(cs_ref);
		Properties props = cs.getProps();
		//Check properties
		assertFalse("check CheckService invocation -1", ((Boolean)props.get("result")).booleanValue()); // False is returned (nullable)
		assertEquals("check void bind invocation -1", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation -1", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation -1", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation -1", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation -1", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation -1", ((Integer)props.get("refU")).intValue(), 0);
		
		fooProvider.start();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);
		
		assertNotNull("Check CheckService availability", cs_ref);
		cs = (CheckService) context.getService(cs_ref);
		props = cs.getProps();
		//Check properties
		assertFalse("check CheckService invocation -2", ((Boolean)props.get("result")).booleanValue());
		assertEquals("check void bind invocation -2", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation -2", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation -2", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation -2", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation -2", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation -2", ((Integer)props.get("refU")).intValue(), 0);
		
		fooProvider.stop();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
		
		cs = (CheckService) context.getService(cs_ref);
		props = cs.getProps();
		//Check properties
		assertFalse("check CheckService invocation -3", ((Boolean)props.get("result")).booleanValue());
		assertEquals("check void bind invocation -3", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation -3", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation -3", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation -3", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation -3", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation -3", ((Integer)props.get("refU")).intValue(), 0);
		
		id = null;
		cs = null;
		context.ungetService(arch_ref);
		context.ungetService(cs_ref);
	}
	
	public void atestBoth() {
        ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance5.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance5.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) context.getService(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertFalse("check CheckService invocation -1", ((Boolean)props.get("result")).booleanValue()); // False is returned (nullable)
        assertEquals("check void bind invocation -1", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation -1", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -1", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation -1", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation -1", ((Integer)props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -1", ((Integer)props.get("bothU")).intValue(), 0);
        
        fooProvider.start();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);
        
        assertNotNull("Check CheckService availability", cs_ref);
        cs = (CheckService) context.getService(cs_ref);
        props = cs.getProps();
        //Check properties
        assertFalse("check CheckService invocation -2", ((Boolean)props.get("result")).booleanValue());
        assertEquals("check void bind invocation -2", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation -2", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -2", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation -2", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation -2", ((Integer)props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation -2", ((Integer)props.get("refU")).intValue(), 0);
        assertEquals("check both bind callback invocation -2", ((Integer)props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -2", ((Integer)props.get("bothU")).intValue(), 0);
        
        fooProvider.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3 (" + id.getState() + ")", id.getState() == ComponentInstance.VALID);
        
        cs = (CheckService) context.getService(cs_ref);
        props = cs.getProps();
        //Check properties
        assertFalse("check CheckService invocation -3", ((Boolean)props.get("result")).booleanValue());
        assertEquals("check void bind invocation -3", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation -3", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -3", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation -3", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation -3", ((Integer)props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation -3", ((Integer)props.get("refU")).intValue(), 0);
        assertEquals("check both bind callback invocation -3", ((Integer)props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -3", ((Integer)props.get("bothU")).intValue(), 0);
  
        id = null;
        cs = null;
        context.ungetService(arch_ref);
        context.ungetService(cs_ref);
    }


}
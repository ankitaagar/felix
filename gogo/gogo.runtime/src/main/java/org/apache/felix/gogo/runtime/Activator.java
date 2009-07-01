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
package org.apache.felix.gogo.runtime;

import org.apache.felix.gogo.runtime.lang.Support;
import org.apache.felix.gogo.runtime.osgi.OSGiShell;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.Converter;
import org.osgi.service.threadio.ThreadIO;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Hashtable;

public class Activator implements BundleActivator
{

    private OSGiShell shell;
    private ThreadIOImpl threadio;
    private ServiceRegistration shellRegistration;
    private ServiceRegistration threadioRegistration;
    private ServiceTracker converterTracker;
    private ServiceTracker commandTracker;

    public void start(final BundleContext context) throws Exception
    {
        Hashtable props = new Hashtable();
        props.put("osgi.command.scope", "log");
        props.put("osgi.command.function", "display");

        threadio = new ThreadIOImpl();
        threadio.start();
        shell = new OSGiShell();
        shell.setBundle(context.getBundle());
        shell.setThreadio(threadio);
        shell.setConverter(new Support());
        shell.start();
        converterTracker = new ServiceTracker(context, Converter.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference)
            {
                Converter converter = (Converter) super.addingService(reference);
                shell.setConverter(converter);
                return converter;
            }

            @Override
            public void removedService(ServiceReference reference, Object service)
            {
                shell.unsetConverter((Converter) service);
                super.removedService(reference, service);
            }
        };
        converterTracker.open();
        commandTracker = new ServiceTracker(context, context.createFilter("(&(osgi.command.scope=*)(osgi.command.function=*))"), null) {
            @Override
            public Object addingService(ServiceReference reference)
            {
                Object scope = reference.getProperty("osgi.command.scope");
                Object function = reference.getProperty("osgi.command.function");
                if(scope != null && function != null)
                {
                    Object target = context.getService(reference);
                    if (function.getClass().isArray())
                    {
                        for (Object f : ((Object[]) function))
                        {
                            shell.addCommand(scope.toString(), target, f.toString());
                        }
                    }
                    else
                    {
                        shell.addCommand(scope.toString(), target, function.toString());
                    }
                    return target;
                }
                return null;
            }

            @Override
            public void removedService(ServiceReference reference, Object service)
            {
                super.removedService(reference, service);
            }
        };
        commandTracker.open();
        threadioRegistration = context.registerService(ThreadIO.class.getName(), threadio, new Hashtable());
        shellRegistration = context.registerService(CommandProcessor.class.getName(), shell, new Hashtable());
    }

    private String getProperty(BundleContext context, String name, String def) {
        String v = context.getProperty(name);
        if (v == null) {
            v = def;
        }
        return v;
    }

    public void stop(BundleContext context) throws Exception
    {
        shellRegistration.unregister();
        threadioRegistration.unregister();
        threadio.stop();
        converterTracker.close();
        commandTracker.close();
    }
}
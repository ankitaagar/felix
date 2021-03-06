package org.apache.felix.ipojo.transaction.test;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.with;

import java.io.File;
import java.net.URL;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.tinybundles.BundleAsiPOJO;
import org.apache.felix.ipojo.transaction.test.component.ComponentUsingAnnotations;
import org.apache.felix.ipojo.transaction.test.component.FooImpl;
import org.apache.felix.ipojo.transaction.test.service.CheckService;
import org.apache.felix.ipojo.transaction.test.service.Foo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

@RunWith( JUnit4TestRunner.class )
public class TestAnnotations {

    @Inject
    private BundleContext context;

    private OSGiHelper osgi;

    private IPOJOHelper ipojo;

    public static final File ROOT = new File("target/tmp");
    public static final File TEST = new File("src/test/resources");


    @Before
    public void init() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);
    }

    @After
    public void stop() {
        ipojo.dispose();
        osgi.dispose();
    }

    @Configuration
    public static Option[] configure() {
        ROOT.mkdirs();

        URL service = TinyBundles.newBundle()
            .addClass(CheckService.class)
            .addClass(Foo.class)
           .prepare(
                with()
                .set(Constants.BUNDLE_SYMBOLICNAME,"Service")
                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.transaction.test.service")
                )
            .build( TinyBundles.asURL());

        String fooimpl = TinyBundles.newBundle()
            .addClass(FooImpl.class)
            .prepare(
                    with()
                    .set(Constants.BUNDLE_SYMBOLICNAME,"Foo Provider")
                    .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.transaction.test.service")
                )
                .build( new BundleAsiPOJO(new File(ROOT, "FooImpl.jar"), new File(TEST, "foo.xml"))  ).toExternalForm();

        String test = TinyBundles.newBundle()
        .addClass(ComponentUsingAnnotations.class)
        .prepare(
                with()
                .set(Constants.BUNDLE_SYMBOLICNAME,"Transaction Annotation Test")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.transaction.test.service, javax.transaction")
            )
            .build( new BundleAsiPOJO(new File(ROOT, "annotations.jar"), new File(TEST, "annotation.xml"))  ).toExternalForm();


        Option[] opt =  options(

                provision(
                        mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-api").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.handler.transaction").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.transaction").version(asInProject()),
                        mavenBundle()
                            .groupId( "org.ops4j.pax.swissbox" )
                            .artifactId( "pax-swissbox-tinybundles" )
                            .version(asInProject()),
                        bundle(service.toExternalForm()),
                        bundle(fooimpl),
                        bundle(test)
                    )
                )

                ;
        return opt;
    }

    @Test
    public void annotations() {
        Element elem = IPOJOHelper.getMetadata(getBundle(), "org.apache.felix.ipojo.transaction.test.component.ComponentUsingAnnotations");
        Assert.assertNotNull(elem);

        Element tr = elem.getElements("transaction", "org.apache.felix.ipojo.transaction")[0];
        Assert.assertEquals("transaction", tr.getAttribute("field"));

        Assert.assertNull(tr.getAttribute("oncommit"));
        Assert.assertNull(tr.getAttribute("onrollback"));

        Element[] methods = tr.getElements();
        Assert.assertEquals(4, methods.length);

        Element m1 = getElementByMethod(methods, "doSomethingBad");
        Assert.assertNotNull(m1);

        Element m2 = getElementByMethod(methods, "doSomethingBad2");
        Assert.assertNotNull(m2);
        Assert.assertEquals("required", m2.getAttribute("propagation"));

        Element m3 = getElementByMethod(methods, "doSomethingGood");
        Assert.assertNotNull(m3);
        Assert.assertEquals("supported", m3.getAttribute("propagation"));
        Assert.assertEquals("{java.lang.Exception}", m3.getAttribute("norollbackfor"));

        Element m4 = getElementByMethod(methods, "doSomethingLong");
        Assert.assertNotNull(m4);
        Assert.assertEquals("1000", m4.getAttribute("timeout"));
        Assert.assertEquals("true", m4.getAttribute("exceptiononrollback"));
    }

    private Element getElementByMethod(Element[] e, String m) {
        for(Element elem : e) {
            if(m.equals(elem.getAttribute("method"))) {
                return elem;
            }
        }
        Assert.fail("Method " + m + " not found");
        return null;
    }

    private Bundle getBundle() {
        for(Bundle b : context.getBundles()) {
           if ("Transaction Annotation Test".equals(b.getSymbolicName())) {
               return b;
           }
        }
        Assert.fail("Cannot find the tested bundle");
        return null;
    }


}

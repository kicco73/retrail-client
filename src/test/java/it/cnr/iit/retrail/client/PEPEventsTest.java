package it.cnr.iit.retrail.client;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.commons.PepSession;
import java.io.IOException;
import java.net.URL;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author oneadmin
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PEPEventsTest {

    static final Logger log = LoggerFactory.getLogger(PEPEventsTest.class);
    static PEP pep = null;
    static PepAccessRequest pepRequest = null;
    static PepSession pepSession1 = null;
    static PepSession pepSession2 = null;

    public PEPEventsTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        log.warn("Setting up environment...");
        try {
            URL pdpUrl = new URL("http://localhost:8080");
            URL myUrl = new URL("http://localhost:8081");
            pep = new PEP(pdpUrl, myUrl);
            // clean up previous sessions, if any, by clearing the recoverable
            // access flag. This ensures the next heartbeat we'll have a clean
            // ucon status (the first heartbeat is waited by init()).
            pep.setAccessRecoverableByDefault(false);
            pep.init();
        } catch (XmlRpcException | IOException e) {
            fail("unexpected exception: " + e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        try {
            pepRequest = PepAccessRequest.newInstance(
                    "fedoraRole",
                    "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                    " ",
                    "issuer");
            PepRequestAttribute attribute = new PepRequestAttribute(
                    "urn:fedora:names:fedora:2.1:resource:datastream:id",
                    PepRequestAttribute.DATATYPES.STRING,
                    "FOPDISSEM",
                    "issuer",
                    PepRequestAttribute.CATEGORIES.RESOURCE);
            pepRequest.add(attribute);
        } catch (Exception e) {
            fail("unexpected exception: " + e.getMessage());
        }
    }

    @After
    public void tearDown() throws Exception {
        for(PepSession s: pep.sessions.values())
            pep.endAccess(s);
    }
    

    /**
     * Test of onRecoverAccess method, of class PEP.
     * @throws java.lang.Exception
     */
    @Test
    public void testOnRecoverAccess() throws Exception {
        System.out.println("onRecoverAccess");
        PepSession session = null;
        PEP instance = null;
        instance.onRecoverAccess(session);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of shouldRecoverAccess method, of class PEP.
     */
    @Test
    public void testShouldRecoverAccess() {
        System.out.println("shouldRecoverAccess");
        PepSession session = null;
        PEP instance = null;
        boolean expResult = false;
        boolean result = instance.shouldRecoverAccess(session);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of onRevokeAccess method, of class PEP.
     * @throws java.lang.Exception
     */
    @Test
    public void testOnRevokeAccess() throws Exception {
        System.out.println("onRevokeAccess");
        PepSession session = null;
        PEP instance = null;
        instance.onRevokeAccess(session);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}

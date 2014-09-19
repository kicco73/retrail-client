/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.client;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
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
public class PEPNegativeTest {
    static final String pdpUrlString = "http://localhost:8080";
    static final Logger log = LoggerFactory.getLogger(PEPNegativeTest.class);
    static PEP pep = null;
    PepAccessRequest pepRequest = null;

    public PEPNegativeTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        log.warn("Setting up environment...");
        try {
            URL pdpUrl = new URL(pdpUrlString);
            URL myUrl = new URL("http://localhost:8081");
            pep = new PEP(pdpUrl, myUrl);
            // clean up previous sessions, if any, by clearing the recoverable
            // access flag. This ensures the next heartbeat we'll have a clean
            // ucon status (the first heartbeat is waited by init()).
            pep.setAccessRecoverableByDefault(false);
            pep.init();        // We should have no sessions now
        } catch (XmlRpcException | IOException e) {
            fail("unexpected exception: " + e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        for (PepSession s : pep.sessions.values()) {
            pep.endAccess(s);
        }
        pep.term();
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
    }

       private void beforeTryAccess() {
        assertEquals(0, pep.sessions.size());        
    }
    
    private PepSession afterTryAccess(PepSession pepSession) throws Exception {
        assertTrue(pep.hasSession(pepSession));
        assertEquals(1, pep.sessions.size());
        assertEquals(PepSession.Status.TRY, pepSession.getStatus());
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
        assertEquals(pdpUrlString, pepSession.getUconUrl().toString());
        return pepSession;
    }
    
    private void beforeStartAccess(PepSession pepSession) throws Exception {
        assertEquals(1, pep.sessions.size());
        assertEquals(PepSession.Status.TRY, pepSession.getStatus());
    }

    private void afterStartAccess(PepSession pepSession) throws Exception {
        assertEquals(1, pep.sessions.size());
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
        assertEquals(PepSession.Status.ONGOING, pepSession.getStatus());
    }

    private void beforeEndAccess(PepSession pepSession) throws Exception {
        assertEquals(1, pep.sessions.size());
        assertNotEquals(PepSession.Status.DELETED, pepSession.getStatus());
        assertNotEquals(PepSession.Status.UNKNOWN, pepSession.getStatus());
        assertNotEquals(PepSession.Status.REVOKED, pepSession.getStatus());
        assertTrue(pep.hasSession(pepSession));
    }
    
    private void afterEndAccess(PepSession response) throws Exception {
        assertFalse(pep.hasSession(response));
        assertEquals(0, pep.sessions.size());
        assertEquals(pdpUrlString, response.getUconUrl().toString());
        assertEquals(PepSession.Status.DELETED, response.getStatus());
    }
    
    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test1_TryWithNull() throws Exception {
        log.info("start");
        assertEquals(0, pep.sessions.size());
        try {
            PepSession pepSession = pep.tryAccess(null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(NullPointerException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }

    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test2_StartNoTry() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = pep.startAccess("unexistentUuid", null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }
       /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test2_StartNoTryWithNull() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = pep.startAccess(null, null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }
    
    @Test
    public void test2_StartNoTryWithUuidEmpty() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = pep.startAccess("", null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }
    
    @Test
    public void test2_StartNoTryWithCustomId() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = pep.startAccess(null, "unexistentCustomId");
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }

    @Test
    public void test2_StartNoTryWithCustomIdEmpty() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = pep.startAccess(null, "");
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }
    
    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test3_EndNoTry() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = pep.endAccess("unexistentUuid", null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }
    
    @Test
    public void test3_EndNoTryEmpty() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = pep.endAccess("", null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }
   
    @Test
    public void test3_EndNoTryCustomId() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = pep.endAccess(null, "badCustomId");
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }
    @Test
    public void test3_EndNoTryCustomIdEmpty() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = pep.endAccess(null, "");
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }
    
    @Test
    public void test3_EndTryWithNull() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = pep.endAccess(null, null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }

    /**
     * Test of assignCustomId method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test4_AssignNoTry() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = pep.assignCustomId("unexistentUuid", null, null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }

    
    /**
     * Test of assignCustomId method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test4_TryAssignWithNullId() throws Exception {
        log.info("start");
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(1, pep.sessions.size());
        try {
            PepSession assignResponse = pep.assignCustomId(pepSession.getUuid(), null, null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  assignResponse);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        } finally {
            pep.endAccess(pepSession);
        }
        assertEquals(0, pep.sessions.size());
        log.info("end");
    }

    
    /**
     * Test of assignCustomId method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test5_EndTwice() throws Exception {
        log.info("start");
        PepSession pepSession = pep.tryAccess(pepRequest, "ziopino2");
        pep.assignCustomId(null, pepSession.getCustomId(), "ziopino");
        assertEquals("ziopino", pepSession.getCustomId());
        assertTrue(pep.hasSession(pepSession));
        pep.endAccess(null, pepSession.getCustomId());
        assertFalse(pep.hasSession(pepSession));
        log.info("end");
    }
    /**
     * Test of assignCustomId method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test5_StartTwice() throws Exception {
        log.info("start");
        PepSession pepSession = pep.tryAccess(pepRequest);
        PepSession startResponse = pep.startAccess(pepSession);
        try {
            PepSession startResponse2 = pep.startAccess(pepSession);
            fail("Must throw XmlRcpException, got instead " +  startResponse2);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        pep.endAccess(startResponse);
        assertFalse(pep.hasSession(pepSession));
        log.info("end");
    }

    /**
     * Test of startAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test6_AssignWithNoIds() throws Exception {
        log.info("start");
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
        assertEquals(PepSession.Status.TRY, pepSession.getStatus());
        PepSession response = pep.startAccess(pepSession);
        log.info("response {}", response);
        assertEquals(PepAccessResponse.DecisionEnum.Permit, response.decision);
        assertEquals(PepSession.Status.ONGOING, response.getStatus());
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
        assertEquals(PepSession.Status.ONGOING, pepSession.getStatus());
        response = pep.endAccess(response);
        assertEquals(PepSession.Status.DELETED, response.getStatus());
        assertEquals(PepSession.Status.DELETED, pepSession.getStatus());
        log.info("end");
    }

}

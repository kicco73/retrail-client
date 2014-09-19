/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.client;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.commons.PepSession;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.beanutils.BeanUtils;
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
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PEPPositiveTest {
    static final String pdpUrlString = "http://localhost:8080";
    static final Logger log = LoggerFactory.getLogger(PEPPositiveTest.class);
    static PEP pep = null;
    PepAccessRequest pepRequest = null;

    public PEPPositiveTest() {
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
     * Test of hasSession method, of class PEP.
     * @throws java.io.IOException
     */
    @Test
    public void test1_init() throws IOException {
        log.info("Check if the server made us recover some local sessions");
        assertEquals(0, pep.sessions.size());
        log.info("Ok, no recovered sessions");
    }

    /**
     * Test of echo method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test2_Echo() throws Exception {
        log.info("checking if the server is up and running");
        String echoTest = "<echoTest/>";
        Node node = DomUtils.read(echoTest);
        Node result = pep.echo(node);
        assertEquals(DomUtils.toString(node), DomUtils.toString(result));
        log.info("server echo ok");
    }

    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test3_TryEndCycle() throws Exception {
        log.info("performing a tryAccess-EndAccess short cycle");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        beforeEndAccess(pepSession);
        PepSession pepResponse = pep.endAccess(pepSession);
        afterEndAccess(pepResponse);
        afterEndAccess(pepSession);
        log.info("short cycle ok");
    }

    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test3_TryEndCycleAccessWithCustomId() throws Exception {
        log.info("TryAccessWithCustomId");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest, "ziopino");
        afterTryAccess(pepSession);
        assertEquals("ziopino", pepSession.getCustomId());
        beforeEndAccess(pepSession);
        PepSession response = pep.endAccess(null, pepSession.getCustomId());
        afterEndAccess(response);
        afterEndAccess(pepSession);
    }

    /**
     * Test of assignCustomId method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test4_AssignCustomIdByUuid() throws Exception {
        log.info("AssignCustomIdByUuid");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest, "ziopino");
        afterTryAccess(pepSession);
        PepSession assignResponse = pep.assignCustomId(pepSession.getUuid(), null, "ziopino2");
        assertEquals(pdpUrlString, assignResponse.getUconUrl().toString());
        assertEquals("ziopino2", assignResponse.getCustomId());
        assertEquals(pdpUrlString, pepSession.getUconUrl().toString());
        assertEquals("ziopino2", pepSession.getCustomId());
        afterTryAccess(assignResponse);
        afterTryAccess(pepSession);
        beforeEndAccess(pepSession);
        beforeEndAccess(assignResponse);
        PepSession response = pep.endAccess(assignResponse.getUuid(), null);
        afterEndAccess(response);
        afterEndAccess(assignResponse);
        afterEndAccess(pepSession);
    }

    /**
     * Test of assignCustomId method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test4_AssignCustomIdByCustomId() throws Exception {
        log.info("AssignCustomIdByCustomId");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest, "ziopino2");
        afterTryAccess(pepSession);
        PepSession assignResponse = pep.assignCustomId(null, pepSession.getCustomId(), "ziopino");
        afterTryAccess(assignResponse);
        afterTryAccess(pepSession);
        assertEquals("ziopino", pepSession.getCustomId());
        assertTrue(pep.hasSession(pepSession));
        assertEquals("ziopino", assignResponse.getCustomId());
        assertTrue(pep.hasSession(assignResponse));
        beforeEndAccess(pepSession);
        beforeEndAccess(assignResponse);
        PepSession response = pep.endAccess(null, pepSession.getCustomId());
        afterEndAccess(response);
        afterEndAccess(assignResponse);
        afterEndAccess(pepSession);
    }

    /**
     * Test of startAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test5_TryStartEndCycle() throws Exception {
        log.info("testing try - start - end cycle");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        beforeStartAccess(pepSession);
        PepSession startResponse = pep.startAccess(pepSession);
        afterStartAccess(startResponse);
        afterStartAccess(pepSession);
        beforeEndAccess(startResponse);
        beforeEndAccess(pepSession);
        PepSession endResponse = pep.endAccess(startResponse);
        afterEndAccess(endResponse);
        afterEndAccess(startResponse);
        afterEndAccess(pepSession);
        log.info("ok");
    }
    

    /**
     * Test of startAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test5_TryStartEndCycleWithUuid() throws Exception {
        log.info("testing try - start - end cycle");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        beforeStartAccess(pepSession);
        PepSession startResponse = pep.startAccess(pepSession.getUuid(), null);
        afterStartAccess(pepSession);
        afterStartAccess(pepSession);
        beforeEndAccess(startResponse);
        beforeEndAccess(pepSession);
        PepSession endResponse = pep.endAccess(pepSession.getUuid(), null);
        afterEndAccess(endResponse);
        afterEndAccess(startResponse);
        afterEndAccess(pepSession);
        log.info("ok");
    }
    
    /**
     * Test of startAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test5_TryStartEndCycleWithCustomId() throws Exception {
        log.info("testing try - start - end cycle");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        beforeStartAccess(pepSession);
        PepSession startResponse = pep.startAccess(null, pepSession.getCustomId());
        afterStartAccess(startResponse);
        afterStartAccess(pepSession);
        beforeEndAccess(startResponse);
        beforeEndAccess(pepSession);
        PepSession endResponse = pep.endAccess(null, pepSession.getCustomId());
        afterEndAccess(endResponse);
        afterEndAccess(startResponse);
        afterEndAccess(pepSession);
        log.info("ok");
    }
    
}

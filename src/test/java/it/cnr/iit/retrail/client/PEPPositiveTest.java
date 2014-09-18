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

    static final Logger log = LoggerFactory.getLogger(PEPPositiveTest.class);
    static PEP pep = null;
    PepAccessRequest pepRequest = null;

    public PEPPositiveTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        log.warn("Setting up environment...");
        try {
            URL pdpUrl = new URL("http://localhost:8080");
            URL myUrl = new URL("http://localhost:8081");
            pep = new PEP(pdpUrl, myUrl);
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
     * Test of hasSession method, of class PEP.
     */
    @Test
    public void test1_init() throws IOException {
        log.info("Check if the server made us recover some local sessions");
        // clean up previous sessions, if any, by clearing the recoverable
        // access flag. This ensures the next heartbeat we'll have a clean
        // ucon status (the first heartbeat is waited by init()).
        pep.setAccessRecoverableByDefault(false);
        pep.init();        // We should have no sessions now
        assertEquals(0, pep.sessions.size());
        log.info("Ok, no recovered sessions");
    }
    
    /**
     * Test of echo method, of class PEP.
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
     * @throws java.lang.Exception
     */
    @Test
    public void test3_TryEndCycle() throws Exception {
        log.info("performing a tryAccess-EndAccess short cycle");
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
        assertTrue(pep.hasSession(pepSession));
        pep.endAccess(pepSession);
        assertFalse(pep.hasSession(pepSession));
        log.info("short cycle ok");
    }
    
    /**
     * Test of tryAccess method, of class PEP.
     * @throws java.lang.Exception
     */
    @Test
    public void test3_TryEndCycleAccessWithCustomId() throws Exception {
        log.info("TryAccessWithCustomId");
        PepSession pepSession = pep.tryAccess(pepRequest, "ziopino");
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
        assertEquals("ziopino", pepSession.getCustomId()); 
        assertTrue(pep.hasSession(pepSession));
        pep.endAccess(null, pepSession.getCustomId());
        assertFalse(pep.hasSession(pepSession));
    }
    
        /**
     * Test of assignCustomId method, of class PEP.
     * @throws java.lang.Exception
     */
    @Test
    public void test4_AssignCustomIdByUuid() throws Exception {
        log.info("AssignCustomIdByUuid");
        PepSession pepSession = pep.tryAccess(pepRequest, "ziopino");
        pep.assignCustomId(pepSession.getUuid(), null, "ziopino2");
        assertEquals("ziopino2", pepSession.getCustomId());
        assertTrue(pep.hasSession(pepSession));
        pep.endAccess(null, pepSession.getCustomId());
        assertFalse(pep.hasSession(pepSession));
    }

    /**
     * Test of assignCustomId method, of class PEP.
     * @throws java.lang.Exception
     */
    @Test
    public void test4_AssignCustomIdByCustomId() throws Exception {
        log.info("AssignCustomIdByCustomId");
        PepSession pepSession = pep.tryAccess(pepRequest, "ziopino2");
        pep.assignCustomId(null, pepSession.getCustomId(), "ziopino");
        assertEquals("ziopino", pepSession.getCustomId());
        assertTrue(pep.hasSession(pepSession));
        pep.endAccess(null, pepSession.getCustomId());
        assertFalse(pep.hasSession(pepSession));
    }

    /**
     * Test of startAccess method, of class PEP.
     * @throws java.lang.Exception
     */
    @Test
    public void test5_TryStartEndCycle() throws Exception {
        log.info("testing try - start - end cycle");
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
        log.info("ok");
    }

}

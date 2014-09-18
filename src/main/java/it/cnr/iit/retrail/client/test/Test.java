/*
 */
package it.cnr.iit.retrail.client.test;

import it.cnr.iit.retrail.client.PEP;
import it.cnr.iit.retrail.client.PEPInterface;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.commons.PepSession;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Test {
    protected static final Logger log = LoggerFactory.getLogger(Test.class);
    public static void main(String[] args) throws Exception {
        URL pdpUrl = new URL("http://localhost:8080");
        URL myUrl = new URL("http://localhost:8081");
        PEPInterface pep = new PEP(pdpUrl, myUrl);
        pep.init();
        PepAccessRequest accessRequest = PepAccessRequest.newInstance(
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
        accessRequest.add(attribute);
        
        boolean result;
        
        PepSession session1 = pep.tryAccess(accessRequest);
        log.info("tryAccess 1: {}", session1);
        Thread.sleep(3000);
        
        if(session1.decision == PepAccessResponse.DecisionEnum.Permit) {
            PepSession response = pep.startAccess(session1);
            log.info("startAccess 1: {}", response);
            log.info("startAccess 1: {}", session1);
        }
        /*
        PepSession session2 = pep.tryAccess(accessRequest);
        log.info("tryAccess: {}", session2);
        if(session2.decision == PepAccessResponse.DecisionEnum.Permit) {
            pep.startAccess(session2);
            log.info("startAccess 2: {}", session2);
        } 
                */
    }
}

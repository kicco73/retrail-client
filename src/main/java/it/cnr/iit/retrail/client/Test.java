/*
 */
package it.cnr.iit.retrail.client;

import it.cnr.iit.retrail.commons.PepAccessRequest;
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
        PEP client = new PEP(pdpUrl, myUrl);
        client.init();
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
        
        //result = client.tryAccess(accessRequest);
        //System.out.println("tryAccess = " + result);

        PepSession session1 = client.startAccess(accessRequest);
        log.info("startAccess = " + session1);
        //PepSession session2 = client.startAccess(accessRequest);
        //System.out.println("startAccess = " + session2 + ", sessionId = "+session2.getId());
        //System.out.println("endAccess = " +session1.getId());
        //client.endAccess(session1);
        //System.out.println("endAccess = " +session2.getId());
        //client.endAccess(session2);
    }
}

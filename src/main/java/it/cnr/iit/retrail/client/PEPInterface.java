/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.client;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepSession;
import java.io.IOException;

/**
 *
 * @author oneadmin
 */
public interface PEPInterface {

    void init() throws IOException;

    PepSession tryAccess(PepAccessRequest req) throws Exception;
    
    PepSession startAccess(PepSession session) throws Exception;

    PepSession endAccess(PepSession session) throws Exception;

    void onRecoverAccess(PepSession session) throws Exception;

    void onRevokeAccess(PepSession session) throws Exception;
    
    boolean hasSession(PepSession session);
    
    PepSession getSession(String uuid);

    void term() throws InterruptedException;
}

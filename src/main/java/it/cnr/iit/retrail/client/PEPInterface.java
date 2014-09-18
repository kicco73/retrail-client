/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

    void term() throws InterruptedException;
}

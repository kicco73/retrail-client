/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.client;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepSession;
import java.io.IOException;
import java.util.Collection;

/**
 *
 * @author oneadmin
 */
public interface PEPInterface {

    /**
     * init
     * 
     * Performs PEP initializations and start up. It's mandatory to call this 
     * before invoking any other API of the component.
     * 
     * @throws IOException
     */
    void init() throws IOException;

    /**
     * tryAccess
     * 
     * Attempt to access a resource and gets back the UCon decision.
     * Calling this is mandatory before starting the actual access.
     * 
     * @param req the access request with subject, action, resource attributes.
     * @return the freshly opened PEP session. In case of denial, no session
     * is actually started.
     * @throws Exception if something went wrong. This has nothing to do with
     * the decisions of the UCon about the request, but it's an actual error
     * of the framework.
     */
    PepSession tryAccess(PepAccessRequest req) throws Exception;
    
    /**
     * tryAccess
     * 
     * Attempt to access a resource and gets back the UCon decision.
     * Also set 
     * Calling this is mandatory before starting the actual access.
     * @param req the access request with subject, action, resource attributes.
     * @param customId (optional) a custom unique identifier for the request
     * @return the freshly opened PEP session. In case of denial, no session
     * is actually started.
     * @throws Exception if something went wrong. This has nothing to do with
     * the decisions of the UCon about the request, but it's an actual error
     * of the framework.
     */
    PepSession tryAccess(PepAccessRequest req, String customId) throws Exception;
    
    /**
     *
     * @param session
     * @return
     * @throws Exception
     */
    PepSession startAccess(PepSession session) throws Exception;

    /**
     *
     * @param session
     * @return
     * @throws Exception
     */
    PepSession endAccess(PepSession session) throws Exception;

    /**
     *
     * @param session
     * @throws Exception
     */
    void onRecoverAccess(PepSession session) throws Exception;

    /**
     *
     * @param session
     * @throws Exception
     */
    void onRevokeAccess(PepSession session) throws Exception;
    
    /**
     *
     * @param session
     * @return
     */
    boolean hasSession(PepSession session);
    
    /**
     *
     * @param uuid
     * @return
     */
    PepSession getSession(String uuid);
    
    /**
     *
     * @return
     */
    Collection<PepSession> getSessions();
     
    /**
     *
     * @param accessRecoverableByDefault
     */
    void setAccessRecoverableByDefault(boolean accessRecoverableByDefault);
    
    /**
     *
     * @return
     */
    boolean isAccessRecoverableByDefault();
    
    /**
     *
     * @throws InterruptedException
     */
    void term() throws InterruptedException;
}

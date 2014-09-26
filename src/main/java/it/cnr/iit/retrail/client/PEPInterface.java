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
     * Attempts to access a resource and gets back the UCon decision.
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
     * startAccess
     * 
     * Declares that the resource is going to be really accessed from now on.
     * The Usage Control System may still deny the access.
     * Calling tryAccess is mandatory before starting the actual access.
     * 
     * @param session the access session returned by tryAccess.
     * @return the updated PEP session, with its own UCon decision.
     * @throws Exception if something went wrong. This has nothing to do with
     * the decisions of the UCon about the request, but it's an actual error
     * of the framework.
     */
    PepSession startAccess(PepSession session) throws Exception;

    /**
     * endAccess
     * 
     * Terminates the session opened by tryAccess.
     * This method must be always invoked by the client to declare the end of
     * resource access for any session, even when the access has been revoked 
     * by the UCon itself.
     * 
     * @param session
     * @return the updated PEP session.
     * @throws Exception if something went wrong. 
     */
    PepSession endAccess(PepSession session) throws Exception;

    /**
     * onRecoverAccess
     * 
     * Event handler invoked when the PEP discovers a server side session 
     * opened by this endpoint in a previous run. Default action is to
     * add the session to the handled sessions. Possible method overruns 
     * 
     * Usage Control System. Default implementation is calling endAccess.
     * @param session the updated PEP session.
     * @throws Exception if something went wrong. 
     */
    void onRecoverAccess(PepSession session) throws Exception;

    /**
     * onRecoverAccess
     * 
     * Event handler invoked when a revoke access has been issued by the
     * Usage Control System. Default implementation is calling endAccess.
     * 
     * @param session the updated PEP session.
     * @throws Exception if something went wrong. 
     */
    void onRevokeAccess(PepSession session) throws Exception;
    
    /**
     * hasSession
     * 
     * Tells is the given session is currently registered (i.e., ONGOING).
     * 
     * 
     * @param session the PEP session we want to know about.
     * @return true if the session is currently handled by this PEP.
     */
    boolean hasSession(PepSession session);
    
    /**
     *
     * @param uuid
     * @return the PepSession for the given session id. May be null if
     * the PEP does not handle this session.
     */
    PepSession getSession(String uuid);
    
    /**
     *
     * @return the collection of sessions handled by this PEP.
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

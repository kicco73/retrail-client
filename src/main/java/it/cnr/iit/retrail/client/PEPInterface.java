/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.client;

import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepSession;
import java.io.IOException;
import java.util.Collection;

/**
 *
 * @author oneadmin
 */
public interface PEPInterface {

    /**
     * init()
     * 
     * performs PEP initializations and start up. It's mandatory to call this 
     * before invoking any other API of the component.
     * 
     * @throws Exception if anything goes wrong.
     */
    void init() throws Exception;

    /**
     * tryAccess()
     * 
     * attempts to access a resource and gets back the UCon decision.
     * Calling this is mandatory before starting the actual access.
     * 
     * @param req the access request with subject, action, resource attributes.
     * @return the freshly opened PEP session. In case of denial, no session
     * is actually started.
     * @throws Exception if something went wrong. This has nothing to do with
     * the decisions of the UCon about the request, but it's an actual error
     * of the framework.
     */
    PepSession tryAccess(PepRequest req) throws Exception;
    
    /**
     * tryAccess()
     * 
     * attempts to access a resource and gets back the UCon decision.
     * Calling this is mandatory before starting the actual access.
     * 
     * @param req the access request with subject, action, resource attributes.
     * @param customId (optional) a custom unique identifier for the request
     * @return the freshly opened PEP session. In case of denial, no session
     * is actually started.
     * @throws Exception if something went wrong. This has nothing to do with
     * the decisions of the UCon about the request, but it's an actual error
     * of the framework.
     */
    PepSession tryAccess(PepRequest req, String customId) throws Exception;
    
    /**
     * startAccess()
     * 
     * declares that the resource is going to be really accessed from now on.
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
     * endAccess()
     * 
     * terminates the session opened by tryAccess.
     * This method must be always invoked by the client to declare the end of
     * resource access for any session, even when the access has been revoked 
     * by the UCon itself.
     * 
     * @param session the session to be ended.
     * @return the updated PEP session.
     * @throws Exception if something went wrong. 
     */
    PepSession endAccess(PepSession session) throws Exception;

    /**
     * onRecoverAccess()
     * 
     * is the default event handler invoked when the PEP discovers a server side
     * session opened by this endpoint in a previous run. The Default action is
     * to add the orphaned session to the handled sessions if the 
     * accessRecoverableByDefault property is true, or to call endAccess 
     * otherwise. It is recommended to overload this method to handle other
     * specialized actions.
     * 
     * @param session the updated PEP session.
     * @throws Exception if something went wrong. 
     */
    void onRecoverAccess(PepSession session) throws Exception;

    /**
     * onRevokeAccess()
     * 
     * is the event handler invoked when a revoke access has been issued by the
     * Usage Control System. 
     * Default implementation is calling endAccess to terminate all sessions.
     * It is recommended to overload this method to take proper customized 
     * actions.
     * 
     * @param session the updated PEP session.
     * @throws Exception if something went wrong. 
     */
    void onRevokeAccess(PepSession session) throws Exception;
    
    /**
     * hasSession()
     * 
     * tells if the given session is currently handled by this PEP.
     * 
     * @param session the PEP session we want to know about.
     * @return true if the session is currently handled by this PEP.
     */
    boolean hasSession(PepSession session);
    
    /**
     * getSession()
     * 
     * returns the session with the given uuid, if any.
     * 
     * @param uuid
     * @return the PepSession for the given session id. May be null if
     * the PEP does not handle this session.
     */
    PepSession getSession(String uuid);
    
    /**
     * getSessions()
     * 
     * returns the full collection of sessions handled by this PEP.
     * 
     * @return the collection of sessions handled by this PEP. May be empty
     * but not null.
     */
    Collection<PepSession> getSessions();
     
    /**
     * setAccessRecoverableByDefault()
     * 
     * sets the property that allows to choose if orphaned sessions are to be 
     * ended automatically, or handled manually. 
     * If the PEP is for some reason restarted, all open sessions by the
     * previous instance of the PEP in the UCon counterpart still remain open.
     * These are notified by the UCon as soon as the heartbeat with the same
     * endpoint is re-established. Once this has happened, the PEP is notified
     * of the orphaned sessions and for each of them may decide to recover it
     * or not.
     * 
     * @param accessRecoverableByDefault true if orphaned sessions are to be
     * re-established by default, false otherwise. In the latter case, endAccess
     * is called automatically. If true, it's also advisable to overload the
     * onRecoverAccess() method to handle further actions.
     */
    void setAccessRecoverableByDefault(boolean accessRecoverableByDefault);
    
    /**
     * isAccessRecoverableByDefault()
     * 
     * returns true if orphaned sessions are to be re-established by default,
     * false to terminate them. In the latter case, endAccess
     * is called automatically by the default onRecoverAccess() handler. 
     * If the PEP is for some reason restarted, all open sessions by the
     * previous instance of the PEP in the UCon counterpart still remain open.
     * These are notified by the UCon as soon as the heartbeat with the same
     * endpoint is re-established. Once this has happened, the PEP is notified
     * of the orphaned sessions and for each of them may decide to recover it
     * or not.
     * It's recommended to overload the onRecoverAccess() method to perform 
     * further customized actions. 
     * 
     * @return true if the orphaned sessions will be recovered, false otherwise.
     */
    boolean isAccessRecoverableByDefault();
    
    /**
     * term()
     * 
     * has to be called on PEP shutdown. Heartbeat will be terminated as well, 
     * and the object will be eventually disposed.
     * 
     * @throws InterruptedException if the managing thread is, for example, 
     * asked for interruption by the main program.
     */
    void term() throws InterruptedException;
}

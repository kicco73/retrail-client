/*
 */

package it.cnr.iit.retrail.client;

import org.w3c.dom.Node;

public class API {
    public void revokeAccess(Node pepSession) {
        System.out.println("*** API.revokeAccess(): CALLED!");
        // TODO: must send an event to PEPs interested in revokation
    }
}

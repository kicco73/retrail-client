/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.client;

import org.w3c.dom.Node;

public interface PEPProtocol {
    Node revokeAccess(Node pepSession) throws Exception;
    Node runObligations(Node pepSession) throws Exception;
}

/*
 */

package it.cnr.iit.retrail.client;

import org.w3c.dom.Node;

public interface API {
    Node revokeAccess(Node pepSession, String pdpUrl);
}

/*
 */

package it.cnr.iit.retrail.client;

import java.net.MalformedURLException;
import org.w3c.dom.Node;

public interface API {
    Node revokeAccess(Node pepSession, String pdpUrl) throws MalformedURLException;
}

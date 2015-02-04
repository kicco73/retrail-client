/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.client;

import java.util.List;
import org.w3c.dom.Node;

public interface PEPProtocol {

    List<Node> revokeAccess(List<Node> pepSessions) throws Exception;

    List<Node> runObligations(List<Node> pepSessions) throws Exception;
}

/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.client.impl;

import it.cnr.iit.retrail.client.PEPInterface;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepSession;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author oneadmin
 */
public class Replay implements Runnable {

    protected static final Logger log = LoggerFactory.getLogger(Replay.class);
    Document doc;
    Thread thread;
    PEPInterface pep;

    public synchronized void play(File inputFile) throws Exception {
        stop();
        doc = DomUtils.read(inputFile);
        thread = new Thread(this, "Recorder playback");
        thread.start();
    }

    public synchronized void stop() throws Exception {
        if (thread != null) {
            thread.interrupt();
            thread.wait();
            thread = null;
        }
    }

    public void run() {
        log.warn("starting playback of recorded retrail method calls");
        long millis = 0;
        NodeList nl = doc.getElementsByTagName("record");
        Map<String,String> uuidMap = new HashMap<>();
        for (int i = 0; i < nl.getLength(); i++) {
            try {
                Element record = (Element) nl.item(i);
                long ms = Long.parseLong(record.getAttribute("ms"));
                Element methodCall = (Element) record.getFirstChild();
                String methodName = methodCall.getElementsByTagName("methodName").item(0).getTextContent();
                NodeList params = methodCall.getElementsByTagName("param");
                log.info("waiting {} ms before running {}", ms-millis, methodName);
                Thread.sleep(ms-millis);
                millis = ms;                
                log.info("running: {}", methodName);
                Element methodResponse = (Element) record.getLastChild();
                Element session = (Element) methodResponse.getElementsByTagName("Session").item(0);
                switch(methodName) {
                    case "UCon.tryAccess": {
                        Element request = (Element) methodCall.getElementsByTagName("Request").item(0);
                        String customId = params.item(2).getTextContent();
                        PepRequest req = new PepRequest(request);
                        PepSession pepSession = pep.tryAccess(req, customId);
                        String uuid = session.getAttribute("uuid");
                        uuidMap.put(uuid, pepSession.getUuid());
                        break;
                    }
                    case "UCon.startAccess": {
                        String uuid = params.item(0).getTextContent();
                        String customId = params.item(1).getTextContent();
                        PepSession pepSession = pep.getSession(uuid != null? uuidMap.get(uuid) : customId);
                        pep.startAccess(pepSession);
                        break;
                    }
                    case "UCon.endAccess": {
                        String uuid = params.item(0).getTextContent();
                        String customId = params.item(1).getTextContent();
                        PepSession pepSession = pep.getSession(uuid != null? uuidMap.get(uuid) : customId);
                        pep.endAccess(pepSession);
                        break;
                    }
                    default:
                        log.info("skipped method: {}", methodName);
                }
            } catch (InterruptedException ex) {
                log.warn("interrupted");
                break;
            } catch (Exception ex) {
                log.warn("unexpected exception: {}", ex.getMessage());
            }
        }
        log.warn("terminating");
        doc = null;
    }
}

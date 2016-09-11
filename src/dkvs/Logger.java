package dkvs;

import java.util.logging.Level;

public class Logger {


    private java.util.logging.Logger log;

    public Logger(int id) {
        this.log = java.util.logging.Logger.getLogger("node." + id);
    }

    public void connection(String where, String s) {
        log.info(where + ":\n=== " + s);
    }

    public void messageOut(String where, String message) {
        log.info(where + ":\n<< " + message);
    }

    public void messageIn(String where, String message) {
        log.info(where + ":\n>> " + message);
    }

    public void paxos(String where, String s) {
        log.info(where + ":\n### " + s);
    }

    public void paxos(String s) {
        log.info("paxos" + ":\n### " + s);
    }

    public void error(String where, String s) {
        log.log(Level.INFO, where + ":\n!!! error: " + s);
    }

}

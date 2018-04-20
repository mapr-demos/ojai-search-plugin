package com.mapr.ojai.search.client;

import java.net.Socket;

public final class Utils {

    private Utils() {
    }

    public static boolean portListening(String host, int port) {
        Socket s = null;
        try {
            s = new Socket(host, port);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (s != null)
                try {
                    s.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }

}

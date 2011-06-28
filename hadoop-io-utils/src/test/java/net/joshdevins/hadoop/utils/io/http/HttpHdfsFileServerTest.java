package net.joshdevins.hadoop.utils.io.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;

import net.joshdevins.hadoop.utils.io.FileUtils;
import net.joshdevins.hadoop.utils.io.converter.FilesIntoBloomMapFile;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HttpHdfsFileServerTest {

    private static class Runner extends Thread {

        private final HttpHdfsFileServer server;

        private Runner(final HttpHdfsFileServer server) {
            this.server = server;
        }

        @Override
        public void run() {
            server.run();
        }
    }

    private static final String TEST_ROOT = "target/test/output/HttpHdfsFileServerTest";

    private HttpHdfsFileServer server;

    private Runner runner;

    private int port;

    @SuppressWarnings("deprecation")
    @After
    public void after() {
        // kill runner, this should be more graceful
        runner.stop();
    }

    @Before
    public void before() throws InterruptedException {

        // create BloomMapFiles from plain text files
        FileUtils.createDirectoryDestructive(TEST_ROOT);
        FilesIntoBloomMapFile setup = new FilesIntoBloomMapFile("src/test/resources/input", TEST_ROOT
                + "/dataset/bloom.map");
        setup.run();

        // create the server
        port = getRandomUnusedPort();
        server = new HttpHdfsFileServer(port, TEST_ROOT);

        // run the server
        runner = new Runner(server);
        runner.start();

        while (server.getJettyServer().isStarting()) {
            Thread.sleep(100);
        }

        // for good measure
        Thread.sleep(100);
    }

    public String makeHttpGetRequest(final String path) throws IOException {

        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setReadTimeout(10000);

        connection.connect();

        // get response
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line = null;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }

    @Test
    public void testSuccess() throws Exception {

        Assert.assertEquals("Contents of file 0", makeHttpGetRequest("/dataset/0.txt"));
        Assert.assertEquals("Contents of file 1", makeHttpGetRequest("/dataset/1.txt"));
    }

    public static int getRandomUnusedPort() {

        final int port;
        ServerSocket socket = null;

        try {
            socket = new ServerSocket(0);
            port = socket.getLocalPort();

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);

        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }

        return port;
    }
}

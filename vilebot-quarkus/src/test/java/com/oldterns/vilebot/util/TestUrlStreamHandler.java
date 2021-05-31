package com.oldterns.vilebot.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link URLStreamHandler} that allows us to control the {@link URLConnection URLConnections} that are returned
 * by {@link URL URLs} in the code under test.
 *
 * Based on https://claritysoftware.co.uk/mocking-javas-url-with-mockito/
 */
public class TestUrlStreamHandler extends URLStreamHandler {

    private Map<URL, URLConnection> urlToConnectionMap = new HashMap<>();

    /**
     * Should be called in the @BeforeAll of a test class,
     * replaces the URLStreamHandlerFactory with a mock
     * that returns an instance of TestUrl
     * @return An instance of TestUrlStreamHandler to be used in tests
     */
    public static TestUrlStreamHandler setup() {
        // Allows for mocking URL connections
        URLStreamHandlerFactory urlStreamHandlerFactory = mock(URLStreamHandlerFactory.class);
        URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);

        TestUrlStreamHandler out = new TestUrlStreamHandler();
        when(urlStreamHandlerFactory.createURLStreamHandler(any())).thenReturn(out);
        return out;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        if (urlToConnectionMap.containsKey(url)) {
            return urlToConnectionMap.get(url);
        } else {
            throw new IllegalStateException("An attempted access was made on url (" + url + ") but no mocked connection was supplied. Maybe add a connection using mockConnection(url, data)?");
        }
    }

    /**
     * Clears the connection map; should be done in @BeforeEach or @AfterEach
     * (unless the same connections are reused)
     */
    public void reset() {
        urlToConnectionMap.clear();
    }

    /**
     * Return urlConnection when url connection is open. The urlConnection
     * should be mocked. Ex:
     * {@code
     * URLConnection urlConnection = mock(URLConnection.class);
     * InputStream connectionInputStream = new ByteArrayInputStream("Hello".getBytes());
     * when(urlConnection.getInputStream()).thenReturn(connectionInputStream);
     * }
     * @param url The URL to set the connection of
     * @param urlConnection The mocked url connection
     * @return this for chaining
     */
    public TestUrlStreamHandler mockConnection(URL url, URLConnection urlConnection) {
        urlToConnectionMap.put(url, urlConnection);
        return this;
    }

    /**
     * Return a mocked URLConnection whose input stream contains text when url connection is open.
     * @param url The URL to set the connection of
     * @param text The text the url connection input stream should match
     * @return this for chaining
     */
    public TestUrlStreamHandler mockConnection(URL url, String text) {
        URLConnection urlConnection = mock(URLConnection.class);
        InputStream connectionInputStream = new ByteArrayInputStream(text.getBytes());
        try {
            when(urlConnection.getInputStream()).thenReturn(connectionInputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible state: exception thrown when mocking");
        }
        return mockConnection(url, urlConnection);
    }

    /**
     * Return a mocked URLConnection with the supplied input stream when url connection is open.
     * @param url The URL to set the connection of
     * @param resource The input stream the url connection should use
     * @return this for chaining
     */
    public TestUrlStreamHandler mockConnection(URL url, InputStream resource) {
        URLConnection urlConnection = mock(URLConnection.class);
        try {
            when(urlConnection.getInputStream()).thenReturn(resource);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible state: exception thrown when mocking");
        }
        return mockConnection(url, urlConnection);
    }
}
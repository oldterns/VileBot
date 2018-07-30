/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a simple, one-file, read-only, text config file reader, under the Map interface. The default file format can
 * be found in {@link ConfMap.DefaultConfParser}.
 * 
 * @see {@link ConfMap.DefaultConfParser}
 * @see {@link ConfMap.ConfParser}
 */
public class ConfMap
    extends AbstractMap<String, String>
{
    /**
     * Interface used to parse a conf file to an entry set.
     */
    public static interface ConfParser
    {
        public Set<Entry<String, String>> parse( BufferedReader reader )
            throws IOException;
    }

    /**
     * The default implementation of the ConfParser interface. Supports lines of the following format:
     * <p>
     * <code>key=value</code>
     * </p>
     * Leading and trailing whitespace on both key and value is trimmed. Keys may not contain the separator character,
     * equals ( = ). Keys must be at least 1 character long once trimmed. Values may be blank (which results in the
     * value being an empty String).
     * 
     * @see {@link ConfMap.ConfParser}
     */
    public static class DefaultConfParser
        implements ConfParser
    {
        private static final String SEP = "=";

        private static final Pattern linePattern =
            Pattern.compile( "\\s*([^" + SEP + "\\s]+)\\s*" + SEP + "\\s*(.*?)\\s*" );

        @Override
        public Set<Entry<String, String>> parse( BufferedReader reader )
            throws IOException
        {
            LinkedHashSet<Entry<String, String>> entrySet = new LinkedHashSet<Entry<String, String>>();

            String line = null;
            while ( ( line = reader.readLine() ) != null )
            {
                Matcher matcher = linePattern.matcher( line );

                if ( matcher.matches() )
                {
                    String key = matcher.group( 1 );
                    String value = matcher.group( 2 );

                    entrySet.add( new SimpleEntry<String, String>( key, value ) );
                }
            }

            return entrySet;
        }
    }

    private final Set<Entry<String, String>> entries;

    /**
     * Create a ConfMap with the default UTF-8 charset.
     * 
     * @param confFile Path to a configuration file in a valid format.
     * @throws IOException If an I/O error occurs with confFile
     */
    public ConfMap( Path confFile )
        throws IOException
    {
        this( confFile, Charset.forName( "UTF-8" ) );
    }

    /**
     * Create a ConfMap with a custom charset, but the default ConfParser.
     * 
     * @param confFile Path to a configuration file in a valid format.
     * @param charset The character set confFile is in.
     * @throws IOException If an I/O error occurs with confFile
     */
    public ConfMap( Path confFile, Charset charset )
        throws IOException
    {
        this( confFile, charset, new DefaultConfParser() );
    }

    /**
     * Create a ConfMap with a custom charset and a custom ConfParser.
     * 
     * @param confFile Path to a configuration file in a valid format.
     * @param charset The character set confFile is in.
     * @param parser The parser to use against confFile's text data.
     * @throws IOException If an I/O error occurs with confFile
     */
    public ConfMap( Path confFile, Charset charset, ConfParser parser )
        throws IOException
    {
        if ( !Files.exists( confFile ) )
            throw new IllegalArgumentException( "confFile ( " + confFile + " ) does not point to an existing target." );
        if ( !Files.isRegularFile( confFile ) )
            throw new IllegalArgumentException( "confFile ( " + confFile + " ) points to an existing target, "
                + "but this target is not a regular file." );

        // Create a BufferedReader from the Path and Charset, then pass to the ConfParser
        try ( BufferedReader reader = Files.newBufferedReader( confFile, charset ) )
        {
            this.entries = parser.parse( reader );
        }
    }

    /**
     * Create a ConfMap with the default UTF-8 charset, but a custom ConfParser.
     * 
     * @param confFile Path to a configuration file in a valid format.
     * @param parser The parser to use against confFile's text data.
     * @throws IOException If an I/O error occurs with confFile
     */
    public ConfMap( Path confFile, ConfParser parser )
        throws IOException
    {
        this( confFile, Charset.forName( "UTF-8" ), parser );
    }

    @Override
    public Set<Entry<String, String>> entrySet()
    {
        return entries;
    }

    /**
     * @throws RuntimeException if key does not exist
     */
    @Override
    public String get( Object key )
    {
        String ret = super.get( key );
        if ( ret == null )
        {
            throw new RuntimeException( "Configuration key '" + key + "' does not exist." );
        }
        return ret;
    }

    /**
     * Cannot be used.
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    public String put( String key, String value )
    {
        throw new UnsupportedOperationException( "ConfMaps cannot be written to." );
    }

    /**
     * Cannot be used.
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    public void putAll( Map<? extends String, ? extends String> m )
    {
        throw new UnsupportedOperationException( "ConfMaps cannot be written to." );
    }

    /**
     * Cannot be used.
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    public String remove( Object key )
    {
        throw new UnsupportedOperationException( "ConfMaps cannot be written to." );
    }
}

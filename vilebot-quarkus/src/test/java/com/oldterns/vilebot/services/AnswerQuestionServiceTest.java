package com.oldterns.vilebot.services;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import com.oldterns.vilebot.util.TestUrlStreamHandler;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class AnswerQuestionServiceTest
{
    @Inject
    AnswerQuestionService answerQuestionService;

    @Inject
    TestUrlStreamHandler urlMocker;

    @ConfigProperty( name = "vilebot.wolfram.key" )
    String API_KEY;

    static PrintStream realErrorStream = System.err;

    @BeforeAll
    public static void setupAll()
    {
        // Hide printed stack trace for the unit test that test the case the API fails
        realErrorStream = System.err;
        System.setErr( new PrintStream( new OutputStream()
        {
            public void write( int i )
            {
            }
        } ) );
    }

    @AfterAll
    public static void teardownAll()
    {
        // Restore the error stream
        System.setErr( realErrorStream );
    }

    @Test
    public void testCannotFindAnswer()
    {
        assertThat( answerQuestionService.getAnswerForQuery( "the meaning of life" ) ).isEqualTo( "I couldn't find an answer for that" );
    }

    @Test
    public void testShortAnswer()
    {
        String searchTerm = URLEncoder.encode( "1 + 1", StandardCharsets.UTF_8 );
        String url = "http://api.wolframalpha.com/v2/query?input=" + searchTerm + "&appid=" + API_KEY
            + "&format=plaintext&output=XML";
        urlMocker.mockConnection( url, AnswerQuestionServiceTest.class.getResourceAsStream( "answer-short.xml" ) );
        assertThat( answerQuestionService.getAnswerForQuery( "1 + 1" ) ).isEqualTo( "1 + 1\n2" );
    }

    @Test
    public void testLongAnswer()
    {
        String searchTerm = URLEncoder.encode( "George Washington", StandardCharsets.UTF_8 );
        String url = "http://api.wolframalpha.com/v2/query?input=" + searchTerm + "&appid=" + API_KEY
            + "&format=plaintext&output=XML";
        urlMocker.mockConnection( url, AnswerQuestionServiceTest.class.getResourceAsStream( "answer-long.xml" ) );
        assertThat( answerQuestionService.getAnswerForQuery( "George Washington" ) ).isEqualTo( "George Washington (politician)\n"
            + "full name | George Washington\n"
            + "date of birth | Friday, February 11, 1732 (Julian calendar) (289 years ago)\n"
            + "place of birth | Westmoreland County, Virginia\n"
            + "date of death | Saturday, December 14, 1799 (Gregorian calendar) (age: 67 years)\n"
            + " (221 years ago)\n" + "place of death | Mount Vernon, Virginia, United States" );
    }
}

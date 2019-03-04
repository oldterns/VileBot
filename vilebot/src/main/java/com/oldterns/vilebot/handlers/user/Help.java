/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Help
    extends ListenerAdapter
{
    private static final Pattern helpPattern = Pattern.compile( "!help" );

    private static final String helpMessage = generateHelpMessage();

    /**
     * Reply to user !help command with help info
     */
    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = helpPattern.matcher( text );

        if ( matcher.matches() )
        {
            event.respondPrivateMessage( helpMessage );
        }
    }

    private static String generateHelpMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Available Commands:" );

        sb.append( "\n" );
        sb.append( "  General:" );
        sb.append( " { !help }" );
        sb.append( " { !ping }" );
        sb.append( "\n" );
        sb.append( "  Karma:" );
        sb.append( " { noun++ }" );
        sb.append( " { noun-- }" );
        sb.append( " { noun+- }" );
        sb.append( " { !rank <noun> }" );
        sb.append( " { !revrank <place> }" );
        sb.append( " { !rankn <place> }" );
        sb.append( " { !revrankn <place> }" );
        sb.append( " { !topthree }" );
        sb.append( " { !bottomthree }" );
        sb.append( " { !roll [for] [<karma wager>] }" );
        sb.append( " { !rollcancel }" );
        sb.append( "\n" );
        sb.append( "  Games:" );
        sb.append( " { !rps <noun> [<dared thing>]}" );
        sb.append( " { !rpsrules }" );
        sb.append( " { !rpscancel }" );
        sb.append( " { !(rock|paper|scissors) }" );
        sb.append( " { !omgword }" );
        sb.append( " { !answer <answer to omgword>}" );
        sb.append( " { !jeopardy }" );
        sb.append( " { !whatis <answer to jeopardy question>}" );
        sb.append( " { !whois <answer to jeopardy question>}" );
        sb.append( " { !countdown } " );
        sb.append( " { !solution < numbered answer using symbols +-/*() > } " );
        sb.append( " { !countdownrules } " );
        sb.append( "\n" );
        sb.append( "  Fun:" );
        sb.append( " { !kaomoji <emotion> }" );
        sb.append( " { !excuse }" );
        sb.append( " { !randommeme }" );
        sb.append( " { !chuck }" );
        sb.append( " { !dance }" );
        sb.append( " { !speak }" );
        sb.append( "\n" );
        sb.append( "  Facts/Quotes:" );
        sb.append( " { !fact <noun> [!jaziz] }" );
        sb.append( " { !quote <noun> [!jaziz] }" );
        sb.append( " { !factrandom5 <noun> }" );
        sb.append( " { !quoterandom5 <noun> }" );
        sb.append( " { !factdump <noun> }" );
        sb.append( " { !quotedump <noun> }" );
        sb.append( " { !factadd <noun> <quote> }" );
        sb.append( " { !quoteadd <noun> <quote> }" );
        sb.append( " { !factsearch <noun> <regex> [!jaziz] }" );
        sb.append( " { !quotesearch <noun> <regex> [!jaziz] }" );
        sb.append( " { !factnumber <noun> }" );
        sb.append( " { !quotenumber <noun> }" );
        sb.append( "\n" );
        sb.append( "  Utility:" );
        sb.append( " { s/foo/bar[/[g] [<noun>]] }" );
        sb.append( " { !decide [<noun>] {<prefix>} <choice1>[|<choice2>]... }" );
        sb.append( " { !weather [<IATA code>] }" );
        sb.append( " { !lessweather [<IATA code>] }" );
        sb.append( " { !moreweather [<IATA code>] }" );
        sb.append( " { !tellme <question> }" );
        sb.append( " { !infoon <wikipedia query> }" );
        sb.append( " { !ttc }" );
        sb.append( " { !remindme <message> <time><d/m/s> }" );
        sb.append( " { !ascii [<font>] <message> }" );
        sb.append( " { !asciifonts }" );
        sb.append( "\n" );
        sb.append( "  Userlists:" );
        sb.append( " { !list <listname> }" );
        sb.append( " { !listadd <listname> <noun>[,|, | ][<noun2>]... }" );
        sb.append( " { !listrem <listname> <noun>[,|, | ][<noun2>]... }" );
        sb.append( " { listname: [message] }" );
        sb.append( "\n" );
        sb.append( "  Church:" );
        sb.append( " { !churchtotal }" );
        sb.append( " { !donate <karma amount> }" );
        sb.append( " { !gospel }" );
        sb.append( " { !inquisit <noun> }" );
        sb.append( " { !settitle <noun> }" );
        sb.append( " { !topdonors }" );
        return sb.toString();
    }
}

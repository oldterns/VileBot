/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

@HandlerContainer
public class Help
{
    private static final Pattern helpPattern = Pattern.compile( "!help" );

    private static final String helpMessage = generateHelpMessage();

    /**
     * Reply to user !help command with help info
     */
    @Handler
    private void userHelp( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = helpPattern.matcher( text );

        if ( matcher.matches() )
        {
            event.replyPrivately( helpMessage );
        }
    }

    private static String generateHelpMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Available Commands:" );

        sb.append( "  General:" );
        sb.append( " { !help }" );
        sb.append( " { !ping }" );
        sb.append( "  Karma:" );
        sb.append( " { noun++ }" );
        sb.append( " { noun-- }" );
        sb.append( " { !rank <noun> }" );
        sb.append( " { !revrank <place> }" );
        sb.append( " { !rankn <place> }" );
        sb.append( " { !revrankn <place> }" );
        sb.append( " { !topthree }" );
        sb.append( " { !bottomthree }" );
        sb.append( " { !roll [for] [<karma wager>] }" );
        sb.append( " { !rollcancel }" );
        sb.append( "  Fun:" );
        sb.append( "  { !omgword }" );
        sb.append( "  { !jeopardy }" );
        sb.append( "  { !whatis <answer to jeopardy question>}" );
        sb.append( "  { !whois <answer to jeopardy question>}" );
        sb.append( "  Karma:" );
        sb.append( "  Facts/Quotes:" );
        sb.append( " { !fact <noun> }" );
        sb.append( " { !quote <noun> }" );
        sb.append( " { !factdump <noun> }" );
        sb.append( " { !quotedump <noun> }" );
        sb.append( " { !factadd <noun> <quote> }" );
        sb.append( " { !quoteadd <noun> <quote> }" );
        sb.append( "\n" );
        sb.append( "{ !factsearch <noun> <regex> }" );
        sb.append( " { !quotesearch <noun> <regex> }" );
        sb.append( "  Utility:" );
        sb.append( " { s/foo/bar[/[g] [<noun>]] }" );
        sb.append( " { !decide [<noun>] {<prefix>} <choice1>[|<choice2>]... }" );
        sb.append( " { !weather [<IATA code>] }" );
        sb.append( " { !lessweather [<IATA code>] }" );
        sb.append( " { !moreweather [<IATA code>] }" );
        sb.append( " { !tellme <question> }" );
        sb.append( " { !infoon <wikipedia query> }" );
        sb.append( " { !ttc }" );
        sb.append( "  Userlists:" );
        sb.append( " { !list <listname> }" );
        sb.append( " { !listadd <listname> <noun>[,|, | ][<noun2>]... }" );
        sb.append( " { !listrem <listname> <noun>[,|, | ][<noun2>]... }" );
        sb.append( " { listname: [message] }" );
        sb.append( "  Fun:" );
        sb.append( " { !excuse }" );
        sb.append( " { !randommeme }" );
        sb.append( " { !chuck }" );
        sb.append( " { !dance }" );
        sb.append( " { !speak }" );
        sb.append( "\n" );
        sb.append( "Church:" );
        sb.append( " { !churchtotal }" );
        sb.append( " { !donate <karma amount> }" );
        sb.append( " { !gospel }" );
        sb.append( " { !inquisit <noun> }" );
        sb.append( " { !settitle <noun> }" );
        sb.append( " { !topdonors }" );
        return sb.toString();
    }
}

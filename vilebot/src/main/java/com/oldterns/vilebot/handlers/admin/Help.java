package com.oldterns.vilebot.handlers.admin;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@HandlerContainer
public class Help
    extends ListenerAdapter
{
    private static final Pattern helpPattern = Pattern.compile( "!admin help" );

    private static final String helpMessage = generateHelpMessage();

    // @Handler
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

        sb.append( " { !admin help }" );
        sb.append( " { !admin ping }" );
        sb.append( " { !admin auth <user> <pass> }" );
        sb.append( " { !admin quit }" );
        sb.append( " { !admin nick <nick> }" );
        sb.append( " { !admin op <nick> }" );
        sb.append( " { !admin unrank <noun> }" );
        sb.append( " { !admin unop <nick> }" );
        sb.append( " { !admin setadmin <nick> <pass> }" );
        sb.append( " { !admin remadmin <nick> }" );

        return sb.toString();
    }
}

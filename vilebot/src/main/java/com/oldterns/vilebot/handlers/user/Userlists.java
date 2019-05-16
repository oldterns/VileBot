package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.db.UserlistDB;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Userlists
    extends ListenerAdapter
{
    private static final Pattern enumeratePattern = Pattern.compile( "!lists" );

    private static final Pattern queryPattern = Pattern.compile( "!list (\\S+)" );

    private static final Pattern nickBlobPattern = Pattern.compile( "(?:(\\S+?)(?:, +| +|$))" );

    private static final Pattern addRemovePattern =
        Pattern.compile( "!list(add|rem) (\\S+) (" + nickBlobPattern + "+)" );

    private Pattern expandPattern = Pattern.compile( "(\\S+): (.*)" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();

        Matcher enumerateMatcher = enumeratePattern.matcher( text );
        Matcher queryMatcher = queryPattern.matcher( text );
        Matcher addRemoveMatcher = addRemovePattern.matcher( text );
        Matcher expandMatcher = expandPattern.matcher( text );

        if ( enumerateMatcher.matches() )
            listsEnumerate( event );
        if ( queryMatcher.matches() )
            listQuery( event, queryMatcher );
        if ( addRemoveMatcher.matches() )
            listAddRemove( event, addRemoveMatcher );
        if ( expandMatcher.matches() )
            listExpansion( event, expandMatcher );
    }

    private void listsEnumerate( GenericMessageEvent event )
    {
        Set<String> lists = UserlistDB.getLists();

        if ( lists != null && lists.size() > 0 )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "Available lists: " );
            for ( String list : lists )
            {
                sb.append( list );
                sb.append( ", " );
            }
            sb.delete( sb.length() - 2, sb.length() );
            event.respondWith( sb.toString() );
        }
        else
        {
            event.respondWith( "There are no lists." );
        }
    }

    private void listQuery( GenericMessageEvent event, Matcher matcher )
    {
        String listName = matcher.group( 1 );

        Set<String> users = UserlistDB.getUsersIn( listName );
        if ( users != null && users.size() > 0 )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "The list " );
            sb.append( listName );
            sb.append( " contains: " );

            for ( String user : users )
            {
                sb.append( user );
                sb.append( ", " );
            }
            sb.delete( sb.length() - 2, sb.length() );

            event.respondPrivateMessage( sb.toString() );
        }
        else
        {
            event.respondPrivateMessage( "The list " + listName + " does not exist or is empty." );
        }
    }

    private void listAddRemove( GenericMessageEvent event, Matcher matcher )
    {
        String mode = matcher.group( 1 );
        String listName = matcher.group( 2 );
        String nickBlob = matcher.group( 3 );
        if ( nickBlob == null )
        {
            nickBlob = matcher.group( 4 );
        }

        List<String> nicks = new LinkedList<>();
        Matcher nickMatcher = nickBlobPattern.matcher( nickBlob );
        while ( nickMatcher.find() )
        {
            nicks.add( nickMatcher.group( 1 ) );
        }

        StringBuilder sb = new StringBuilder();

        if ( "add".equals( mode ) )
        {
            UserlistDB.addUsersTo( listName, nicks );
            sb.append( "Added the following names to list " );
        }
        else if ( "rem".equals( mode ) )
        {
            UserlistDB.removeUsersFrom( listName, nicks );
            sb.append( "Removed the following names from list " );
        }

        sb.append( listName );
        sb.append( ": " );

        for ( String nick : nicks )
        {
            sb.append( nick );
            sb.append( ", " );
        }
        sb.delete( sb.length() - 2, sb.length() );

        event.respondWith( sb.toString() );
    }

    private void listExpansion( GenericMessageEvent event, Matcher matcher )
    {
        String sender = event.getUser().getNick();
        String listName = matcher.group( 1 );
        String msg = matcher.group( 2 );

        Set<String> users = UserlistDB.getUsersIn( listName );
        if ( users != null && users.size() > 0 )
        {
            StringBuilder sb = new StringBuilder();
            for ( String user : users )
            {
                if ( !user.equals( sender ) )
                {
                    sb.append( user );
                    sb.append( ", " );
                }
            }
            sb.delete( sb.length() - 2, sb.length() );
            sb.append( ": " );
            sb.append( msg );

            event.respondWith( sb.toString() );
        }
    }
}

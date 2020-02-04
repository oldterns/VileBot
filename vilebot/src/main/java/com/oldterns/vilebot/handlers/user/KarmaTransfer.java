/**
 * Copyright (C) 2020 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.util.BaseNick;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KarmaTransfer
    extends ListenerAdapter
{
    private static final Pattern transferPattern = Pattern.compile( "^!transfer\\s+(\\S+)\\s+([0-9]+)" );

    private static final Pattern cancelTransferPattern = Pattern.compile( "^!transfercancel" );

    private static final Pattern acceptTransferPattern = Pattern.compile( "^!accept" );

    private static final Pattern rejectTransferPattern = Pattern.compile( "^!reject" );

    private KarmaTransaction currentTransaction;

    private final Object currentTransferMutex = new Object();

    private static ExecutorService timer = Executors.newScheduledThreadPool( 1 );

    private static final long TIMEOUT = 30000L;

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();

        Matcher transferMatcher = transferPattern.matcher( text );
        Matcher cancelTransferMatcher = cancelTransferPattern.matcher( text );
        Matcher acceptTransferMatcher = acceptTransferPattern.matcher( text );
        Matcher rejectTransferMatcher = rejectTransferPattern.matcher( text );

        if ( transferMatcher.matches() )
            transferKarma( event, transferMatcher );
        if ( cancelTransferMatcher.matches() )
            cancelTransfer( event );
        if ( acceptTransferMatcher.matches() )
            acceptTransfer( event );
        if ( rejectTransferMatcher.matches() )
            rejectTransfer( event );
    }

    private void transferKarma( GenericMessageEvent event, Matcher transferMatcher )
    {
        // Prevent users from transferring karma outside of #thefoobar
        if ( !( event instanceof MessageEvent )
            || !( (MessageEvent) event ).getChannel().getName().equals( Vilebot.getConfig().get( "ircChannel1" ) ) )
        {
            event.respondWith( "You must be in " + Vilebot.getConfig().get( "ircChannel1" )
                + " to make or accept wagers." );
            return;
        }

        synchronized ( currentTransferMutex )
        {
            // No existing transfer
            if ( currentTransaction == null )
            {
                // Check if sender is the same as the receiver
                String sender = BaseNick.toBaseNick( event.getUser().getNick() );
                String receiver = BaseNick.toBaseNick( transferMatcher.group( 1 ) );

                if ( sender.equals( receiver ) )
                {
                    event.respondWith( "Really? What's the point of transferring karma to yourself?" );
                    return;
                }

                // Check if transfer amount is valid
                int transferAmount = Integer.parseInt( transferMatcher.group( 2 ) );
                Integer senderKarma = KarmaDB.getNounKarma( sender );
                senderKarma = senderKarma == null ? 0 : senderKarma;

                if ( !validAmount( transferAmount, senderKarma ) )
                {
                    event.respondWith( transferAmount + " isn't a valid amount."
                        + " Transfer amount must be greater than 0, and you must have at least as much karma"
                        + " as the amount you want to transfer." );
                }
                else
                {
                    // Valid amount, start transaction
                    startTransaction( event, sender, receiver, transferAmount );
                }
            }
            // A transfer is active
            else
            {
                String sender = currentTransaction.getSender();
                String receiver = currentTransaction.getReceiver();
                int amount = currentTransaction.getTransferAmount();

                event.respondWith( "A transfer is already active. " + sender + " wants to transfer " + amount
                    + " karma to " + receiver + ". " + receiver + ", please !accept or !reject this transfer."
                    + " You have 30 seconds to respond." );
            }
        }
    }

    private boolean validAmount( int transferAmount, int senderKarma )
    {
        return ( transferAmount > 0 ) && ( senderKarma >= transferAmount );
    }

    private void startTransaction( GenericMessageEvent event, String sender, String receiver, int transferAmount )
    {
        currentTransaction = new KarmaTransaction( sender, receiver, transferAmount );
        event.respondWith( sender + " wants to transfer " + transferAmount + " karma to " + receiver + ". " + receiver
            + ", please !accept or !reject the transfer. You have 30 seconds to respond." );

        startTimer( event );
    }

    private void startTimer( final GenericMessageEvent event )
    {
        timer.submit( () -> {
            try
            {
                Thread.sleep( TIMEOUT );
                timeoutTimer( event );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
        } );
    }

    private void timeoutTimer( final GenericMessageEvent event )
    {
        event.respondWith( "Transfer failed. No response was received within 30 seconds." );

        synchronized ( currentTransferMutex )
        {
            currentTransaction = null;
        }
    }

    private void stopTimer()
    {
        timer.shutdownNow();
        timer = Executors.newFixedThreadPool( 1 );
    }

    private void cancelTransfer( GenericMessageEvent event )
    {
        if ( !doesCurrentTransactionExist( event ) )
            return;

        String user = BaseNick.toBaseNick( ( event.getUser().getNick() ) );
        String sender = currentTransaction.getSender();

        if ( !sender.equals( user ) )
        {
            event.respondWith( "Only " + sender + " may cancel this transfer." );
            return;
        }

        // Cancel transfer
        stopTimer();
        synchronized ( currentTransferMutex )
        {
            currentTransaction = null;
        }

        event.respondWith( user + " has cancelled this transfer." );
    }

    private void acceptTransfer( GenericMessageEvent event )
    {
        if ( !doesCurrentTransactionExist( event ) )
            return;

        String user = BaseNick.toBaseNick( ( event.getUser().getNick() ) );
        String sender = currentTransaction.getSender();
        String receiver = currentTransaction.getReceiver();
        int amount = currentTransaction.getTransferAmount();

        if ( !receiver.equals( user ) )
        {
            event.respondWith( "Only " + receiver + " may accept this transfer." );
            return;
        }

        // Accept transfer
        stopTimer();

        KarmaDB.modNounKarma( receiver, amount );
        KarmaDB.modNounKarma( sender, -1 * amount );

        event.respondWith( "Transfer success! " + sender + " has transferred " + amount + " karma to " + receiver
            + "!" );

        // Reset
        synchronized ( currentTransferMutex )
        {
            currentTransaction = null;
        }
    }

    private void rejectTransfer( GenericMessageEvent event )
    {
        if ( !doesCurrentTransactionExist( event ) )
            return;

        String user = BaseNick.toBaseNick( ( event.getUser().getNick() ) );
        String sender = currentTransaction.getSender();
        String receiver = currentTransaction.getReceiver();

        if ( !receiver.equals( user ) )
        {
            event.respondWith( "Only " + receiver + " may reject this transfer." );
            return;
        }

        // Reject transfer
        stopTimer();
        synchronized ( currentTransferMutex )
        {
            currentTransaction = null;
        }

        event.respondWith( user + " has rejected " + sender + "'s transfer." );
    }

    private boolean doesCurrentTransactionExist( GenericMessageEvent event )
    {
        if ( currentTransaction == null )
        {
            event.respondWith( "No active transfer. To transfer karma enter '!transfer <noun> <karma amount>'." );
            return false;
        }
        return true;
    }

    private static class KarmaTransaction
    {
        private String sender;

        private String receiver;

        private final int transferAmount;

        KarmaTransaction( String senderNick, String receiverNick, int transferAmount )
        {
            if ( senderNick == null )
                throw new IllegalArgumentException( "senderNick can't be null" );
            if ( receiverNick == null )
                throw new IllegalArgumentException( "receiverNick can't be null" );

            this.sender = senderNick;
            this.receiver = receiverNick;
            this.transferAmount = transferAmount;
        }

        String getSender()
        {
            return sender;
        }

        String getReceiver()
        {
            return receiver;
        }

        int getTransferAmount()
        {
            return transferAmount;
        }
    }
}

package com.oldterns.vilebot.services;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.database.KarmaDB;
import com.oldterns.vilebot.util.TimeService;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class KarmaTransferService {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Inject
    KarmaDB karmaDB;

    @Inject
    TimeService timeService;

    private final Lock currentTransferMutex = new ReentrantLock();
    private Future<?> transferTimeout;

    private KarmaTransaction currentTransaction;

    @OnChannelMessage("!transfer @recieverNick @transferAmount")
    public String transferKarma( ChannelMessageEvent event, Nick recieverNick, Long transferAmount) {
        try {
            currentTransferMutex.lock();
            if (currentTransaction != null) {
                String sender = currentTransaction.getSender();
                String receiver = currentTransaction.getReceiver();
                long amount = currentTransaction.getTransferAmount();

                return "A transfer is already active. " + sender + " wants to transfer " + amount
                        + " karma to " + receiver + ". " + receiver + ", please !accept or !reject this transfer."
                        + " You have 30 seconds to respond.";
            }
            String sender = Nick.getNick(event).getBaseNick();
            String receiver = recieverNick.getBaseNick();

            if ( sender.equals( receiver ) )
            {
                return  "Really? What's the point of transferring karma to yourself?";
            }

            Long senderKarma = karmaDB.getNounKarma( sender ).orElse(0L);

            if ( !validAmount( transferAmount, senderKarma ) )
            {
                return transferAmount + " isn't a valid amount."
                        + " Transfer amount must be greater than 0, and you must have at least as much karma"
                        + " as the amount you want to transfer.";
            } else {
                return startTransaction( event, sender, receiver, transferAmount );
            }
        } finally {
            currentTransferMutex.unlock();
        }
    }

    @OnChannelMessage("!transfercancel")
    public String cancelTransfer(ChannelMessageEvent event) {
        if ( !reportHasCurrentTransaction( event ) )
            return null;

        String user = Nick.getNick(event).getBaseNick();
        String sender = currentTransaction.getSender();

        if ( !sender.equals( user ) )
        {
            return  "Only " + sender + " may cancel this transfer.";
        }

        // Cancel transfer
        try {
            currentTransferMutex.lock();
            transferTimeout.cancel(true);
            currentTransaction = null;
        } finally {
            currentTransferMutex.unlock();
        }

        return user + " has cancelled this transfer.";
    }

    @OnChannelMessage("!accept")
    public String acceptTransfer( ChannelMessageEvent event )
    {
        if ( !reportHasCurrentTransaction( event ) )
            return null;

        String user = Nick.getNick(event).getBaseNick();
        String sender = currentTransaction.getSender();
        String receiver = currentTransaction.getReceiver();
        long amount = currentTransaction.getTransferAmount();

        if ( !receiver.equals( user ) )
        {
            return "Only " + receiver + " may accept this transfer.";
        }

        // Accept transfer
        try {
            currentTransferMutex.lock();
            transferTimeout.cancel(true);

            karmaDB.modNounKarma( receiver, amount );
            karmaDB.modNounKarma( sender, -1 * amount );

            currentTransaction = null;

            return "Transfer success! " + sender + " has transferred " + amount + " karma to " + receiver
                + "!";
        } finally {
            currentTransferMutex.unlock();
        }
    }

    @OnChannelMessage("!reject")
    public String rejectTransfer( ChannelMessageEvent event )
    {
        if ( !reportHasCurrentTransaction( event ) )
            return null;

        String user = Nick.getNick(event).getBaseNick();
        String sender = currentTransaction.getSender();
        String receiver = currentTransaction.getReceiver();

        if ( !receiver.equals( user ) )
        {
            return "Only " + receiver + " may reject this transfer.";
        }

        // Reject transfer
        try
        {
            currentTransferMutex.lock();
            transferTimeout.cancel(true);
            currentTransaction = null;
            return user + " has rejected " + sender + "'s transfer.";
        } finally {
            currentTransferMutex.unlock();
        }
    }

    private boolean validAmount( long transferAmount, long senderKarma )
    {
        return ( transferAmount > 0 ) && ( senderKarma >= transferAmount );
    }

    private String startTransaction( ChannelMessageEvent event, String sender, String receiver, long transferAmount )
    {
        currentTransaction = new KarmaTransaction( sender, receiver, transferAmount );
        transferTimeout = timeService.onTimeout(TIMEOUT, () -> {
            try {
                currentTransferMutex.lock();
                event.sendReply( "Transfer failed. No response was received within 30 seconds." );
                currentTransaction = null;
                transferTimeout = null;
            } finally {
                currentTransferMutex.unlock();
            }
        });
        return sender + " wants to transfer " + transferAmount + " karma to " + receiver + ". " + receiver
                + ", please !accept or !reject the transfer. You have 30 seconds to respond.";
    }

    private boolean reportHasCurrentTransaction( ChannelMessageEvent event )
    {
        if ( currentTransaction == null )
        {
            event.sendReply( "No active transfer. To transfer karma enter '!transfer <noun> <karma amount>'." );
            return false;
        }
        return true;
    }

    private static class KarmaTransaction
    {
        private String sender;

        private String receiver;

        private final long transferAmount;

        KarmaTransaction( String senderNick, String receiverNick, long transferAmount )
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

        long getTransferAmount()
        {
            return transferAmount;
        }
    }
}

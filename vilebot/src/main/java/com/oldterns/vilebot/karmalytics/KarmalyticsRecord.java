package com.oldterns.vilebot.karmalytics;

import java.time.LocalDateTime;
import java.util.Base64;

public final class KarmalyticsRecord
{
    private final LocalDateTime dateTime;

    private final String nick;

    private final String source;

    private final String extraInfo;

    private final int karmaModAmount;

    public KarmalyticsRecord( String record )
    {
        String[] fields = record.split( ">" );
        dateTime = LocalDateTime.parse( fields[0] );
        nick = fields[1];
        source = fields[2];
        extraInfo = new String( Base64.getDecoder().decode( fields[3] ) );
        karmaModAmount = Integer.parseInt( fields[4] );
    }

    public LocalDateTime getDateTime()
    {
        return dateTime;
    }

    public String getNick()
    {
        return nick;
    }

    public String getSource()
    {
        return source;
    }

    public String getExtraInfo()
    {
        return extraInfo;
    }

    public int getKarmaModAmount()
    {
        return karmaModAmount;
    }
}

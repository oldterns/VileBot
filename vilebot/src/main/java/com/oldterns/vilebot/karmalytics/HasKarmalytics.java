package com.oldterns.vilebot.karmalytics;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.oldterns.vilebot.db.KarmalyticsDB;
import com.oldterns.vilebot.util.BaseNick;

public interface HasKarmalytics
{
    /**
     * Returns the groups this HasKarmalytics is in. (Example: Omgword is in "Gaming" and "Omgword")
     * 
     * @return the groups this HasKarmalytics is in
     */
    List<String> getGroups();

    /**
     * Returns a unique id that identifies this HasKarmalytics (note: the character ">" is illegal)
     * 
     * @return this HasKarmalytics id
     */
    String getKarmalyticsId();

    /**
     * Returns a function that maps KarmalyticsRecord with source `getKarmalyticsId()` to a String that describes the
     * karma transaction.
     * 
     * @return a mapping from this HasKarmalytics records to String
     */
    Function<KarmalyticsRecord, String> getRecordDescriptorFunction();

    default void modNounKarma( String nick, int mod )
    {
        KarmalyticsDB.modNounKarma( this, Optional.empty(), BaseNick.toBaseNick( nick ), mod );
    }

    default void modNounKarma( String nick, String extraInfo, int mod )
    {
        KarmalyticsDB.modNounKarma( this, Optional.of( extraInfo ), BaseNick.toBaseNick( nick ), mod );
    }
}

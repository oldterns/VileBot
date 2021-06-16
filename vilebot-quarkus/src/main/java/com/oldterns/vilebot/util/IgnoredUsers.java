package com.oldterns.vilebot.util;

import javax.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@ApplicationScoped
public class IgnoredUsers
{
    Set<String> onJoin = new ConcurrentSkipListSet<String>();

    Set<String> autoOp = new ConcurrentSkipListSet<String>();

    public Set<String> getOnJoin()
    {
        return onJoin;
    }

    public Set<String> getAutoOp()
    {
        return autoOp;
    }

    public void setOnJoin( Set<String> onJoin )
    {
        this.onJoin = onJoin;
    }

    public void setAutoOp( Set<String> autoOp )
    {
        this.autoOp = autoOp;
    }

    public void addOnJoin( String nick )
    {
        onJoin.add( nick );
    }

    public void addAutoOp( String channel )
    {
        autoOp.add( channel );
    }
}

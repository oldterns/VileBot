package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.util.ASCII;
import net.engio.mbassy.listener.Handler;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by eunderhi on 17/08/15.
 */

// @HandlerContainer
public class ImageToAscii
    extends ListenerAdapter
{
    private static final Pattern questionPattern = Pattern.compile( "^!(convert)\\s(.+)$" );

    // @Handler
    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = questionPattern.matcher( text );

        if ( matcher.matches() )
        {
            String URL = matcher.group( 2 );
            try
            {
                String image = convertImage( URL );
                event.respondWith( image );
            }
            catch ( Exception e )
            {
                event.respondWith( "Could not convert image." );
            }
        }
    }

    private String convertImage( String strURL )
        throws Exception
    {
        URL url = new URL( strURL );
        BufferedImage image = ImageIO.read( url );
        image = shrink( image );
        return new ASCII().convert( image );
    }

    private BufferedImage shrink( BufferedImage image )
    {
        int MAX_WIDTH = 50;
        int height = image.getHeight();
        int width = image.getWidth();
        float ratio = (float) height / width;
        int newHeight = Math.round( ( MAX_WIDTH * ratio ) * 0.56f );

        BufferedImage newImage = new BufferedImage( MAX_WIDTH, newHeight, BufferedImage.TYPE_INT_RGB );

        Graphics g = newImage.createGraphics();
        g.drawImage( image, 0, 0, MAX_WIDTH, newHeight, null );
        g.dispose();
        return newImage;
    }
}

package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.vilebot.util.ASCII;
import com.oldterns.vilebot.util.URLFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Created by eunderhi on 17/08/15.
 */
@ApplicationScoped
public class ImageToAsciiService
{
    @Inject
    URLFactory urlFactory;

    @OnChannelMessage( "!convert @url" )
    public String convert( String url )
    {
        try
        {
            return convertImage( url );
        }
        catch ( Exception e )
        {
            return "Could not convert image.";
        }
    }

    private String convertImage( String strURL )
        throws Exception
    {
        URL url = urlFactory.build( strURL );
        BufferedImage image = ImageIO.read( url );
        image = shrink( image );
        return ASCII.convert( image );
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

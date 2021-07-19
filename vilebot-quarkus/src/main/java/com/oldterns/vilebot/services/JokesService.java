package com.oldterns.vilebot.services;

import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.util.CharactersThatBreakEclipse;
import com.oldterns.vilebot.util.RandomProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class JokesService {

    @Inject
    RandomProvider randomProvider;

    final List<String> memes = generateMemes();
    final List<String> dances = generateDances();
    final List<String> chucks = generateChucks();
    final List<String> containerJokes = generateContainerJokes();

    @OnChannelMessage(".*container.*")
    public String onContainerMessage() {
        return randomProvider.getRandomElement(containerJokes);
    }

    @OnChannelMessage(".*[rR][eE][dD][dD][iI][tT].*")
    public String onRedditMessage() {
        if (randomProvider.getRandomInt(10) > 6) {
            return CharactersThatBreakEclipse.LODEMOT;
        }
        return null;
    }

    @OnChannelMessage("!randommeme")
    public String onRandomMeme() {
        return randomProvider.getRandomElement(memes);
    }

    @OnChannelMessage("!dance")
    public String onDance() {
        return randomProvider.getRandomElement(dances);
    }

    @OnChannelMessage("!chuck")
    public String onChuckNorris() {
        return randomProvider.getRandomElement(chucks);
    }

    private static List<String> generateChucks()
    {
        List<String> chucks = new ArrayList<>();

        // Anyone called Chuck except Chuck Norris

        chucks.add( "Chuck Palahniuk once said, \"Your birth is a mistake you'll spend your whole life trying to correct.\"" );
        chucks.add( "Chuck Tanner once said, \"If you don't like the way the Atlanta Braves are playing then you don't like baseball.\"" );
        chucks.add( "Chuck Berry once said, \"Roll over, Beethoven, and tell Tchaikovsky the news.\"" );
        chucks.add( "Chuck Yeager once said, \"Rules are made for people who aren't willing to make up their own.\"" );
        chucks.add( "Chuck Klosterman once said, \"I guess what I'm saying is, I'm not your enemy.\"" );
        chucks.add( "Chuck Jones once said, \"A lion's work hours are only when he's hungry; once he's satisfied, the predator and prey live peacefully together.\"" );
        chucks.add( "Chuck Close once said, \"I'm very learning-disabled, and I think it drove me to what I'm doing.\"" );
        chucks.add( "Chuck D once said, \"I think governments are the cancer of civilization.\"" );
        chucks.add( "Chuck Knox once said, \"Always have a plan, and believe in it. Nothing happens by accident.\"" );
        chucks.add( "Chuck Zito once said, \"If the challenge to fight was there, I always took it.\"" );

        return chucks;
    }

    private static List<String> generateDances()
    {
        List<String> dances = new ArrayList<String>();

        dances.add( "No. " + CharactersThatBreakEclipse.LODEMOT );
        dances.add( "Bots as vile as I do not dance." );
        dances.add( "ScumbagBot does not dance." );
        dances.add( "Chuck Norris once asked me to dance. He didn't have a comeback after the roundhouse kickban." );
        dances.add( CharactersThatBreakEclipse.KIRBYFLIP + "\n*kills Kirby*\n*kills dance*" );
        dances.add( "Does doing a barrel roll count?" );

        return dances;
    }

    private static List<String> generateMemes()
    {
        List<String> memes = new ArrayList<>();

        memes.add( CharactersThatBreakEclipse.LODEMOT );
        memes.add( "Derp." );
        memes.add( "looks like this string was, *puts on glasses*\nnull terminated.\nYEEEEEEEEEEEEEEEEEEEEEEAAAAAAHHHHHHHHH" );
        memes.add( "Yo Dawg" );
        memes.add( "Imma gunna cut you so bad, you gunna wish I dun cut you so bad" );
        memes.add( "So i herd u liek mudkipz" );
        memes.add( "Never gunna give you up, Never gunna let you down!" );
        memes.add( "DO A BARREL ROLL ._. |: .-. :| ._." );
        memes.add( "ScumbagBot, shows up to the party, drinks all your beer." );
        memes.add( "Don't tase me bro" );

        return memes;
    }

    private static List<String> generateContainerJokes()
    {
        List<String> jokes = new ArrayList<String>();
        jokes.add( "C O N T A I N E D" );
        jokes.add( "Containing containers containingly." );
        jokes.add( "Containers carry a vast amount of stuff." );
        jokes.add( "H Y B R I D  C L O U D" );
        jokes.add( "\"Linux is Containers and Containers is RHEL\"" );
        jokes.add( "It can contain anything, even monster apps!" );
        jokes.add( "I sell Container and Container accessories" );
        jokes.add( "Do U Even Small VM brah?" );
        jokes.add( "Y SO CONTAINED?" );
        jokes.add( "Containers give me lyfe." );
        jokes.add( "Containers pay the bills." );
        jokes.add( "My name is yzhang and I'm a containerholic." );
        jokes.add( "\"Containers\"" );
        jokes.add( "Keep your containers to yourself!" );
        jokes.add( "*insert bad container joke here*" );
        jokes.add( "AM I BEING CONTAINED?" );
        jokes.add( "friggin' containers, how do they work?" );
        jokes.add( "I put the \"I\" in contaIners" );
        jokes.add( "DON'T CONTAZE ME" );
        jokes.add( "Every day we stray farther from gods light." );
        jokes.add( "Please, contain yourself." );
        jokes.add( "PRESS Z TO DO A CONTAINER ROLL" );
        jokes.add( "STOP. CONTAINER TIME" );
        jokes.add( "Dat feel when you can only contain monster apps and not your feelings" );
        jokes.add( "- Refer to the Container Coloring Book" );

        return jokes;
    }

}

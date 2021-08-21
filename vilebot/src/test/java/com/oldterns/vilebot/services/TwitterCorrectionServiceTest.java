package com.oldterns.vilebot.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TwitterCorrectionServiceTest
{

    @Test
    public void testCorrection()
    {
        TwitterCorrectionService twitterCorrectionService = new TwitterCorrectionService();
        assertThat( twitterCorrectionService.onTwitterSyntax( "@bob" ) ).isEqualTo( "You seem to be using twitter addressing syntax. On IRC you would say this instead: bob: message" );
        assertThat( twitterCorrectionService.onTwitterSyntax( "@bob:" ) ).isEqualTo( "You seem to be using twitter addressing syntax. On IRC you would say this instead: bob: message" );
    }

}

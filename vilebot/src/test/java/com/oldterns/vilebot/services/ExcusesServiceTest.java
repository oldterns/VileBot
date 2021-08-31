package com.oldterns.vilebot.services;

import javax.inject.Inject;

import com.oldterns.vilebot.database.ExcuseDB;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ExcusesServiceTest
{

    @Inject
    ExcusesService excusesService;

    @InjectMock
    ExcuseDB excuseDB;

    @Test
    public void testNoExcuses()
    {
        Mockito.when( excuseDB.getRandExcuse() ).thenReturn( null );
        assertThat( excusesService.getExcuse() ).isEqualTo( "No excuses available" );
    }

    @Test
    public void testExcuse()
    {
        Mockito.when( excuseDB.getRandExcuse() ).thenReturn( "This is an excuse" );
        assertThat( excusesService.getExcuse() ).isEqualTo( "This is an excuse" );
    }

    @Test
    public void addExcuse()
    {
        assertThat( excusesService.addExcuse( "my excuse" ) ).isEqualTo( "Excuse was added to database" );
        Mockito.verify( excuseDB ).addExcuse( "my excuse" );
    }
}

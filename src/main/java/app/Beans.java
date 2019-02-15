package app;

import app.conf.SheetsApp;
import app.dao.SheetApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class Beans {

    @Bean
    public SheetApi sheetApi() throws IOException
    {
        return new SheetApi(SheetsApp.getSheetsService());
    }
}

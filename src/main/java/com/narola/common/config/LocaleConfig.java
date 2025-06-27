package com.narola.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        List<Locale> supportedLocales = Arrays.asList(
                Locale.ENGLISH,
                Locale.FRENCH,
                Locale.GERMAN,
                new Locale.Builder().setLanguage("es").build()
        );
        resolver.setSupportedLocales(supportedLocales);
        return resolver;
    }
}

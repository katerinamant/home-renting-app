package com.homerentals.domain;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

public class DomainUtils {
    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
}

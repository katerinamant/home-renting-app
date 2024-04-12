package com.homerentals.domain;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DomainUtils {
    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
}

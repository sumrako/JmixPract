package com.company.jmixpract.screen.calendar;

import java.time.LocalDate;

public interface CalendarNavigation {
    void navigate(CalendarNavigationMode navigationMode, LocalDate referenceDate);
}

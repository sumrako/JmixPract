package com.company.jmixpract.screen.calendar;

import com.company.jmixpract.entity.Event;
import com.company.jmixpract.entity.Format;
import com.company.jmixpract.entity.Student;
import com.vaadin.v7.shared.ui.calendar.CalendarState;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.TimeSource;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.ui.ScreenBuilders;
import io.jmix.ui.action.Action;
import io.jmix.ui.component.*;
import io.jmix.ui.component.calendar.SimpleCalendarEvent;
import io.jmix.ui.screen.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.company.jmixpract.screen.calendar.CalendarNavigationMode.*;
import static com.company.jmixpract.screen.calendar.RelativeDates.startOfWeek;

@UiController("Calendar")
@UiDescriptor("calendar.xml")
public class Calendar extends Screen {

    @Autowired
    protected io.jmix.ui.component.Calendar<LocalDateTime> calendar;

    @Autowired
    protected Messages messages;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private ScreenBuilders screenBuilders;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private ValuesPicker<Student> valuePicker;
    @Autowired
    private RadioButtonGroup<CalendarMode> calendarMode;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private DatePicker<LocalDate> calendarNavigator;
    @Autowired
    private CalendarNavigators calendarNavigators;
    @Autowired
    private Label<String> calendarTitle;
    @Autowired
    private DatatypeFormatter datatypeFormatter;


    @Subscribe
    protected void onInit(InitEvent event) {
        initSortCalendarEventsInMonthlyView();
        generateEvents();
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Calendar Date Navigation
    /////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        current(CalendarMode.WEEK);
    }

    @SuppressWarnings("deprecation")
    private void initSortCalendarEventsInMonthlyView() {
        calendar.unwrap(com.vaadin.v7.ui.Calendar.class)
                .setEventSortOrder(CalendarState.EventSortOrder.START_DATE_DESC);
    }


    @Subscribe("calendar")
    protected void onCalendarCalendarDayClick(io.jmix.ui.component.Calendar.CalendarDateClickEvent<LocalDateTime> event) {
        atDate(
                CalendarMode.DAY,
                event.getDate().toLocalDate()
        );
    }

    @Subscribe("calendar")
    protected void onCalendarCalendarWeekClick(io.jmix.ui.component.Calendar.CalendarWeekClickEvent<LocalDateTime> event) {
        atDate(
                CalendarMode.WEEK,
                startOfWeek(
                        event.getYear(),
                        event.getWeek(),
                        currentAuthentication.getLocale()
                )
        );
    }

    @Subscribe("navigatorPrevious")
    protected void onNavigatorPreviousClick(Button.ClickEvent event) {
        previous(calendarMode.getValue());
    }

    @Subscribe("navigatorNext")
    protected void onNavigatorNextClick(Button.ClickEvent event) {
        next(calendarMode.getValue());
    }

    @Subscribe("navigatorCurrent")
    protected void onNavigatorCurrentClick(Button.ClickEvent event) {
        current(calendarMode.getValue());
    }

    @Subscribe("calendarNavigator")
    protected void onCalendarRangePickerValueChange(HasValue.ValueChangeEvent<LocalDate> event) {
        if (event.isUserOriginated()) {
            atDate(calendarMode.getValue(), event.getValue());
        }
    }

    private void current(CalendarMode calendarMode) {
        change(calendarMode, AT_DATE, timeSource.now().toLocalDate());
    }

    private void atDate(CalendarMode calendarMode, LocalDate date) {
        change(calendarMode, AT_DATE, date);
    }

    private void next(CalendarMode calendarMode) {
        change(calendarMode, NEXT, calendarNavigator.getValue());
    }

    private void previous(CalendarMode calendarMode) {
        change(calendarMode, PREVIOUS, calendarNavigator.getValue());
    }

    private void change(CalendarMode calendarMode, CalendarNavigationMode navigationMode, LocalDate referenceDate) {
        this.calendarMode.setValue(calendarMode);

        calendarNavigators
                .forMode(
                        CalendarScreenAdjustment.of(calendar, calendarNavigator, calendarTitle),
                        datatypeFormatter,
                        calendarMode
                )
                .navigate(navigationMode, referenceDate);

        if (valuePicker.isEmpty())
            generateEvents();
        else
            generateEvents(event1 -> !Collections.disjoint(event1.getStudents(), valuePicker.getValue()));
    }

    @Subscribe("calendarMode")
    protected void onCalendarRangeValueChange(HasValue.ValueChangeEvent event) {
        if (event.isUserOriginated()) {
            atDate((CalendarMode) event.getValue(), calendarNavigator.getValue());
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Calendar Data Manipulation
    /////////////////////////////////////////////////////////////////////////////////////////////////////////


    protected void generateEvents() {
        calendar.getEventProvider().removeAllEvents();
        List<Event> eventList = dataManager.load(Event.class).all().list();

        eventList.forEach(this::generateEvent);
        addStartEndPracticeEvents();
     }

    protected void generateEvents(Predicate<? super Event> filter) {
        calendar.getEventProvider().removeAllEvents();
        List<Event> eventList = dataManager.load(Event.class).all().list();

        eventList = eventList.stream().filter(filter).collect(Collectors.toList());
        eventList.forEach(this::generateEvent);
        addStartEndPracticeEvents(student -> valuePicker.getValue().contains(student));
    }

    private void addStartEndPracticeEvents(Predicate<? super Student> filter) {
        dataManager.load(Student.class)
                .all().list().stream().filter(filter)
                .collect(Collectors.toList())
                .forEach(this::addStartEndPracticeEvent);
    }

    private void addStartEndPracticeEvents() {
        dataManager.load(Student.class)
                .all().list()
                .forEach(this::addStartEndPracticeEvent);
    }

    private void addStartEndPracticeEvent(Student student) {
        LocalDateTime startDateTime = student.getStartPracticeDate().atTime(0, 0);
        LocalDateTime endDateTime = student.getEndPracticeDate().atTime(0, 0);

        generateEvent(messages.getMessage("com.company.jmixpract", "start.application"),
                String.format("Student:\n%s", student.getFirstName()), startDateTime,
                startDateTime, Format.OFFLINE.getId(), true);

        generateEvent(messages.getMessage("com.company.jmixpract", "end.application"),
                String.format("Student:\n%s", student.getFirstName()), endDateTime,
                endDateTime, Format.OFFLINE.getId(), true);
    }

    protected void generateEvent(Event event) {
        String caption = event.getDescription();
        String students = String.join("\n", event.getStudents().stream().map(Student::getFirstName)
                .collect(Collectors.toList()));
        String description = String.format("Place: %s\nStudents:\n%s", event.getPlace(), students);
        String stylename = event.getFormat().getId();

        generateEvent(
                caption,
                description,
                event.getStartDateTime(),
                event.getEndDateTime(),
                stylename,
                false
        );
    }

    protected void generateEvent(String caption, String description, LocalDateTime start, LocalDateTime end,
                                 String stylename, boolean isAllDay) {
        SimpleCalendarEvent<LocalDateTime> calendarEvent = new SimpleCalendarEvent<>();
        calendarEvent.setCaption(caption);
        calendarEvent.setDescription(description);
        calendarEvent.setStart(start);
        calendarEvent.setEnd(end);
        calendarEvent.setAllDay(isAllDay);
        calendarEvent.setStyleName(stylename);

        calendar.getEventProvider().addEvent(calendarEvent);
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Calendar Filter Handlers
    /////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Subscribe("calendar")
    public void onCalendarCalendarDayClick(io.jmix.ui.component.Calendar.CalendarDayClickEvent<LocalDateTime> event) {
        Screen eventEditor = screenBuilders.editor(Event.class, this)
                .newEntity()
                .withOpenMode(OpenMode.DIALOG)
                .build();

        eventEditor.addAfterCloseListener(afterCloseEvent -> {
            if (afterCloseEvent.closedWith(StandardOutcome.COMMIT)) {
                getScreenData().loadAll();
                generateEvents();
            }
        });

        eventEditor.show();
    }

    @Subscribe("calendar")
    public void onCalendarCalendarEventClick(io.jmix.ui.component.Calendar.CalendarEventClickEvent<LocalDateTime> event) {
        try {
            Format format = ((Event) event.getEntity()).getFormat();
            Screen eventEditor = screenBuilders.editor(Event.class, this)
                    .editEntity((Event) event.getEntity())
                    .withOpenMode(OpenMode.DIALOG)
                    .build();

            eventEditor.addAfterCloseListener(afterCloseEvent -> {
                if (afterCloseEvent.closedWith(StandardOutcome.COMMIT)) {
                    getScreenData().loadAll();
                    generateEvents();
                }
            });

            eventEditor.show();
        } catch (NullPointerException e) {
        }
    }

    @Subscribe("valuePicker")
    public void onValuePickerValueChange(HasValue.ValueChangeEvent event) {
        if (!valuePicker.isEmpty())
            generateEvents(event1 -> !Collections.disjoint(event1.getStudents(), valuePicker.getValue()));
    }

    @Subscribe("valuePicker.clear")
    public void onValuePickerClear(Action.ActionPerformedEvent event) {
        valuePicker.clear();
        generateEvents();
    }

}
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
import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
//        initTypeFilter();
        initSortCalendarEventsInMonthlyView();
        generateEvents();
    }

//    private void initTypeFilter() {
//        typeMultiFilter.setOptionsEnum(VisitType.class);
//        typeMultiFilter.setValue(EnumSet.allOf(VisitType.class));
//        typeMultiFilter.setOptionIconProvider(o -> VisitTypeIcon.valueOf(o.getIcon()).source());
//    }

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

        generateEvents();
//        loadEvents();
    }



//    private void loadEvents() {
//        visitsCalendarDl.setParameter("visitStart", calendar.getStartDate());
//        visitsCalendarDl.setParameter("visitEnd", calendar.getEndDate());
//        visitsCalendarDl.load();
//    }

//    @Subscribe("addEvent")
//    protected void onAddEventClick(Button.ClickEvent event) {
//        generateEvent(
//                descriptionField.getValue(),
//                startDateField.getValue(),
//                endDateField.getValue(),
//                isAllDay.getValue()
//        );
//    }

    protected void generateEvents() {
        List<Event> eventList = dataManager.load(Event.class).all().list();
        List<LocalDateTime> startPracticeDateList = new ArrayList<>();
        List<LocalDateTime> endPracticeDateList = new ArrayList<>();

        dataManager.load(Student.class)
                .all().list()
                .forEach(student -> {
                    LocalDateTime startDateTime = student.getStartPracticeDate().atTime(0, 0);
                    LocalDateTime endDateTime = student.getEndPracticeDate().atTime(0, 0);

                    mapPracticeDateTime(eventList, startPracticeDateList, startDateTime, student,
                            "Start practice");

                    mapPracticeDateTime(eventList, endPracticeDateList, endDateTime, student,
                            "End practice");

                });

        eventList.forEach(this::generateEvent);
     }

    private void mapPracticeDateTime(List<Event> eventList, List<LocalDateTime> dateTimeList,
                                     LocalDateTime dateTime, Student student, String description) {
        if (dateTimeList.contains(dateTime)) {
            Event event = eventList.stream().filter(event1 ->
                    event1.getStartDateTime().equals(dateTime)).findFirst().get();
            event.getStudents().add(student);
        } else {
            Event event = createEvent(description,
                    dateTime,
                    dateTime.plusHours(1),
                    "", Format.ONLINE, List.of(student));

            eventList.add(event);
            dateTimeList.add(dateTime);
        }
    }

    protected Event createEvent(String description,
                                LocalDateTime startDateTime,
                                LocalDateTime endDateTime,
                                String place,
                                Format format,
                                List<Student> students) {

        Event event = dataManager.create(Event.class);
        event.setFormat(format);
        event.setStartDateTime(startDateTime);
        event.setEndDateTime(endDateTime);
        event.setPlace(place);
        event.setDescription(description);
        event.setStudents(students);

        return event;
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
//                convertLocalDateTime(event.getStartDateTime()),
//                convertLocalDateTime(event.getEndDateTime()),
                stylename
        );
    }

    public Date convertLocalDateTime(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    protected void generateEvent(String caption, String description, LocalDateTime start, LocalDateTime end,
                                 String stylename) {
        SimpleCalendarEvent<LocalDateTime> calendarEvent = new SimpleCalendarEvent<>();
        calendarEvent.setCaption(caption);
        calendarEvent.setDescription(description);
        calendarEvent.setStart(start);
        calendarEvent.setEnd(end);
        calendarEvent.setStyleName(stylename);

        calendar.getEventProvider().addEvent(calendarEvent);
    }

    @Subscribe("calendar")
    public void onCalendarCalendarDayClick(io.jmix.ui.component.Calendar.CalendarDayClickEvent<LocalDateTime> event) {
        Screen eventEditor = screenBuilders.editor(Event.class, this)
                .newEntity()
//                .withInitializer(event1 -> {
//                    event1.setStartDateTime(event.getDate().plusMinutes(0));
//                    event1.setEndDateTime(event.getDate().plusHours(1));
//                })
                .withOpenMode(OpenMode.DIALOG)
                .build();

        eventEditor.addAfterCloseListener(afterCloseEvent -> {
            if (afterCloseEvent.closedWith(StandardOutcome.COMMIT)) {
                getScreenData().loadAll();
            }
        });

        eventEditor.show();
    }

    @Subscribe("calendar")
    public void onCalendarCalendarEventClick(io.jmix.ui.component.Calendar.CalendarEventClickEvent<LocalDateTime> event) {
        Screen visitEditor = screenBuilders.editor(Event.class, this)
                .editEntity((Event) event.getEntity())
                .withOpenMode(OpenMode.DIALOG)
                .build();

        visitEditor.addAfterCloseListener(afterCloseEvent -> {
            if (afterCloseEvent.closedWith(StandardOutcome.COMMIT)) {
                getScreenData().loadAll();

            }
        });

        visitEditor.show();
    }

    @Subscribe("valuePicker.select")
    public void onValuePickerSelect(Action.ActionPerformedEvent event) {

    }
}
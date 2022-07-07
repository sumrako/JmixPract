package com.company.jmixpract.screen.event;

import io.jmix.ui.screen.*;
import com.company.jmixpract.entity.Event;

@UiController("Event.edit")
@UiDescriptor("event-edit.xml")
@EditedEntityContainer("eventDc")
public class EventEdit extends StandardEditor<Event> {
}
package com.company.jmixpract.screen.event;

import io.jmix.ui.screen.*;
import com.company.jmixpract.entity.Event;

@UiController("Event.browse")
@UiDescriptor("event-browse.xml")
@LookupComponent("eventsTable")
public class EventBrowse extends StandardLookup<Event> {
}
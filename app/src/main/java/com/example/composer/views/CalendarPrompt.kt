package com.example.composer.views

import android.app.Activity
import android.content.ContentValues
import android.net.Uri


class CalendarPrompt {
    companion object {
        fun pushAppointmentsToCalender(
            curActivity: Activity,
            title: String?,
            addInfo: String?,
            place: String?,
            status: Int,
            startDate: Long,
            needReminder: Boolean,
            needMailService: Boolean
        ): Long? {
            /***************** Event: note(without alert)  */
            val eventUriString = "content://com.android.calendar/events"
            val eventValues = ContentValues()
            eventValues.put("calendar_id", 1)
            eventValues.put("title", title)
            eventValues.put("description", addInfo)
            eventValues.put("eventLocation", place)
            val endDate = startDate + 1000 * 60 * 60 // For next 1hr
            eventValues.put("dtstart", startDate)
            eventValues.put("dtend", endDate)
            eventValues.put("allDay", 0)
            eventValues.put("eventStatus", status)
            eventValues.put("eventTimezone", "UTC/GMT +2:00")

            /*eventValues.put("visibility", 3); // visibility to default (0),
                                            // confidential (1), private
                                            // (2), or public (3):
        eventValues.put("transparency", 0); // You can control whether
                                            // an event consumes time
                                            // opaque (0) or transparent
                                            // (1).
          */
            eventValues.put("hasAlarm", 1) // 0 for false, 1 for true
            val eventUri: Uri? = curActivity.applicationContext.contentResolver.insert(
                Uri.parse(eventUriString),
                eventValues
            )
            val eventID: Long? = eventUri?.lastPathSegment?.toLong()
            if (needReminder) {
                val reminderUriString = "content://com.android.calendar/reminders"
                val reminderValues = ContentValues()
                reminderValues.put("event_id", eventID)
                reminderValues.put("minutes", 5) // Default value of the
                // system. Minutes is a
                // integer
                reminderValues.put("method", 1) // Alert Methods: Default(0),
                // Alert(1), Email(2),
                // SMS(3)
                val reminderUri: Uri? = curActivity.applicationContext.contentResolver.insert(
                    Uri.parse(reminderUriString),
                    reminderValues
                )
            }
            /***************** Event: Meeting(without alert) Adding Attendies to the meeting  */
            if (needMailService) {
                val attendeuesesUriString = "content://com.android.calendar/attendees"

                /********
                 * To add multiple attendees need to insert ContentValues multiple
                 * times
                 */
                val attendeesValues = ContentValues()
                attendeesValues.put("event_id", eventID)
                attendeesValues.put("attendeeName", "xxxxx") // Attendees name
                attendeesValues.put("attendeeEmail", "yyyy@gmail.com") // Attendee
                // E
                // mail
                // id
                attendeesValues.put("attendeeRelationship", 0) // Relationship_Attendee(1),
                // Relationship_None(0),
                // Organizer(2),
                // Performer(3),
                // Speaker(4)
                attendeesValues.put("attendeeType", 0) // None(0), Optional(1),
                // Required(2), Resource(3)
                attendeesValues.put("attendeeStatus", 0) // NOne(0), Accepted(1),
                // Decline(2),
                // Invited(3),
                // Tentative(4)
                val attendeuesesUri: Uri? = curActivity.applicationContext.contentResolver.insert(
                    Uri.parse(attendeuesesUriString), attendeesValues
                )
            }
            return eventID
        }
    }
}
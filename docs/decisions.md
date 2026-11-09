# Project Decisions: Appointment Scheduling System

A record of business and design decisions. Each decision includes context, the choice made, and the reasoning. 

> Real client: a self-employed manicurist who works alone, from home.

---

## 1. Business Context

- Open Monday to Saturday, starting at 2:00 PM.
- Single professional, no other staff.
- Currently, appointments are arranged over WhatsApp and noted in the iPhone calendar.
- **Main pain points:** scheduling conflicts and rescheduling.
- Two booking flows: the manicurist enters appointments manually, and customers can also book on their own (to accommodate customers who are less comfortable with technology).
- Payment is made at the end of the service, via Pix or cash. No deposit, no discounts, fixed prices.

## 2. Services Offered

| Service | Default duration | Notes |
|---|---|---|
| Traditional manicure (hands) | ~1h | |
| Traditional pedicure (feet) | ~1h | |
| Hands + feet | ~2h| |
| Gel polish (hands) | ~2h | |
| Gel polish (feet) | ~2h | |
| Nail extensions | 4h | Can take 3h or 4h; default set to 4h |
| Extension maintenance | ~3h| |
| Gel polish maintenance | ~2h| |
| Foot spa | 2h or more | Callus treatment, ingrown nails, etc. |

---

## 3. Scheduling Rules

### D1. Working hours
- **Start:** 2:00 PM.
- **Limit:** the last appointment must **end** by midnight. The rule applies to the end time, not the start time.
- Consequence: a 4-hour nail extension can start at 8:00 PM at the latest.

### D2. Buffer between appointments
- **30-minute buffer** that **occupies the schedule** (it counts as blocked time).
- For the **last appointment of the day**, the buffer is not required: if it does not fit, the end of the day is treated as midnight.
- Back-to-back appointments (one ends at 4:00 PM, the next starts at 4:00 PM) are **not a conflict**. There is simply no buffer between them.

### D3. Two-level duration
- **Default duration** stored on the service.
- **Actual duration** stored on the appointment (initially copied from the default).
- Nail extensions: default of 4 hours.

### D4. Rescheduling
- No minimum notice and no fee.
- The rescheduling history **must be visible** (the manicurist wants to see that a reschedule happened).

### D5. No-show
- There is a dedicated status for "customer did not show up", separate from "canceled".

### D6. Availability
- **Default weekly rule:** Monday to Saturday, starting at 2:00 PM.
- **Date-specific exceptions:** the manicurist can block a normally open day (day off, holiday) or open a normally closed day (work on a holiday or Sunday).

### D7. Payment
- Payment is recorded at the end of the service: amount, method (Pix or cash), and date.
- No payment gateway integration in V1.
- Monthly revenue reporting is a requirement.
- **Price snapshot:** the price charged is stored at booking time, so later price changes do not alter historical records.

### D8. Customer
- Booking without an account: name and phone number only.
- Creating an account (login) is optional.
- **Modeling:** `Customer` (name, phone) is separate from `User` (credentials, V2), which points to the customer when it exists.

---

## 5. Pending Decisions

For each item, write the decision and the reasoning in your own words.

- [X] **P1. "Hands + feet":** its own service, or an appointment with multiple services?
  - Consider: fixed prices, no discount, 2-hour duration.
  - Pros/cons of each approach:
  - **Decision:** It will be a single service.
  - **Reasoning:** This is treated as a single service by the customer, so it will be the same in the system.

- [X] **P2. Mandatory or flexible buffer?**
  - When the manicurist books manually, can she make an exception and fit someone in without the buffer? 
  - **Decision:** Yes, it tends to have more clients on weekend days so she wants to be capable of booking clients in a row

- [X] **P3. How to record rescheduling?**
  - Option A: change history table (previous time, new time, change date).
  - Option B: the old appointment is marked as "rescheduled" and points to the new one.
  - Which one better answers "how many times has this customer rescheduled?" and which keeps the daily schedule simpler?
  - **Decision:** Option B: the old appointment is marked as "rescheduled" and points to the new one, as it is just to the customer to view a rescheduling, not necessarily keep tracking of it.

- [X] **P4. Actual appointment duration**
  - When is it defined? Can it be changed later?
  - If a change causes an overlap with the next appointment, what happens?
  - **Decision:** Each treatment has a default duration, copied to the appointment when it is created. The manicurist can edit the duration of an appointment before, during, or after it takes place (for example, when she cannot stop mid-service to record that it ran late).
  - **Overlap handling:** if the new duration overlaps the next appointment, all following appointments are pushed forward in a chain until no conflicts remain.
  - **Buffer when pushing:** the 30-minute buffer is not restored. Pushed appointments are only moved as far as needed to remove the overlap.
  - **Midnight exception:** an appointment may end after midnight, but never start after midnight. This is the only situation in which an appointment may end after midnight; the system warns the manicurist and allows it.
  - **Pushing past midnight:** when pushing the chain would make an appointment start at or after midnight, that appointment (and every later one that day) is canceled, with the reason "no room after delay" (distinct from "customer request" and "day closed"). The system lists the affected customers (name and phone) so the manicurist can contact them herself.
  - **Safeguard:** before applying the change, the system shows a preview of its effects (which appointments will be pushed and which will be canceled) and asks for confirmation. The whole operation is atomic: either all changes apply or none does.
  - **Shortening a duration:** if the manicurist reduces the duration (the service ended early), the following appointments stay where they are. The freed time becomes extra break time for her. No rescheduling is created.
  - **Affected customers:** each pushed appointment counts as a rescheduling (see P3): the old appointment is marked as "rescheduled" and points to the new one. The system lists the affected customers (name and phone) so the manicurist can contact them herself, since V1 has no notifications.
  - **Reasoning:** (write in your own words)

- [X] **P5. Precedence between weekly rule and date exception**
  - When calculating free slots for a day, which layer is checked first?
  - **Decision:** For any given date, the system checks, in this order:
    1. **Manual override:** if the manicurist explicitly opened or closed that date, her choice wins.
    2. **Holiday:** if there is no override and the date is a holiday (national, state, or municipal), the day is closed by default.
    3. **Weekly rule:** otherwise, the default weekly rule applies (Monday to Saturday open, Sunday closed).
  - **Opening hours of an open day:** always the standard 2:00 PM to midnight, including reopened holidays and extra working days. In V1, overrides are only "open" or "closed", with no custom hours.
  - **Closing a day that already has appointments:** all appointments on that date are marked as canceled, with the reason "day closed" so they can be told apart from cancellations requested by the customer. Reopening the day does not restore them.
  - **Safeguards:** before closing, the system shows how many appointments will be canceled and asks for confirmation. It also lists the affected customers (name and phone), since V1 has no notifications and the manicurist needs to contact them herself.
  - **Reasoning:** The most specific information wins over the general one. An explicit choice by the manicurist is more specific than a holiday, and a holiday is more specific than the weekly rule. This covers the normal day, the day off, the extra working day (for example a Sunday), and the reopened holiday.

- [X] **P6. Appointment states and transitions**
  - Candidate states: scheduled, completed, canceled, no-show, rescheduled.
  - Can a "no-show" go back to "scheduled"? What about "completed"? 
  - Draw the state transition diagram.
  - **Decision:** no-show is marked as "no-show"

- [X] **P7. Monthly revenue**
  - Is the month defined by the service date or the payment date?
  - Can a completed appointment exist without payment (pay later)? 
  - What happens to the payment if the appointment is marked as "no-show"?
  - **Decision:** The month is defined by the service date; no pay later is acceptable; in no-show cases there's no payment.

- [X] **P8. Phone number as customer identifier**
  - What if two people share the same number (mother and daughter)?
  - **Decision:** It will be treated as the same customer.

- [X] **P9. Time zones and date/time types**
  - `LocalDateTime`, `ZonedDateTime`, or `Instant`? How will the database store it?
  - **Decision:** ZonedDateTime, to store in the same datetime as the customer uses.

- [X] **P10. Naming of the service entity**
  - Naming a class `Service` can be confusing next to Spring's `@Service` annotation. Consider alternatives (e.g. `Treatment`, `Offering`) and record the choice.
  - **Decision:** Using "Treatment" considering the services as a beauty treatment.

- [X] **P11. Source of the holiday list**
  - Options considered: a holiday library (e.g. Jollyday), a public API, or a table in the database.
  - **Decision:** A holiday table in the database (date, name, scope: national, state, or municipal), populated with a Flyway migration.
  - **Reasoning:** It covers state (Rio Grande do Sul) and municipal holidays, which libraries may not cover. It has no external dependencies. It also lets the manicurist decide how to treat optional holidays (such as Carnival), which are not official holidays. The cost is yearly maintenance, since movable holidays depend on the date of Easter. This can be revisited later by switching to a library or automating the update.

---

## 6. Questions for the Client (Manicurist)

- [X] Can she fit a customer in without the 30-minute buffer? Yes
- [X] Is there "pay later" (service completed, paid afterwards)? No

---

## 7. Change Log

| Date | Change |
|---|---|
| _2026-11-09_ | Initial version |
# Lapwise

Personal swim log synced from Strava. One athlete, their swim activities, and an optional insight per activity.

## Language

**User**:
The person who connected Strava to Lapwise. Holds that athlete's Strava tokens and when Lapwise last pulled their activities.
_Avoid_: athlete, account, client, customer

**SwimActivity**:
One swim recorded on Strava and stored in Lapwise. Identified by Strava's activity id for that User.
_Avoid_: session, workout, training, lap (a lap is a Split inside the activity)

**Split**:
One lap/length pace sample Strava returned for a SwimActivity. Stored as raw JSON until an Insight is generated.
_Avoid_: lap, length, interval

**Insight**:
A short generated reading of pace degradation across a SwimActivity's Splits. At most one per SwimActivity. Absent when Strava sent no usable Splits.
_Avoid_: analysis, summary, comment, AI response

**Session token**:
A credential Lapwise issues to the iOS app so it can call this API. Not a Strava token.
_Avoid_: access token, refresh token (those two belong to Strava, on the User)

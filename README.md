# Clan Chat Warnings


*No longer supported, if you'd like to take ownership of the plugin send me a message in the RL dev channel so that they are aware and it can be trasnfered*

Notifies you when players (either by specific name or by regex) join clan chat. Supports adding notes to signal why you put them on the watchlist, primarily aimed at being used as a warning system against spam, can be used for telling you when X player joins and you need to get your collat back.


**Player Warnings**-Will only alert you if names matches one on the list, seperate with commas.

Example-

JimBob~\*Scammer\*, Jimbo, Greg~He knows what he did.

You will be notified:

Jimbob has joined Clan chat. \*Scammer\*

Jimbo has joined Clan chat.

Greg has joined Clan chat. He knows what he did.

You will not be notified if Meg then joins cc


**Regex Warnings**-Alerts you if someone matches the regex provided, seperate with new lines. Note having plain names here may result in false positives.

Example-

^Jim~Known raiding clan,eg$~He knows what he did.,Bob-Another Bob bro

^Jim catches anyone with Jim at the start of their name which can be useful for catching members of a clan. eg$ will catch anyone with eg at the end of their name which can be useful for catching alts.Bob will catch anyone with the name Bob in their name. In Jimbob's case it will prioritize ^Jim as its first in the list. 

You will be notified:

Jimbob has joined Clan chat. Known raiding clan

Jimbo has joined Clan chat. Known raiding clan

Greg has joined Clan chat. He knows what he did.

Meg has joined Clan chat. He knows what he did.


**Exempt Players**-Will not notify you if a player would otherwise trigger an alert, seperate with commas.

This one is self explanatory and is used to help alleviate some of the rules youd need to do with the regex to not be notified of known good partys, in this case you'd want to add Meg, who has no relation to Greg.

**Other Warning settings**

Alert On Warning will send a notification to grab your attention, Check on self join will run the check when you are joining a cc, it will not run this check if you are joining back from hopping however, Ping on self join will ping you if someone is caught from the Check on self join, Kick from warning changes message format to be less readable but allows to kick the player without finding them in clan list.

**Tracker Settings**

Tracker will enable the tracker. Tracker Keyword is what triggers the tracker to look for player names, if multiple names are found it will track each one seperately, similarly Stop Tracking Keyword will stop tracking any players, Tracker Length is how long until you wont be notified that they leave, and Tracker Ping is if the notification will also alert you. 

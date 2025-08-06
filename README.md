# SFEvents

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#download">Download</a></li>
    <li><a href="#about-the-addon">About The Project</a></li>
    <li><a href="#requirements">Requirements</a></li>
    <li><a href="#commands">Commands</a></li>
    <li><a href="#basic-instructions">Basic Instructions</a></li>
    <li><a href="#donate">Donate</a></li>
  </ol>
</details>

## Download

- <a href="https://github.com/VoperAD/SFEvents/releases/latest">Download from GitHub Releases</a> <b>(the JAR is available within the Assets section)</b>

## About the Addon

SFEvents is a Slimefun addon that adds special events to the game, which are related to Slimefun actions, such as
crafting items. Currently, the only event available is the **Crafting Event**, which is triggered when a player crafts
a Slimefun item. In the future, I plan to add more events, such as a Clan version of the Crafting Event, where players
can compete in crafting items for their clans. Feel free to suggest any other events you would like to see in this addon!

> I created this addon mainly to practice my Kotlin skills :)


## Requirements

- This addon works on **Spigot**/**Paper** servers as well as on their forks, such as **Purpur**.
- **Java Version:** 17+
- **Minecraft Version:** 1.16+
- **Slimefun Version:** RC-37+

## Commands

|                        Command                         |         Permission          |                               Description                                |
|:------------------------------------------------------:|:---------------------------:|:------------------------------------------------------------------------:|
|                           -                            |      sfevents.anyone.*      |                Permission node for all _anyone_ commands                 |
|                /sfevents event progress                | sfevents.anyone.seeprogress |             Show the player's progress in the current event              |
|                  /sfevents event top                   |  sfevents.anyone.eventtop   |                Show the top players in the current event                 |
|                           -                            |      sfevents.admin.*       |                 Permission node for all _admin_ commands                 |
|                    /sfevents reload                    |    sfevents.admin.reload    |           Reload the addon configuration and event data files            |
|          /sfevents event start \<eventName\>           |  sfevents.admin.startevent  |                         Start an event manually                          |
|                 /sfevents event cancel                 | sfevents.admin.cancelevent  |                Manually cancel the current event running                 |
| /sfevents event create \<eventType\> \<eventFileName\> | sfevents.admin.createevent  | Create a new event file with the given name for the specified event type |

## Basic Instructions

As said before, the addon currently has only one event — the Crafting Event.

All events are stored in a folder called `events`, which is located inside SFEvents folder. Using the command `/sfevents event create <eventType> <eventFileName>`,
you can create a new event file with the specified name and type. Then, you can edit the file to customize the event.

Also, it is possible to schedule events to run at specific times using the `scheduler` field in the **config.yml** file.

## Donate

If you want to support the project and make someone's day, you can donate on <a href="https://ko-fi.com/voper">Ko-Fi</a> 🙂
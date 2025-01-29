_Disclaimer: Ethical IP Tracking_

_This server mod logs player IP addresses to help identify alt accounts and maintain server integrity. Please use this mod responsibly and in compliance with privacy guidelines._


# AltX
AltX is a lightweight serverside mod for minecraft fabric that:

    Logs player IP addresses.
    Detects alternate accounts or shared IPs.

Features

    IP Logging: Automatically logs player IPs.

Installation

    Download the .jar from the Releases.
    Drop it into your server's mods folder.
    Restart the server.

Commands/Permissions
| Permission | Command            | Description                                                 |
|------------|--------------------|-------------------------------------------------------------|
| `altx.command` | `/altx` | Shows a list of available Altx commands |
| `altx.list` | `/altx list`       | Shows a list of players using the same IP address |
| `altx.trace` | `/altx trace <player>`| Shows all players on given players IP address |
| `altx.viewips` | - | Allows the player to see & search for IP addresses in `altx list` & `altx trace <ip>` |
| `altx.notify` | - | Notifies players when a new player joins using an IP address that is already in the database |

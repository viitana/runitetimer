# Runitetimer

A simple [Runelite](https://github.com/runelite) plugin that adds capabilities to track the 12 minute respawn time on runite rocks on any world and mine. Allows users to perfectly time world hops to the rock respawns for a massive advantage over other miners.

![Screenshot](/img/screenshot_trahaearn.png?raw=true "Example use case at the Trahaearn mine in Prifddinas")

### Main features
- Automatic rock spawn tracking for each world as you mine
- Dynamic up-to-date world list generation on startup (skipping unwanted worlds like PVP, Deadman etc.)
- Multiple mine support via dropdown
- World hopping directly by clicking on a given world on the plugin sidebar
- Rock tile highlight overlay for both the main game window and minimap

### Usage
Any ready-to-use externally loadable plugin libs are not provided or planned. If you wish to use this, build the client from source:
- Clone the [official Runelite project](https://github.com/runelite) or a fork of your choosing
- Add in the files in [/runelite-client](/runelite-client)
- Build & run [*(further instructions)*](https://github.com/runelite/runelite/wiki/Building-with-IntelliJ-IDEA)

*Note: The base of the plugin consists of some very old code that has simply been recently revived to working condition with pretty minimal effort. If you wish to work on this don't expect it to be very maintainable or well-written.*

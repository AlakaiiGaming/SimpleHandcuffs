# Simple Handcuffs

Handcuffs for Purpur/Paper 1.21 and up (built for 26.3).

Hold the cuffs and right-click a player to arrest them. They follow you and can't use their inventory or commands. Right-click them again to let them go.

## Install

Download the jar from [Releases](https://github.com/AlakaiiGaming/SimpleHandcuffs/releases), drop it in `plugins/` and restart.

## Commands

| Command | Permission |
| --- | --- |
| `/cuffs give [player] [amount]` | `handcuffs.create` |
| `/cuffs removeall` | `handcuffs.removeall` |

`/cuff`, `/sh` and `/handcuffs` work too. `handcuffs.immune` stops a player from being cuffed.

## Build

`mvn package` (Java 21). The jar ends up in `target/`.

# Require OpenAudioMc
A simple plugin blocking a configurable set of events when the player isn't connected to the client.

Can be used to enforce/require voicechat or other audio related features.

Default config:
```yaml
# The following events will be canceled if the player isn't connected to voice chat, and the player
# will receive in game messages
message-when-canceled: '&4You need to have the audio client open in order to do this (/audio)'
require-voice-chat: true
events:
  - 'org.bukkit.event.block.BlockBreakEvent'
  - 'org.bukkit.event.block.BlockPlaceEvent'
  - 'org.bukkit.event.entity.EntityCombustEvent'
  - 'org.bukkit.event.entity.EntityDamageByEntityEvent'
  - 'org.bukkit.event.entity.EntityDamageEvent'
  - 'org.bukkit.event.player.PlayerCommandPreprocessEvent'
  - 'org.bukkit.event.player.PlayerDropItemEvent'
  - 'org.bukkit.event.player.PlayerInteractEvent'
  - 'org.bukkit.event.entity.ProjectileLaunchEvent'
  - 'org.bukkit.event.player.PlayerMoveEvent'
```
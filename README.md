# Serverside Avatar renderer

Renders player skins as avatar image.

Based on https://github.com/JNNGL/vanilla-shaders/tree/main/gui_avatars

## Text Placeholder:
`%avatar-renderer:avatar <player-name> <offset> <flipped>%`

Offset moves the avatar down by N amount.

Flipped causes the image to be flipped

## Example:

`%avatar-renderer:avatar Pinnit%`

`%avatar-renderer:avatar Pinnit 0%`

`%avatar-renderer:avatar Pinnit 0 flipped%`

![skin](./example.png)
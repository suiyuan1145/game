# Character directory format

Each playable character has one directory:

```text
characters/
  index.txt
  character-id/
    character.properties
```

`index.txt` controls character order. A character is loaded only when its
directory is listed there.

Required `character.properties` fields:

```properties
name=Display Name
avatar=path/to/avatar.png
color=#38bdf8
accent=#facc15
speed=1.0
power=1.0
defense=1.0
imageHeight=240
idle=frame-a.png;frame-b.png
punch=frame-a.png;frame-b.png
kick=frame-a.png;frame-b.png
guard=frame.png
hit=frame.png
```

Paths are relative to `resources/assets`. Sprite-sheet frames may use:

```text
sheet|sheet-path.png|x|y|width|height|chroma-key
```

This mirrors MUGEN's one-directory-per-character organization while keeping
the files directly loadable from a packaged JavaFX application.

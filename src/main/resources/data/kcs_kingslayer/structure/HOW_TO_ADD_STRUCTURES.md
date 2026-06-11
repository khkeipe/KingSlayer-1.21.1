# Custom structure templates (Route A)

Drop saved `.nbt` templates **in this folder** (note: singular `structure`, not `structures`).
Minecraft ignores non-`.nbt` files here, so this readme is harmless.

## Workflow
1. In a creative world: `/give @s minecraft:structure_block`
2. Build the structure (≤ 48×48×48). Use `/give @s minecraft:structure_void` for empty cells
   so it doesn't stamp a solid box of air.
3. Place a Structure Block → **SAVE** mode → name it `kcs_kingslayer:knight_tent` →
   size the box → **Save**.
4. Copy the generated file from
   `saves/<world>/generated/kcs_kingslayer/structures/knight_tent.nbt`
   to here: `data/kcs_kingslayer/structure/knight_tent.nbt`
5. It generates automatically — the `knight_tent` slot is already wired into worldgen
   (TemplateFeature → KNIGHT_TENT_KEY → KNIGHT_TENT_PLACED_KEY → ADD_DECOR).

## Adding more templates
- Register a new configured feature in `ModConfiguredFeatures` using
  `ModFeatures.TEMPLATE.get()` + a `TemplateConfiguration(<id>, <integrity>)`.
- Add a placed feature in `ModPlacedFeatures` (`onSurface(rarity)`).
- Add its placed key to the `ADD_DECOR` list in `ModBiomeModifiers`.

`integrity` < 1.0 randomly omits that fraction of blocks for a ruined/weathered look.

## Later: "proper" structures (Route B)
For `/locate`, guaranteed spacing, terrain-matching, or multi-room builds, graduate to a
jigsaw structure: `worldgen/template_pool/`, `worldgen/structure/`, `worldgen/structure_set/`.

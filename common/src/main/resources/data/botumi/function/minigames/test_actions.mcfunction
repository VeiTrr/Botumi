execute as @a[sort=nearest,limit=1] run give @s minecraft:diamond 3
execute as @a[sort=nearest,limit=1] run give @s minecraft:netherite_pickaxe[minecraft:custom_name='{"text":"Незеритовая кирка","color":"#33ccff"}',minecraft:unbreakable={},minecraft:enchantments={levels:{"minecraft:fortune":3,"minecraft:efficiency":5}}] 1
execute as @a[sort=nearest,limit=1] run loot give @s loot minecraft:chests/abandoned_mineshaft
fill ~-1 ~-1 ~-1 ~1 ~-1 ~1 minecraft:stone replace
fill ~-4 ~-4 ~-4 ~4 ~4 ~4 minecraft:chest[facing=south,waterlogged=true]{LootTable:"minecraft:chests/abandoned_mineshaft"} replace minecraft:stone
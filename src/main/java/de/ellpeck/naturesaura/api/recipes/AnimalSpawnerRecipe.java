package de.ellpeck.naturesaura.api.recipes;

import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

public class AnimalSpawnerRecipe {

    public final ResourceLocation name;
    public final Ingredient[] ingredients;
    public final ResourceLocation entity;
    public final int aura;
    public final int time;

    public AnimalSpawnerRecipe(ResourceLocation name, ResourceLocation entity, int aura, int time, Ingredient... ingredients) {
        this.name = name;
        this.ingredients = ingredients;
        this.entity = entity;
        this.aura = aura;
        this.time = time;
    }

    public Entity makeEntity(World world, double x, double y, double z) {
        EntityType entry = ForgeRegistries.ENTITIES.getValue(this.entity);
        if (entry == null)
            return null;
        Entity entity = entry.create(world);
        if (x == 0 && y == 0 && z == 0)
            return entity;
        entity.setLocationAndAngles(x, y, z, MathHelper.wrapDegrees(world.rand.nextFloat() * 360F), 0F);
        if (entity instanceof MobEntity) {
            MobEntity living = (MobEntity) entity;
            living.rotationYawHead = entity.rotationYaw;
            living.renderYawOffset = entity.rotationYaw;
            living.onInitialSpawn(world, world.getDifficultyForLocation(living.getPosition()), SpawnReason.SPAWNER, null, null); // TODO test if null is okay here
        }
        return entity;
    }

    public AnimalSpawnerRecipe register() {
        NaturesAuraAPI.ANIMAL_SPAWNER_RECIPES.put(this.name, this);
        return this;
    }
}

package de.ellpeck.naturesaura.entities;

import de.ellpeck.naturesaura.NaturesAura;
import net.minecraft.entity.EntityType;
import net.minecraftforge.registries.ObjectHolder;

@SuppressWarnings("FieldNamingConvention")
@ObjectHolder(NaturesAura.MOD_ID)
public final class ModEntities {
    public static EntityType<EntityMoverMinecart> MOVER_MINECART;
    public static EntityType<EntityEffectInhibitor> EFFECT_INHIBITOR;
}

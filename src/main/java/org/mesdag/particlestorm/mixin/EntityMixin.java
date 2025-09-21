package org.mesdag.particlestorm.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.mixed.IEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Hashtable;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntity {
    @Unique
    private VariableTable particlestorm$variableTable;

    @Override
    public VariableTable particlestorm$getVariableTable() {
        if (particlestorm$variableTable == null) {
            Hashtable<String, Variable> table = new Hashtable<>();
            table.put("variable.entity_scale", new Variable("variable.entity_scale", p -> {
                if (p.getAttachedEntity() instanceof LivingEntity living) {
                    return living.getAttributeValue(Attributes.SCALE);
                }
                return 1.0;
            }));
            this.particlestorm$variableTable = new VariableTable(table, null);
        }
        return particlestorm$variableTable;
    }
}

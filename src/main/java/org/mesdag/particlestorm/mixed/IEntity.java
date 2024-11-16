package org.mesdag.particlestorm.mixed;

import net.minecraft.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;

public interface IEntity {
    VariableTable particlestorm$getVariableTable();

    VariableTable INITIAL_TABLE = Util.make(new VariableTable(null), table -> {
        table.table.put("variable.entity_scale", new Variable("variable.entity_scale", p -> {
            if (p.getEmitter().attached instanceof LivingEntity living) {
                return living.getAttributeValue(Attributes.SCALE);
            }
            return 1.0;
        }));
    });
}

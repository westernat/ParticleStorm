package org.mesdag.particlestorm.data.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;

public final class NodeMolangExp extends MolangExp implements IEventNode {
    private final boolean log;

    public NodeMolangExp(String expStr, boolean log) {
        super(expStr);
        this.log = log;
    }

    public boolean shouldLog() {
        return log;
    }

    @Override
    public void execute(MolangInstance instance) {
        if (variable == null && !expStr.isEmpty() && !expStr.isBlank()) {
            MolangParser parser = new MolangParser(instance.getVars());
            this.variable = parser.compileMolang(expStr);
        }
        if (variable != null) {
            double v = variable.get(instance);
            if (log) {
                ParticleStorm.LOGGER.info("{}[{}]: {}={}", instance.getIdentity(), instance.getPosition(), expStr, v);
            }
        }
    }

    public static NodeMolangExp fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        String exp = GsonHelper.getAsString(object, "exp");
        boolean log = GsonHelper.getAsBoolean(object, "log", false);
        return new NodeMolangExp(exp, log);
    }
}

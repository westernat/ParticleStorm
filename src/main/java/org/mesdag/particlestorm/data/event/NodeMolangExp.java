package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;

public final class NodeMolangExp extends MolangExp implements IEventNode {
    public static final Codec<NodeMolangExp> CODEC = Codec.STRING.xmap(NodeMolangExp::new, NodeMolangExp::getExpStr);

    public NodeMolangExp(String expStr) {
        super(expStr);
    }

    @Override
    public void execute(MolangInstance instance) {
        if (variable == null && !expStr.isEmpty() && !expStr.isBlank()) {
            MolangParser parser = new MolangParser(instance.getVariableTable());
            this.variable = parser.compileMolang(expStr);
        }
        if (variable != null) {
            variable.get(instance);
        }
    }
}

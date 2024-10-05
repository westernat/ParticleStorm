package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathParser;

public class NodeMolangExp extends MolangExp implements IEventNode {
    public static final Codec<NodeMolangExp> CODEC = Codec.STRING.xmap(NodeMolangExp::new, NodeMolangExp::getExpStr);

    public NodeMolangExp(String expStr) {
        super(expStr);
    }

    @Override
    public void execute(MolangInstance instance) {
        if (variable == null && !expStr.isEmpty() && !expStr.isBlank()) {
            MathParser parser = new MathParser(instance.getVariableTable().table);
            this.variable = parser.compileMolang(expStr);
        }
        if (variable != null) {
            variable.get(instance);
        }
    }
}

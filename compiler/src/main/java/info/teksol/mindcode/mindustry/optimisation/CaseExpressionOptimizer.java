package info.teksol.mindcode.mindustry.optimisation;

import info.teksol.mindcode.ast.AstNodeBuilder;
import info.teksol.mindcode.mindustry.instructions.LogicInstruction;
import info.teksol.mindcode.mindustry.LogicInstructionPipeline;
import info.teksol.mindcode.mindustry.instructions.InstructionProcessor;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// Removes unncessary case expression variable (__ast*) and replaces it with the original variable containing
// the value of the case expresion. The set instruction is removed, while the other instructions (one jump 
// instruction per "when" branch) are updated to replace the ast variable with the one used in the set statement.
// The optimization is performed only when the following conditions are met:
// 1. The set instruction assigns to an __ast variable.
// 2. The set instruction is the first of all those using the __ast variable (the check is based on absolute
//    instruction sequence in the program, not on the actual program flow).
// 3. Each subsequent instruction using the __ast variable conforms to the code generated by the compiler
//    (ie. has the form of jump target equal/lessThan/lessThanEq __astX testValue)
//
// Variables in push and pop instructions are ignored. If a variable is eliminated, the push/pop instructions
// are eliminated too.
class CaseExpressionOptimizer extends GlobalOptimizer {
    public CaseExpressionOptimizer(InstructionProcessor instructionProcessor, LogicInstructionPipeline next) {
        super(instructionProcessor, next);
    }

    @Override
    protected boolean optimizeProgram() {
        for (Iterator<LogicInstruction> it = program.iterator(); it.hasNext(); ) {
            LogicInstruction instruction = it.next();
            if (!instruction.isSet()) continue;
            
            String arg0 = instruction.getArgs().get(0);
            // Not an __ast variable
            if (!arg0.startsWith(AstNodeBuilder.AST_PREFIX)) continue;

            String arg1 = instruction.getArgs().get(1);
            List<LogicInstruction> list = findInstructions(ix -> ix.getArgs().contains(arg0) && !ix.isPushOrPop());
            // The set instruciton is not the first one
            if (list.get(0) != instruction) continue;
            
            // Some of the other instructions aren't part of the case expression
            if (!list.stream().skip(1).allMatch(ix -> isStandardCaseWhenInstruction(ix, arg0))) continue;
            
            // Replace __ast with actual value in all case branches
            list.stream().skip(1).forEach(ix -> replaceInstruction(ix, replaceAllArgs(ix, arg0, arg1)));
            it.remove();
        }

        return false;
    }

    private static final Set<String> EXPECTED_OPERATORS = Set.of("equal", "lessThan", "lessThanEq");

    private boolean isStandardCaseWhenInstruction(LogicInstruction ix, String ast) {
        return ix.isJump() 
                && EXPECTED_OPERATORS.contains(ix.getArgs().get(1))
                && ix.getArgs().get(2).equals(ast);
    }
}

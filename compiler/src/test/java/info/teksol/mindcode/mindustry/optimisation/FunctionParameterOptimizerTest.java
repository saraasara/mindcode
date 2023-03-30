package info.teksol.mindcode.mindustry.optimisation;

import info.teksol.mindcode.ast.Seq;
import info.teksol.mindcode.mindustry.AbstractGeneratorTest;
import info.teksol.mindcode.mindustry.LogicInstructionPipeline;
import java.util.List;
import org.junit.jupiter.api.Test;

import static info.teksol.mindcode.mindustry.logic.Opcode.*;

public class FunctionParameterOptimizerTest extends AbstractGeneratorTest {

    private final LogicInstructionPipeline pipeline = OptimisationPipeline.createPipelineOf(getInstructionProcessor(),
            terminus,
            Optimisation.DEAD_CODE_ELIMINATION,
            Optimisation.FUNCTION_PARAM_OPTIMIZATION);

    @Test
    public void eliminatesSimpleParameters() {
        generateInto(
                pipeline,
                (Seq) translateToAst(""
                        + "inline def bar(n) print(n) end "
                        + "bar(5)"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(SET, var(0), "5"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(PRINT, var(0)),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    public void eliminatesNestedParameters() {
        generateInto(
                pipeline,
                (Seq) translateToAst(""
                        + "inline def foo(n) print(n) end "
                        + "inline def bar(n) foo(n) end "
                        + "bar(5)"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(SET, var(0), "5"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(LABEL, var(1002)),
                        createInstruction(PRINT, var(0)),
                        createInstruction(LABEL, var(1003)),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    public void eliminatesGlobalVariables() {
        generateInto(
                pipeline,
                (Seq) translateToAst(""
                        + "inline def bar(n) print(n) end "
                        + "X = 5 "
                        + "bar(X)"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(SET, var(0), "5"),
                        createInstruction(SET, "X", var(0)),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(PRINT, "X"),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    public void eliminatesBlockNames() {
        generateInto(
                pipeline,
                (Seq) translateToAst(""
                        + "inline def bar(n) print(n) end "
                        + "bar(switch1)"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(LABEL, var(1000)),
                        createInstruction(PRINT, "switch1"),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    public void eliminatesChainedVariables() {
        generateInto(
                pipeline,
                (Seq) translateToAst(""
                        + "inline def bar(n) a = n print(a) end "
                        + "bar(5)"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(SET, var(0), "5"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(PRINT, var(0)),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    public void eliminatesVariablesInExpressions() {
        generateInto(
                pipeline,
                (Seq) translateToAst(""
                        + "inline def bar(n) print(n + 1) end "
                        + "bar(5)"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(SET, var(0), "5"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(SET, var(2), "1"),
                        createInstruction(OP, "add", var(3), var(0), var(2)),
                        createInstruction(PRINT, var(3)),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    public void preservesVolatileVariables() {
        generateInto(
                pipeline,
                (Seq) translateToAst(""
                        + "inline def bar(n) print(n) end "
                        + "bar(@time)"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(LABEL, var(1000)),
                        createInstruction(SET, "__fn0_n", "@time"),
                        createInstruction(PRINT, "__fn0_n"),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    public void preservesModifiedVariables() {
        generateInto(
                pipeline,
                (Seq) translateToAst(""
                        + "inline def bar(n) n += 1 print(n) end "
                        + "bar(0)"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(SET, var(0), "0"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(SET, "__fn0_n", var(0)),
                        createInstruction(SET, var(2), "1"),
                        createInstruction(OP, "add", var(3), "__fn0_n", var(2)),
                        createInstruction(SET, "__fn0_n", var(3)),
                        createInstruction(PRINT, "__fn0_n"),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    public void preservesGlobalVariablesWithFunctionCalls() {
        generateInto(
                pipeline,
                (Seq) translateToAst(""
                        + "inline def bar(n) foo(n) print(n) end "
                        + "def foo(n) print(n) end "
                        + "X = 5 "
                        + "foo(X)"
                        + "bar(X)"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(SET, var(0), "5"),
                        createInstruction(SET, "X", var(0)),
                        createInstruction(SET, "__fn0_n", "X"),
                        createInstruction(SET, "__fn0retaddr", var(1001)),
                        createInstruction(SET, "@counter", var(1000)),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(LABEL, var(1002)),
                        createInstruction(SET, "__fn1_n", "X"),
                        createInstruction(SET, "__fn0_n", "__fn1_n"),
                        createInstruction(SET, "__fn0retaddr", var(1004)),
                        createInstruction(SET, "@counter", var(1000)),
                        createInstruction(LABEL, var(1004)),
                        createInstruction(PRINT, "__fn1_n"),
                        createInstruction(LABEL, var(1003)),
                        createInstruction(END),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(PRINT, "__fn0_n"),
                        createInstruction(LABEL, var(1005)),
                        createInstruction(SET, "@counter", "__fn0retaddr"),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void passesParameterToFunctionRegressionTest() {
        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(LABEL, var(1000)),
                        createInstruction(SET, var(0), "2"),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(OP, "lessThan", var(1), "1", var(0)),
                        createInstruction(PRINT, var(1)),
                        createInstruction(PRINTFLUSH, "message1"),
                        createInstruction(END)
                ),
                generateAndOptimize(
                        (Seq) translateToAst(""
                                + "inline def d(n) n end "
                                + "print(1 < d(2)) "
                                + "printflush(message1)"
                        )
                )
        );
    }
}

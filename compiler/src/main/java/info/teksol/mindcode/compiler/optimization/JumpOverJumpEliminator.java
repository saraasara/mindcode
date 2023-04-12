package info.teksol.mindcode.compiler.optimization;

import info.teksol.mindcode.compiler.LogicInstructionPipeline;
import info.teksol.mindcode.compiler.instructions.InstructionProcessor;
import info.teksol.mindcode.compiler.instructions.JumpInstruction;
import info.teksol.mindcode.compiler.instructions.LogicInstruction;
import info.teksol.mindcode.logic.Opcode;

import java.util.ArrayList;
import java.util.List;

/**
 * This optimizer detects situations where a conditional jump skips a following, unconditional one and replaces it
 * with a single conditional jump with a reversed condition and the target of the second jump. Example:
 * <pre>{@code
 * jump __label0 equal __tmp9 false
 * jump __label1
 * label __label0
 * }</pre>
 * will be turned to
 * <pre>{@code
 * jump __label1 notEqual __tmp9 false
 * }</pre>
 * Optimization won't be done if the condition doesn't have an inverse (i.e. {@code ===}).
 * <p>
 * These sequences of instructions may arise when using break or continue statements:
 * <pre>{@code
 * while true
 *     ...
 *     if not_alive
 *         break
 *     end
 * end
 * }</pre>
 */
public class JumpOverJumpEliminator extends PipelinedOptimizer {

    public JumpOverJumpEliminator(InstructionProcessor instructionProcessor, LogicInstructionPipeline next) {
        super(instructionProcessor, next);
    }

    @Override
    protected State initialState() {
        return new EmptyState();
    }

    private final class EmptyState implements State {
        @Override
        public State emit(LogicInstruction instruction) {
            if (instruction.isJump() && hasInverse(instruction.asJump().getCondition())) {
                // "always" doesn't have an inverse
                return new ExpectJump(instruction.asJump());
            } else {
                emitToNext(instruction);
                return this;
            }
        }

        @Override
        public State flush() {
            return this;
        }
    }

    private final class ExpectJump implements State {
        private final JumpInstruction conditionalJump;

        public ExpectJump(JumpInstruction conditionalJump) {
            this.conditionalJump = conditionalJump;
        }

        @Override
        public State emit(LogicInstruction instruction) {
            if (instruction.isJump() && instruction.asJump().isUnconditional()) {
                return new ExpectLabel(conditionalJump, instruction.asJump());
            } else {
                emitToNext(conditionalJump);
                return new EmptyState().emit(instruction);
            }
        }

        @Override
        public State flush() {
            emitToNext(conditionalJump);
            return new EmptyState();
        }
    }

    private final class ExpectLabel implements State {
        private final String targetLabel;
        private final JumpInstruction conditionalJump;
        private final JumpInstruction unconditionalJump;
        private final List<LogicInstruction> labels = new ArrayList<>();
        private boolean isJumpOverJump = false;

        public ExpectLabel(JumpInstruction conditionalJump, JumpInstruction unconditionalJump) {
            this.conditionalJump = conditionalJump;
            this.unconditionalJump = unconditionalJump;
            this.targetLabel = conditionalJump.getTarget();
        }

        @Override
        public State emit(LogicInstruction instruction) {
            if (instruction.isLabel()) {
                if (instruction.asLabel().getLabel().equals(targetLabel)) {
                    isJumpOverJump = true;
                }
                labels.add(instruction);
                return this;
            }

            if (isJumpOverJump) {
                emitToNext(
                        createInstruction(
                                Opcode.JUMP,
                                unconditionalJump.getTarget(),
                                getInverse(conditionalJump.getCondition()),
                                conditionalJump.getFirstOperand(),
                                conditionalJump.getSecondOperand()
                        )
                );
            } else {
                emitToNext(conditionalJump);
                emitToNext(unconditionalJump);
            }

            labels.forEach(JumpOverJumpEliminator.this::emitToNext);
            return new EmptyState().emit(instruction);
        }

        @Override
        public State flush() {
            emitToNext(conditionalJump);
            emitToNext(unconditionalJump);
            labels.forEach(JumpOverJumpEliminator.this::emitToNext);
            return new EmptyState();
        }
    }
}

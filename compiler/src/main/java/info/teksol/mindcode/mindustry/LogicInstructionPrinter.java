package info.teksol.mindcode.mindustry;

import info.teksol.mindcode.mindustry.instructions.InstructionProcessor;
import info.teksol.mindcode.mindustry.instructions.LogicInstruction;
import java.util.List;

public class LogicInstructionPrinter {
    public static String toString(InstructionProcessor instructionProcessor, List<LogicInstruction> instructions) {
        final StringBuilder buffer = new StringBuilder();
        instructions.forEach((instruction) -> {
            buffer.append(instruction.getOpcode().getOpcode());
            addArgs(instructionProcessor.getPrintArgumentCount(instruction), buffer, instruction);
        });

        return buffer.toString();
    }

    private static void addArgs(int count, StringBuilder buffer, LogicInstruction instruction) {
        for (int i = 0; i < count; i++) {
            buffer.append(" ");
            if (instruction.getArgs().size() > i) {
                buffer.append(instruction.getArg(i));
            } else {
                buffer.append("0");
            }
        }

        buffer.append("\n");
    }
}

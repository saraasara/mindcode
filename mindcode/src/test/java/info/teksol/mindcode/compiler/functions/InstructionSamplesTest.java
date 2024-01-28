package info.teksol.mindcode.compiler.functions;

import info.teksol.mindcode.compiler.LogicInstructionPrinter;
import info.teksol.mindcode.compiler.generator.AstContext;
import info.teksol.mindcode.compiler.instructions.InstructionProcessor;
import info.teksol.mindcode.compiler.instructions.InstructionProcessorFactory;
import info.teksol.mindcode.compiler.instructions.LogicInstruction;
import info.teksol.mindcode.logic.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static info.teksol.mindcode.logic.ProcessorEdition.W;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test produces files containing permutations of instructions and their arguments allowed by metadata.
 * The produced files (Instruction_Samples_*.txt) should then be pasted into a Mindustry processor editor
 * (World processor when available, otherwise a standard processor) and then copied back into a different text file.
 * There should be no differences between the file generated by this code and the file obtained from Mindustry
 * Processor editor. Any difference means the metadata in this project are flawed.
 */
public class InstructionSamplesTest {

    private static final AstContext STATIC_AST_CONTEXT = AstContext.createRootNode();

    @Test
    void createInstructionSamplesForV6() throws IOException {
        createInstructionSamples(ProcessorVersion.V6);
    }

    @Test
    void createInstructionSamplesForV7() throws IOException {
        createInstructionSamples(ProcessorVersion.V7);
    }

    @Test
    void createInstructionSamplesForV7A() throws IOException {
        createInstructionSamples(ProcessorVersion.V7A);
    }

    private void createInstructionSamples(ProcessorVersion version) throws IOException {
        assertTrue(new File(".." + File.separatorChar + "README.markdown").isFile());
        InstructionProcessor processor = InstructionProcessorFactory.getInstructionProcessor(version, W);
        List<LogicInstruction> instructions = processor.getOpcodeVariants().stream()
                .filter(v -> !v.opcode().isVirtual())
                .flatMap(v -> createOpcodeSamples(processor, v).stream())
                .collect(Collectors.toList());

        File file = new File(".." + File.separatorChar + "Instruction_Samples_" + version + ".txt");
        try (final PrintWriter w = new PrintWriter(file, StandardCharsets.UTF_8)) {
            w.print(LogicInstructionPrinter.toString(processor, instructions));
        }
    }

    private List<LogicInstruction> createOpcodeSamples(InstructionProcessor processor, OpcodeVariant opcodeVariant) {
        if (opcodeVariant.opcode() == Opcode.LABEL) {
            return List.of();
        }

        ProcessorVersion processorVersion = processor.getProcessorVersion();
        List<LogicInstruction> result = new ArrayList<>();
        List<List<String>> combinations = new ArrayList<>();

        for (NamedParameter arg : opcodeVariant.namedParameters()) {
            if (arg.type() == LogicParameter.LABEL) {
                combinations.add(List.of("0"));
            } else if (arg.type().isKeyword()) {
                combinations.add(arg.type().getAllowedValues().stream()
                        .filter(v -> v.versions.contains(processorVersion))
                        .flatMap(v -> v.values.stream())
                        .sorted()
                        .toList());
            } else {
                combinations.add(List.of(arg.name()));
            }
        }
        int variants = combinations.stream()
                .mapToInt(List::size)
                .max().orElse(1);

        for (int i = 0; i < variants; i++) {
            final int index = i;
            List<LogicArgument> params = combinations.stream()
                    .map(l -> l.get(index % l.size()))
                    .map(BaseArgument::new)
                    .collect(Collectors.toList());

            result.add(processor.createInstruction(STATIC_AST_CONTEXT, opcodeVariant.opcode(), params));
        }

        return result;
    }
}

package info.teksol.mindcode.mindustry;

import info.teksol.mindcode.mindustry.optimisation.ConditionalJumpsNormalizer;
import info.teksol.mindcode.mindustry.optimisation.InaccesibleCodeEliminator;
import info.teksol.mindcode.mindustry.optimisation.InputTempEliminator;
import info.teksol.mindcode.mindustry.optimisation.PropagateJumpTargets;
import java.util.EnumSet;
import java.util.function.Function;

// The optimizations are applied in the declared order, ie. ConditionalJumpsNormalizer gets instructions from the 
// compiler, makes optimizations and passes them onto the next optimizer.
public enum Optimisation {
    CONDITIONAL_JUMPS_NORMALIZATION     (next -> new ConditionalJumpsNormalizer(next)),
    DEAD_CODE_ELIMINATION               (next -> new DeadCodeEliminator(next)),
    SINGLE_STEP_JUMP_ELIMINATION        (next -> new SingleStepJumpEliminator(next)),
    SENSOR_THEN_SET_OPTIMIZATION        (next -> new OptimizeSensorThenSet(next)),
    OP_THEN_SET_OPTIMIZATION            (next -> new OptimizeOpThenSet(next)),
    READ_THEN_SET_OPTIMIZATION          (next -> new OptimizeReadThenSet(next)),
    GETLINK_THEN_SET_OPTIMIZATION       (next -> new OptimizeGetlinkThenSet(next)),
    INPUT_TEMPS_ELIMINATION             (next -> new InputTempEliminator(next)),
    CONDITIONAL_JUMPS_IMPROVEMENT       (next -> new ImproveConditionalJumps(next)),
    JUMP_TARGET_PROPAGATION             (next -> new PropagateJumpTargets(next)),
    INACCESSIBLE_CODE_ELIMINATION       (next -> new InaccesibleCodeEliminator(next)),
    
    // This needs to be run twice to eliminate additional orphaned jumps generated by above optimizer
    SINGLE_STEP_JUMP_ELIMINATION_2      (next -> new SingleStepJumpEliminator(next)),
    ;
    
    private final Function<LogicInstructionPipeline, LogicInstructionPipeline> instanceCreator;

    private Optimisation(Function<LogicInstructionPipeline, LogicInstructionPipeline> instanceCreator) {
        this.instanceCreator = instanceCreator;
    }
    
    private LogicInstructionPipeline createInstance(LogicInstructionPipeline next) {
        return instanceCreator.apply(next);
    }
    
    public static LogicInstructionPipeline createCompletePipeline(LogicInstructionPipeline terminus) {
        LogicInstructionPipeline pipeline = terminus;
        Optimisation[] values = values();
        for (int i = values.length - 1; i >= 0; i--) {
            pipeline = values[i].createInstance(pipeline);
            
        }
        return pipeline;
    }
    
    public static LogicInstructionPipeline createPipelineOf(LogicInstructionPipeline terminus, Optimisation... optimisation) {
        EnumSet<Optimisation> includes = EnumSet.of(optimisation[0], optimisation);
        LogicInstructionPipeline pipeline = terminus;
        Optimisation[] values = values();
        for (int i = values.length - 1; i >= 0; i--) {
            if (includes.contains(values[i])) {
                pipeline = values[i].createInstance(pipeline);
            }
        }
        return pipeline;
    }
}
